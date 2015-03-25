/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

import com.google.android.apps.santatracker.R;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * TODO(macd,jfschmakeit): add javadoc here.
 */
public class MarkerManager extends RecyclerView.Adapter<MarkerManager.ViewHolder> {

    public static final int SANTA = 0;
    public static final int VIDEO01 = 1;
    public static final int GUMBALL = 2;
    public static final int MEMORY = 3;
    public static final int JETPACK = 4;
    public static final int VIDEO15 = 5;
    public static final int VIDEO23 = 6;
    public static final int NUM_PINS = 7;

    private AbstractLaunch[] mLaunchers = new AbstractLaunch[NUM_PINS];
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;

    public MarkerManager() {
    }

    public void initialise(StartupActivity.SantaContext santaContext,
            final RecyclerView recyclerView) {
        // launcher pins
        if (mLaunchers[SANTA] == null) {
            mLaunchers[SANTA] = new LaunchSanta(santaContext);
            mLaunchers[VIDEO01] = new LaunchVideo(santaContext, 0, R.color.SantaYellow, 1);
            mLaunchers[GUMBALL] = new LaunchGumball(santaContext);
            mLaunchers[MEMORY] = new LaunchMemory(santaContext);
            mLaunchers[JETPACK] = new LaunchJetpack(santaContext);
            mLaunchers[VIDEO15] = new LaunchVideo(santaContext, R.drawable.marker_badge_locked_15,
                    R.color.SantaGreen, 15);
            mLaunchers[VIDEO23] = new LaunchVideo(santaContext, R.drawable.marker_badge_locked_23,
                    R.color.SantaOrange, 23);
        } else {
            // reset states only upon restart
            for (int i = 0; i < mLaunchers.length; i++) {
                mLaunchers[i].setContext(santaContext);
            }
        }

        mLayoutManager = new LinearLayoutManager(santaContext.getContext());
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView = recyclerView;
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(mLaunchers.length);

        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            boolean mForward = true;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // Snap to show partial markers if two or more fit on screen
                    if (recyclerView.getWidth() >= recyclerView.getChildAt(0).getWidth() * 2) {
                        if (mForward) {
                            if (mLayoutManager.findLastVisibleItemPosition() !=
                                    mLayoutManager.findLastCompletelyVisibleItemPosition()) {
                                recyclerView.smoothScrollToPosition(
                                        mLayoutManager.findLastVisibleItemPosition());
                            }
                        } else {
                            if (mLayoutManager.findFirstVisibleItemPosition() !=
                                    mLayoutManager.findFirstCompletelyVisibleItemPosition()) {
                                recyclerView.smoothScrollToPosition(
                                        mLayoutManager.findFirstVisibleItemPosition());
                            }
                        }
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dx >= 0) {
                    mForward = true;
                } else {
                    mForward = false;
                }
            }
        });

        // Start the scrollview one screen to the right and scroll back to show that the
        // view is scrollable
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                // RecyclerView might be null if the screen is off (launch via Android Studio)
                // and we need to check child count to ensure the LayoutManager doesnhas an internal
                // OrientationHelper, otherwise we NPE here.
                if (mRecyclerView != null && mRecyclerView.getChildCount() > 0) {
                    mRecyclerView
                            .scrollToPosition(mLayoutManager.findLastVisibleItemPosition() + 2);
                    mRecyclerView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mRecyclerView.smoothScrollToPosition(0);
                        }
                    }, 250);
                }
            }
        });
    }

    public AbstractLaunch[] getLaunchers() {
        return mLaunchers;
    }

    public AbstractLaunch getLauncher(int markerId) {
        return mLaunchers[markerId];
    }

    // Return the number of pins (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mLaunchers.length;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
            int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_village_markers, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mLauncher = mLaunchers[position];
        holder.mLauncher.attachToView(holder.mImage);
        holder.mLauncher.applyState();
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.mLauncher.detachFromView();
        holder.mLauncher = null;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {

        // each data item is just a string in this case
        public AbstractLaunch mLauncher;
        public MarkerView mImage;

        public ViewHolder(View v) {
            super(v);
            mImage = (MarkerView) v.findViewById(R.id.marker);
        }
    }
}
