/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.tracker.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;
import com.google.android.apps.santatracker.tracker.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

/**
 * An interaction behavior plugin for a child view of {@link CoordinatorLayout} to make it work as a
 * bottom sheet.
 */
public class BottomSheetBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {

    /** Listener for monitoring events about bottom sheets. */
    public abstract static class BottomSheetListener {

        /**
         * Called when the bottom sheet changes its state.
         *
         * @param newState The new state. This will be one of {@link #STATE_DRAGGING}, {@link
         *     #STATE_SETTLING}, {@link #STATE_EXPANDED}, {@link #STATE_COLLAPSED}, or {@link
         *     #STATE_HIDDEN}.
         */
        public abstract void onStateChanged(@State int newState);

        /**
         * Called when the bottom sheet is being dragged.
         *
         * @param slideOffset The new offset of this bottom sheet within its range, from 0 to 1 when
         *     it is moving upward, and from 0 to -1 when it moving downward.
         */
        public abstract void onSlide(float slideOffset);
    }

    /** The bottom sheet is dragging. */
    public static final int STATE_DRAGGING = 1;

    /** The bottom sheet is settling. */
    public static final int STATE_SETTLING = 2;

    /** The bottom sheet is expanded. */
    public static final int STATE_EXPANDED = 3;

    /** The bottom sheet is collapsed. */
    public static final int STATE_COLLAPSED = 4;

    /** The bottom sheet is hidden. */
    public static final int STATE_HIDDEN = 5;

    /** @hide */
    @IntDef({STATE_EXPANDED, STATE_COLLAPSED, STATE_DRAGGING, STATE_SETTLING, STATE_HIDDEN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {}

    private static final float HIDE_THRESHOLD = 0.5f;

    private static final float HIDE_FRICTION = 0.1f;

    // Whether to enable workaround for black non-rendered square
    //    private static final boolean NEEDS_INVALIDATING = Build.VERSION.SDK_INT < 23;

    private float mMaximumVelocity;

    private int mPeekHeight;

    private int mHiddenPeekHeight;

    private int mMinOffset;

    private int mMaxOffset;

    private boolean mHideable;

    @State private int mState = STATE_COLLAPSED;

    private ViewDragHelper mViewDragHelper;

    private boolean mIgnoreEvents;

    private int mLastNestedScrollDy;

    private boolean mNestedScrolled;

    private int mParentHeight;

    private WeakReference<V> mViewRef;

    WeakReference<View> mNestedScrollingChildRef;

    private BottomSheetListener mListener;

    private VelocityTracker mVelocityTracker;

    private int mActivePointerId;

    private int mInitialY;

    boolean mTouchingScrollingChild;

    /** Default constructor for instantiating BottomSheetBehaviors. */
    public BottomSheetBehavior() {}

    /**
     * Default constructor for inflating BottomSheetBehaviors from layout.
     *
     * @param context The {@link Context}.
     * @param attrs The {@link AttributeSet}.
     */
    public BottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a =
                context.obtainStyledAttributes(attrs, R.styleable.BottomSheetBehavior_Params);
        setPeekHeight(
                a.getDimensionPixelSize(
                        R.styleable.BottomSheetBehavior_Params_santa_behavior_peekHeight, 0));
        setHideable(
                a.getBoolean(
                        R.styleable.BottomSheetBehavior_Params_santa_behavior_hideable, false));
        setHiddenPeekHeight(
                a.getDimensionPixelSize(
                        R.styleable.BottomSheetBehavior_Params_santa_behavior_hiddenPeekHeight, 0));
        a.recycle();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @Override
    public Parcelable onSaveInstanceState(CoordinatorLayout parent, V child) {
        return new SavedState(super.onSaveInstanceState(parent, child), mState);
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(parent, child, ss.getSuperState());
        // Intermediate states are restored as collapsed state
        if (ss.state == STATE_DRAGGING || ss.state == STATE_SETTLING) {
            mState = STATE_COLLAPSED;
        } else {
            mState = ss.state;
        }
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            child.setFitsSystemWindows(true);
        }
        int savedTop = child.getTop();
        // First let the parent lay it out
        parent.onLayoutChild(child, layoutDirection);
        // Offset the bottom sheet
        mParentHeight = parent.getHeight();
        mMinOffset = Math.max(0, mParentHeight - child.getHeight());
        mMaxOffset = mParentHeight - mPeekHeight;
        if (mState == STATE_EXPANDED) {
            ViewCompat.offsetTopAndBottom(child, mMinOffset);
        } else if (mHideable && mState == STATE_HIDDEN) {
            ViewCompat.offsetTopAndBottom(child, mParentHeight - mHiddenPeekHeight);
        } else if (mState == STATE_COLLAPSED) {
            ViewCompat.offsetTopAndBottom(child, mMaxOffset);
        } else if (mState == STATE_DRAGGING || mState == STATE_SETTLING) {
            ViewCompat.offsetTopAndBottom(child, savedTop - child.getTop());
        }
        if (mViewDragHelper == null) {
            mViewDragHelper = ViewDragHelper.create(parent, mDragCallback);
        }
        mViewRef = new WeakReference<>(child);
        mNestedScrollingChildRef = new WeakReference<>(findScrollingChild(child));
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown()) {
            mIgnoreEvents = true;
            return false;
        }
        int action = event.getActionMasked();
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchingScrollingChild = false;
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                // Reset the ignore flag
                if (mIgnoreEvents) {
                    mIgnoreEvents = false;
                    return false;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                int initialX = (int) event.getX();
                mInitialY = (int) event.getY();
                View scroll =
                        mNestedScrollingChildRef != null ? mNestedScrollingChildRef.get() : null;
                if (scroll != null && parent.isPointInChildBounds(scroll, initialX, mInitialY)) {
                    mActivePointerId = event.getPointerId(event.getActionIndex());
                    mTouchingScrollingChild = true;
                }
                mIgnoreEvents =
                        mActivePointerId == MotionEvent.INVALID_POINTER_ID
                                && !parent.isPointInChildBounds(child, initialX, mInitialY);
                break;
        }
        if (!mIgnoreEvents && mViewDragHelper.shouldInterceptTouchEvent(event)) {
            return true;
        }
        // We have to handle cases that the ViewDragHelper does not capture the bottom sheet because
        // it is not the top most view of its parent. This is not necessary when the touch event is
        // happening over the scrolling content as nested scrolling logic handles that case.
        View scroll = mNestedScrollingChildRef.get();
        return action == MotionEvent.ACTION_MOVE
                && scroll != null
                && !mIgnoreEvents
                && mState != STATE_DRAGGING
                && !parent.isPointInChildBounds(scroll, (int) event.getX(), (int) event.getY())
                && Math.abs(mInitialY - event.getY()) > mViewDragHelper.getTouchSlop();
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown()) {
            return false;
        }
        int action = event.getActionMasked();
        if (mState == STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (mViewDragHelper != null) {
            mViewDragHelper.processTouchEvent(event);
        }
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        // The ViewDragHelper tries to capture only the top-most View. We have to explicitly tell it
        // to capture the bottom sheet in case it is not captured and the touch slop is passed.
        if (action == MotionEvent.ACTION_MOVE && !mIgnoreEvents) {
            if (Math.abs(mInitialY - event.getY()) > mViewDragHelper.getTouchSlop()) {
                mViewDragHelper.captureChildView(child, event.getPointerId(event.getActionIndex()));
            }
        }
        return !mIgnoreEvents;
    }

