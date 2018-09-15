package com.iven.musicplayergo.indexbar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import com.iven.musicplayergo.R;
import com.iven.musicplayergo.adapters.ArtistsAdapter;
import com.iven.musicplayergo.adapters.ColorsAdapter;

public class IndexBarView extends RecyclerView.AdapterDataObserver {

    private final float mIndexBarWidth;
    private final float mPreviewPadding;
    private final float mScaledDensity;
    private final boolean sThemeInverted;
    private final IndexBarRecyclerView mArtistsRecyclerView;
    private final ArtistsAdapter mArtistsAdapter;
    private final LinearLayoutManager mLinearLayoutManager;
    private int mAccent;
    private int mArtistsRecyclerViewWidth;
    private int mArtistsRecyclerViewHeight;
    private RectF mIndexBarRect;
    private String[] mSections;
    private boolean mIsIndexing;
    private int mCurrentSection = -1;
    private final Typeface mTypefaceIndexes, mTypefaceIndexesBold, mTypefacePreview;
    private final Context mContext;

    public IndexBarView(IndexBarRecyclerView rv, ArtistsAdapter artistsAdapter, LinearLayoutManager linearLayoutManager, boolean isThemeInverted) {

        mContext = rv.getContext();
        mArtistsRecyclerView = rv;
        mArtistsAdapter = artistsAdapter;
        mLinearLayoutManager = linearLayoutManager;
        sThemeInverted = isThemeInverted;

        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        final float density = displayMetrics.density;
        mScaledDensity = displayMetrics.scaledDensity;

        mIndexBarWidth = 20 * density;
        mPreviewPadding = 5 * density;
        mArtistsRecyclerView.setPadding(0, 0, (int) mIndexBarWidth, 0);

        mTypefaceIndexes = ResourcesCompat.getFont(mContext, R.font.raleway_medium);
        mTypefaceIndexesBold = ResourcesCompat.getFont(mContext, R.font.raleway_black);
        mTypefacePreview = ResourcesCompat.getFont(mContext, R.font.raleway_semibold);
    }

    void draw(Canvas canvas) {

        mSections = mArtistsAdapter.getIndexes();
        final int indexTextSize = 12;

        int indexBarTextColor = sThemeInverted ? Color.GRAY : Color.BLACK;

        if (mSections != null && mSections.length > 0) {
            // Preview is shown when mCurrentSection is set
            try {
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
                    previewTextPaint.setTypeface(mTypefacePreview);

                    float previewTextWidth = previewTextPaint.measureText(mSections[mCurrentSection]);
                    float previewSize = 2 * mPreviewPadding + previewTextPaint.descent() - previewTextPaint.ascent();
                    RectF previewRect = new RectF((mArtistsRecyclerViewWidth - previewSize) / 2
                            , (mArtistsRecyclerViewHeight - previewSize) / 2
                            , (mArtistsRecyclerViewWidth - previewSize) / 2 + previewSize
                            , (mArtistsRecyclerViewHeight - previewSize) / 2 + previewSize);

                    int cx = mArtistsRecyclerViewWidth / 2;
                    int cy = mArtistsRecyclerViewHeight / 2;
                    canvas.drawCircle(cx, cy, 40 * mScaledDensity, previewPaint);
                    canvas.drawText(mSections[mCurrentSection], previewRect.left + (previewSize - previewTextWidth) / 2 - 1
                            , previewRect.top + (previewSize - (previewTextPaint.descent() - previewTextPaint.ascent())) / 2 - previewTextPaint.ascent(), previewTextPaint);
                    mArtistsRecyclerView.invalidate();
                }

                Paint indexPaint = new Paint();
                indexPaint.setColor(indexBarTextColor);
                indexPaint.setAlpha(95);
                indexPaint.setAntiAlias(true);
                indexPaint.setTextSize(indexTextSize * mScaledDensity);
                indexPaint.setTypeface(mTypefaceIndexes);

                float sectionHeight = (mIndexBarRect.height() - 2) / mSections.length;
                float paddingTop = (sectionHeight - (indexPaint.descent() - indexPaint.ascent())) / 2;
                for (int i = 0; i < mSections.length; i++) {

                    if (mCurrentSection > -1 && i == mCurrentSection) {
                        indexPaint.setTypeface(mTypefaceIndexesBold);
                        indexPaint.setColor(mAccent);
                    } else {
                        indexPaint.setTextSize(indexTextSize * mScaledDensity);
                        indexPaint.setTypeface(mTypefaceIndexes);
                        indexPaint.setColor(indexBarTextColor);
                    }
                    float paddingLeft = (mIndexBarWidth - indexPaint.measureText(mSections[i])) / 2;
                    canvas.drawText(mSections[i], mIndexBarRect.left + paddingLeft
                            , mIndexBarRect.top + sectionHeight * i + paddingTop - indexPaint.ascent(), indexPaint);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {

            case MotionEvent.ACTION_DOWN:
                // If down event occurs inside index bar region, start indexing
                if (contains(ev.getX(), ev.getY())) {
                    mAccent = ContextCompat.getColor(mContext, ColorsAdapter.getRandomColor());
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

        mIndexBarRect = new RectF(w - mIndexBarWidth
                , 0
                , w
                , h);
    }

    boolean contains(float x, float y) {
        // Determine if the point is in index bar region, which includes the right margin of the bar
        return mIndexBarRect != null && (x >= mIndexBarRect.left && y >= mIndexBarRect.top && y <= mIndexBarRect.top + mIndexBarRect.height());
    }

    private int getSectionByPoint(float y) {
        if (mSections == null || mSections.length == 0) {
            return 0;
        }
        if (y < mIndexBarRect.top) {
            return 0;
        }
        if (y >= mIndexBarRect.top + mIndexBarRect.height()) {
            return mSections.length - 1;
        }
        return (int) ((y - mIndexBarRect.top) / ((mIndexBarRect.height() - 2) / mSections.length));
    }
}