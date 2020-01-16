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
import com.afollestad.recyclical.datasource.emptyDataSource
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
import kotlinx.android.synthetic.main.fragment_artists.*
import kotlinx.android.synthetic.main.search_toolbar.*


/**
 * A simple [Fragment] subclass.
 * Use the [ArtistsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ArtistsFragment : Fragment(), SearchView.OnQueryTextListener {

    //views
    private lateinit var mArtistsRecyclerView: RecyclerView

    private lateinit var mSearchToolbar: Toolbar

    //tab_indicator fast scroller by reddit
    private lateinit var mIndicatorFastScrollerView: FastScrollerView
    private lateinit var mIndicatorFastScrollThumb: FastScrollerThumbView

    private var mArtists: MutableList<String>? = null

    private val mDataSource = emptyDataSource()

    private lateinit var mUIControlInterface: UIControlInterface

    private lateinit var mSortMenuItem: MenuItem
    private var mSorting = ASCENDING_SORTING

    private var sDeviceLand = false

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
        return inflater.inflate(R.layout.fragment_artists, container, false)
    }

    private fun getSortedArtists(): MutableList<String>? {
        return Utils.getSortedList(
            mSorting,
            musicLibrary.allAlbumsByArtist?.keys?.toMutableList(),
            musicLibrary.allAlbumsByArtist?.keys?.toMutableList()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mSearchToolbar = search_toolbar
        mArtistsRecyclerView = artists_rv
        mIndicatorFastScrollerView = fastscroller
        mIndicatorFastScrollThumb = fastscroller_thumb

        mSorting = goPreferences.artistsSorting

        mArtists = getSortedArtists()

        setArtistsDataSource(mArtists)

        context?.let {

            sDeviceLand = ThemeHelper.isDeviceLand(it.resources)

            mArtistsRecyclerView.apply {

                // setup{} is an extension method on RecyclerView
                setup {

                    // item is a `val` in `this` here
                    withDataSource(mDataSource)

                    if (sDeviceLand)
                        withLayoutManager(GridLayoutManager(it, 3))
                    else
                        addItemDecoration(
                            ThemeHelper.getRecyclerViewDivider(it)
                        )

                    withItem<String, GenericViewHolder>(R.layout.generic_item) {

                        onBind(::GenericViewHolder) { _, item ->
                            // GenericViewHolder is `this` here
                            title.text = item
                            subtitle.text = getArtistSubtitle(item)
                        }

                        onClick {
                            if (::mUIControlInterface.isInitialized)
                                mUIControlInterface.onArtistOrFolderSelected(item, false)
                        }
                    }
                }
            }

            setupIndicatorFastScrollerView()

            mSearchToolbar.apply {

                inflateMenu(R.menu.menu_search)

                overflowIcon =
                    AppCompatResources.getDrawable(it, R.drawable.ic_sort)

                title = getString(R.string.artists)

                setNavigationOnClickListener {
                    mUIControlInterface.onCloseActivity()
                }

                menu.apply {

                    mSortMenuItem = Utils.getSelectedSortingMenuItem(mSorting, this)

                    mSortMenuItem.setTitleColor(ThemeHelper.resolveThemeAccent(it))

                    val searchView = findItem(R.id.action_search).actionView as SearchView

                    searchView.apply {
                        setOnQueryTextListener(this@ArtistsFragment)
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

    private fun setArtistsDataSource(artistsList: List<String>?) {
        artistsList?.apply {
            mDataSource.set(this)
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        setArtistsDataSource(Utils.processQueryForStringsLists(newText, mArtists) ?: mArtists)
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    private fun getArtistSubtitle(item: String): String {
        return getString(
            R.string.artist_info,
            musicLibrary.allAlbumsByArtist?.getValue(item)?.size,
            musicLibrary.allSongsByArtist?.getValue(item)?.size
        )
    }

    @SuppressLint("DefaultLocale")
    private fun setupIndicatorFastScrollerView() {

        if (mSorting == DEFAULT_SORTING) mIndicatorFastScrollerView.visibility =
            View.GONE

        //set indexes if artists rv is scrollable
        mArtistsRecyclerView.afterMeasured {
            if (computeVerticalScrollRange() > height) {

                mIndicatorFastScrollerView.setupWithRecyclerView(
                    this,
                    { position ->
                        val item = mArtists?.get(position) // Get your model object
                        // or fetch the section at [position] from your database

                        FastScrollItemIndicator.Text(
                            item?.substring(
                                0,
                                1
                            )?.toUpperCase()!! // Grab the first letter and capitalize it
                        ) // Return a text tab_indicator
                    }, showIndicator = { _, indicatorPosition, _ ->
                        // Hide every other indicator
                        if (sDeviceLand) indicatorPosition % 2 == 0 else true
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
            } else {
                if (sDeviceLand) {
                    mIndicatorFastScrollerView.visibility = View.GONE
                    mIndicatorFastScrollThumb.visibility = View.GONE
                }
            }
        }
    }

    private fun setMenuOnItemClickListener(context: Context, menu: Menu) {
        mSearchToolbar.setOnMenuItemClickListener {

            if (it.itemId != R.id.action_search) {

                mSorting = it.order

                mArtists = getSortedArtists()

                mIndicatorFastScrollerView.visibility =
                    if (mSorting != DEFAULT_SORTING) View.VISIBLE else View.GONE

                setArtistsDataSource(mArtists)

                mSortMenuItem.setTitleColor(
                    ThemeHelper.resolveColorAttr(
                        context,
                        android.R.attr.textColorPrimary
                    )
                )

                mSortMenuItem = Utils.getSelectedSortingMenuItem(mSorting, menu)

                mSortMenuItem.setTitleColor(ThemeHelper.resolveThemeAccent(context))

                goPreferences.artistsSorting = mSorting
            }

            return@setOnMenuItemClickListener true
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MusicFragment.
         */
        @JvmStatic
        fun newInstance() = ArtistsFragment()
    }
}
