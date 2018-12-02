package com.iven.musicplayergo.indexbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class IndexBarRecyclerView : RecyclerView {

    private var mScroller: IndexBarView? = null
    private var mGestureDetector: GestureDetector? = null
    private var sIndexingEnabled = true

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setFastScroller(indexBarView: IndexBarView) {
        mScroller = indexBarView
        mScroller!!.onSetScroller(this.width, this.height)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        // Overlay index bar
        if (mScroller != null) {
            mScroller!!.draw(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (sIndexingEnabled) {
            // Intercept RecyclerView's touch event
            if (mScroller != null && mScroller!!.onTouchEvent(ev)) {
                return true
            }
            if (mGestureDetector == null) {
                mGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

                    @Suppress("OVERRIDE")
                    override fun onFling(
                        e1: MotionEvent?,
                        e2: MotionEvent?,
                        velocityX: Float,
                        velocityY: Float
                    ): Boolean {
                        return super.onFling(e1, e2, velocityX, velocityY)
                    }
                })
            }
            mGestureDetector!!.onTouchEvent(ev)
        }
        return super.onTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return mScroller != null && mScroller!!.contains(ev.x, ev.y) || super.onInterceptTouchEvent(ev)
    }

    fun setIndexingEnabled(indexingEnabled: Boolean) {
        sIndexingEnabled = indexingEnabled
    }
}