package com.iven.musicplayergo.fastscroller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class FastScrollerRecyclerView extends RecyclerView {

    private FastScrollerView mScroller;
    private GestureDetector mGestureDetector = null;
    private int mAccent;
    private boolean sDark;

    public FastScrollerRecyclerView(Context context) {
        super(context);
    }

    public FastScrollerRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FastScrollerRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAccent(int accent) {
        mAccent = accent;
    }

    public void setIsDark(boolean isDark) {
        sDark = isDark;
    }

    private void init(Context context) {

        mScroller = new FastScrollerView(context, this);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Overlay index bar
        if (mScroller != null) {
            mScroller.draw(canvas, mAccent, sDark);
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent ev) {
        // Intercept RecyclerView's touch event
        if (mScroller != null && mScroller.onTouchEvent(ev))
            return true;

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

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
        if (mScroller != null)
            mScroller.setAdapter(adapter);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        if (mScroller != null)
            mScroller.onSizeChanged(w, h);
    }
}