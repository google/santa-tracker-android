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
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

/**
 * Animates score text onto the screen when a game has finished.
 */
public class ScoreTextAnimator {

    private final TextView mScoreView;
    private final ValueAnimator mScaleAnim;
    private final ValueAnimator mAlphaAnim;

    public ScoreTextAnimator(TextView textView) {
        mScoreView = textView;

        // Animate scale from 0 to 1
        mScaleAnim = ValueAnimator.ofFloat(0f, 1f);
        mScaleAnim.setDuration(500);
        mScaleAnim.setInterpolator(new DecelerateInterpolator());
        mScaleAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                mScoreView.setScaleX(val);
                mScoreView.setScaleY(val);
            }
        });

        // Animate alpha from 1 to 0
        mAlphaAnim = ValueAnimator.ofFloat(1f, 0f);
        mAlphaAnim.setDuration(1000);
        mAlphaAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                mScoreView.setAlpha(val);
            }
        });

        // When alpha animation is finished, hide the view
        mAlphaAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mScoreView.setVisibility(View.GONE);
            }
        });
    }

    public void start(String text) {
        // Set initial scale to 0, then show the view
        mScoreView.setScaleX(0f);
        mScoreView.setScaleY(0f);
        mScoreView.setVisibility(View.VISIBLE);

        // Set the text
        mScoreView.setText(text);

        // Play the scale animation, then the alpha animation
        AnimatorSet set = new AnimatorSet();
        set.play(mAlphaAnim).after(mScaleAnim);
        set.start();
    }

}
