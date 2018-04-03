package com.iven.musicplayergo.fastscroller;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import com.iven.musicplayergo.R;
import com.iven.musicplayergo.adapters.ArtistsAdapter;
import com.iven.musicplayergo.utils.SettingsUtils;

public class FastScrollerView extends RecyclerView.AdapterDataObserver {

    private final float mIndexBarWidth;
    private final float mIndexBarMargin;
    private final float mPreviewPadding;
    private final float mDensity;
    private final float mScaledDensity;
    private final FastScrollerHandler.IndexBarHandler mIndexBarHandler;
    private final FastScrollerHandler.IndexThumbHandler mIndexThumbHandler;
    private final int mAccent;
    private final int mThemeContrast;
    private final FastScrollerRecyclerView mArtistsRecyclerView;
    private final ArtistsAdapter mArtistsAdapter;
    private final LinearLayoutManager mLinearLayoutManager;
    private final Context mContext;
    private int mArtistsRecyclerViewWidth;
    private int mArtistsRecyclerViewHeight;
    private RectF mIndexBarRect;
    private String[] mSections;
    private boolean mIsIndexing;
    private int mCurrentSection = -1;
    private boolean sHidden = true;

    public FastScrollerView(FastScrollerRecyclerView rv, ArtistsAdapter artistsAdapter, LinearLayoutManager linearLayoutManager, int accent, int themeContrast) {

        mContext = rv.getContext();
        mArtistsRecyclerView = rv;
        mArtistsAdapter = artistsAdapter;
        mLinearLayoutManager = linearLayoutManager;
        mAccent = accent;
        mThemeContrast = themeContrast;

        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        mDensity = displayMetrics.density;
        mScaledDensity = displayMetrics.scaledDensity;

        mIndexBarHandler = new FastScrollerHandler.IndexBarHandler(this);
        mIndexThumbHandler = new FastScrollerHandler.IndexThumbHandler(rv);

        mIndexBarWidth = 20 * mDensity;
        mIndexBarMargin = 5 * mDensity;
        mPreviewPadding = 5 * mDensity;
    }

    boolean isFastScrollerVisible() {
        return !sHidden;
    }

    void setHidden() {
        if (!mIsIndexing) {
            sHidden = true;
            mArtistsRecyclerView.invalidate();
        }
    }

    private void setShow() {
        sHidden = false;
        mArtistsRecyclerView.invalidate();
    }

    void draw(Canvas canvas) {

        mSections = mArtistsAdapter.getIndexes();
        int indexBarBackgroundColor = sHidden ? Color.TRANSPARENT : ColorUtils.setAlphaComponent(mAccent, 20);

        final int indexTextSize = 12;
        final int indexBarCornerRadius = 5;

        Paint indexBarPaint = new Paint();

        int indexBarTextColor = mThemeContrast != SettingsUtils.THEME_LIGHT ? ContextCompat.getColor(mContext, R.color.grey_200) : ContextCompat.getColor(mContext, R.color.grey_900_darker);

        indexBarPaint.setColor(indexBarBackgroundColor);
        indexBarPaint.setAntiAlias(true);
        canvas.drawRoundRect(mIndexBarRect, indexBarCornerRadius * mDensity, indexBarCornerRadius * mDensity, indexBarPaint);

        if (mSections != null && mSections.length > 0) {
            // Preview is shown when mCurrentSection is set
            if (mCurrentSection >= 0 && !mSections[mCurrentSection].isEmpty()) {
                Paint previewPaint = new Paint();

                previewPaint.setColor(mAccent);
                previewPaint.setAlpha(95);
                previewPaint.setAntiAlias(true);

                Paint previewTextPaint = new Paint();
                previewTextPaint.setColor(indexBarTextColor);
                previewTextPaint.setAlpha(95);
                previewTextPaint.setAntiAlias(true);
                previewTextPaint.setTextSize(50 * mScaledDensity);
                previewTextPaint.setTypeface(Typeface.DEFAULT);

                float previewTextWidth = previewTextPaint.measureText(mSections[mCurrentSection]);
                float previewSize = 2 * mPreviewPadding + previewTextPaint.descent() - previewTextPaint.ascent();
                RectF previewRect = new RectF((mArtistsRecyclerViewWidth - previewSize) / 2
                        , (mArtistsRecyclerViewHeight - previewSize) / 2
                        , (mArtistsRecyclerViewWidth - previewSize) / 2 + previewSize
                        , (mArtistsRecyclerViewHeight - previewSize) / 2 + previewSize);


                canvas.drawRoundRect(previewRect, 5 * mDensity, 5 * mDensity, previewPaint);
                canvas.drawText(mSections[mCurrentSection], previewRect.left + (previewSize - previewTextWidth) / 2 - 1
                        , previewRect.top + (previewSize - (previewTextPaint.descent() - previewTextPaint.ascent())) / 2 - previewTextPaint.ascent(), previewTextPaint);
                mIndexThumbHandler.removeMessages(0);
                mIndexThumbHandler.sendEmptyMessageAtTime(FastScrollerHandler.INDEX_THUMB, SystemClock.uptimeMillis() + FastScrollerHandler.INDEX_THUMB);
            }

            Paint indexPaint = new Paint();
            indexPaint.setColor(indexBarTextColor);
            indexPaint.setAlpha(95);
            indexPaint.setAntiAlias(true);
            indexPaint.setTextSize(indexTextSize * mScaledDensity);
            indexPaint.setTypeface(Typeface.DEFAULT);

            float sectionHeight = (mIndexBarRect.height() - 2 * mIndexBarMargin) / mSections.length;
            float paddingTop = (sectionHeight - (indexPaint.descent() - indexPaint.ascent())) / 2;
            for (int i = 0; i < mSections.length; i++) {

                if (mCurrentSection > -1 && i == mCurrentSection) {
                    indexPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    indexPaint.setColor(mAccent);
                } else {

                    indexPaint.setTextSize(indexTextSize * mScaledDensity);
                    indexPaint.setTypeface(Typeface.DEFAULT);
                    indexPaint.setColor(indexBarTextColor);
                }
                float paddingLeft = (mIndexBarWidth - indexPaint.measureText(mSections[i])) / 2;
                canvas.drawText(mSections[i], mIndexBarRect.left + paddingLeft
                        , mIndexBarRect.top + mIndexBarMargin + sectionHeight * i + paddingTop - indexPaint.ascent(), indexPaint);


            }
        }
    }

