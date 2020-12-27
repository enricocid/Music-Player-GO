package com.iven.musicplayergo.fragments

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
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

    private lateinit var mAllMusicFragmentBinding: FragmentAllMusicBinding

    // View model
    private lateinit var mMusicViewModel: MusicViewModel

    private var mAllMusic: MutableList<Music>? = null
    private val mDataSource = emptyDataSource()

    private lateinit var mSortMenuItem: MenuItem
    private var mSorting = goPreferences.allMusicSorting

    private var sIsFastScroller = false
    private val sIsFastScrollerVisible get() = sIsFastScroller && mSorting != GoConstants.DEFAULT_SORTING

    private lateinit var mUIControlInterface: UIControlInterface

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAllMusicFragmentBinding = FragmentAllMusicBinding.bind(view)

        mMusicViewModel =
            ViewModelProvider(requireActivity()).get(MusicViewModel::class.java).apply {
                deviceMusic.observe(viewLifecycleOwner, { returnedMusic ->
                    if (!returnedMusic.isNullOrEmpty()) {
                        mAllMusic =
                            ListsHelper.getSortedMusicList(
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
        mAllMusicFragmentBinding.allMusicRv.run {

            // setup{} is an extension method on RecyclerView
            setup {

                // item is a `val` in `this` here
                withDataSource(mDataSource)

                withItem<Music, SongsViewHolder>(R.layout.music_item) {
                    onBind(::SongsViewHolder) { _, item ->
                        // GenericViewHolder is `this` here
                        duration.text = item.duration.toFormattedDuration(
                            isAlbum = false,
                            isSeekBar = false
                        )
                        title.text = if (goPreferences.songsVisualization != GoConstants.TITLE) {
                            item.displayName.toFilenameWithoutExtension()
                        } else {
                            item.title
                        }
                        subtitle.text =
                            getString(R.string.artist_and_album, item.artist, item.album)
                    }

                    onClick {
                        mUIControlInterface.onSongSelected(
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
                            findViewHolderForAdapterPosition(index)?.itemView,
                            item,
                            GoConstants.ARTIST_VIEW,
                            mUIControlInterface
                        )
                    }
                }
            }
        }

        setupIndicatorFastScrollerView()

        mAllMusicFragmentBinding.shuffleFab.text = mAllMusic?.size.toString()

        mAllMusicFragmentBinding.shuffleFab.setOnClickListener {
            mUIControlInterface.onShuffleSongs(
                    null,
                    null,
                    mAllMusic,
                    mAllMusic?.size!! <= 1000,
                    GoConstants.ARTIST_VIEW
            )
        }

        mAllMusicFragmentBinding.searchToolbar.run {

            inflateMenu(R.menu.menu_search)

            overflowIcon = requireActivity().getDrawable(R.drawable.ic_sort)

            setNavigationOnClickListener {
                mUIControlInterface.onCloseActivity()
            }

            menu.run {

                mSortMenuItem = ListsHelper.getSelectedSorting(mSorting, this).apply {
                    setTitleColor(ThemeHelper.resolveThemeAccent(requireActivity()))
                }

                val searchView = findItem(R.id.action_search).actionView as SearchView
                searchView.run {
                    setOnQueryTextListener(this@AllMusicFragment)
                    setOnQueryTextFocusChangeListener { _, hasFocus ->
                        if (sIsFastScrollerVisible) {
                            mAllMusicFragmentBinding.fastscroller.handleViewVisibility(!hasFocus)
                            mAllMusicFragmentBinding.fastscrollerThumb.handleViewVisibility(
                                !hasFocus
                            )
                            setupMusicRecyclerViewPadding(hasFocus)
                        }
                        menu.setGroupVisible(R.id.sorting, !hasFocus)
                    }
                }

                setMenuOnItemClickListener(this)
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
        mAllMusicFragmentBinding.allMusicRv.afterMeasured {

            sIsFastScroller = computeVerticalScrollRange() > height

            if (sIsFastScroller) {

                mAllMusicFragmentBinding.fastscroller.setupWithRecyclerView(
                    this,
                    { position ->
                        // Return a text tab_indicator
                        mAllMusic?.get(position)?.title?.run {
                            getFastScrollerItem(requireActivity())
                        }
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
                    }
                )

                mAllMusicFragmentBinding.fastscrollerThumb.setupWithFastScroller(
                    mAllMusicFragmentBinding.fastscroller
                )

                mAllMusicFragmentBinding.fastscroller.useDefaultScroller = false
                mAllMusicFragmentBinding.fastscroller.itemIndicatorSelectedCallbacks += object :
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

    private fun setupMusicRecyclerViewPadding(forceNoPadding: Boolean) {
        val rvPaddingEnd =
            if (sIsFastScrollerVisible && !forceNoPadding) {
                resources.getDimensionPixelSize(R.dimen.fast_scroller_view_dim)
            } else {
                0
            }
        mAllMusicFragmentBinding.allMusicRv.setPadding(0, 0, rvPaddingEnd, 0)
    }

    private fun handleIndicatorFastScrollerViewVisibility() {
        mAllMusicFragmentBinding.fastscroller.handleViewVisibility(sIsFastScrollerVisible)
        mAllMusicFragmentBinding.fastscrollerThumb.handleViewVisibility(sIsFastScrollerVisible)
    }

    private fun setMenuOnItemClickListener(menu: Menu) {

        mAllMusicFragmentBinding.searchToolbar.setOnMenuItemClickListener {

            if (it.itemId == R.id.default_sorting || it.itemId == R.id.ascending_sorting || it.itemId == R.id.descending_sorting) {

                mSorting = it.order

                mAllMusic =
                    ListsHelper.getSortedMusicList(mSorting, mMusicViewModel.deviceMusicFiltered)

                handleIndicatorFastScrollerViewVisibility()

                setupMusicRecyclerViewPadding(false)

                setMusicDataSource(mAllMusic)

                mSortMenuItem.setTitleColor(
                    ThemeHelper.resolveColorAttr(
                        requireActivity(),
                        android.R.attr.textColorPrimary
                    )
                )

                mSortMenuItem = ListsHelper.getSelectedSorting(mSorting, menu).apply {
                    setTitleColor(ThemeHelper.resolveThemeAccent(requireActivity()))
                }

                goPreferences.allMusicSorting = mSorting
            }

            return@setOnMenuItemClickListener true
        }
    }

    fun onSongVisualizationChanged() = if (::mAllMusicFragmentBinding.isInitialized) {
        mAllMusicFragmentBinding.allMusicRv.adapter?.notifyDataSetChanged()
        true
    } else {
        false
    }

    fun onListFiltered(newMusic: MutableList<Music>?) {
        mAllMusic = newMusic
        setMusicDataSource(mAllMusic)
        mAllMusicFragmentBinding.shuffleFab.text = newMusic?.size.toString()
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
