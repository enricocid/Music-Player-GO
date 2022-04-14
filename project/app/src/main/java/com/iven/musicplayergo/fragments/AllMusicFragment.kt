package com.iven.musicplayergo.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentAllMusicBinding
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.UIControlInterface
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import com.reddit.indicatorfastscroll.FastScrollerView

/**
 * A simple [Fragment] subclass.
 * Use the [AllMusicFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AllMusicFragment : Fragment(), SearchView.OnQueryTextListener {

    private var _allMusicFragmentBinding: FragmentAllMusicBinding? = null
    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mMediaControlInterface: MediaControlInterface

    // view model
    private lateinit var mMusicViewModel: MusicViewModel

    // sorting
    private lateinit var mSortMenuItem: MenuItem
    private var mSorting = goPreferences.allMusicSorting

    private var sIsFastScroller = false

    private var mAllMusic: List<Music>? = null

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
                deviceMusic.observe(viewLifecycleOwner) { returnedMusic ->
                    if (!returnedMusic.isNullOrEmpty()) {
                        mAllMusic =
                            ListsHelper.getSortedMusicListForAllMusic(
                                mSorting,
                                mMusicViewModel.deviceMusicFiltered
                            )
                        finishSetup()
                    }
                }
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setMusicDataSource(musicList: List<Music>?) {
        musicList?.run {
            mAllMusic = this
            _allMusicFragmentBinding?.allMusicRv?.adapter?.notifyDataSetChanged()
        }
    }

    private fun finishSetup() {

        setupIndicatorFastScrollerView()

        _allMusicFragmentBinding?.run {

            allMusicRv.adapter = AllMusicAdapter()

            shuffleFab.text = mAllMusic?.size.toString()
            val fabColor = ColorUtils.blendARGB(
                ThemeHelper.resolveColorAttr(requireContext(), R.attr.toolbar_bg),
                ThemeHelper.resolveThemeAccent(requireContext()),
                0.10f
            )
            shuffleFab.backgroundTintList = ColorStateList.valueOf(fabColor)
            shuffleFab.setOnClickListener {
                mMediaControlInterface.onSongsShuffled(
                    mAllMusic,
                    GoConstants.ARTIST_VIEW
                )
            }

            searchToolbar.let { stb ->

                stb.inflateMenu(R.menu.menu_music_search)
                stb.overflowIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_sort)
                stb.setNavigationOnClickListener {
                    mUIControlInterface.onCloseActivity()
                }

                with(stb.menu) {

                    mSortMenuItem = ListsHelper.getSelectedSortingForAllMusic(mSorting, this).apply {
                        setTitleColor(ThemeHelper.resolveThemeAccent(requireContext()))
                    }

                    with (findItem(R.id.action_search).actionView as SearchView) {
                        setOnQueryTextListener(this@AllMusicFragment)
                        setOnQueryTextFocusChangeListener { _, hasFocus ->
                            fastscroller.handleViewVisibility(show = !hasFocus)
                            fastscrollerThumb.handleViewVisibility(show = !hasFocus)
                            allMusicRv.setupFastScrollerPadding(forceNoPadding = hasFocus, resources)
                            stb.menu.setGroupVisible(R.id.sorting, !hasFocus)
                            stb.menu.findItem(R.id.sleeptimer).isVisible = !hasFocus
                        }
                    }

                    setMenuOnItemClickListener(stb.menu)
                }
            }
        }
    }

    private fun setupIndicatorFastScrollerView() {

        // Set indexes if artists rv is scrollable
        _allMusicFragmentBinding?.run {

            allMusicRv.afterMeasured {

                sIsFastScroller = computeVerticalScrollRange() > height

                if (sIsFastScroller) {

                    fastscroller.setupWithRecyclerView(this,
                        { position ->
                            // Return a text tab_indicator
                            val song = mAllMusic?.get(position)
                            val stringToProcess = song.toName()
                            stringToProcess?.getFastScrollerItem(requireContext())
                        }, showIndicator = { _, indicatorPosition, totalIndicators ->
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

                    fastscrollerThumb.setupWithFastScroller(fastscroller)
                    fastscroller.useDefaultScroller = false
                    fastscroller.itemIndicatorSelectedCallbacks += object :
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
                allMusicRv.setupFastScrollerPadding(forceNoPadding = false, resources)
            }
        }
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
                mAllMusic = ListsHelper.getSortedMusicListForAllMusic(mSorting, mAllMusic)

                setMusicDataSource(mAllMusic)

                mSortMenuItem.setTitleColor(
                    ThemeHelper.resolveColorAttr(
                        requireContext(),
                        android.R.attr.textColorPrimary
                    )
                )

                mSortMenuItem = ListsHelper.getSelectedSortingForAllMusic(mSorting, menu).apply {
                    setTitleColor(ThemeHelper.resolveThemeAccent(requireContext()))
                }

                goPreferences.allMusicSorting = mSorting

            } else if (it.itemId != R.id.action_search) {
                mUIControlInterface.onOpenSleepTimerDialog()
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
        setMusicDataSource(
            ListsHelper.processQueryForMusic(newText,
                ListsHelper.getSortedMusicListForAllMusic(
                    mSorting,
                    mMusicViewModel.deviceMusicFiltered
                )
            ) ?: mAllMusic)
        return false
    }

    override fun onQueryTextSubmit(query: String?) = false

    private inner class AllMusicAdapter : RecyclerView.Adapter<AllMusicAdapter.SongsHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SongsHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.music_item,
                parent,
                false
            )
        )

        override fun getItemCount(): Int {
            return mAllMusic?.size!!
        }

        override fun onBindViewHolder(holder: SongsHolder, position: Int) {
            holder.bindItems(mAllMusic?.get(holder.absoluteAdapterPosition))
        }

        inner class SongsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bindItems(itemSong: Music?) {

                itemView.run {

                    val title = itemView.findViewById<TextView>(R.id.title)
                    val duration = itemView.findViewById<TextView>(R.id.duration)
                    val subtitle = itemView.findViewById<TextView>(R.id.subtitle)

                    val formattedDuration = itemSong?.duration?.toFormattedDuration(
                        isAlbum = false,
                        isSeekBar = false
                    )

                    duration.text = getString(R.string.duration_date_added, formattedDuration,
                        itemSong?.dateAdded?.toFormattedDate())
                    title.text = itemSong.toName()
                    subtitle.text =
                        getString(R.string.artist_and_album, itemSong?.artist, itemSong?.album)

                    setOnClickListener {
                        mMediaControlInterface.onSongSelected(
                            itemSong,
                            mAllMusic,
                            GoConstants.ARTIST_VIEW
                        )
                    }

                    setOnLongClickListener {
                        val vh = _allMusicFragmentBinding?.allMusicRv?.findViewHolderForAdapterPosition(absoluteAdapterPosition)
                        DialogHelper.showPopupForSongs(
                            requireActivity(),
                            vh?.itemView,
                            itemSong,
                            GoConstants.ARTIST_VIEW
                        )
                        return@setOnLongClickListener true
                    }
                }
            }
        }
    }

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
