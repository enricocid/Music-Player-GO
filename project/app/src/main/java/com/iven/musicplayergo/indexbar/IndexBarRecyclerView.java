package com.iven.musicplayergo.indexbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class IndexBarRecyclerView extends RecyclerView {

    private IndexBarView mScroller;
    private GestureDetector mGestureDetector = null;

    public IndexBarRecyclerView(Context context) {
        super(context);
    }

    public IndexBarRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IndexBarRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setFastScroller(IndexBarView indexBarView) {
        mScroller = indexBarView;
        mScroller.onSetScroller(this.getWidth(), this.getHeight());
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Overlay index bar
        if (mScroller != null) {
            mScroller.draw(canvas);
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent ev) {
        // Intercept RecyclerView's touch event
        if (mScroller != null && mScroller.onTouchEvent(ev)) {
            return true;
        }
        if (mGestureDetector == null) {
            mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2,
                                       float velocityX, float velocityY) {
                    return super.onFling(e1, e2, velocityX, velocityY);
                }
            });
        }
        mGestureDetector.onTouchEvent(ev);

        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mScroller != null && mScroller.contains(ev.getX(), ev.getY()) || super.onInterceptTouchEvent(ev);
    }
}