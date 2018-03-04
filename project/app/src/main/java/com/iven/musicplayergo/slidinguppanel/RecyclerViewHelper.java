package com.iven.musicplayergo.slidinguppanel;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Helper class for determining the current scroll positions for scrollable views. Currently works
 * for ListView, ScrollView and RecyclerView, but the library users can override it to add support
 * for other views.
 */
class RecyclerViewHelper {
    /**
     * Returns the current scroll position of the scrollable view. If this method returns zero or
     * less, it means at the scrollable view is in a position such as the panel should handle
     * scrolling. If the method returns anything above zero, then the panel will let the scrollable
     * view handle the scrolling
     *
     * @param recyclerView the scrollable view
     * @param isSlidingUp  whether or not the panel is sliding up or down
     * @return the scroll position
     */
    int getRecyclerViewScrollPosition(RecyclerView recyclerView, boolean isSlidingUp) {
        if (recyclerView == null) return 0;
        if (recyclerView.getChildCount() > 0) {
            RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
            if (recyclerView.getAdapter() == null) return 0;
            if (isSlidingUp) {
                View firstChild = recyclerView.getChildAt(0);
                // Approximate the scroll position based on the top child and the first visible item
                return recyclerView.getChildLayoutPosition(firstChild) * lm.getDecoratedMeasuredHeight(firstChild) - lm.getDecoratedTop(firstChild);
            } else {
                View lastChild = recyclerView.getChildAt(recyclerView.getChildCount() - 1);
                // Approximate the scroll position based on the bottom child and the last visible item
                return (recyclerView.getAdapter().getItemCount() - 1) * lm.getDecoratedMeasuredHeight(lastChild) + lm.getDecoratedBottom(lastChild) - recyclerView.getBottom();
            }
        } else {
            return 0;
        }
    }
}
