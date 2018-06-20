package com.iven.musicplayergo.roundedfastscroller;

import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Keep;
import android.text.TextUtils;

import com.iven.musicplayergo.R;

public class RoundedFastScrollPopup {

    private RoundedFastScrollRecyclerView mRecyclerView;

    private int mBackgroundSize;
    private int mCornerRadius;
    private int mCornerRadius2;

    private Path mBackgroundPath = new Path();
    private RectF mBackgroundRect = new RectF();
    private Paint mBackgroundPaint;
    private int mBackgroundColor = 0xff000000;

    private Rect mInvalidateRect = new Rect();
    private Rect mTmpRect = new Rect();

    // The absolute bounds of the fast scroller bg
    private Rect mBgBounds = new Rect();

    private String mSectionName;

    private Paint mTextPaint;
    private Rect mTextBounds = new Rect();

    private float mAlpha = 1;

    private ObjectAnimator mAlphaAnimator;
    private boolean mVisible;

    RoundedFastScrollPopup(Resources resources, RoundedFastScrollRecyclerView recyclerView) {

        mRecyclerView = recyclerView;

        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setAlpha(0);

        setTextSize(resources.getDimensionPixelSize(R.dimen.rounded_fast_scroll_popup_text_size));
        setBackgroundSize(resources.getDimensionPixelSize(R.dimen.rounded_fast_scroll_popup_size));
    }

    public void setBgColor(int color) {
        mBackgroundColor = color;
        mBackgroundPaint.setColor(color);
        mRecyclerView.invalidate(mBgBounds);
    }

    public void setTextColor(int color) {
        mTextPaint.setColor(color);
        mRecyclerView.invalidate(mBgBounds);
    }

    public void setTextSize(int size) {
        mTextPaint.setTextSize(size);
        mRecyclerView.invalidate(mBgBounds);
    }

    public void setBackgroundSize(int size) {
        mBackgroundSize = size;
        mCornerRadius = mBackgroundSize / 2;
        mCornerRadius2 = mCornerRadius / 5;
        mRecyclerView.invalidate(mBgBounds);
    }

    /**
     * Animates the visibility of the fast scroller popup.
     */
    public void animateVisibility(boolean visible) {
        if (mVisible != visible) {
            mVisible = visible;
            if (mAlphaAnimator != null) {
                mAlphaAnimator.cancel();
            }
            mAlphaAnimator = ObjectAnimator.ofFloat(this, "alpha", visible ? 1f : 0f);
            mAlphaAnimator.setDuration(visible ? 200 : 150);
            mAlphaAnimator.start();
        }
    }

    @Keep
    @SuppressWarnings("unused")
    public float getAlpha() {
        return mAlpha;
    }

    // Setter/getter for the popup alpha for animations
    @Keep
    @SuppressWarnings("unused")
    public void setAlpha(float alpha) {
        mAlpha = alpha;
        mRecyclerView.invalidate(mBgBounds);
    }

    public void draw(Canvas canvas) {
        if (isVisible()) {

            // Draw the fast scroller popup
            int restoreCount = canvas.save();
            canvas.translate(mBgBounds.left, mBgBounds.top);
            mTmpRect.set(mBgBounds);
            mTmpRect.offsetTo(0, 0);

            mBackgroundPath.reset();
            mBackgroundRect.set(mTmpRect);

            float[] radii = new float[]{mCornerRadius, mCornerRadius, mCornerRadius, mCornerRadius, mCornerRadius2, mCornerRadius2, mCornerRadius, mCornerRadius};

            mBackgroundPath.addRoundRect(mBackgroundRect, radii, Path.Direction.CCW);

            mBackgroundPaint.setAlpha((int) (Color.alpha(mBackgroundColor) * mAlpha));
            mTextPaint.setAlpha((int) (mAlpha * 255));

            canvas.drawPath(mBackgroundPath, mBackgroundPaint);
            canvas.drawText(mSectionName, (mBgBounds.width() - mTextBounds.width()) / 2,
                    mBgBounds.height() - (mBgBounds.height() - mTextBounds.height()) / 2,
                    mTextPaint);
            canvas.restoreToCount(restoreCount);
        }
    }

    public void setSectionName(String sectionName) {
        if (!sectionName.equals(mSectionName)) {
            mSectionName = sectionName;
            mTextPaint.getTextBounds(sectionName, 0, sectionName.length(), mTextBounds);
            // Update the width to use measureText since that is more accurate
            mTextBounds.right = (int) (mTextBounds.left + mTextPaint.measureText(sectionName));
        }
    }

    /**
     * Updates the bounds for the fast scroller.
     *
     * @return the invalidation rect for this update.
     */
    public Rect updateFastScrollerBounds(RoundedFastScrollRecyclerView recyclerView, int thumbOffsetY) {
        mInvalidateRect.set(mBgBounds);

        if (isVisible()) {
            // Calculate the dimensions and position of the fast scroller popup
            int edgePadding = recyclerView.getScrollBarWidth();
            int bgPadding = Math.round((mBackgroundSize - mTextBounds.height()) / 10) * 5;
            int bgHeight = mBackgroundSize;
            int bgWidth = Math.max(mBackgroundSize, mTextBounds.width() + (2 * bgPadding));


            mBgBounds.right = recyclerView.getWidth() - (2 * recyclerView.getScrollBarWidth());
            mBgBounds.left = mBgBounds.right - bgWidth;
            mBgBounds.top = thumbOffsetY - bgHeight + recyclerView.getScrollBarThumbHeight() / 2;
            mBgBounds.top = Math.max(edgePadding, Math.min(mBgBounds.top, recyclerView.getHeight() - edgePadding - bgHeight));

            mBgBounds.bottom = mBgBounds.top + bgHeight;
        } else {
            mBgBounds.setEmpty();
        }

        // Combine the old and new fast scroller bounds to create the full invalidate rect
        mInvalidateRect.union(mBgBounds);
        return mInvalidateRect;
    }

    private boolean isVisible() {
        return (mAlpha > 0f) && (!TextUtils.isEmpty(mSectionName));
    }
}