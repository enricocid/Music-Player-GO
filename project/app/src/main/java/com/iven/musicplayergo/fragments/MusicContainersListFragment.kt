package com.iven.musicplayergo.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.afollestad.recyclical.datasource.emptyDataSource
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentMusicContainerListBinding
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.ui.ContainersAlbumViewHolder
import com.iven.musicplayergo.ui.GenericViewHolder
import com.iven.musicplayergo.ui.UIControlInterface
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import com.reddit.indicatorfastscroll.FastScrollerView
import kotlinx.coroutines.*

/**
 * A simple [Fragment] subclass.
 * Use the [MusicContainersListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MusicContainersListFragment : Fragment(R.layout.fragment_music_container_list),
    SearchView.OnQueryTextListener {

    private var _musicContainerListBinding: FragmentMusicContainerListBinding? = null

    // View model
    private lateinit var mMusicViewModel: MusicViewModel

    private var mLaunchedBy = GoConstants.ARTIST_VIEW

    private var mList: MutableList<String>? = null

    private val mDataSource = emptyDataSource()

    private lateinit var mUIControlInterface: UIControlInterface

    private lateinit var mSortMenuItem: MenuItem
    private var mSorting = GoConstants.DESCENDING_SORTING

    private val sLaunchedByAlbumView get() = mLaunchedBy == GoConstants.ALBUM_VIEW

    private var sIsFastScroller = false
    private val sIsFastScrollerVisible get() = sIsFastScroller && mSorting != GoConstants.DEFAULT_SORTING

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_LAUNCHED_BY)?.let { launchedBy ->
            mLaunchedBy = launchedBy
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _musicContainerListBinding = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _musicContainerListBinding = FragmentMusicContainerListBinding.inflate(inflater, container, false)
        return _musicContainerListBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMusicViewModel =
            ViewModelProvider(requireActivity()).get(MusicViewModel::class.java).apply {
                deviceMusic.observe(viewLifecycleOwner, { returnedMusic ->
                    if (!returnedMusic.isNullOrEmpty()) {
                        mSorting = getSortingMethodFromPrefs()
                        mList = getSortedItemKeys()
                        setListDataSource(mList)
                        finishSetup()
                    }
                })
            }
    }

    private fun finishSetup() {
        _musicContainerListBinding?.artistsFoldersRv?.let { rv ->

            // setup{} is an extension method on RecyclerView
            rv.setup {

                // item is a `val` in `this` here
                withDataSource(mDataSource)

                if (sLaunchedByAlbumView) {
                    withItem<String, ContainersAlbumViewHolder>(R.layout.containers_album_item) {
                        onBind(::ContainersAlbumViewHolder) { _, album ->
                            // ContainersAlbumViewHolder is `this` here
                            if (goPreferences.isCovers) {
                                val uri = mMusicViewModel.deviceMusicByAlbum?.get(album)?.get(0)?.albumId?.toAlbumArtURI()
                                albumCover.load(uri)
                            } else {
                                albumCover.setImageResource(R.drawable.album_art)
                            }

                            title.text = album
                            subtitle.text = getItemsSubtitle(album)
                        }

                        onClick {
                            respondToTouch(isLongClick = false, item, null)
                        }

                        onLongClick { index ->
                            respondToTouch(isLongClick = true, item, rv.findViewHolderForAdapterPosition(index)?.itemView)
                        }
                    }
                } else {
                    withItem<String, GenericViewHolder>(R.layout.generic_item) {
                        onBind(::GenericViewHolder) { _, artistOrFolder ->
                            // GenericViewHolder is `this` here
                            title.text = artistOrFolder
                            subtitle.text = getItemsSubtitle(artistOrFolder)
                        }

                        onClick {
                            respondToTouch(isLongClick = false, item, null)
                        }

                        onLongClick { index ->
                            respondToTouch(true, item, rv.findViewHolderForAdapterPosition(index)?.itemView)
                        }
                    }
                }
            }
        }

        setupIndicatorFastScrollerView()

        _musicContainerListBinding?.searchToolbar?.let { stb ->

            stb.inflateMenu(R.menu.menu_search)

            stb.overflowIcon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_sort)

            stb.title = getFragmentTitle()

            stb.setNavigationOnClickListener {
                mUIControlInterface.onCloseActivity()
            }

            with (stb.menu) {

                mSortMenuItem = ListsHelper.getSelectedSorting(mSorting, this).apply {
                    setTitleColor(ThemeHelper.resolveThemeAccent(requireActivity()))
                }

                with(findItem(R.id.action_search).actionView as SearchView) {
                    setOnQueryTextListener(this@MusicContainersListFragment)
                    setOnQueryTextFocusChangeListener { _, hasFocus ->
                        if (sIsFastScrollerVisible) {
                            _musicContainerListBinding?.fastscroller?.handleViewVisibility(!hasFocus)
                            _musicContainerListBinding?.fastscrollerThumb?.handleViewVisibility(
                                !hasFocus
                            )
                            setupArtistsRecyclerViewPadding(hasFocus)
                        }
                        stb.menu.setGroupVisible(R.id.sorting, !hasFocus)
                    }
                }
                setMenuOnItemClickListener(this)
            }
        }
    }

    private fun respondToTouch(isLongClick: Boolean, item: String, itemView: View?) {
        if (isLongClick) {
            if (::mUIControlInterface.isInitialized) {
                DialogHelper.showPopupForHide(
                    requireActivity(),
                    itemView,
                    item
                )
            }
        } else {
            if (::mUIControlInterface.isInitialized) {
                mUIControlInterface.onArtistOrFolderSelected(
                    item,
                    mLaunchedBy
                )
            }
        }
    }

    private fun getItemsSubtitle(item: String): String? {
        return when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                getArtistSubtitle(item)
            GoConstants.FOLDER_VIEW ->
                getString(
                    R.string.folder_info,
                    mMusicViewModel.deviceMusicByFolder?.getValue(item)?.size
                )
            else ->
                mMusicViewModel.deviceMusicByAlbum?.get(item)?.first()?.artist

        }
    }

    private fun getSortedItemKeys(): MutableList<String>? {
        return when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                ListsHelper.getSortedList(
                    mSorting,
                    mMusicViewModel.deviceAlbumsByArtist?.keys?.toMutableList()
                )
            GoConstants.FOLDER_VIEW ->
                ListsHelper.getSortedList(
                    mSorting,
                    mMusicViewModel.deviceMusicByFolder?.keys?.toMutableList()
                )

            else ->
                ListsHelper.getSortedListWithNull(
                    mSorting,
                    mMusicViewModel.deviceMusicByAlbum?.keys?.toMutableList()
                )
        }
    }

    private fun getSortingMethodFromPrefs(): Int {
        return when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                goPreferences.artistsSorting
            GoConstants.FOLDER_VIEW ->
                goPreferences.foldersSorting
            else ->
                goPreferences.albumsSorting
        }
    }

    private fun getFragmentTitle(): String {
        val stringId = when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                R.string.artists
            GoConstants.FOLDER_VIEW ->
                R.string.folders
            else ->
                R.string.albums
        }
        return getString(stringId)
    }

    private fun setListDataSource(selectedList: List<String>?) {
        if (!selectedList.isNullOrEmpty()) {
            mDataSource.set(selectedList)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun onUpdateCoverOption() {
        _musicContainerListBinding?.artistsFoldersRv?.adapter?.notifyDataSetChanged()
    }

    fun onListFiltered(stringToFilter: String) = if (mList == null) {
        false
    } else {
        mList?.remove(stringToFilter)
        setListDataSource(mList)
        true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        setListDataSource(ListsHelper.processQueryForStringsLists(newText, mList) ?: mList)
        return false
    }

    override fun onQueryTextSubmit(query: String?) = false

    private fun getArtistSubtitle(item: String) = getString(
        R.string.artist_info,
        mMusicViewModel.deviceAlbumsByArtist?.getValue(item)?.size,
        mMusicViewModel.deviceSongsByArtist?.getValue(item)?.size
    )

    private fun setupIndicatorFastScrollerView() {

        _musicContainerListBinding?.let { _binding ->

            // Set indexes if artists rv is scrollable
            _binding.artistsFoldersRv.afterMeasured {

                sIsFastScroller = computeVerticalScrollRange() > height

                if (sIsFastScroller) {

                    _binding.fastscroller.setupWithRecyclerView(
                            this,
                            { position ->
                                // Return a text tab_indicator
                                mList?.get(position)?.run {
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
                    })

                    _binding.fastscrollerThumb.setupWithFastScroller(_binding.fastscroller)
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
                setupArtistsRecyclerViewPadding(forceNoPadding = false)
            }
        }
    }

    private fun setupArtistsRecyclerViewPadding(forceNoPadding: Boolean) {
        val rvPaddingEnd =
            if (sIsFastScrollerVisible && !forceNoPadding) {
                resources.getDimensionPixelSize(R.dimen.fast_scroller_view_dim)
            } else {
                0
            }
        _musicContainerListBinding?.artistsFoldersRv?.setPadding(0, 0, rvPaddingEnd, 0)
    }

    private fun handleIndicatorFastScrollerViewVisibility() {
        _musicContainerListBinding?.fastscroller?.handleViewVisibility(sIsFastScrollerVisible)
        _musicContainerListBinding?.fastscrollerThumb?.handleViewVisibility(sIsFastScrollerVisible)
    }

    private fun setMenuOnItemClickListener(menu: Menu) {
        _musicContainerListBinding?.searchToolbar?.setOnMenuItemClickListener {

            if (it.itemId != R.id.action_search) {

                mSorting = it.order

                mList = getSortedItemKeys()

                handleIndicatorFastScrollerViewVisibility()

                setupArtistsRecyclerViewPadding(forceNoPadding = false)

                setListDataSource(mList)

                mSortMenuItem.setTitleColor(
                    ThemeHelper.resolveColorAttr(
                        requireActivity(),
                        android.R.attr.textColorPrimary
                    )
                )

                mSortMenuItem = ListsHelper.getSelectedSorting(mSorting, menu).apply {
                    setTitleColor(ThemeHelper.resolveThemeAccent(requireActivity()))
                }

                saveSortingMethodToPrefs(mSorting)
            }

            return@setOnMenuItemClickListener true
        }
    }

    private fun saveSortingMethodToPrefs(sortingMethod: Int) {
        when (mLaunchedBy) {
            GoConstants.ARTIST_VIEW ->
                goPreferences.artistsSorting = sortingMethod
            GoConstants.FOLDER_VIEW ->
                goPreferences.foldersSorting = sortingMethod
            else ->
                goPreferences.albumsSorting = sortingMethod
        }
    }

    companion object {

        private const val TAG_LAUNCHED_BY = "SELECTED_FRAGMENT"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MusicContainersListFragment.
         */
        @JvmStatic
        fun newInstance(launchedBy: String) = MusicContainersListFragment().apply {
            arguments = bundleOf(TAG_LAUNCHED_BY to launchedBy)
        }
    }
}
