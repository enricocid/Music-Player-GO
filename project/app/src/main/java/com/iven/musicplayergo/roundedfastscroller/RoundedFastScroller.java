package com.iven.musicplayergo.roundedfastscroller;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.iven.musicplayergo.R;

public class RoundedFastScroller {
    private static final int DEFAULT_AUTO_HIDE_DELAY = 1500;
    private final Runnable mHideRunnable;
    private final RoundedFastScrollRecyclerView mRecyclerView;
    private final RoundedFastScrollPopup mPopup;
    private final int mThumbHeight;
    private final int mWidth;
    private final Paint mThumb;
    private final Paint mTrack;
    private final Rect mTmpRect = new Rect();
    private final Rect mInvalidateRect = new Rect();
    private final Rect mInvalidateTmpRect = new Rect();
    // The inset is the buffer around which a point will still register as a click on the scrollbar
    private final int mTouchInset;
    private final Point mThumbPosition = new Point(-1, -1);
    private final Point mOffset = new Point(0, 0);
    // This is the offset from the top of the scrollbar when the user first starts touching.  To
    // prevent jumping, this offset is applied as the user scrolls.
    private int mTouchOffset;
    private boolean mIsDragging;
    private Animator mAutoHideAnimator;
    private boolean mAnimatingShow;

    private int mThumbActiveColor;
    private int mThumbInactiveColor;
    private boolean mThumbInactiveState;

    RoundedFastScroller(Context context, RoundedFastScrollRecyclerView recyclerView, AttributeSet attrs) {

        Resources resources = context.getResources();

        mRecyclerView = recyclerView;
        mPopup = new RoundedFastScrollPopup(resources, recyclerView);

        mThumbHeight = resources.getDimensionPixelSize(R.dimen.rounded_fast_scroll_thumb_height);
        mWidth = resources.getDimensionPixelSize(R.dimen.rounded_fast_scroll_track_max_width);

        mTouchInset = resources.getDimensionPixelSize(R.dimen.rounded_fast_scroll_thumb_touch_inset);

        mThumb = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTrack = new Paint(Paint.ANTI_ALIAS_FLAG);

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.RoundedFastScrollRecyclerView, 0, 0);
        try {
            mThumbInactiveState = typedArray.getBoolean(R.styleable.RoundedFastScrollRecyclerView_roundedFastScrollThumbInactiveColor, true);
            mThumbActiveColor = typedArray.getColor(R.styleable.RoundedFastScrollRecyclerView_roundedFastScrollThumbColor, resources.getColor(R.color.rounded_thumb_default_color));
            mThumbInactiveColor = typedArray.getColor(R.styleable.RoundedFastScrollRecyclerView_roundedFastScrollThumbInactiveColor, resources.getColor(R.color.rounded_thumb_default_color));

            int trackColor = typedArray.getColor(R.styleable.RoundedFastScrollRecyclerView_roundedFastScrollTrackColor, resources.getColor(R.color.rounded_track_default_color));
            int popupBgColor = typedArray.getColor(R.styleable.RoundedFastScrollRecyclerView_roundedFastScrollPopupBgColor, Color.BLACK);
            int popupTextColor = typedArray.getColor(R.styleable.RoundedFastScrollRecyclerView_roundedFastScrollPopupTextColor, Color.WHITE);
            int popupTextSize = resources.getDimensionPixelSize(R.dimen.rounded_fast_scroll_popup_text_size);
            int popupBackgroundSize = resources.getDimensionPixelSize(R.dimen.rounded_fast_scroll_popup_size);

            mTrack.setColor(trackColor);
            mThumb.setColor(mThumbInactiveState ? mThumbInactiveColor : mThumbActiveColor);
            mPopup.setBgColor(popupBgColor);
            mPopup.setTextColor(popupTextColor);
            mPopup.setTextSize(popupTextSize);
            mPopup.setBackgroundSize(popupBackgroundSize);
        } finally {
            typedArray.recycle();
        }

        mHideRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mIsDragging) {
                    if (mAutoHideAnimator != null) {
                        mAutoHideAnimator.cancel();
                    }
                    mAutoHideAnimator = ObjectAnimator.ofInt(RoundedFastScroller.this, "offsetX", mWidth);
                    mAutoHideAnimator.setInterpolator(new FastOutLinearInInterpolator());
                    mAutoHideAnimator.setDuration(200);
                    mAutoHideAnimator.start();
                }
            }
        };

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!mRecyclerView.isInEditMode()) {
                    show();
                }
            }
        });

        postAutoHideDelayed();
    }

    public int getThumbHeight() {
        return mThumbHeight;
    }

    public int getWidth() {
        return mWidth;
    }

    public boolean isDragging() {
        return mIsDragging;
    }

    /**
     * Handles the touch event and determines whether to show the fast scroller (or updates it if
     * it is already showing).
     */
    public void handleTouchEvent(MotionEvent ev, int downX, int downY, int lastY) {
        ViewConfiguration config = ViewConfiguration.get(mRecyclerView.getContext());

        int action = ev.getAction();
        int y = (int) ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (isNearPoint(downX, downY)) {
                    mTouchOffset = downY - mThumbPosition.y;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // Check if we should start scrolling
                if (!mIsDragging && isNearPoint(downX, downY) &&
                        Math.abs(y - downY) > config.getScaledTouchSlop()) {
                    mRecyclerView.getParent().requestDisallowInterceptTouchEvent(true);
                    mIsDragging = true;
                    mTouchOffset += (lastY - downY);
                    mPopup.animateVisibility(true);
                    if (mThumbInactiveState) {
                        mThumb.setColor(mThumbActiveColor);
                    }
                }
                if (mIsDragging) {
                    // Update the fast scroller section name at this touch position
                    int top = 0;
                    int bottom = mRecyclerView.getHeight() - mThumbHeight;
                    float boundedY = (float) Math.max(top, Math.min(bottom, y - mTouchOffset));
                    String sectionName = mRecyclerView.scrollToPositionAtProgress((boundedY - top) / (bottom - top));
                    mPopup.setSectionName(sectionName);
                    mPopup.animateVisibility(!sectionName.isEmpty());
                    mRecyclerView.invalidate(mPopup.updateFastScrollerBounds(mRecyclerView, mThumbPosition.y));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchOffset = 0;
                if (mIsDragging) {
                    mIsDragging = false;
                    mPopup.animateVisibility(false);
                }
                if (mThumbInactiveState) {
                    mThumb.setColor(mThumbInactiveColor);
                }
                break;
        }
    }

    public void draw(Canvas canvas) {

        if (mThumbPosition.x < 0 || mThumbPosition.y < 0) {
            return;
        }

        //Background (left, right, bottom)
        canvas.drawRoundRect(mThumbPosition.x + mOffset.x, 0, mThumbPosition.x + mOffset.x + mWidth, mRecyclerView.getHeight() + mOffset.y, mWidth, mWidth, mTrack);

        //Handle
        float r = mWidth;
        canvas.drawRoundRect(mThumbPosition.x + mOffset.x, mThumbPosition.y + mOffset.y, mThumbPosition.x + mOffset.x + mWidth, mThumbPosition.y + mOffset.y + mThumbHeight, r, r, mThumb);

        //Popup
        mPopup.draw(canvas);
    }

    /**
     * Returns whether the specified points are near the scroll bar bounds.
     */
    private boolean isNearPoint(int x, int y) {
        mTmpRect.set(mThumbPosition.x, mThumbPosition.y, mThumbPosition.x + mWidth,
                mThumbPosition.y + mThumbHeight);
        mTmpRect.inset(mTouchInset, mTouchInset);
        return mTmpRect.contains(x, y);
    }

    public void setThumbPosition(int x, int y) {
        if (mThumbPosition.x == x && mThumbPosition.y == y) {
            return;
        }
        // do not create new objects here, this is called quite often
        mInvalidateRect.set(mThumbPosition.x + mOffset.x, mOffset.y, mThumbPosition.x + mOffset.x + mWidth, mRecyclerView.getHeight() + mOffset.y);
        mThumbPosition.set(x, y);
        mInvalidateTmpRect.set(mThumbPosition.x + mOffset.x, mOffset.y, mThumbPosition.x + mOffset.x + mWidth, mRecyclerView.getHeight() + mOffset.y);
        mInvalidateRect.union(mInvalidateTmpRect);
        mRecyclerView.invalidate(mInvalidateRect);
    }

    private void setOffset(int x, int y) {
        if (mOffset.x == x && mOffset.y == y) {
            return;
        }
        // do not create new objects here, this is called quite often
        mInvalidateRect.set(mThumbPosition.x + mOffset.x, mOffset.y, mThumbPosition.x + mOffset.x + mWidth, mRecyclerView.getHeight() + mOffset.y);
        mOffset.set(x, y);
        mInvalidateTmpRect.set(mThumbPosition.x + mOffset.x, mOffset.y, mThumbPosition.x + mOffset.x + mWidth, mRecyclerView.getHeight() + mOffset.y);
        mInvalidateRect.union(mInvalidateTmpRect);
        mRecyclerView.invalidate(mInvalidateRect);
    }

    @Keep
    @SuppressWarnings("unused")
    public int getOffsetX() {
        return mOffset.x;
    }

    // Setter/getter for the popup alpha for animations
    @Keep
    @SuppressWarnings("unused")
    public void setOffsetX(int x) {
        setOffset(x, mOffset.y);
    }

    private void show() {
        if (!mAnimatingShow) {
            if (mAutoHideAnimator != null) {
                mAutoHideAnimator.cancel();
            }
            mAutoHideAnimator = ObjectAnimator.ofInt(this, "offsetX", 0);
            mAutoHideAnimator.setInterpolator(new LinearOutSlowInInterpolator());
            mAutoHideAnimator.setDuration(150);
            mAutoHideAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    mAnimatingShow = false;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mAnimatingShow = false;
                }
            });
            mAnimatingShow = true;
            mAutoHideAnimator.start();
        }
        postAutoHideDelayed();
    }

    private void postAutoHideDelayed() {
        if (mRecyclerView != null) {
            cancelAutoHide();
            mRecyclerView.postDelayed(mHideRunnable, DEFAULT_AUTO_HIDE_DELAY);
        }
    }

    private void cancelAutoHide() {
        if (mRecyclerView != null) {
            mRecyclerView.removeCallbacks(mHideRunnable);
        }
    }

    public void setTrackColor(@ColorInt int color) {
        mTrack.setColor(color);
        mRecyclerView.invalidate(mInvalidateRect);
    }

    private void enableThumbInactiveColor(boolean enableInactiveColor) {
        mThumbInactiveState = enableInactiveColor;
        mThumb.setColor(mThumbInactiveState ? mThumbInactiveColor : mThumbActiveColor);
    }

    @Deprecated
    public void setThumbInactiveColor(boolean thumbInactiveColor) {
        enableThumbInactiveColor(thumbInactiveColor);
    }
}