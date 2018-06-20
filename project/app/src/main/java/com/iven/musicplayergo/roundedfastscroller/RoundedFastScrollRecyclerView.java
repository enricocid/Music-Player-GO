package com.iven.musicplayergo.roundedfastscroller;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;

public class RoundedFastScrollRecyclerView extends RecyclerView implements RecyclerView.OnItemTouchListener {

    private RoundedFastScroller mScrollbar;
    private ScrollPositionState mScrollPosState = new ScrollPositionState();
    private int mDownX;
    private int mDownY;
    private int mLastY;
    private SparseIntArray mScrollOffsets;
    private ScrollOffsetInvalidator mScrollOffsetInvalidator;

    public RoundedFastScrollRecyclerView(Context context) {
        this(context, null);
    }

    public RoundedFastScrollRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedFastScrollRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScrollbar = new RoundedFastScroller(context, this, attrs);
        mScrollOffsetInvalidator = new ScrollOffsetInvalidator();
        mScrollOffsets = new SparseIntArray();
    }

    public int getScrollBarWidth() {
        return mScrollbar.getWidth();
    }

    public int getScrollBarThumbHeight() {
        return mScrollbar.getThumbHeight();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        addOnItemTouchListener(this);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (getAdapter() != null) {
            getAdapter().unregisterAdapterDataObserver(mScrollOffsetInvalidator);
        }

        if (adapter != null) {
            adapter.registerAdapterDataObserver(mScrollOffsetInvalidator);
        }

        super.setAdapter(adapter);
    }

    /**
     * We intercept the touch handling only to support fast scrolling when initiated from the
     * scroll bar.  Otherwise, we fall back to the default RecyclerView touch handling.
     */
    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent ev) {
        return handleTouchEvent(ev);
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent ev) {
        handleTouchEvent(ev);
    }

    /**
     * Handles the touch event and determines whether to show the fast scroller (or updates it if
     * it is already showing).
     */
    private boolean handleTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Keep track of the down positions
                mDownX = x;
                mDownY = mLastY = y;
                mScrollbar.handleTouchEvent(ev, mDownX, mDownY, mLastY);
                break;
            case MotionEvent.ACTION_MOVE:
                mLastY = y;
                mScrollbar.handleTouchEvent(ev, mDownX, mDownY, mLastY);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mScrollbar.handleTouchEvent(ev, mDownX, mDownY, mLastY);
                break;
        }
        return mScrollbar.isDragging();
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    /**
     * Returns the available scroll height:
     * AvailableScrollHeight = Total height of the all items - last page height
     **/
    protected int getAvailableScrollHeight(int adapterHeight) {
        int scrollHeight = getPaddingTop() + adapterHeight + getPaddingBottom();
        return scrollHeight - getHeight();
    }

    /**
     * Returns the available scroll bar height:
     * AvailableScrollBarHeight = Total height of the visible view - thumb height
     */
    protected int getAvailableScrollBarHeight() {
        return getHeight() - mScrollbar.getThumbHeight();
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);
        onUpdateScrollbar();
        mScrollbar.draw(c);
    }

    /**
     * Updates the scrollbar thumb offset to match the visible scroll of the recycler view.  It does
     * this by mapping the available scroll area of the recycler view to the available space for the
     * scroll bar.
     *
     * @param scrollPosState the current scroll position
     * @param rowCount       the number of rows, used to calculate the total scroll height (assumes that
     */
    protected void updateThumbPosition(ScrollPositionState scrollPosState, int rowCount) {

        int availableScrollHeight;
        int availableScrollBarHeight;
        int scrolledPastHeight;

        if (getAdapter() instanceof MeasurableAdapter) {
            availableScrollHeight = getAvailableScrollHeight(calculateAdapterHeight());
            scrolledPastHeight = calculateScrollDistanceToPosition(scrollPosState.rowIndex);
        } else {
            availableScrollHeight = getAvailableScrollHeight(rowCount * scrollPosState.rowHeight);
            scrolledPastHeight = scrollPosState.rowIndex * scrollPosState.rowHeight;
        }

        availableScrollBarHeight = getAvailableScrollBarHeight();

        // Only show the scrollbar if there is height to be scrolled
        if (availableScrollHeight <= 0) {
            mScrollbar.setThumbPosition(-1, -1);
            return;
        }

        // Calculate the current scroll position, the scrollY of the recycler view accounts for the
        // view padding, while the scrollBarY is drawn right up to the background padding (ignoring
        // padding)
        int scrollY = getPaddingTop() + scrolledPastHeight - scrollPosState.rowTopOffset;
        int scrollBarY = (int) (((float) scrollY / availableScrollHeight) * availableScrollBarHeight);

        // Calculate the position and size of the scroll bar
        int scrollBarX;
        scrollBarX = getWidth() - mScrollbar.getWidth();
        mScrollbar.setThumbPosition(scrollBarX, scrollBarY);
    }

    /**
     * Maps the touch (from 0..1) to the adapter position that should be visible.
     */
    public String scrollToPositionAtProgress(float touchFraction) {
        int itemCount = getAdapter().getItemCount();
        if (itemCount == 0) {
            return "";
        }
        int spanCount = 1;

        // Stop the scroller if it is scrolling
        stopScroll();

        getCurScrollState(mScrollPosState);

        float itemPos;
        int availableScrollHeight;

        int scrollPosition;
        int scrollOffset;

        itemPos = findItemPosition(touchFraction);
        availableScrollHeight = getAvailableScrollHeight(itemCount * mScrollPosState.rowHeight);

        //The exact position of our desired item
        int exactItemPos = (int) (availableScrollHeight * touchFraction);

        //The offset used here is kind of hard to explain.
        //If the position we wish to scroll to is, say, position 10.5, we scroll to position 10,
        //and then offset by 0.5 * rowHeight. This is how we achieve smooth scrolling.
        scrollPosition = spanCount * exactItemPos / mScrollPosState.rowHeight;
        scrollOffset = -(exactItemPos % mScrollPosState.rowHeight);

        LinearLayoutManager layoutManager = ((LinearLayoutManager) getLayoutManager());
        layoutManager.scrollToPositionWithOffset(scrollPosition, scrollOffset);

        if (!(getAdapter() instanceof SectionedAdapter)) {
            return "";
        }

        int posInt = (int) ((touchFraction == 1) ? itemPos - 1 : itemPos);

        SectionedAdapter sectionedAdapter = (SectionedAdapter) getAdapter();
        return sectionedAdapter.getSectionName(posInt);
    }

    @SuppressWarnings("unchecked")
    private float findItemPosition(float touchFraction) {

        if (getAdapter() instanceof MeasurableAdapter) {
            MeasurableAdapter measurer = (MeasurableAdapter) getAdapter();
            int viewTop = (int) (touchFraction * calculateAdapterHeight());

            for (int i = 0; i < getAdapter().getItemCount(); i++) {
                int top = calculateScrollDistanceToPosition(i);
                int bottom = top + measurer.getViewTypeHeight(this, findViewHolderForAdapterPosition(i), getAdapter().getItemViewType(i));
                if (viewTop >= top && viewTop <= bottom) {
                    return i;
                }
            }
            return touchFraction * getAdapter().getItemCount();
        } else {
            return getAdapter().getItemCount() * touchFraction;
        }
    }

    /**
     * Updates the bounds for the scrollbar.
     */
    public void onUpdateScrollbar() {

        if (getAdapter() == null) {
            return;
        }

        int rowCount = getAdapter().getItemCount();
        if (getLayoutManager() instanceof GridLayoutManager) {
            int spanCount = ((GridLayoutManager) getLayoutManager()).getSpanCount();
            rowCount = (int) Math.ceil((double) rowCount / spanCount);
        }
        // Skip early if, there are no items.
        if (rowCount == 0) {
            mScrollbar.setThumbPosition(-1, -1);
            return;
        }

        // Skip early if, there no child laid out in the container.
        getCurScrollState(mScrollPosState);
        if (mScrollPosState.rowIndex < 0) {
            mScrollbar.setThumbPosition(-1, -1);
            return;
        }

        updateThumbPosition(mScrollPosState, rowCount);
    }

    /**
     * Returns the current scroll state of the apps rows.
     */
    private void getCurScrollState(ScrollPositionState stateOut) {
        stateOut.rowIndex = -1;
        stateOut.rowTopOffset = -1;
        stateOut.rowHeight = -1;

        int itemCount = getAdapter().getItemCount();

        // Return early if there are no items, or no children.
        if (itemCount == 0 || getChildCount() == 0) {
            return;
        }

        View child = getChildAt(0);

        stateOut.rowIndex = getChildAdapterPosition(child);
        if (getLayoutManager() instanceof GridLayoutManager) {
            stateOut.rowIndex = stateOut.rowIndex / ((GridLayoutManager) getLayoutManager()).getSpanCount();
        }
        stateOut.rowTopOffset = getLayoutManager().getDecoratedTop(child);
        stateOut.rowHeight = child.getHeight() + getLayoutManager().getTopDecorationHeight(child)
                + getLayoutManager().getBottomDecorationHeight(child);
    }

    /**
     * Calculates the total height of all views above a position in the recycler view. This method
     * should only be called when the attached adapter implements {@link MeasurableAdapter}.
     *
     * @param adapterIndex The index in the adapter to find the total height above the
     *                     corresponding view
     * @return The total height of all views above {@code adapterIndex} in pixels
     */
    @SuppressWarnings("unchecked")
    private int calculateScrollDistanceToPosition(int adapterIndex) {
        if (!(getAdapter() instanceof MeasurableAdapter)) {
            throw new IllegalStateException("calculateScrollDistanceToPosition() should only be called where the RecyclerView.Adapter is an instance of MeasurableAdapter");
        }

        if (mScrollOffsets.indexOfKey(adapterIndex) >= 0) {
            return mScrollOffsets.get(adapterIndex);
        }

        int totalHeight = 0;
        MeasurableAdapter measurer = (MeasurableAdapter) getAdapter();

        for (int i = 0; i < adapterIndex; i++) {
            mScrollOffsets.put(i, totalHeight);
            int viewType = getAdapter().getItemViewType(i);
            totalHeight += measurer.getViewTypeHeight(this, findViewHolderForAdapterPosition(i), viewType);
        }

        mScrollOffsets.put(adapterIndex, totalHeight);
        return totalHeight;
    }

    /**
     * Calculates the total height of the recycler view. This method should only be called when the
     * attached adapter implements {@link MeasurableAdapter}.
     *
     * @return The total height of all rows in the RecyclerView
     */
    private int calculateAdapterHeight() {
        if (!(getAdapter() instanceof MeasurableAdapter)) {
            throw new IllegalStateException("calculateAdapterHeight() should only be called where the RecyclerView.Adapter is an instance of MeasurableAdapter");
        }
        return calculateScrollDistanceToPosition(getAdapter().getItemCount());
    }

    public void setTrackColor(@ColorInt int color) {
        mScrollbar.setTrackColor(color);
    }

    public interface SectionedAdapter {
        @NonNull
        String getSectionName(int position);
    }

    /**
     * FastScrollRecyclerView by default assumes that all items in a RecyclerView will have
     * ItemViews with the same heights so that the total height of all views in the RecyclerView
     * can be calculated. If your list uses different view heights, then make your adapter implement
     * this interface.
     */
    public interface MeasurableAdapter<VH extends ViewHolder> {
        /**
         * Gets the height of a specific view type, including item decorations
         *
         * @param recyclerView The recyclerView that this item view will be placed in
         * @param viewHolder   The viewHolder that corresponds to this item view
         * @param viewType     The view type to get the height of
         * @return The height of a single view for the given view type in pixels
         */
        int getViewTypeHeight(RecyclerView recyclerView, @Nullable VH viewHolder, int viewType);
    }

    /**
     * The current scroll state of the recycler view.  We use this in onUpdateScrollbar()
     * and scrollToPositionAtProgress() to determine the scroll position of the recycler view so
     * that we can calculate what the scroll bar looks like, and where to jump to from the fast
     * scroller.
     */
    public static class ScrollPositionState {
        // The index of the first visible row
        int rowIndex;
        // The offset of the first visible row
        int rowTopOffset;
        // The height of a given row (they are currently all the same height)
        int rowHeight;
    }

    private class ScrollOffsetInvalidator extends AdapterDataObserver {
        private void invalidateAllScrollOffsets() {
            mScrollOffsets.clear();
        }

        @Override
        public void onChanged() {
            invalidateAllScrollOffsets();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            invalidateAllScrollOffsets();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            invalidateAllScrollOffsets();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            invalidateAllScrollOffsets();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            invalidateAllScrollOffsets();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            invalidateAllScrollOffsets();
        }
    }
}
