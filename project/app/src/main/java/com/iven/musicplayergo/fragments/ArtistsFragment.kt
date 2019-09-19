package com.iven.musicplayergo.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.R
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.musicLibrary
import com.iven.musicplayergo.ui.GenericViewHolder
import com.iven.musicplayergo.ui.SongsSheetInterface
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import com.reddit.indicatorfastscroll.FastScrollerThumbView
import com.reddit.indicatorfastscroll.FastScrollerView
import kotlinx.android.synthetic.main.fragment_artists.*
import kotlinx.android.synthetic.main.recycler_view_item.*

/**
 * A simple [Fragment] subclass.
 * Use the [ArtistsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ArtistsFragment : Fragment() {

    private lateinit var mArtists: MutableList<String>
    private lateinit var mArtistsRecyclerView: RecyclerView

    //indicator fast scroller by reddit
    private lateinit var mIndicatorFastScrollerView: FastScrollerView
    private lateinit var mIndicatorFastScrollThumb: FastScrollerThumbView

    private lateinit var mSongsSheetInterface: SongsSheetInterface

    private var mSelectedArtist = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mSongsSheetInterface = activity as SongsSheetInterface
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

            mArtistsRecyclerView = artists_rv

            mArtists = musicLibrary.allCategorizedMusic.keys.toMutableList()
            mArtists.sort()
            val dataSource = dataSourceOf(mArtists)

            // setup{} is an extension method on RecyclerView
            mArtistsRecyclerView.setup {
                // item is a `val` in `this` here
                withDataSource(dataSource)
                withItem<String, GenericViewHolder>(R.layout.recycler_view_item) {
                    onBind(::GenericViewHolder) { _, item ->
                        // GenericViewHolder is `this` here
                        title.text = item
                        title.isSelected = true
                        val albums = musicLibrary.allCategorizedMusic.getValue(item)

                        subtitle.text = getString(
                            R.string.artist_count,
                            albums.keys.size,
                            MusicUtils.getArtistSongsCount(albums)
                        )
                        subtitle.isSelected = true
                    }

                    onClick { _ ->
                        if (::mSongsSheetInterface.isInitialized) {

                            if (mSelectedArtist != item) {

                                mSelectedArtist = item

                                mSongsSheetInterface.onPopulateAndShowSheet(
                                    false,
                                    item,
                                    subtitle.text.toString(),
                                    MusicUtils.buildSortedArtistAlbums(
                                        resources,
                                        musicLibrary.allCategorizedMusic.getValue(item)
                                    )[0].music!!
                                )
                            } else {
                                mSongsSheetInterface.onShowSheet()
                            }
                        }
                    }
                    onLongClick { index ->
                        // item is a `val` in `this` here
                        Log.d("doSomething", "Clicked $index: ${item}")
                    }
                }
            }

            //indicator fast scroller view
            mIndicatorFastScrollerView = fastscroller
            mIndicatorFastScrollThumb = fastscroller_thumb

            setupIndicatorFastScrollerView()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun setupIndicatorFastScrollerView() {

        //set indexes if artists rv is scrollable
        mArtistsRecyclerView.afterMeasured {
            if (mArtistsRecyclerView.computeVerticalScrollRange() > height) {

                mIndicatorFastScrollerView.setupWithRecyclerView(
                    mArtistsRecyclerView,
                    { position ->
                        val item = mArtists[position] // Get your model object
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
                        mArtistsRecyclerView.scrollToPosition(itemPosition)
                    }
                }
            } else {
                mIndicatorFastScrollerView.visibility = View.GONE
            }
        }
    }

    //viewTreeObserver extension to measure layout params
    //https://antonioleiva.com/kotlin-ongloballayoutlistener/
    inline fun <T : View> T.afterMeasured(crossinline f: T.() -> Unit) {
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
