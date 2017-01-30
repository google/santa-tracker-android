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
package com.google.android.apps.santatracker.presentquest.ui;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Pulses a view that we want the user to click.
 */
public class ClickMeAnimator {

    final float SCALE_BIG = 1.1f;
    final float SCALE_NORMAL = 1.0f;
    final float SCALE_SMALL = 0.9f;

    private ValueAnimator mAnimator;
    private View mView;

    private boolean mIsRunning = false;

    public ClickMeAnimator(final View view) {
        mView = view;
        mAnimator = ValueAnimator.ofFloat(SCALE_BIG, SCALE_SMALL);

        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mView.setScaleX(value);
                mView.setScaleY(value);
            }
        });

        mAnimator.setDuration(600);
        mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        mAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
    }

    public void start() {
        if (mIsRunning) {
            return;
        }

        mAnimator.start();
        mIsRunning = true;
    }

    public void stop() {
        if (!mIsRunning) {
            return;
        }

        mAnimator.cancel();
        mIsRunning = false;

        mView.setScaleX(SCALE_NORMAL);
        mView.setScaleY(SCALE_NORMAL);
    }

}
