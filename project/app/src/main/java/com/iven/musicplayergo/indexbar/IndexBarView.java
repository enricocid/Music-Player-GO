package com.iven.musicplayergo.indexbar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import com.iven.musicplayergo.R;
import com.iven.musicplayergo.adapters.ArtistsAdapter;

public class IndexBarView extends RecyclerView.AdapterDataObserver {

    private final float mIndexBarWidth;
    private final float mPreviewPadding;
    private final float mScaledDensity;
    private final boolean sThemeInverted;
    private final IndexBarRecyclerView mArtistsRecyclerView;
    private final ArtistsAdapter mArtistsAdapter;
    private final LinearLayoutManager mLinearLayoutManager;
    private final Typeface mTypefaceIndexes, mTypefaceIndexesBold, mTypefacePreview;
    private final Context mContext;
    private int mArtistsRecyclerViewWidth;
    private int mArtistsRecyclerViewHeight;
    private RectF mIndexBarRect;
    private String[] mSections;
    private boolean mIsIndexing;
    private int mCurrentSection = -1;
    private int mAccent;

    public IndexBarView(@NonNull final Context context, @NonNull final IndexBarRecyclerView rv, @NonNull final ArtistsAdapter artistsAdapter, @NonNull final LinearLayoutManager linearLayoutManager, final boolean isThemeInverted, final int accent) {

        mContext = context;
        mArtistsRecyclerView = rv;
        mArtistsAdapter = artistsAdapter;
        mLinearLayoutManager = linearLayoutManager;
        sThemeInverted = isThemeInverted;
        mAccent = accent;

        final DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        final float density = displayMetrics.density;
        mScaledDensity = displayMetrics.scaledDensity;

        mIndexBarWidth = 20 * density;
        mPreviewPadding = 5 * density;
        mArtistsRecyclerView.setPadding(0, 0, (int) mIndexBarWidth, 0);

        mTypefaceIndexes = ResourcesCompat.getFont(mContext, R.font.open_sans);
        mTypefaceIndexesBold = ResourcesCompat.getFont(mContext, R.font.open_sans_bold);
        mTypefacePreview = ResourcesCompat.getFont(mContext, R.font.open_sans_bold);
    }

    void draw(@NonNull final Canvas canvas) {

        mSections = mArtistsAdapter.getIndexes();
        final int indexTextSize = 12;
        final int indexBarTextColor = sThemeInverted ? Color.GRAY : Color.BLACK;

        if (mSections != null && mSections.length > 0) {
            // Preview is shown when mCurrentSection is set
            try {
                if (mCurrentSection >= 0 && !mSections[mCurrentSection].isEmpty()) {
                    final Paint previewPaint = new Paint();

                    previewPaint.setColor(mAccent);
                    previewPaint.setAlpha(95);
                    previewPaint.setAntiAlias(true);

                    final Paint previewTextPaint = new Paint();
                    previewTextPaint.setColor(indexBarTextColor);
                    previewTextPaint.setAlpha(95);
                    previewTextPaint.setAntiAlias(true);
                    previewTextPaint.setTextSize(50 * mScaledDensity);
                    previewTextPaint.setTypeface(mTypefacePreview);

                    final float previewTextWidth = previewTextPaint.measureText(mSections[mCurrentSection]);
                    final float previewSize = 2 * mPreviewPadding + previewTextPaint.descent() - previewTextPaint.ascent();
                    final RectF previewRect = new RectF((mArtistsRecyclerViewWidth - previewSize) / 2
                            , (mArtistsRecyclerViewHeight - previewSize) / 2
                            , (mArtistsRecyclerViewWidth - previewSize) / 2 + previewSize
                            , (mArtistsRecyclerViewHeight - previewSize) / 2 + previewSize);

                    final int cx = mArtistsRecyclerViewWidth / 2;
                    final int cy = mArtistsRecyclerViewHeight / 2;
                    canvas.drawCircle(cx, cy, 40 * mScaledDensity, previewPaint);
                    canvas.drawText(mSections[mCurrentSection], previewRect.left + (previewSize - previewTextWidth) / 2 - 1
                            , previewRect.top + (previewSize - (previewTextPaint.descent() - previewTextPaint.ascent())) / 2 - previewTextPaint.ascent(), previewTextPaint);
                    mArtistsRecyclerView.invalidate();
                }

                final Paint indexPaint = new Paint();
                indexPaint.setColor(indexBarTextColor);
                indexPaint.setAlpha(95);
                indexPaint.setAntiAlias(true);
                indexPaint.setTextSize(indexTextSize * mScaledDensity);
                indexPaint.setTypeface(mTypefaceIndexes);

                final float sectionHeight = (mIndexBarRect.height() - 2) / mSections.length;
                final float paddingTop = (sectionHeight - (indexPaint.descent() - indexPaint.ascent())) / 2;
                for (int i = 0; i < mSections.length; i++) {

                    if (mCurrentSection > -1 && i == mCurrentSection) {
                        indexPaint.setTypeface(mTypefaceIndexesBold);
                        indexPaint.setColor(mAccent);
                    } else {
                        indexPaint.setTextSize(indexTextSize * mScaledDensity);
                        indexPaint.setTypeface(mTypefaceIndexes);
                        indexPaint.setColor(indexBarTextColor);
                    }
                    final float paddingLeft = (mIndexBarWidth - indexPaint.measureText(mSections[i])) / 2;
                    canvas.drawText(mSections[i], mIndexBarRect.left + paddingLeft
                            , mIndexBarRect.top + sectionHeight * i + paddingTop - indexPaint.ascent(), indexPaint);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    boolean onTouchEvent(@NonNull final MotionEvent ev) {
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
                break;
        }
        return false;
    }

    private void scrollToPosition() {
        try {
            final int position = mArtistsAdapter.getIndexPosition(mCurrentSection);
            mLinearLayoutManager.scrollToPositionWithOffset(position, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void onSetScroller(final int w, final int h) {
        mArtistsRecyclerViewWidth = w;
        mArtistsRecyclerViewHeight = h;

        mIndexBarRect = new RectF(w - mIndexBarWidth
                , 0
                , w
                , h);
    }

    boolean contains(final float x, final float y) {
        // Determine if the point is in index bar region, which includes the right margin of the bar
        return mIndexBarRect != null && (x >= mIndexBarRect.left && y >= mIndexBarRect.top && y <= mIndexBarRect.top + mIndexBarRect.height());
    }

    private int getSectionByPoint(final float y) {
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