    @Override
    public boolean onStartNestedScroll(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child,
            @NonNull View directTargetChild,
            @NonNull View target,
            int axes,
            int type) {
        mLastNestedScrollDy = 0;
        mNestedScrolled = false;
        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedPreScroll(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child,
            @NonNull View target,
            int dx,
            int dy,
            @NonNull int[] consumed,
            int type) {
        View scrollingChild = mNestedScrollingChildRef.get();
        if (target != scrollingChild) {
            return;
        }
        int currentTop = child.getTop();
        int newTop = currentTop - dy;
        if (dy > 0) { // Upward
            if (newTop < mMinOffset) {
                consumed[1] = currentTop - mMinOffset;
                ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                setStateInternal(STATE_EXPANDED);
            } else {
                consumed[1] = dy;
                ViewCompat.offsetTopAndBottom(child, -dy);
                setStateInternal(STATE_DRAGGING);
            }
        } else if (dy < 0) { // Downward
            if (!target.canScrollVertically(-1)) {
                if (newTop <= mMaxOffset) {
                    consumed[1] = dy;
                    ViewCompat.offsetTopAndBottom(child, -dy);
                    setStateInternal(STATE_DRAGGING);
                } else if (mHideable) {
                    if (newTop <= mParentHeight - mHiddenPeekHeight) {
                        consumed[1] = dy;
                        ViewCompat.offsetTopAndBottom(child, -dy);
                        setStateInternal(STATE_DRAGGING);
                    } else {
                        consumed[1] = currentTop - (mParentHeight - mHiddenPeekHeight);
                        ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                        setStateInternal(STATE_HIDDEN);
                    }
                } else {
                    consumed[1] = currentTop - mMaxOffset;
                    ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                    setStateInternal(STATE_COLLAPSED);
                }
            }
        }
        dispatchOnSlide(child.getTop());
        mLastNestedScrollDy = dy;
        mNestedScrolled = true;
    }

    @Override
    public void onStopNestedScroll(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child,
            @NonNull View target,
            int type) {
        if (child.getTop() == mMinOffset) {
            setStateInternal(STATE_EXPANDED);
            mNestedScrolled = false;
            return;
        }
        if (mNestedScrollingChildRef == null
                || target != mNestedScrollingChildRef.get()
                || !mNestedScrolled) {
            return;
        }
        int top;
        int targetState;
        if (mLastNestedScrollDy > 0) { // Upward
            if (child.getTop() > mMaxOffset) {
                top = mMaxOffset;
                targetState = STATE_COLLAPSED;
            } else {
                top = mMinOffset;
                targetState = STATE_EXPANDED;
            }
        } else if (mHideable && shouldHide(child, getYVelocity())) { // Downward (hide)
            top = mParentHeight - mHiddenPeekHeight;
            targetState = STATE_HIDDEN;
        } else {
            top = mMaxOffset;
            targetState = STATE_COLLAPSED;
        }
        if (mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), top)) {
            setStateInternal(STATE_SETTLING);
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, targetState));
        } else {
            setStateInternal(targetState);
        }
        mNestedScrolled = false;
    }

    @Override
    public boolean onNestedPreFling(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child,
            @NonNull View target,
            float velocityX,
            float velocityY) {
        return target == mNestedScrollingChildRef.get()
                && (mState != STATE_EXPANDED
                        || super.onNestedPreFling(
                                coordinatorLayout, child, target, velocityX, velocityY));
    }

    /**
     * Sets the height of the bottom sheet when it is collapsed.
     *
     * @param peekHeight The height of the collapsed bottom sheet in pixels.
     */
    public final void setPeekHeight(int peekHeight) {
        mPeekHeight = Math.max(0, peekHeight);
        mMaxOffset = mParentHeight - peekHeight;
    }

    /**
     * Gets the height of the bottom sheet when it is collapsed.
     *
     * @return The height of the collapsed bottom sheet.
     */
    public final int getPeekHeight() {
        return mPeekHeight;
    }

    /**
     * Sets whether this bottom sheet can hide when it is swiped down.
     *
     * @param hideable {@code true} to make this bottom sheet hideable.
     */
    public void setHideable(boolean hideable) {
        mHideable = hideable;
    }

    /**
     * Gets whether this bottom sheet can hide when it is swiped down.
     *
     * @return {@code true} if this bottom sheet can hide.
     */
    public boolean isHideable() {
        return mHideable;
    }

    public void setHiddenPeekHeight(int hiddenPeekHeight) {
        mHiddenPeekHeight = hiddenPeekHeight;
    }

    public int getHiddenPeekHeight() {
        return mHiddenPeekHeight;
    }

    /**
     * Sets a listener to be notified of bottom sheet events.
     *
     * @param listener The listener to notify when bottom sheet events occur.
     */
    public void setBottomSheetListener(BottomSheetListener listener) {
        mListener = listener;
    }

    /**
     * Sets the state of the bottom sheet. The bottom sheet will transition to that state with
     * animation.
     *
     * @param state One of {@link #STATE_COLLAPSED}, {@link #STATE_EXPANDED}, or {@link
     *     #STATE_HIDDEN}.
     */
    public final void setState(@State int state) {
        V child = mViewRef.get();
        if (child == null) {
            return;
        }
        int top;
        if (state == STATE_COLLAPSED) {
            top = mMaxOffset;
        } else if (state == STATE_EXPANDED) {
            top = mMinOffset;
        } else if (mHideable && state == STATE_HIDDEN) {
            top = mParentHeight - mHiddenPeekHeight;
        } else {
            throw new IllegalArgumentException("Illegal state argument: " + state);
        }
        setStateInternal(STATE_SETTLING);
        if (mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), top)) {
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, state));
        }
    }

    /**
     * Gets the current state of the bottom sheet.
     *
     * @return One of {@link #STATE_EXPANDED}, {@link #STATE_COLLAPSED}, {@link #STATE_DRAGGING},
     *     and {@link #STATE_SETTLING}.
     */
    @State
    public final int getState() {
        return mState;
    }

    private void setStateInternal(@State int state) {
        if (mState == state) {
            return;
        }
        mState = state;
        if (mListener != null) {
            mListener.onStateChanged(state);
        }
    }

    private void reset() {
        mActivePointerId = ViewDragHelper.INVALID_POINTER;
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private boolean shouldHide(View child, float yvel) {
        if (child.getTop() < mMaxOffset) {
            // It should not hide, but collapse.
            return false;
        }
        final float newTop = child.getTop() + yvel * HIDE_FRICTION;
        return Math.abs(newTop - mMaxOffset) / (float) mPeekHeight > HIDE_THRESHOLD;
    }

    private View findScrollingChild(View view) {
        if (ViewCompat.isNestedScrollingEnabled(view)) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View scrollingChild = findScrollingChild(group.getChildAt(i));
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }
        return null;
    }

    private float getYVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
        return mVelocityTracker.getYVelocity(mActivePointerId);
    }

    private final ViewDragHelper.Callback mDragCallback =
            new ViewDragHelper.Callback() {

                @Override
                public boolean tryCaptureView(View child, int pointerId) {
                    if (mState == STATE_DRAGGING) {
                        return false;
                    }
                    if (mTouchingScrollingChild) {
                        return false;
                    }
                    if (mState == STATE_EXPANDED && mActivePointerId == pointerId) {
                        View scroll = mNestedScrollingChildRef.get();
                        if (scroll != null && scroll.canScrollVertically(-1)) {
                            // Let the content scroll up
                            return false;
                        }
                    }
                    return mViewRef != null && mViewRef.get() == child;
                }

                @Override
                public void onViewPositionChanged(
                        View changedView, int left, int top, int dx, int dy) {
                    dispatchOnSlide(top);
                }

                @Override
                public void onViewDragStateChanged(int state) {
                    if (state == ViewDragHelper.STATE_DRAGGING) {
                        setStateInternal(STATE_DRAGGING);
                    }
                }

                @Override
                public void onViewReleased(View releasedChild, float xvel, float yvel) {
                    int top;
                    @State int targetState;
                    if (yvel < 0) { // Moving up
                        top = mMinOffset;
                        targetState = STATE_EXPANDED;
                    } else if (mHideable && shouldHide(releasedChild, yvel)) {
                        top = mParentHeight - mHiddenPeekHeight;
                        targetState = STATE_HIDDEN;
                    } else {
                        top = mMaxOffset;
                        targetState = STATE_COLLAPSED;
                    }
                    if (mViewDragHelper.settleCapturedViewAt(releasedChild.getLeft(), top)) {
                        setStateInternal(STATE_SETTLING);
                        ViewCompat.postOnAnimation(
                                releasedChild, new SettleRunnable(releasedChild, targetState));
                    } else {
                        setStateInternal(targetState);
                    }
                }

                @Override
                public int clampViewPositionVertical(View child, int top, int dy) {
                    return constrain(
                            top,
                            mMinOffset,
                            mHideable ? mParentHeight - mHiddenPeekHeight : mMaxOffset);
                }

                @Override
                public int clampViewPositionHorizontal(View child, int left, int dx) {
                    return child.getLeft();
                }

                @Override
                public int getViewVerticalDragRange(View child) {
                    if (mHideable) {
                        return mParentHeight - mHiddenPeekHeight;
                    } else {
                        return mMaxOffset - mMinOffset;
                    }
                }
            };

    private void dispatchOnSlide(int top) {
        if (mListener != null) {
            if (top > mMaxOffset) {
                mListener.onSlide((float) (mMaxOffset - top) / mPeekHeight);
            } else {
                mListener.onSlide((float) (mMaxOffset - top) / ((mMaxOffset - mMinOffset)));
            }
        }
    }

    private class SettleRunnable implements Runnable {

        private final View mView;

        @State private final int mTargetState;

        SettleRunnable(View view, @State int targetState) {
            mView = view;
            mTargetState = targetState;
        }

        @Override
        public void run() {
            if (mViewDragHelper != null && mViewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this);
            } else {
                setStateInternal(mTargetState);
            }
        }
    }

    protected static class SavedState extends View.BaseSavedState {

        @State final int state;

        public SavedState(Parcel source) {
            super(source);
            //noinspection ResourceType
            state = source.readInt();
        }

        public SavedState(Parcelable superState, @State int state) {
            super(superState);
            this.state = state;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(state);
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel source) {
                        return new SavedState(source);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    /**
     * A utility function to get the {@link BottomSheetBehavior} associated with the {@code view}.
     *
     * @param view The {@link View} with {@link BottomSheetBehavior}.
     * @return The {@link BottomSheetBehavior} associated with the {@code view}.
     */
    @SuppressWarnings("unchecked")
    public static <V extends View> BottomSheetBehavior<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior behavior =
                ((CoordinatorLayout.LayoutParams) params).getBehavior();
        if (!(behavior instanceof BottomSheetBehavior)) {
            throw new IllegalArgumentException(
                    "The view is not associated with BottomSheetBehavior");
        }
        return (BottomSheetBehavior<V>) behavior;
    }

    private static int constrain(int amount, int low, int high) {
        return amount < low ? low : (amount > high ? high : amount);
    }
}
