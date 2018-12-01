package com.iven.musicplayergo.indexbar

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.ArtistsAdapter

class IndexBarView(
    context: Context,
    private val artistsRecyclerView: IndexBarRecyclerView,
    private val artistsAdapter: ArtistsAdapter,
    private val artistsLayoutManager: LinearLayoutManager,
    private val isThemeInverted: Boolean,
    private val accent: Int
) : RecyclerView.AdapterDataObserver() {

    private val mIndexBarWidth: Float
    private val mPreviewPadding: Float
    private val mScaledDensity: Float
    private val mTypefaceIndexes: Typeface?
    private val mTypefaceIndexesBold: Typeface?
    private var mArtistsRecyclerViewWidth: Int = 0
    private var mArtistsRecyclerViewHeight: Int = 0
    private var mIndexBarRect: RectF? = null
    private var mSections: Array<String>? = null
    private var mIsIndexing: Boolean = false
    private var mCurrentSection = -1

    init {

        val displayMetrics = Resources.getSystem().displayMetrics
        val density = displayMetrics.density
        mScaledDensity = displayMetrics.scaledDensity

        mIndexBarWidth = 20 * density
        mPreviewPadding = 5 * density
        artistsRecyclerView.setPadding(0, 0, mIndexBarWidth.toInt(), 0)

        mTypefaceIndexes = ResourcesCompat.getFont(context, R.font.raleway_medium)
        mTypefaceIndexesBold = ResourcesCompat.getFont(context, R.font.raleway_black)
    }

    internal fun draw(canvas: Canvas) {

        mSections = artistsAdapter.getIndexes()
        val indexTextSize = 12
        val indexBarTextColor = if (isThemeInverted) Color.GRAY else Color.BLACK

        if (mSections != null && mSections!!.isNotEmpty()) {
            // Preview is shown when mCurrentSection is set
            try {
                if (mCurrentSection >= 0 && !mSections!![mCurrentSection].isEmpty()) {
                    val previewPaint = Paint()

                    previewPaint.color = accent
                    previewPaint.alpha = 95
                    previewPaint.isAntiAlias = true

                    val previewTextPaint = Paint()
                    previewTextPaint.color = indexBarTextColor
                    previewTextPaint.alpha = 95
                    previewTextPaint.isAntiAlias = true
                    previewTextPaint.textSize = 50 * mScaledDensity
                    previewTextPaint.typeface = mTypefaceIndexesBold

                    val previewTextWidth = previewTextPaint.measureText(mSections!![mCurrentSection])
                    val previewSize = 2 * mPreviewPadding + previewTextPaint.descent() - previewTextPaint.ascent()
                    val previewRect = RectF(
                        (mArtistsRecyclerViewWidth - previewSize) / 2,
                        (mArtistsRecyclerViewHeight - previewSize) / 2,
                        (mArtistsRecyclerViewWidth - previewSize) / 2 + previewSize,
                        (mArtistsRecyclerViewHeight - previewSize) / 2 + previewSize
                    )

                    val cx = mArtistsRecyclerViewWidth / 2
                    val cy = mArtistsRecyclerViewHeight / 2
                    canvas.drawCircle(cx.toFloat(), cy.toFloat(), 40 * mScaledDensity, previewPaint)
                    canvas.drawText(
                        mSections!![mCurrentSection],
                        previewRect.left + (previewSize - previewTextWidth) / 2 - 1,
                        previewRect.top + (previewSize - (previewTextPaint.descent() - previewTextPaint.ascent())) / 2 - previewTextPaint.ascent(),
                        previewTextPaint
                    )
                    artistsRecyclerView.invalidate()
                }

                val indexPaint = Paint()
                indexPaint.color = indexBarTextColor
                indexPaint.alpha = 95
                indexPaint.isAntiAlias = true
                indexPaint.textSize = indexTextSize * mScaledDensity
                indexPaint.typeface = mTypefaceIndexes

                val sectionHeight = (mIndexBarRect!!.height() - 2) / mSections!!.size
                val paddingTop = (sectionHeight - (indexPaint.descent() - indexPaint.ascent())) / 2

                for (i in mSections!!.indices) {

                    if (mCurrentSection > -1 && i == mCurrentSection) {
                        indexPaint.typeface = mTypefaceIndexesBold
                        indexPaint.color = accent
                    } else {
                        indexPaint.textSize = indexTextSize * mScaledDensity
                        indexPaint.typeface = mTypefaceIndexes
                        indexPaint.color = indexBarTextColor
                    }
                    val paddingLeft = (mIndexBarWidth - indexPaint.measureText(mSections!![i])) / 2
                    canvas.drawText(
                        mSections!![i],
                        mIndexBarRect!!.left + paddingLeft,
                        mIndexBarRect!!.top + sectionHeight * i + paddingTop - indexPaint.ascent(),
                        indexPaint
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    internal fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {

            MotionEvent.ACTION_DOWN ->
                // If down event occurs inside index bar region, start indexing
                if (contains(ev.x, ev.y)) {
                    // It demonstrates that the motion event started from index bar
                    mIsIndexing = true
                    // Determine which section the point is in, and move the list to that section
                    mCurrentSection = getSectionByPoint(ev.y)
                    scrollToPosition()
                    return true
                }
            MotionEvent.ACTION_MOVE -> if (mIsIndexing) {
                // If this event moves inside index bar
                if (contains(ev.x, ev.y)) {
                    // Determine which section the point is in, and move the list to that section
                    mCurrentSection = getSectionByPoint(ev.y)
                    scrollToPosition()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                mIsIndexing = false
                mCurrentSection = -1
            }
        }
        return false
    }

    private fun scrollToPosition() {
        try {
            val position = artistsAdapter.getIndexPosition(mCurrentSection)
            artistsLayoutManager.scrollToPositionWithOffset(position, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    internal fun onSetScroller(w: Int, h: Int) {
        mArtistsRecyclerViewWidth = w
        mArtistsRecyclerViewHeight = h

        mIndexBarRect = RectF(w - mIndexBarWidth, 0f, w.toFloat(), h.toFloat())
    }

    internal fun contains(x: Float, y: Float): Boolean {
        // Determine if the point is in index bar region, which includes the right margin of the bar
        return mIndexBarRect != null && x >= mIndexBarRect!!.left && y >= mIndexBarRect!!.top && y <= mIndexBarRect!!.top + mIndexBarRect!!.height()
    }

    private fun getSectionByPoint(y: Float): Int {
        if (mSections == null || mSections!!.isEmpty()) {
            return 0
        }
        if (y < mIndexBarRect!!.top) {
            return 0
        }
        return if (y >= mIndexBarRect!!.top + mIndexBarRect!!.height()) {
            mSections!!.size - 1
        } else ((y - mIndexBarRect!!.top) / ((mIndexBarRect!!.height() - 2) / mSections!!.size)).toInt()
    }
}