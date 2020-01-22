package com.iven.musicplayergo.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.*
import com.iven.musicplayergo.ui.GenericViewHolder
import com.iven.musicplayergo.ui.ThemeHelper
import com.iven.musicplayergo.ui.UIControlInterface
import com.iven.musicplayergo.ui.Utils
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import com.reddit.indicatorfastscroll.FastScrollerThumbView
import com.reddit.indicatorfastscroll.FastScrollerView
import kotlinx.android.synthetic.main.fragment_folders.*
import kotlinx.android.synthetic.main.search_toolbar.*


/**
 * A simple [Fragment] subclass.
 * Use the [FoldersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FoldersFragment : Fragment(), SearchView.OnQueryTextListener {

    //views
    private lateinit var mFoldersRecyclerView: RecyclerView

    private lateinit var mSearchToolbar: Toolbar

    //tab_indicator fast scroller by reddit
    private lateinit var mIndicatorFastScrollerView: FastScrollerView
    private lateinit var mIndicatorFastScrollThumb: FastScrollerThumbView

    private var mFolders: MutableList<String>? = null

    private val mDataSource = dataSourceOf()

    private lateinit var mUIControlInterface: UIControlInterface

    private lateinit var mSortMenuItem: MenuItem
    private var mSorting = DEFAULT_SORTING

    private var sIsFastScroller = false
    private var sLandscape = false

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_folders, container, false)
    }

    private fun getSortedFolders(): MutableList<String>? {
        return Utils.getSortedList(
            mSorting,
            musicLibrary.allSongsByFolder?.keys?.toMutableList()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mSearchToolbar = search_toolbar
        mFoldersRecyclerView = folders_rv
        mIndicatorFastScrollerView = fastscroller
        mIndicatorFastScrollThumb = fastscroller_thumb

        mSorting = goPreferences.foldersSorting

        mFolders = getSortedFolders()
        setFoldersDataSource(mFolders)

        context?.let {

            sLandscape = ThemeHelper.isDeviceLand(it.resources)

            mFoldersRecyclerView.apply {

                // setup{} is an extension method on RecyclerView
                setup {

                    withDataSource(mDataSource)

                    if (sLandscape)
                        withLayoutManager(GridLayoutManager(it, 3))
                    else addItemDecoration(
                        ThemeHelper.getRecyclerViewDivider(it)
                    )

                    withItem<String, GenericViewHolder>(if (sLandscape) R.layout.generic_item else R.layout.folder_item) {
                        onBind(::GenericViewHolder) { _, item ->
                            // GenericViewHolder is `this` here
                            title.text = item
                            subtitle.text = getString(
                                R.string.folder_info,
                                musicLibrary.allSongsByFolder?.getValue(item)?.size
                            )
                        }
                        onClick {
                            // item is a `val` in `this` here
                            if (::mUIControlInterface.isInitialized)
                                mUIControlInterface.onArtistOrFolderSelected(item, true)
                        }
                    }
                }
            }

            setupIndicatorFastScrollerView()

            mSearchToolbar.apply {

                inflateMenu(R.menu.menu_search)

                overflowIcon =
                    AppCompatResources.getDrawable(it, R.drawable.ic_sort)

                title = getString(R.string.folders)

                setNavigationOnClickListener {
                    mUIControlInterface.onCloseActivity()
                }

                menu.apply {

                    mSortMenuItem = Utils.getSelectedSortingMenuItem(mSorting, this).apply {
                        setTitleColor(ThemeHelper.resolveThemeAccent(it))
                    }

                    val searchView = findItem(R.id.action_search).actionView as SearchView

                    searchView.apply {
                        setOnQueryTextListener(this@FoldersFragment)
                        setOnQueryTextFocusChangeListener { _, hasFocus ->
                            if (mSorting != DEFAULT_SORTING) {
                                val fastScrollerVisibility =
                                    if (!hasFocus) View.VISIBLE else View.GONE
                                mIndicatorFastScrollerView.visibility = fastScrollerVisibility
                                mIndicatorFastScrollThumb.visibility = fastScrollerVisibility
                            }
                            menu.setGroupVisible(R.id.sorting, !hasFocus)
                        }
                    }
                    setMenuOnItemClickListener(it, this)
                }
            }
        }
    }

    private fun setFoldersDataSource(foldersList: List<String>?) {
        foldersList?.apply {
            mDataSource.set(this)
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        setFoldersDataSource(Utils.processQueryForStringsLists(newText, mFolders) ?: mFolders)
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    @SuppressLint("DefaultLocale")
    private fun setupIndicatorFastScrollerView() {

        if (mSorting == DEFAULT_SORTING) mIndicatorFastScrollerView.visibility =
            View.GONE

        //set indexes if artists rv is scrollable
        mFoldersRecyclerView.afterMeasured {

            sIsFastScroller = computeVerticalScrollRange() > height

            if (sIsFastScroller) {

                mIndicatorFastScrollerView.setupWithRecyclerView(
                    this,
                    { position ->
                        val item = mFolders?.get(position) // Get your model object
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

                mIndicatorFastScrollThumb.setupWithFastScroller(mIndicatorFastScrollerView)

                mIndicatorFastScrollerView.useDefaultScroller = false
                mIndicatorFastScrollerView.itemIndicatorSelectedCallbacks += object :
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

                if (sLandscape) setupFoldersRecyclerViewPadding(true)

            } else {
                if (sLandscape) {
                    mIndicatorFastScrollerView.visibility = View.GONE
                    mIndicatorFastScrollThumb.visibility = View.GONE
                }
            }
        }
    }

    private fun setupFoldersRecyclerViewPadding(isIndicatorFastScrollerViewVisible: Boolean) {
        if (isIndicatorFastScrollerViewVisible) mIndicatorFastScrollerView.afterMeasured {
            mFoldersRecyclerView.setPadding(0, 0, width, 0)
        } else mFoldersRecyclerView.setPadding(0, 0, 0, 0)
    }

    private fun setMenuOnItemClickListener(context: Context, menu: Menu) {
        mSearchToolbar.setOnMenuItemClickListener {

            if (it.itemId != R.id.action_search) {

                mSorting = it.order

                mFolders = getSortedFolders()

                val isIndicatorFastScrollerViewVisible =
                    mSorting != DEFAULT_SORTING && sIsFastScroller

                mIndicatorFastScrollerView.visibility =
                    if (isIndicatorFastScrollerViewVisible) View.VISIBLE else View.GONE

                if (sLandscape) setupFoldersRecyclerViewPadding(
                    isIndicatorFastScrollerViewVisible
                )

                mDataSource.set(mFolders!!)

                mSortMenuItem.setTitleColor(
                    ThemeHelper.resolveColorAttr(
                        context,
                        android.R.attr.textColorPrimary
                    )
                )

                mSortMenuItem = Utils.getSelectedSortingMenuItem(mSorting, menu).apply {
                    setTitleColor(ThemeHelper.resolveThemeAccent(context))
                }

                goPreferences.foldersSorting = mSorting
            }

            return@setOnMenuItemClickListener true
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment NowPlaying.
         */
        @JvmStatic
        fun newInstance() = FoldersFragment()
    }
}
