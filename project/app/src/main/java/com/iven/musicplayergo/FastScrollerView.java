package com.iven.musicplayergo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SectionIndexer;

public class FastScrollerView extends RecyclerView.AdapterDataObserver {

    private float mIndexbarWidth;
    private float mIndexbarMargin;
    private float mPreviewPadding;
    private float mDensity;
    private float mScaledDensity;
    private int mListViewWidth;
    private int mListViewHeight;
    private int mCurrentSection = -1;
    private boolean mIsIndexing;
    private FastScrollerRecyclerView mRecyclerView;
    private SectionIndexer mIndexer;
    private String[] mSections;
    private RectF mIndexbarRect;
    private int mIndexTextSize;
    private int mIndexBarCornerRadius;
    private boolean sHidden = true;

    FastScrollerView(Context context, FastScrollerRecyclerView rv) {

        mIndexTextSize = 12;
        float indexbarWidth = 20;
        float indexbarMargin = 5;
        int previewPadding = 5;
        mIndexBarCornerRadius = 5;

        mDensity = context.getResources().getDisplayMetrics().density;
        mScaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        mRecyclerView = rv;
        setAdapter(mRecyclerView.getAdapter());

        mIndexbarWidth = indexbarWidth * mDensity;
        mIndexbarMargin = indexbarMargin * mDensity;
        mPreviewPadding = previewPadding * mDensity;
    }

    void setHidden() {
        if (!mIsIndexing) {
            sHidden = true;
            mRecyclerView.invalidate();
        }
    }

    private void setShow() {
        sHidden = false;
        mRecyclerView.invalidate();
    }

    void draw(Canvas canvas, int accent, boolean isDark) {

        mSections = sHidden ? null : (String[]) mIndexer.getSections();

        int indexBarBackgroundColor = sHidden ? Color.TRANSPARENT : Color.argb(20, Color.red(accent), Color.green(accent), Color.blue(accent));

        Paint indexbarPaint = new Paint();
        int indexbarTextColor = isDark ? Color.WHITE : Color.BLACK;

        indexbarPaint.setColor(indexBarBackgroundColor);
        indexbarPaint.setAntiAlias(true);
        canvas.drawRoundRect(mIndexbarRect, mIndexBarCornerRadius * mDensity, mIndexBarCornerRadius * mDensity, indexbarPaint);

        if (mSections != null && mSections.length > 0) {
            // Preview is shown when mCurrentSection is set
            if (mCurrentSection >= 0 && !mSections[mCurrentSection].isEmpty()) {
                Paint previewPaint = new Paint();

                previewPaint.setColor(accent);
                previewPaint.setAlpha(95);
                previewPaint.setAntiAlias(true);

                Paint previewTextPaint = new Paint();
                previewTextPaint.setColor(indexbarTextColor);
                previewTextPaint.setAlpha(95);
                previewTextPaint.setAntiAlias(true);
                previewTextPaint.setTextSize(50 * mScaledDensity);
                previewTextPaint.setTypeface(Typeface.DEFAULT);

                float previewTextWidth = previewTextPaint.measureText(mSections[mCurrentSection]);
                float previewSize = 2 * mPreviewPadding + previewTextPaint.descent() - previewTextPaint.ascent();
                RectF previewRect = new RectF((mListViewWidth - previewSize) / 2
                        , (mListViewHeight - previewSize) / 2
                        , (mListViewWidth - previewSize) / 2 + previewSize
                        , (mListViewHeight - previewSize) / 2 + previewSize);


                canvas.drawRoundRect(previewRect, 5 * mDensity, 5 * mDensity, previewPaint);
                canvas.drawText(mSections[mCurrentSection], previewRect.left + (previewSize - previewTextWidth) / 2 - 1
                        , previewRect.top + mPreviewPadding - previewTextPaint.ascent() + 1, previewTextPaint);
                mRecyclerView.setBarItemVisibility(FastScrollerRecyclerView.INDEX_THUMB);
            }

            Paint indexPaint = new Paint();
            indexPaint.setColor(indexbarTextColor);
            indexPaint.setAlpha(95);
            indexPaint.setAntiAlias(true);
            indexPaint.setTextSize(mIndexTextSize * mScaledDensity);
            indexPaint.setTypeface(Typeface.DEFAULT);

            float sectionHeight = (mIndexbarRect.height() - 2 * mIndexbarMargin) / mSections.length;
            float paddingTop = (sectionHeight - (indexPaint.descent() - indexPaint.ascent())) / 2;
            for (int i = 0; i < mSections.length; i++) {

                if (mCurrentSection > -1 && i == mCurrentSection) {
                    indexPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    indexPaint.setTextSize((mIndexTextSize * 2) * mScaledDensity);
                    indexPaint.setColor(accent);
                } else {

                    indexPaint.setTextSize(mIndexTextSize * mScaledDensity);
                    indexPaint.setTypeface(Typeface.DEFAULT);
                    indexPaint.setColor(indexbarTextColor);
                }
                float paddingLeft = (mIndexbarWidth - indexPaint.measureText(mSections[i])) / 2;
                canvas.drawText(mSections[i], mIndexbarRect.left + paddingLeft
                        , mIndexbarRect.top + mIndexbarMargin + sectionHeight * i + paddingTop - indexPaint.ascent(), indexPaint);


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

                if (mSections != null) {
                    mRecyclerView.setBarItemVisibility(FastScrollerRecyclerView.INDEX_BAR);
                }
                break;
        }
        return false;
    }

    private void scrollToPosition() {
        try {
            int position = mIndexer.getPositionForSection(mCurrentSection);
            RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) {
                ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(position, 0);
            } else {
                layoutManager.scrollToPosition(position);
            }
        } catch (Exception e) {
            Log.d("INDEX_BAR", "Data size returns null");
        }
    }

    void onSizeChanged(int w, int h) {
        mListViewWidth = w;
        mListViewHeight = h;
        mIndexbarRect = new RectF(w - mIndexbarMargin - mIndexbarWidth
                , mIndexbarMargin
                , w - mIndexbarMargin
                , h - mIndexbarMargin);
    }

    void setAdapter(RecyclerView.Adapter adapter) {
        if (adapter instanceof SectionIndexer) {
            adapter.registerAdapterDataObserver(this);
            mIndexer = (SectionIndexer) adapter;
        }
    }

    @Override
    public void onChanged() {
        super.onChanged();
        mRecyclerView.invalidate();
    }

    boolean contains(float x, float y) {
        // Determine if the point is in index bar region, which includes the right margin of the bar
        return (x >= mIndexbarRect.left && y >= mIndexbarRect.top && y <= mIndexbarRect.top + mIndexbarRect.height());
    }

    private int getSectionByPoint(float y) {
        if (mSections == null || mSections.length == 0)
            return 0;
        if (y < mIndexbarRect.top + mIndexbarMargin)
            return 0;
        if (y >= mIndexbarRect.top + mIndexbarRect.height() - mIndexbarMargin)
            return mSections.length - 1;
        return (int) ((y - mIndexbarRect.top - mIndexbarMargin) / ((mIndexbarRect.height() - 2 * mIndexbarMargin) / mSections.length));
    }
}