package com.iven.musicplayergo.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.recyclical.datasource.DataSource
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.musicLibrary
import com.iven.musicplayergo.ui.GenericViewHolder
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
class ArtistsFragment : Fragment() {

    //views
    private lateinit var mArtistsRecyclerView: RecyclerView

    private lateinit var mSearchToolbar: Toolbar
    private lateinit var mHideItemDialog: MaterialDialog

    private lateinit var mArtistsLayoutManager: LinearLayoutManager

    private lateinit var mSavedArtistRecyclerLayoutState: Parcelable

    //indicator fast scroller by reddit
    private lateinit var mIndicatorFastScrollerView: FastScrollerView
    private lateinit var mIndicatorFastScrollThumb: FastScrollerThumbView

    private lateinit var mArtists: MutableList<String>
    private var mFilteredArtists: List<String>? = null


    private lateinit var mDataSource: DataSource<Any>

    private lateinit var mUIControlInterface: UIControlInterface

    private var sSearchEnabled: Boolean = true


    override fun onResume() {
        super.onResume()
        if (::mSavedArtistRecyclerLayoutState.isInitialized) {
            mArtistsLayoutManager.onRestoreInstanceState(mSavedArtistRecyclerLayoutState)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mArtistsLayoutManager.isInitialized) {
            mSavedArtistRecyclerLayoutState = mArtistsLayoutManager.onSaveInstanceState()!!
        }
        if (::mHideItemDialog.isInitialized && mHideItemDialog.isShowing) mHideItemDialog.dismiss()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement MyInterface ")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_artists, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (context != null) {
            mSearchToolbar = search_toolbar
            mSearchToolbar.inflateMenu(R.menu.menu_search)
            mSearchToolbar.title = getString(R.string.artists)

            val itemSearch = mSearchToolbar.menu.findItem(R.id.action_search)

            sSearchEnabled = goPreferences.isSearchBarEnabled
            itemSearch.isVisible = sSearchEnabled

            setMenuOnItemClickListener()

            mArtistsRecyclerView = artists_rv

            mArtists = Utils.getSortedList(
                goPreferences.artistsSorting,
                musicLibrary.allAlbumsForArtist.keys.toMutableList(),
                musicLibrary.allAlbumsForArtist.keys.toMutableList()
            )

            setupFilteredArtists()

            mDataSource = dataSourceOf(mArtists)

            // setup{} is an extension method on RecyclerView
            mArtistsRecyclerView.setup {
                // item is a `val` in `this` here
                withDataSource(mDataSource)
                withItem<String, GenericViewHolder>(R.layout.generic_item) {
                    onBind(::GenericViewHolder) { _, item ->
                        // GenericViewHolder is `this` here
                        title.text = item
                        subtitle.text = getArtistSubtitle(item)
                    }

                    onClick {
                        if (::mUIControlInterface.isInitialized) mUIControlInterface.onArtistSelected(
                            item
                        )
                    }
                    onLongClick { index ->
                        mHideItemDialog = Utils.makeHideItemDialog(
                            activity!!,
                            Pair(index, item),
                            mArtists,
                            mDataSource
                        )
                    }
                }
            }

            if (goPreferences.isFastScrollEnabled) {
                //indicator fast scroller view
                mIndicatorFastScrollerView = fastscroller
                mIndicatorFastScrollThumb = fastscroller_thumb

                setupIndicatorFastScrollerView()
            }

            if (sSearchEnabled) {
                val searchView = itemSearch.actionView as SearchView
                Utils.setupSearchViewForStringLists(
                    searchView,
                    mArtists,
                    onResultsChanged = { newResults ->
                        mFilteredArtists = if (newResults.isEmpty()) {
                            null
                        } else {
                            newResults
                        }
                        mDataSource.set(mFilteredArtists ?: mArtists)
                    })
            }
        }
    }

    private fun getArtistSubtitle(item: String): String {
        return getString(
            R.string.artist_info,
            musicLibrary.allAlbumsForArtist.getValue(item).size,
            musicLibrary.allSongsForArtist.getValue(item).size
        )
    }

    @SuppressLint("DefaultLocale")
    private fun setupIndicatorFastScrollerView() {

        //set indexes if artists rv is scrollable
        mArtistsRecyclerView.afterMeasured {
            if (mArtistsRecyclerView.computeVerticalScrollRange() > height) {

                mIndicatorFastScrollerView.setupWithRecyclerView(
                    mArtistsRecyclerView,
                    { position ->
                        val item = (mFilteredArtists ?: mArtists)[position] // Get your model object
                        // or fetch the section at [position] from your database

                        FastScrollItemIndicator.Text(
                            item.substring(
                                0,
                                1
                            ).toUpperCase() // Grab the first letter and capitalize it
                        ) // Return a text indicator
                    }
                )

                mIndicatorFastScrollerView.afterMeasured {

                    //set margin for artists recycler to improve fast scroller visibility
                    mArtistsRecyclerView.setPadding(0, 0, width, 0)

                    //set margin for thumb view
                    val newLayoutParams =
                        mIndicatorFastScrollThumb.layoutParams as FrameLayout.LayoutParams
                    newLayoutParams.marginEnd = width
                    mIndicatorFastScrollThumb.layoutParams = newLayoutParams
                }
                mIndicatorFastScrollThumb.setupWithFastScroller(mIndicatorFastScrollerView)

                mIndicatorFastScrollerView.useDefaultScroller = false
                mIndicatorFastScrollerView.itemIndicatorSelectedCallbacks += object :
                    FastScrollerView.ItemIndicatorSelectedCallback {
                    override fun onItemIndicatorSelected(
                        indicator: FastScrollItemIndicator,
                        indicatorCenterY: Int,
                        itemPosition: Int
                    ) {
                        val artistsLayoutManager =
                            mArtistsRecyclerView.layoutManager as LinearLayoutManager
                        artistsLayoutManager.scrollToPositionWithOffset(itemPosition, 0)
                    }
                }

            } else {
                mIndicatorFastScrollerView.visibility = View.GONE
            }
        }
    }

    private fun setMenuOnItemClickListener() {
        mSearchToolbar.setOnMenuItemClickListener {

            mArtists = Utils.getSortedList(
                it.itemId,
                mArtists,
                musicLibrary.allAlbumsForArtist.keys.toMutableList()
            )

            mDataSource.set(mArtists)

            goPreferences.artistsSorting = it.itemId

            return@setOnMenuItemClickListener true
        }
    }

    private fun setupFilteredArtists() {

        val iterator = mArtists.iterator()
        for (i in iterator) {
            if (goPreferences.hiddenItems?.contains(i)!!) iterator.remove()
        }
    }

    //viewTreeObserver extension to measure layout params
    //https://antonioleiva.com/kotlin-ongloballayoutlistener/
    private inline fun <T : View> T.afterMeasured(crossinline f: T.() -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    f()
                }
            }
        })
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
