/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.android.apps.santatracker.map.cardstream;

import android.animation.TimeAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.support.v17.leanback.R;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * Animators for highlighting behavior when an item gains focus.
 */
class TvFocusAnimator {

    private static final int DURATION_MS = 150;

    private void onItemFocused(View view, boolean hasFocus) {
        view.setSelected(hasFocus);
        getOrCreateAnimator(view).animateFocus(hasFocus, false);
    }

    void onInitializeView(View view) {
        getOrCreateAnimator(view).animateFocus(false, true);
    }

    private FocusAnimator getOrCreateAnimator(View view) {
        FocusAnimator animator = (FocusAnimator) view.getTag(R.id.lb_focus_animator);
        if (animator == null) {
            final float scale = view.getResources().getFraction(
                    R.fraction.lb_focus_zoom_factor_xsmall, 1, 1);
            animator = new FocusAnimator(view, scale, DURATION_MS);
            view.setTag(R.id.lb_focus_animator, animator);
        }
        return animator;
    }

    static final class FocusChangeListener implements View.OnFocusChangeListener {

        final private TvFocusAnimator mAnimator;

        FocusChangeListener(TvFocusAnimator animator) {
            mAnimator = animator;
        }

        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            mAnimator.onItemFocused(view, hasFocus);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static class FocusAnimator implements TimeAnimator.TimeListener {
        private final View mView;
        private final int mDuration;
        private final float mScaleDiff;
        private final TimeAnimator mAnimator = new TimeAnimator();
        private final Interpolator mInterpolator = new AccelerateDecelerateInterpolator();
        private float mFocusLevel = 0f;
        private float mFocusLevelStart;
        private float mFocusLevelDelta;

        FocusAnimator(View view, float scale, int duration) {
            mView = view;
            mDuration = duration;
            mScaleDiff = scale - 1f;
            mAnimator.setTimeListener(this);
        }

        void animateFocus(boolean select, boolean immediate) {
            endAnimation();
            final float end = select ? 1 : 0;
            if (immediate) {
                setFocusLevel(end);
            } else if (mFocusLevel != end) {
                mFocusLevelStart = mFocusLevel;
                mFocusLevelDelta = end - mFocusLevelStart;
                mAnimator.start();
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        void setFocusLevel(float level) {
            mFocusLevel = level;
            float scale = 1f + mScaleDiff * level;
            mView.setElevation(5 * scale);
            mView.setScaleX(scale);
            mView.setScaleY(scale);
        }

        void endAnimation() {
            mAnimator.end();
        }

        @Override
        public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
            float fraction;
            if (totalTime >= mDuration) {
                fraction = 1;
                mAnimator.end();
            } else {
                fraction = (float) (totalTime / (double) mDuration);
            }
            if (mInterpolator != null) {
                fraction = mInterpolator.getInterpolation(fraction);
            }
            setFocusLevel(mFocusLevelStart + fraction * mFocusLevelDelta);
        }
    }
}