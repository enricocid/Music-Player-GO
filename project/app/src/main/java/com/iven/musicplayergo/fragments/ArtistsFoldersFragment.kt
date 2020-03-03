package com.iven.musicplayergo.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.recyclical.datasource.emptyDataSource
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicRepository
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentArtistFolderBinding
import com.iven.musicplayergo.extensions.afterMeasured
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
 * Use the [ArtistsFoldersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ArtistsFoldersFragment : Fragment(R.layout.fragment_artist_folder),
    SearchView.OnQueryTextListener {

    private lateinit var mArtistFolderFragmentBinding: FragmentArtistFolderBinding
    private lateinit var mMusicRepository: MusicRepository

    private var sIsFoldersFragment = false

    private var mList: MutableList<String>? = null

    private val mDataSource = emptyDataSource()

    private lateinit var mUIControlInterface: UIControlInterface

    private lateinit var mSortMenuItem: MenuItem
    private var mSorting = GoConstants.DESCENDING_SORTING

    private var sIsFastScroller = false
    private val sIsFastScrollerVisible get() = sIsFastScroller && mSorting != GoConstants.DEFAULT_SORTING
    private var sLandscape = false

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_SELECTED_FRAGMENT)?.let { isFoldersFragment ->
            sIsFoldersFragment = isFoldersFragment != GoConstants.TAG_ARTISTS
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    private fun getSortedList(): MutableList<String>? {
        val selectedList =
            if (sIsFoldersFragment) mMusicRepository.deviceMusicByFolder?.keys else mMusicRepository.deviceAlbumsByArtist?.keys
        return ListsHelper.getSortedList(
            mSorting,
            selectedList?.toMutableList()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mArtistFolderFragmentBinding = FragmentArtistFolderBinding.bind(view)

        mSorting =
            if (sIsFoldersFragment) goPreferences.foldersSorting else goPreferences.artistsSorting

        mMusicRepository = MusicRepository.getInstance()

        mList =
            getSortedList()?.filter { !goPreferences.filters?.contains(it)!! }?.toMutableList()
        setListDataSource(mList)

        sLandscape = ThemeHelper.isDeviceLand(requireContext().resources)

        mArtistFolderFragmentBinding.artistsFoldersRv.apply {

            // setup{} is an extension method on RecyclerView
            setup {

                // item is a `val` in `this` here
                withDataSource(mDataSource)

                if (sLandscape)
                    withLayoutManager(GridLayoutManager(requireContext(), 3))
                else
                    addItemDecoration(
                        ThemeHelper.getRecyclerViewDivider(requireContext())
                    )

                withItem<String, GenericViewHolder>(R.layout.generic_item) {

                    onBind(::GenericViewHolder) { _, item ->
                        // GenericViewHolder is `this` here
                        title.text = item
                        subtitle.text = if (sIsFoldersFragment) getString(
                            R.string.folder_info,
                            mMusicRepository.deviceMusicByFolder?.getValue(item)?.size
                        ) else getArtistSubtitle(item)
                    }

                    onClick {
                        if (::mUIControlInterface.isInitialized)
                            mUIControlInterface.onArtistOrFolderSelected(
                                item,
                                sIsFoldersFragment
                            )
                    }

                    onLongClick { index ->
                        if (::mUIControlInterface.isInitialized)
                            DialogHelper.showHidePopup(
                                context,
                                findViewHolderForAdapterPosition(index)?.itemView,
                                item,
                                mUIControlInterface
                            )
                    }
                }
            }
        }

        setupIndicatorFastScrollerView()

        mArtistFolderFragmentBinding.searchToolbar.apply {

            inflateMenu(R.menu.menu_search)

            overflowIcon =
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_sort)

            title = getString(if (sIsFoldersFragment) R.string.folders else R.string.artists)

            setNavigationOnClickListener {
                mUIControlInterface.onCloseActivity()
            }

            menu.apply {

                mSortMenuItem = ListsHelper.getSelectedSorting(mSorting, this).apply {
                    setTitleColor(ThemeHelper.resolveThemeAccent(requireContext()))
                }

                val searchView = findItem(R.id.action_search).actionView as SearchView

                searchView.apply {
                    setOnQueryTextListener(this@ArtistsFoldersFragment)
                    setOnQueryTextFocusChangeListener { _, hasFocus ->
                        if (sIsFastScrollerVisible) {
                            mArtistFolderFragmentBinding.fastscroller.handleViewVisibility(!hasFocus)
                            mArtistFolderFragmentBinding.fastscrollerThumb.handleViewVisibility(
                                !hasFocus
                            )
                            setupArtistsRecyclerViewPadding(hasFocus)
                        }
                        menu.setGroupVisible(R.id.sorting, !hasFocus)
                    }
                }

                setMenuOnItemClickListener(requireContext(), this)
            }
        }
    }

    private fun setListDataSource(selectedList: List<String>?) {
        if (!selectedList.isNullOrEmpty()) mDataSource.set(selectedList)
    }

    fun onListFiltered(stringToFilter: String?) {
        stringToFilter?.let { string ->
            mList?.remove(string)
            setListDataSource(mList)
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        setListDataSource(ListsHelper.processQueryForStringsLists(newText, mList) ?: mList)
        return false
    }

    override fun onQueryTextSubmit(query: String?) = false

    private fun getArtistSubtitle(item: String) = getString(
        R.string.artist_info,
        mMusicRepository.deviceAlbumsByArtist?.getValue(item)?.size,
        mMusicRepository.deviceSongsByArtist?.getValue(item)?.size
    )

    @SuppressLint("DefaultLocale")
    private fun setupIndicatorFastScrollerView() {

        // Set indexes if artists rv is scrollable
        mArtistFolderFragmentBinding.artistsFoldersRv.afterMeasured {

            sIsFastScroller = computeVerticalScrollRange() > height

            if (sIsFastScroller) {

                mArtistFolderFragmentBinding.fastscroller.setupWithRecyclerView(
                    this,
                    { position ->
                        val item = mList?.get(position) // Get your model object
                        // or fetch the section at [position] from your database

                        FastScrollItemIndicator.Text(
                            item?.substring(
                                0,
                                1
                            )?.toUpperCase()!! // Grab the first letter and capitalize it
                        ) // Return a text tab_indicator
                    }, showIndicator = { _, indicatorPosition, _ ->
                        // Hide every other indicator
                        if (sLandscape) indicatorPosition % 2 == 0 else true
                    }
                )

                mArtistFolderFragmentBinding.fastscrollerThumb.setupWithFastScroller(
                    mArtistFolderFragmentBinding.fastscroller
                )

                mArtistFolderFragmentBinding.fastscroller.useDefaultScroller = false
                mArtistFolderFragmentBinding.fastscroller.itemIndicatorSelectedCallbacks += object :
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
            if (sIsFastScrollerVisible && !forceNoPadding) resources.getDimensionPixelSize(R.dimen.fast_scroller_view_dim) else 0
        mArtistFolderFragmentBinding.artistsFoldersRv.setPadding(0, 0, rvPaddingEnd, 0)
    }

    private fun handleIndicatorFastScrollerViewVisibility() {
        mArtistFolderFragmentBinding.fastscroller.handleViewVisibility(sIsFastScrollerVisible)
        mArtistFolderFragmentBinding.fastscrollerThumb.handleViewVisibility(sIsFastScrollerVisible)
    }

    private fun setMenuOnItemClickListener(context: Context, menu: Menu) {
        mArtistFolderFragmentBinding.searchToolbar.setOnMenuItemClickListener {

            if (it.itemId != R.id.action_search) {

                mSorting = it.order

                mList = getSortedList()

                handleIndicatorFastScrollerViewVisibility()

                setupArtistsRecyclerViewPadding(false)

                setListDataSource(mList)

                mSortMenuItem.setTitleColor(
                    ThemeHelper.resolveColorAttr(
                        context,
                        android.R.attr.textColorPrimary
                    )
                )

                mSortMenuItem = ListsHelper.getSelectedSorting(mSorting, menu).apply {
                    setTitleColor(ThemeHelper.resolveThemeAccent(context))
                }

                if (sIsFoldersFragment) goPreferences.foldersSorting =
                    mSorting else goPreferences.artistsSorting = mSorting
            }

            return@setOnMenuItemClickListener true
        }
    }

    companion object {

        private const val TAG_SELECTED_FRAGMENT = "SELECTED_FRAGMENT"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ArtistsFoldersFragment.
         */
        @JvmStatic
        fun newInstance(tag: String) = ArtistsFoldersFragment().apply {
            arguments = Bundle().apply {
                putString(TAG_SELECTED_FRAGMENT, tag)
            }
        }
    }
}