    boolean onTouchEvent(MotionEvent ev) {

        if (sHidden) {
            setShow();
        }
        switch (ev.getAction()) {

            case MotionEvent.ACTION_DOWN:
                // If down event occurs inside index bar region, start indexing
                if (contains(ev.getX(), ev.getY())) {
                    // It demonstrates that the motion event started from index bar
                    mIsIndexing = true;
                    // Determine which section the point is in, and move the list to that section
                    mCurrentSection = getSectionByPoint(ev.getY());
                    scrollToPosition();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsIndexing) {
                    // If this event moves inside index bar
                    if (contains(ev.getX(), ev.getY())) {
                        // Determine which section the point is in, and move the list to that section
                        mCurrentSection = getSectionByPoint(ev.getY());
                        scrollToPosition();
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                mIsIndexing = false;
                mCurrentSection = -1;

                if (!sHidden) {
                    mIndexBarHandler.removeCallbacksAndMessages(null);
                    mIndexBarHandler.sendEmptyMessageAtTime(FastScrollerHandler.INDEX_BAR, SystemClock.uptimeMillis() + FastScrollerHandler.INDEX_BAR);
                }
                break;
        }
        return false;
    }

    private void scrollToPosition() {
        try {
            int position = mArtistsAdapter.getIndexPosition(mCurrentSection);
            mLinearLayoutManager.scrollToPositionWithOffset(position, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void onSetScroller(int w, int h) {
        mArtistsRecyclerViewWidth = w;
        mArtistsRecyclerViewHeight = h;

        mIndexBarRect = new RectF(w - mIndexBarMargin - mIndexBarWidth
                , mIndexBarMargin
                , w - mIndexBarMargin
                , h - mIndexBarMargin);
    }

    boolean contains(float x, float y) {
        // Determine if the point is in index bar region, which includes the right margin of the bar
        return mIndexBarRect != null && (x >= mIndexBarRect.left && y >= mIndexBarRect.top && y <= mIndexBarRect.top + mIndexBarRect.height());
    }

    private int getSectionByPoint(float y) {
        if (mSections == null || mSections.length == 0) {
            return 0;
        }
        if (y < mIndexBarRect.top + mIndexBarMargin) {
            return 0;
        }
        if (y >= mIndexBarRect.top + mIndexBarRect.height() - mIndexBarMargin) {
            return mSections.length - 1;
        }
        return (int) ((y - mIndexBarRect.top - mIndexBarMargin) / ((mIndexBarRect.height() - 2 * mIndexBarMargin) / mSections.length));
    }
}