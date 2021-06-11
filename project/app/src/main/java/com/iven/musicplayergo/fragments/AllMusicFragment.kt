package com.iven.musicplayergo.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.recyclical.datasource.emptyDataSource
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentAllMusicBinding
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.MusicOrgHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.SongsViewHolder
import com.iven.musicplayergo.ui.UIControlInterface
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import com.reddit.indicatorfastscroll.FastScrollerView

/**
 * A simple [Fragment] subclass.
 * Use the [AllMusicFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AllMusicFragment : Fragment(R.layout.fragment_all_music), SearchView.OnQueryTextListener {

    private var _allMusicFragmentBinding: FragmentAllMusicBinding? = null

    // View model
    private lateinit var mMusicViewModel: MusicViewModel

    private var mAllMusic: List<Music>? = null
    private val mDataSource = emptyDataSource()

    private lateinit var mSortMenuItem: MenuItem
    private var mSorting = goPreferences.allMusicSorting

    private var sIsFastScroller = false
    private val sIsFastScrollerVisible get() = sIsFastScroller && mSorting != GoConstants.DEFAULT_SORTING && mSorting != GoConstants.DATE_ADDED_SORTING && mSorting != GoConstants.DATE_ADDED_SORTING_INV && mSorting != GoConstants.ARTIST_SORTING && mSorting != GoConstants.ARTIST_SORTING_INV && mSorting != GoConstants.ALBUM_SORTING && mSorting != GoConstants.ALBUM_SORTING_INV

    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mMediaControlInterface: MediaControlInterface

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
            mMediaControlInterface = activity as MediaControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _allMusicFragmentBinding = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _allMusicFragmentBinding = FragmentAllMusicBinding.inflate(inflater, container, false)
        return _allMusicFragmentBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMusicViewModel =
            ViewModelProvider(requireActivity()).get(MusicViewModel::class.java).apply {
                deviceMusic.observe(viewLifecycleOwner, { returnedMusic ->
                    if (!returnedMusic.isNullOrEmpty()) {
                        mAllMusic =
                            ListsHelper.getSortedMusicListForAllMusic(
                                mSorting,
                                mMusicViewModel.deviceMusicFiltered
                            )

                        setMusicDataSource(mAllMusic)

                        finishSetup()
                    }
                })
            }
    }

    private fun finishSetup() {
        _allMusicFragmentBinding?.allMusicRv?.let { rv ->

            // setup{} is an extension method on RecyclerView
            rv.setup {

                // item is a `val` in `this` here
                withDataSource(mDataSource)

                withItem<Music, SongsViewHolder>(R.layout.music_item) {
                    onBind(::SongsViewHolder) { _, item ->
                        // GenericViewHolder is `this` here

                        val formattedDuration = item.duration.toFormattedDuration(
                            isAlbum = false,
                            isSeekBar = false
                        )

                        duration.text = getString(R.string.duration_date_added, formattedDuration, item.dateAdded.toFormattedDate())

                        title.text = if (goPreferences.songsVisualization != GoConstants.TITLE) {
                            item.displayName.toFilenameWithoutExtension()
                        } else {
                            item.title
                        }
                        subtitle.text =
                            getString(R.string.artist_and_album, item.artist, item.album)
                    }

                    onClick {
                        mMediaControlInterface.onSongSelected(
                            item,
                            MusicOrgHelper.getAlbumSongs(
                                item.artist,
                                item.album,
                                mMusicViewModel.deviceAlbumsByArtist
                            ),
                            GoConstants.ARTIST_VIEW
                        )
                    }

                    onLongClick { index ->
                        DialogHelper.showPopupForSongs(
                            requireActivity(),
                            rv.findViewHolderForAdapterPosition(index)?.itemView,
                            item,
                            GoConstants.ARTIST_VIEW
                        )
                    }
                }
            }
        }

        setupIndicatorFastScrollerView()

        _allMusicFragmentBinding?.let { _binding ->
            _binding.shuffleFab.text = mAllMusic?.size.toString()

            _binding.shuffleFab.setOnClickListener {
                mMediaControlInterface.onShuffleSongs(
                    null,
                    null,
                    mAllMusic,
                    mAllMusic?.size!! <= 1000,
                    GoConstants.ARTIST_VIEW
                )
            }

            _binding.searchToolbar.let { stb ->

                stb.inflateMenu(R.menu.menu_music_search)

                stb.overflowIcon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_sort)

                stb.setNavigationOnClickListener {
                    mUIControlInterface.onCloseActivity()
                }

                with(stb.menu) {

                    mSortMenuItem = ListsHelper.getSelectedSortingForAllMusic(mSorting, this).apply {
                        setTitleColor(ThemeHelper.resolveThemeAccent(requireActivity()))
                    }

                    with (findItem(R.id.action_search).actionView as SearchView) {
                        setOnQueryTextListener(this@AllMusicFragment)
                        setOnQueryTextFocusChangeListener { _, hasFocus ->
                            if (sIsFastScrollerVisible) {
                                _binding.fastscroller.handleViewVisibility(!hasFocus)
                                _binding.fastscrollerThumb.handleViewVisibility(!hasFocus)
                                setupMusicRecyclerViewPadding(hasFocus)
                            }
                            stb.menu.setGroupVisible(R.id.sorting, !hasFocus)
                        }
                    }

                    setMenuOnItemClickListener(stb.menu)
                }
            }
        }
    }

    private fun setMusicDataSource(musicList: List<Music>?) {
        musicList?.let { songs ->
            mDataSource.set(songs)
        }
    }

    private fun setupIndicatorFastScrollerView() {

        // Set indexes if artists rv is scrollable
        _allMusicFragmentBinding?.let { _binding ->

            _binding.allMusicRv.afterMeasured {

                sIsFastScroller = computeVerticalScrollRange() > height

                if (sIsFastScroller) {

                    _binding.fastscroller.setupWithRecyclerView(
                            this,
                            { position ->
                                // Return a text tab_indicator
                                val song = mAllMusic?.get(position)
                                val stringToProcess = if (goPreferences.songsVisualization != GoConstants.TITLE) {
                                    song?.displayName
                                } else {
                                    song?.title
                                }
                                stringToProcess?.getFastScrollerItem(requireActivity())
                            }, showIndicator = { _, indicatorPosition, totalIndicators ->
                        // Hide every other indicator
                        if (ThemeHelper.isDeviceLand(resources)) {
                            indicatorPosition % 2 == 0
                        } else {
                            if (totalIndicators >= 30) {
                                indicatorPosition % 2 == 0
                            } else {
                                true
                            }
                        }
                    })

                    _binding.fastscrollerThumb.setupWithFastScroller(
                        _binding.fastscroller
                    )

                    _binding.fastscroller.useDefaultScroller = false
                    _binding.fastscroller.itemIndicatorSelectedCallbacks += object :
                        FastScrollerView.ItemIndicatorSelectedCallback {
                        override fun onItemIndicatorSelected(
                            indicator: FastScrollItemIndicator,
                            indicatorCenterY: Int,
                            itemPosition: Int
                        ) {
                            val artistsLayoutManager = layoutManager as LinearLayoutManager
                            artistsLayoutManager.scrollToPositionWithOffset(itemPosition, 0)
                        }
                    }
                }

                handleIndicatorFastScrollerViewVisibility()

                setupMusicRecyclerViewPadding(false)
            }
        }
    }

    private fun setupMusicRecyclerViewPadding(forceNoPadding: Boolean) {
        val rvPaddingEnd =
            if (sIsFastScrollerVisible && !forceNoPadding) {
                resources.getDimensionPixelSize(R.dimen.fast_scroller_view_dim)
            } else {
                0
            }
        _allMusicFragmentBinding?.allMusicRv?.setPadding(0, 0, rvPaddingEnd, 0)
    }

    private fun handleIndicatorFastScrollerViewVisibility() {
        _allMusicFragmentBinding?.fastscroller?.handleViewVisibility(sIsFastScrollerVisible)
        _allMusicFragmentBinding?.fastscrollerThumb?.handleViewVisibility(sIsFastScrollerVisible)
    }

    private fun setMenuOnItemClickListener(menu: Menu) {

        _allMusicFragmentBinding?.searchToolbar?.setOnMenuItemClickListener {

            if (it.itemId == R.id.default_sorting
                || it.itemId == R.id.ascending_sorting
                || it.itemId == R.id.descending_sorting
                || it.itemId == R.id.date_added_sorting
                || it.itemId == R.id.date_added_sorting_inv
                || it.itemId == R.id.artist_sorting
                || it.itemId == R.id.artist_sorting_inv
                || it.itemId == R.id.album_sorting
                || it.itemId == R.id.album_sorting_inv) {

                mSorting = it.order

                mAllMusic =
                    ListsHelper.getSortedMusicListForAllMusic(mSorting, mAllMusic)

                handleIndicatorFastScrollerViewVisibility()

                setupMusicRecyclerViewPadding(false)

                setMusicDataSource(mAllMusic)

                mSortMenuItem.setTitleColor(
                    ThemeHelper.resolveColorAttr(
                        requireActivity(),
                        android.R.attr.textColorPrimary
                    )
                )

                mSortMenuItem = ListsHelper.getSelectedSortingForAllMusic(mSorting, menu).apply {
                    setTitleColor(ThemeHelper.resolveThemeAccent(requireActivity()))
                }

                goPreferences.allMusicSorting = mSorting
            }

            return@setOnMenuItemClickListener true
        }
    }

    fun onSongVisualizationChanged() = if (_allMusicFragmentBinding != null) {
        mAllMusic = ListsHelper.getSortedMusicListForAllMusic(mSorting, mAllMusic)
        setMusicDataSource(mAllMusic)
        true
    } else {
        false
    }

    fun onListFiltered(newMusic: MutableList<Music>?) {
        mAllMusic = newMusic
        setMusicDataSource(mAllMusic)
        _allMusicFragmentBinding?.shuffleFab?.text = newMusic?.size.toString()
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        setMusicDataSource(ListsHelper.processQueryForMusic(newText, mAllMusic) ?: mAllMusic)
        return false
    }

    override fun onQueryTextSubmit(query: String?) = false

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment AllMusicFragment.
         */
        @JvmStatic
        fun newInstance() = AllMusicFragment()
    }
}
