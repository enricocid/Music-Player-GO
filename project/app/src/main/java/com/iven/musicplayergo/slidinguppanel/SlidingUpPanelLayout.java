package com.iven.musicplayergo.slidinguppanel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

public class SlidingUpPanelLayout extends ViewGroup {

    /**
     * Tag for the sliding state stored inside the bundle
     */
    private static final String SLIDING_STATE = "sliding_state";
    /**
     * Default initial state for the component
     */
    private static final PanelState DEFAULT_SLIDE_STATE = PanelState.COLLAPSED;

    private final ViewDragHelper mDragHelper;

    private final RecyclerViewHelper mScrollableViewHelper = new RecyclerViewHelper();
    /**
     * The size of the overhang in pixels.
     */
    private int mPanelHeight = -1;
    /**
     * True if the collapsed panel should be dragged up.
     */
    private boolean mIsSlidingUp;
    /**
     * If provided, the panel can be dragged by only this view. Otherwise, the entire panel can be
     * used for dragging.
     */

    private View mDragView;
    /**
     * If provided, the panel will transfer the scroll from this view to itself when needed.
     */
    private RecyclerView mScrollableView;
    /**
     * The child view that can slide, if any.
     */
    private View mSlideView;
    /**
     * The main view
     */
    private View mMainView;
    private PanelState mSlideState = DEFAULT_SLIDE_STATE;
    /**
     * If the current slide state is DRAGGING, this will store the last non dragging state
     */
    private PanelState mLastNotDraggingSlideState = DEFAULT_SLIDE_STATE;
    /**
     * How far the panel is offset from its expanded position.
     * range [0, 1] where 0 = collapsed, 1 = expanded.
     */
    private float mSlideOffset;
    /**
     * How far in pixels the sliding panel may move.
     */
    private int mSlideRange;

    /**
     * A panel view is locked into internal scrolling or another condition that
     * is preventing a drag.
     */
    private boolean mIsUnableToDrag;
    /**
     * Flag indicating that sliding feature is enabled\disabled
     */
    private float mPrevMotionX;
    private float mPrevMotionY;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private boolean mIsScrollableViewHandlingTouch = false;
    /**
     * Stores whether or not the pane was expanded the last time it was sliding.
     * If expand/collapse operations are invoked this state is modified. Used by
     * instance state save/restore.
     */
    private boolean mFirstLayout = true;

    private View mDimView;
    private int mDimViewColor;

    private ImageButton mArrowUp;

    public SlidingUpPanelLayout(Context context) {
        this(context, null);
    }

    public SlidingUpPanelLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingUpPanelLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (isInEditMode()) {
            mDragHelper = null;
            return;
        }

        final float density = context.getResources().getDisplayMetrics().density;

        setWillNotDraw(false);

