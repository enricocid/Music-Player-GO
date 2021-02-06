package com.iven.musicplayergo.fragments

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.recyclical.datasource.emptyDataSource
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentMusicContainerListBinding
import com.iven.musicplayergo.extensions.afterMeasured
import com.iven.musicplayergo.extensions.getFastScrollerItem
import com.iven.musicplayergo.extensions.handleViewVisibility
import com.iven.musicplayergo.extensions.setTitleColor
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.ui.GenericViewHolder
import com.iven.musicplayergo.ui.UIControlInterface
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import com.reddit.indicatorfastscroll.FastScrollerView

/**
 * A simple [Fragment] subclass.
 * Use the [MusicContainersListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MusicContainersListFragment : Fragment(R.layout.fragment_music_container_list),
    SearchView.OnQueryTextListener {

    private lateinit var mMusicContainerListBinding: FragmentMusicContainerListBinding

    // View model
    private lateinit var mMusicViewModel: MusicViewModel

    private var mLaunchedBy = GoConstants.ARTIST_VIEW

    private var mList: MutableList<String>? = null

    private val mDataSource = emptyDataSource()

    private lateinit var mUIControlInterface: UIControlInterface

    private lateinit var mSortMenuItem: MenuItem
    private var mSorting = GoConstants.DESCENDING_SORTING

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMusicContainerListBinding = FragmentMusicContainerListBinding.bind(view)

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
        mMusicContainerListBinding.artistsFoldersRv.run {

            // setup{} is an extension method on RecyclerView
            setup {

                // item is a `val` in `this` here
                withDataSource(mDataSource)

                withItem<String, GenericViewHolder>(R.layout.generic_item) {

                    onBind(::GenericViewHolder) { _, item ->
                        // GenericViewHolder is `this` here
                        title.text = item
                        subtitle.text = getItemsSubtitle(item)
                    }

                    onClick {
                        if (::mUIControlInterface.isInitialized) {
                            mUIControlInterface.onArtistOrFolderSelected(
                                item,
                                mLaunchedBy
                            )
                        }
                    }

                    onLongClick { index ->
                        if (::mUIControlInterface.isInitialized) {
                            DialogHelper.showPopupForHide(
                                requireActivity(),
                                findViewHolderForAdapterPosition(index)?.itemView,
                                item
                            )
                        }
                    }
                }
            }
        }

        setupIndicatorFastScrollerView()

        mMusicContainerListBinding.searchToolbar.run {

            inflateMenu(R.menu.menu_search)

            overflowIcon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_sort)

            title = getFragmentTitle()

            setNavigationOnClickListener {
                mUIControlInterface.onCloseActivity()
            }

            menu.run {

                mSortMenuItem = ListsHelper.getSelectedSorting(mSorting, this).apply {
                    setTitleColor(ThemeHelper.resolveThemeAccent(requireActivity()))
                }

                val searchView = findItem(R.id.action_search).actionView as SearchView

                searchView.run {
                    setOnQueryTextListener(this@MusicContainersListFragment)
                    setOnQueryTextFocusChangeListener { _, hasFocus ->
                        if (sIsFastScrollerVisible) {
                            mMusicContainerListBinding.fastscroller.handleViewVisibility(!hasFocus)
                            mMusicContainerListBinding.fastscrollerThumb.handleViewVisibility(
                                !hasFocus
                            )
                            setupArtistsRecyclerViewPadding(hasFocus)
                        }
                        menu.setGroupVisible(R.id.sorting, !hasFocus)
                    }
                }
                setMenuOnItemClickListener(this)
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

        // Set indexes if artists rv is scrollable
        mMusicContainerListBinding.artistsFoldersRv.afterMeasured {

            sIsFastScroller = computeVerticalScrollRange() > height

            if (sIsFastScroller) {

                mMusicContainerListBinding.fastscroller.setupWithRecyclerView(
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
                    }
                )

                mMusicContainerListBinding.fastscrollerThumb.setupWithFastScroller(
                    mMusicContainerListBinding.fastscroller
                )

                mMusicContainerListBinding.fastscroller.useDefaultScroller = false
                mMusicContainerListBinding.fastscroller.itemIndicatorSelectedCallbacks += object :
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

            setupArtistsRecyclerViewPadding(false)
        }
    }

    private fun setupArtistsRecyclerViewPadding(forceNoPadding: Boolean) {
        val rvPaddingEnd =
            if (sIsFastScrollerVisible && !forceNoPadding) {
                resources.getDimensionPixelSize(R.dimen.fast_scroller_view_dim)
            } else {
                0
            }
        mMusicContainerListBinding.artistsFoldersRv.setPadding(0, 0, rvPaddingEnd, 0)
    }

    private fun handleIndicatorFastScrollerViewVisibility() {
        mMusicContainerListBinding.fastscroller.handleViewVisibility(sIsFastScrollerVisible)
        mMusicContainerListBinding.fastscrollerThumb.handleViewVisibility(sIsFastScrollerVisible)
    }

    private fun setMenuOnItemClickListener(menu: Menu) {
        mMusicContainerListBinding.searchToolbar.setOnMenuItemClickListener {

            if (it.itemId != R.id.action_search) {

                mSorting = it.order

                mList = getSortedItemKeys()

                handleIndicatorFastScrollerViewVisibility()

                setupArtistsRecyclerViewPadding(false)

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
