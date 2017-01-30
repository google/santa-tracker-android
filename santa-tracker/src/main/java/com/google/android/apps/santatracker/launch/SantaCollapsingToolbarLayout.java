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

import android.animation.FloatEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.design.widget.CollapsingToolbarLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.android.apps.santatracker.village.SnowFlakeView;

public class SantaCollapsingToolbarLayout extends CollapsingToolbarLayout {

    private static final String TAG = "SantaCollapsing";

    private static final float TRANSPARENT = 0.0f;
    private static final float OPAQUE = 1.0f;

    private boolean mScrimShown = false;

    private View mToolbarContentView;
    private View mOverlayView;
    private SnowFlakeView mSnowFlakeView;

    private ObjectAnimator mToolbarAnimator;
    private ObjectAnimator mOverlayAnimator;

    public SantaCollapsingToolbarLayout(Context context) {
        super(context);
    }

    public SantaCollapsingToolbarLayout(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public SantaCollapsingToolbarLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setScrimsShown(boolean shown) {
        super.setScrimsShown(shown);

        // No change
        if (shown == mScrimShown) {
            return;
        }

        if (shown) {
            Log.d(TAG, "setScrimShown:showing");
            animateToolbar(TRANSPARENT, OPAQUE);
            mSnowFlakeView.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "setScrimShown:hiding");
            animateToolbar(OPAQUE, TRANSPARENT);
            mSnowFlakeView.setVisibility(View.VISIBLE);
        }

        mScrimShown = shown;
    }

    private void animateToolbar(float fromAlpha, float toAlpha) {
        if (mToolbarAnimator == null) {
            mToolbarAnimator = ObjectAnimator.ofObject(mToolbarContentView, "alpha", new FloatEvaluator(),
                    fromAlpha, toAlpha)
                    .setDuration(600);
        }

        if (mOverlayAnimator == null) {
            mOverlayAnimator = ObjectAnimator.ofObject(mOverlayView, "alpha", new FloatEvaluator(),
                    fromAlpha, toAlpha)
                    .setDuration(600);
        }

        startAnimator(mOverlayAnimator, fromAlpha, toAlpha);
        startAnimator(mToolbarAnimator, fromAlpha, toAlpha);
    }

    private void startAnimator(ObjectAnimator animator, float from, float to) {
        if (animator.isRunning()) {
            animator.cancel();
        }

        animator.setFloatValues(from, to);
        animator.start();
    }


    public void setToolbarContentView(View toolbarContentView) {
        mToolbarContentView = toolbarContentView;
    }

    public void setOverlayView(View overlayView) {
        mOverlayView = overlayView;
    }

    public void setOverlayColor(int colorResource) {
        mOverlayView.setBackgroundResource(colorResource);
    }

    public void setSnowFlakeView(SnowFlakeView snowFlakeView) {
        mSnowFlakeView = snowFlakeView;
    }
}
