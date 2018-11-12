package com.iven.musicplayergo.indexbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class IndexBarRecyclerView extends RecyclerView {

    private IndexBarView mScroller;
    private GestureDetector mGestureDetector = null;

    public IndexBarRecyclerView(@NonNull final Context context) {
        super(context);
    }

    public IndexBarRecyclerView(@NonNull final Context context, @NonNull final AttributeSet attrs) {
        super(context, attrs);
    }

    public IndexBarRecyclerView(@NonNull final Context context, @NonNull final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setFastScroller(@NonNull final IndexBarView indexBarView) {
        mScroller = indexBarView;
        mScroller.onSetScroller(this.getWidth(), this.getHeight());
    }

    @Override
    public void draw(@NonNull final Canvas canvas) {
        super.draw(canvas);

        // Overlay index bar
        if (mScroller != null) {
            mScroller.draw(canvas);
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(@NonNull final MotionEvent ev) {
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
    public boolean onInterceptTouchEvent(@NonNull final MotionEvent ev) {
        return mScroller != null && mScroller.contains(ev.getX(), ev.getY()) || super.onInterceptTouchEvent(ev);
    }
}