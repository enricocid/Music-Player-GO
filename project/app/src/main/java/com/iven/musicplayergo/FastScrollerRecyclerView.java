package com.iven.musicplayergo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class FastScrollerRecyclerView extends RecyclerView {

    static final int INDEX_BAR = 3000;
    static final int INDEX_THUMB = 500;
    private FastScrollerView mScroller;
    private Handler mVisibilityHandler;
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

    void setAccent(int accent) {
        mAccent = accent;
    }

    void setIsDark(boolean isDark) {
        sDark = isDark;
    }

    private void init(Context context) {

        mScroller = new FastScrollerView(context, this);
        mVisibilityHandler = new Handler(Looper.getMainLooper());
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

    void setBarItemVisibility(final int which) {
        if (mVisibilityHandler.hasMessages(0)) {
            mVisibilityHandler.removeCallbacksAndMessages(null);
        }
        mVisibilityHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (which) {
                    case INDEX_BAR:
                        mScroller.setHidden();
                        break;
                    case INDEX_THUMB:
                        invalidate();
                        break;
                }
            }
        }, which);
    }
}