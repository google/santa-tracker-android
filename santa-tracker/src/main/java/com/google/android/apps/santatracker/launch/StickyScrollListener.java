/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.launch;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Scroll listener that adjusts the RecyclerView after a scroll event to make sure that the
 * top list item is fully visible and not obscured.
 */
public class StickyScrollListener extends RecyclerView.OnScrollListener {

    private static final String TAG = "StickyScrollListener";

    /**
     * Cutoff for stickiness. If less than this fraction of the obscured view can be seen, the
     * RecyclerView is scrolled to a more visible view. Otherwise, this view is scrolled to be
     * fully visible.
     */
    private static final float VISIBILITY_CUTOFF = 0.60f;

    private static final int UP = 1;
    private static final int DOWN = 2;

    private int mScrollDirection = 0;

    /** LinearLayoutManager controlling the RecyclerView **/
    private LinearLayoutManager mManager;

    /** Number of columns displayed by the Manager **/
    private int mNumColumns = 1;

    /** Scroll state of the RecyclerView **/
    private int mScrollState = RecyclerView.SCROLL_STATE_IDLE;

    /** Position of the first completely visible view **/
    private int topVisiblePos;

    /** Position of the view above the first completely visible view **/
    private int topObscuredPos;

    /** Position of the last completely visible view **/
    private int bottomVisiblePos;

    /** Position of the view below the last completely visible view **/
    private int bottomObscuredPos;

    /** View above the first completely visible View **/
    private View topObscuredView;

    /** View below the last completely visible View **/
    private View bottomObscuredView;

    /** Percent of topObscuredView that is visible **/
    private float topObscuredPercentVisible;

    /** Percent of bottomObscuredView that is visible **/
    private float bottomObscuredPercentVisible;

    public StickyScrollListener(LinearLayoutManager manager, int numColumns) {
        this.mManager = manager;
        this.mNumColumns = numColumns;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        topVisiblePos = mManager.findFirstCompletelyVisibleItemPosition();
        topObscuredPos = topVisiblePos - mNumColumns;
        topObscuredView = mManager.findViewByPosition(topObscuredPos);

        bottomVisiblePos = mManager.findLastCompletelyVisibleItemPosition();
        bottomObscuredPos = bottomVisiblePos + mNumColumns;
        bottomObscuredView = mManager.findViewByPosition(bottomObscuredPos);

        if (topObscuredView != null) {
            // Calculate how many pixels of the obscured view are visible.
            float topObscuredPixelsVisible = (topObscuredView.getHeight() + topObscuredView.getY());

            // Calculate what percentage of the obscured view is visible.
            topObscuredPercentVisible = topObscuredPixelsVisible / topObscuredView.getHeight();
        } else {
            clearTop();
        }

        if (bottomObscuredView != null) {
            // Same calculation for bottom
            float bottomObscuredPixelsVisible = (recyclerView.getHeight() - bottomObscuredView.getY());
            bottomObscuredPercentVisible = bottomObscuredPixelsVisible / bottomObscuredView.getHeight();
        } else {
            clearBottom();
        }

        // Mark scroll direction
        if (dy <= 0) {
            mScrollDirection = UP;
        } else {
            mScrollDirection = DOWN;
        }
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        // This detects the end of a non-fling drag
        boolean stoppedDragging = mScrollState == RecyclerView.SCROLL_STATE_DRAGGING &&
                newState == RecyclerView.SCROLL_STATE_IDLE;

        boolean stoppedFlinging = mScrollState == RecyclerView.SCROLL_STATE_SETTLING &&
                newState == RecyclerView.SCROLL_STATE_IDLE;

        if (stoppedDragging || stoppedFlinging) {
            if (mScrollDirection == DOWN) {
                if (topObscuredPercentVisible <= VISIBILITY_CUTOFF) {
                    // Scroll down to the top of the first completely visible view.
                    float abovePixelsVisible = topObscuredView.getY() + topObscuredView.getHeight();
                    recyclerView.smoothScrollBy(0, (int) abovePixelsVisible);
                } else if (topObscuredPos >= 0) {
                    // Scroll up to the top of the view above the first completely visible view.
                    recyclerView.smoothScrollBy(0, (int) topObscuredView.getY());
                }
            }
        }

        // Mark the scroll state to avoid duplicate event detection.
        mScrollState = newState;
    }


    private void clearTop() {
        topObscuredView = null;
        topObscuredPos = -1;
        topVisiblePos = -1;
        topObscuredPercentVisible = 1.0f;
    }

    private void clearBottom() {
        bottomObscuredView = null;
        bottomObscuredPos = -1;
        bottomVisiblePos = -1;
        bottomObscuredPercentVisible = 1.0f;
    }
}

