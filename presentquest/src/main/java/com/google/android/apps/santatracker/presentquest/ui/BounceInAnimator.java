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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.BounceInterpolator;

/**
 * Provide a "bounce in" effect on a View.
 */
public class BounceInAnimator {

    private static final float SCALE_BIG = 1.5f;
    private static final float SCALE_NORMAL = 1.0f;

    public static void animate(final View view) {
        // Scale up
        view.setScaleX(SCALE_BIG);
        view.setScaleY(SCALE_BIG);

        // Animate back down with a bounce-in effect
        final ValueAnimator animator = ValueAnimator.ofFloat(SCALE_BIG, SCALE_NORMAL);
        animator.setDuration(1000);
        animator.setInterpolator(new BounceInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                view.setScaleX(value);
                view.setScaleY(value);
            }
        });

        // On end, return scale to normal
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setScaleX(SCALE_NORMAL);
                view.setScaleY(SCALE_NORMAL);
            }
        });

        animator.start();
    }
}