        mDragHelper = ViewDragHelper.create(this, new DragHelperCallback());
        mDragHelper.setMinVelocity(400 * density);
    }

    public void setGravity(int gravity) {
        mIsSlidingUp = gravity == Gravity.BOTTOM;
        if (!mFirstLayout) {
            requestLayout();
        }
    }

    /**
     * Set the collapsed panel height in pixels
     *
     * @param val A height in pixels
     */
    public void setPanelHeight(int val) {

        mPanelHeight = val;
        if (!mFirstLayout) {
            requestLayout();
        }

        if (getPanelState() == PanelState.COLLAPSED) {
            smoothToBottom();
            invalidate();
        }
    }

    public void setSlidingUpPanel(RecyclerView scrollableView, View dimView, ImageButton arrowUp) {
        mScrollableView = scrollableView;
        mArrowUp = arrowUp;
        mDimView = dimView;
        mDimViewColor = mDimView.getSolidColor();
    }

    private void smoothToBottom() {
        smoothSlideTo(0);
    }

    /**
     * Set the draggable view portion. Use to null, to allow the whole panel to be draggable
     *
     * @param dragView A view that will be used to drag the panel.
     */
    private void setDragView(View dragView) {
        if (mDragView != null) {
            mDragView.setOnClickListener(null);
        }
        mDragView = dragView;
        if (mDragView != null) {
            mDragView.setClickable(true);
            mDragView.setFocusable(false);
            mDragView.setFocusableInTouchMode(false);
            mDragView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isEnabled()) return;
                    if (mSlideState != PanelState.EXPANDED) {
                        setPanelState(PanelState.EXPANDED);
                    } else {
                        setPanelState(PanelState.COLLAPSED);
                    }
                }
            });
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY && widthMode != MeasureSpec.AT_MOST) {
            throw new IllegalStateException("Width must have an exact value or MATCH_PARENT");
        } else if (heightMode != MeasureSpec.EXACTLY && heightMode != MeasureSpec.AT_MOST) {
            throw new IllegalStateException("Height must have an exact value or MATCH_PARENT");
        }

        final int childCount = getChildCount();

        if (childCount != 2) {
            throw new IllegalStateException("Sliding up panel layout must have exactly 2 children!");
        }

        mMainView = getChildAt(0);
        mSlideView = getChildAt(1);
        if (mDragView == null) {
            setDragView(mSlideView);
        }

        int layoutHeight = heightSize - getPaddingTop() - getPaddingBottom();
        int layoutWidth = widthSize - getPaddingLeft() - getPaddingRight();

        // First pass. Measure based on child LayoutParams width/height.
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            // We always measure the sliding panel in order to know it's height (needed for show panel)
            if (child.getVisibility() == GONE && i == 0) {
                continue;
            }

            int height = layoutHeight;
            int width = layoutWidth;
            if (child == mMainView) {

                height -= mPanelHeight;

                width -= lp.leftMargin + lp.rightMargin;
            } else if (child == mSlideView) {
                // The sliding view should be aware of its top margin.
                // See https://github.com/umano/AndroidSlidingUpPanel/issues/412.
                height -= lp.topMargin;
            }

            int childWidthSpec;
            if (lp.width == LayoutParams.WRAP_CONTENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
            } else if (lp.width == LayoutParams.MATCH_PARENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            }

            int childHeightSpec;
            if (lp.height == LayoutParams.WRAP_CONTENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
            } else {
                // Modify the height based on the weight.
                if (lp.weight > 0 && lp.weight < 1) {
                    height = (int) (height * lp.weight);
                } else if (lp.height != LayoutParams.MATCH_PARENT) {
                    height = lp.height;
                }
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            }

            child.measure(childWidthSpec, childHeightSpec);

            if (child == mSlideView) {
                mSlideRange = mSlideView.getMeasuredHeight() - mPanelHeight;
            }
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();

        final int childCount = getChildCount();

        if (mFirstLayout) {
            switch (mSlideState) {
                case EXPANDED:
                    mSlideOffset = 1.0f;
                    break;
                default:
                    mSlideOffset = 0.f;
                    break;
            }
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            // Always layout the sliding view on the first layout
            if (child.getVisibility() == GONE && (i == 0 || mFirstLayout)) {
                continue;
            }

            final int childHeight = child.getMeasuredHeight();
            int childTop = paddingTop;

            if (child == mSlideView) {
                childTop = computePanelTopPosition(mSlideOffset);
            }

            if (!mIsSlidingUp) {
                if (child == mMainView) {
                    childTop = computePanelTopPosition(mSlideOffset) + mSlideView.getMeasuredHeight();
                }
            }
            final int childBottom = childTop + childHeight;
            final int childLeft = paddingLeft + lp.leftMargin;
            final int childRight = childLeft + child.getMeasuredWidth();

            child.layout(childLeft, childTop, childRight, childBottom);
        }

        if (mFirstLayout) {
            mFirstLayout = false;
        }


    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        // Recalculate sliding panes and their details
        if (h != oldH) {
            mFirstLayout = true;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // If the scrollable view is handling touch, never intercept
        if (mIsScrollableViewHandlingTouch) {
            mDragHelper.abort();
            return false;
        }

        final int action = ev.getAction();
        final float x = ev.getX();
        final float y = ev.getY();
        final float adx = Math.abs(x - mInitialMotionX);
        final float ady = Math.abs(y - mInitialMotionY);
        final int dragSlop = mDragHelper.getTouchSlop();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mIsUnableToDrag = false;
                mInitialMotionX = x;
                mInitialMotionY = y;
                if (!isViewUnder(mDragView, (int) x, (int) y)) {
                    mDragHelper.cancel();
                    mIsUnableToDrag = true;
                    return false;
                }

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (ady > dragSlop && adx > ady) {
                    mDragHelper.cancel();
                    mIsUnableToDrag = true;
                    return false;
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // If the dragView is still dragging when we get here, we need to call processTouchEvent
                // so that the view is settled
                // Added to make scrollable views work
                if (mDragHelper.isDragging()) {
                    mDragHelper.processTouchEvent(ev);
                    return true;
                }
                break;
        }
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            return super.onTouchEvent(ev);
        }
        try {
            mDragHelper.processTouchEvent(ev);
            return true;
        } catch (Exception ex) {
            // Ignore the pointer out of range exception
            return false;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();

        if (!isEnabled() || (mIsUnableToDrag && action != MotionEvent.ACTION_DOWN)) {
            mDragHelper.abort();
            return super.dispatchTouchEvent(ev);
        }

        final float x = ev.getX();
        final float y = ev.getY();

        if (action == MotionEvent.ACTION_DOWN) {
            mIsScrollableViewHandlingTouch = false;
            mPrevMotionX = x;
            mPrevMotionY = y;
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dx = x - mPrevMotionX;
            float dy = y - mPrevMotionY;
            mPrevMotionX = x;
            mPrevMotionY = y;

            if (Math.abs(dx) > Math.abs(dy)) {
                // Scrolling horizontally, so ignore
                return super.dispatchTouchEvent(ev);
            }

            // If the scroll view isn't under the touch, pass the
            // event along to the dragView.
            if (!isViewUnder(mScrollableView, (int) mInitialMotionX, (int) mInitialMotionY)) {
                return super.dispatchTouchEvent(ev);
            }

            // Which direction (up or down) is the drag moving?
            if (dy * (mIsSlidingUp ? 1 : -1) > 0) { // Collapsing
                // Is the child less than fully scrolled?
                // Then let the child handle it.
                if (mScrollableViewHelper.getRecyclerViewScrollPosition(mScrollableView, mIsSlidingUp) > 0) {
                    mIsScrollableViewHandlingTouch = true;
                    return super.dispatchTouchEvent(ev);
                }

                // Was the child handling the touch previously?
                // Then we need to rejigger things so that the
                // drag panel gets a proper down event.
                if (mIsScrollableViewHandlingTouch) {
                    // Send an 'UP' event to the child.
                    MotionEvent up = MotionEvent.obtain(ev);
                    up.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(up);
                    up.recycle();

                    // Send a 'DOWN' event to the panel. (We'll cheat
                    // and hijack this one)
                    ev.setAction(MotionEvent.ACTION_DOWN);
                }

                mIsScrollableViewHandlingTouch = false;
                return this.onTouchEvent(ev);
            } else if (dy * (mIsSlidingUp ? 1 : -1) < 0) { // Expanding
                // Is the panel less than fully expanded?
                // Then we'll handle the drag here.
                if (mSlideOffset < 1.0f) {
                    mIsScrollableViewHandlingTouch = false;
                    return this.onTouchEvent(ev);
                }

                // Was the panel handling the touch previously?
                // Then we need to rejigger things so that the
                // child gets a proper down event.
                if (!mIsScrollableViewHandlingTouch && mDragHelper.isDragging()) {
                    mDragHelper.cancel();
                    ev.setAction(MotionEvent.ACTION_DOWN);
                }

                mIsScrollableViewHandlingTouch = true;
                return super.dispatchTouchEvent(ev);
            }
        } else if (action == MotionEvent.ACTION_UP) {
            // If the scrollable view was handling the touch and we receive an up
            // we want to clear any previous dragging state so we don't intercept a touch stream accidentally
            if (mIsScrollableViewHandlingTouch) {
                mDragHelper.setDragState(ViewDragHelper.STATE_IDLE);
            }
        }

        // In all other cases, just let the default behavior take over.
        return super.dispatchTouchEvent(ev);
    }

    private boolean isViewUnder(View view, int x, int y) {
        if (view == null) return false;
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + view.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + view.getHeight();
    }

    /*
     * Computes the top position of the panel based on the slide offset.
     */
    private int computePanelTopPosition(float slideOffset) {
        int slidingViewHeight = mSlideView != null ? mSlideView.getMeasuredHeight() : 0;
        int slidePixelOffset = (int) (slideOffset * mSlideRange);
        // Compute the top of the panel if its collapsed
        return mIsSlidingUp
                ? getMeasuredHeight() - getPaddingBottom() - mPanelHeight - slidePixelOffset
                : getPaddingTop() - slidingViewHeight + mPanelHeight + slidePixelOffset;
    }

    /*
     * Computes the slide offset based on the top position of the panel
     */
    private float computeSlideOffset(int topPosition) {
        // Compute the panel top position if the panel is collapsed (offset 0)
        final int topBoundCollapsed = computePanelTopPosition(0);

        // Determine the new slide offset based on the collapsed top position and the new required
        // top position
        return (mIsSlidingUp
                ? (float) (topBoundCollapsed - topPosition) / mSlideRange
                : (float) (topPosition - topBoundCollapsed) / mSlideRange);
    }

    /**
     * Returns the current state of the panel as an enum.
     *
     * @return the current panel state
     */
    public PanelState getPanelState() {
        return mSlideState;
    }

    /**
     * Change panel state to the given state with
     *
     * @param state - new panel state
     */
    public void setPanelState(PanelState state) {

        // Abort any running animation, to allow state change
        if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_SETTLING) {
            mDragHelper.abort();
        }

        if (state == null || state == PanelState.DRAGGING) {
            throw new IllegalArgumentException("Panel state cannot be null or DRAGGING.");
        }
        if (!isEnabled()
                || (!mFirstLayout && mSlideView == null)
                || state == mSlideState
                || mSlideState == PanelState.DRAGGING) return;

        if (mFirstLayout) {
            setPanelStateInternal(state);
        } else {
            switch (state) {
                case COLLAPSED:
                    smoothSlideTo(0);
                    break;
                case EXPANDED:
                    smoothSlideTo(1.0f);
                    break;
            }
        }
    }

    private void setPanelStateInternal(PanelState state) {
        if (mSlideState == state) return;
        mSlideState = state;
    }

    private void onPanelDragged(int newTop) {
        if (mSlideState != PanelState.DRAGGING) {
            mLastNotDraggingSlideState = mSlideState;
        }
        setPanelStateInternal(PanelState.DRAGGING);
        // Recompute the slide offset based on the new top position
        mSlideOffset = computeSlideOffset(newTop);

        // If the slide offset is negative, and overlay is not on, we need to increase the
        // height of the main content
        LayoutParams lp = (LayoutParams) mMainView.getLayoutParams();
        int defaultHeight = getHeight() - getPaddingBottom() - getPaddingTop() - mPanelHeight;

        if (mSlideOffset <= 0) {
            // expand the main view
            lp.height = mIsSlidingUp ? (newTop - getPaddingBottom()) : (getHeight() - getPaddingBottom() - mSlideView.getMeasuredHeight() - newTop);
            if (lp.height == defaultHeight) {
                lp.height = LayoutParams.MATCH_PARENT;
            }
            mMainView.requestLayout();
        } else if (lp.height != LayoutParams.MATCH_PARENT) {
            lp.height = LayoutParams.MATCH_PARENT;
            mMainView.requestLayout();
        }
        applyDim();
    }

    void applyDim() {

        if (mSlideOffset > 0) {
            int coveredFadeColor = 0x99000000;
            final int baseAlpha = (coveredFadeColor & 0xff000000) >>> 24;
            final int iMag = (int) (baseAlpha * mSlideOffset);
            final int color = iMag << 24 | (coveredFadeColor & mDimViewColor);
            mDimView.setBackgroundColor(color);
            int rotationX = (int) (180 * mSlideOffset);
            mArrowUp.setRotationX(rotationX);
        }
    }

    /**
     * Smoothly animate mDraggingPane to the target X position within its range.
     *
     * @param slideOffset position to animate to
     */
    private void smoothSlideTo(float slideOffset) {

        int panelTop = computePanelTopPosition(slideOffset);

        if (mDragHelper.smoothSlideViewTo(mSlideView, mSlideView.getLeft(), panelTop)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void computeScroll() {
        if (mDragHelper != null && mDragHelper.continueSettling(true)) {
            if (!isEnabled()) {
                mDragHelper.abort();
                return;
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putSerializable(SLIDING_STATE, mSlideState != PanelState.DRAGGING ? mSlideState : mLastNotDraggingSlideState);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mSlideState = (PanelState) bundle.getSerializable(SLIDING_STATE);
            mSlideState = mSlideState == null ? DEFAULT_SLIDE_STATE : mSlideState;
            state = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }

    /**
     * Current state of the sliding view.
     */
    public enum PanelState {
        EXPANDED,
        COLLAPSED,
        DRAGGING
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private static final int[] ATTRS = new int[]{
                android.R.attr.layout_weight
        };

        float weight = 0;

        LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
        }

        LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            final TypedArray ta = c.obtainStyledAttributes(attrs, ATTRS);
            if (ta != null) {
                this.weight = ta.getFloat(0, 0);
                ta.recycle();
            }
        }
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return !mIsUnableToDrag && child == mSlideView;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mDragHelper != null && mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                mSlideOffset = computeSlideOffset(mSlideView.getTop());
                if (mSlideOffset == 1) {
                    setPanelStateInternal(PanelState.EXPANDED);
                } else {
                    setPanelStateInternal(PanelState.COLLAPSED);
                }
            }
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            onPanelDragged(top);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xVel, float yVel) {
            int target;

            //An anchor point where the panel can stop during sliding
            float anchorPoint = 1.f;

            // direction is always positive if we are sliding in the expanded direction
            float direction = mIsSlidingUp ? -yVel : yVel;

            if (direction > 0 && mSlideOffset <= anchorPoint / 2) {
                target = computePanelTopPosition(0.0f);
                // swipe up -> expand and stop at anchor point
                // target = computePanelTopPosition(anchorPoint);
            } else if (direction > 0 && mSlideOffset > anchorPoint) {
                // swipe up past anchor -> expand
                target = computePanelTopPosition(1.0f);
            } else if (direction < 0 && mSlideOffset >= anchorPoint) {
                // swipe down -> collapse and stop at anchor point
                target = computePanelTopPosition(anchorPoint);
            } else if (direction < 0 && mSlideOffset < anchorPoint) {
                // swipe down past anchor -> collapse
                target = computePanelTopPosition(0.0f);
            } else if (mSlideOffset >= (1.f + anchorPoint) / 2) {
                // zero velocity, and far enough from anchor point => expand to the top
                target = computePanelTopPosition(1.0f);
            } else if (mSlideOffset >= anchorPoint / 2) {
                // zero velocity, and close enough to anchor point => go to anchor
                target = computePanelTopPosition(anchorPoint);
            } else {
                // settle at the bottom
                target = computePanelTopPosition(0.0f);
            }

            if (mDragHelper != null) {
                mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), target);
            }
            invalidate();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mSlideRange;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int collapsedTop = computePanelTopPosition(0.f);
            final int expandedTop = computePanelTopPosition(1.0f);
            if (mIsSlidingUp) {
                return Math.min(Math.max(top, expandedTop), collapsedTop);
            } else {
                return Math.min(Math.max(top, collapsedTop), expandedTop);
            }
        }
    }
}
