package com.iven.musicplayergo.fragments

import android.annotation.SuppressLint
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
import com.iven.musicplayergo.musicRepo
import com.iven.musicplayergo.ui.ArtistsViewHolder
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import com.reddit.indicatorfastscroll.FastScrollerThumbView
import com.reddit.indicatorfastscroll.FastScrollerView
import kotlinx.android.synthetic.main.fragment_artists.*

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

            mArtistsRecyclerView = main_rv

            mArtists = musicRepo.allCategorizedMusic.keys.toMutableList()
            mArtists.sort()
            val dataSource = dataSourceOf(mArtists)

            // setup{} is an extension method on RecyclerView
            mArtistsRecyclerView.setup {
                withDataSource(dataSource)
                withItem<String, ArtistsViewHolder>(R.layout.artist_item) {
                    onBind(::ArtistsViewHolder) { _, item ->
                        // PersonViewHolder is `this` here
                        name.text = item
                        name.isSelected = true
                    }
                    onClick { index ->
                        // item is a `val` in `this` here
                        Log.d("yo", "Clicked $index: ${item}")
                    }
                    onLongClick { index ->
                        // item is a `val` in `this` here
                        Log.d("yo2", "Clicked $index: ${item}")
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

                /*   mIndicatorFastScrollerView.textColor =
                       ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, mAccent))*/
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
