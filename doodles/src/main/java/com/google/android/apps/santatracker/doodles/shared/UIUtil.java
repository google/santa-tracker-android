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
package com.google.android.apps.santatracker.doodles.shared;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

/**
 * Utility class for working with Android views.
 */
public final class UIUtil {

  private UIUtil() {
    // Don't instantiate this class.
  }

  /**
   * Shortcut to create a ValuesAnimator with the given configuration.
   */
  public static ValueAnimator animator(long durationMillis, TimeInterpolator interpolator,
      AnimatorUpdateListener listener, PropertyValuesHolder... propertyValuesHolders) {
    ValueAnimator tween = ValueAnimator.ofPropertyValuesHolder(propertyValuesHolders);
    tween.setDuration(durationMillis);
    tween.setInterpolator(interpolator);
    tween.addUpdateListener(listener);
    return tween;
  }

  /**
   * Shortcut for making a PropertyValuesHolder for floats.
   */
  public static PropertyValuesHolder floatValue(String name, float start, float end) {
    return PropertyValuesHolder.ofFloat(name, start, end);
  }

  public static void fadeOutAndHide(final View v, long durationMs, float startAlpha,
      final Runnable onFinishRunnable) {

    if (v.getVisibility() != View.VISIBLE) {
      return; // Already hidden.
    }
    ValueAnimator fadeOut = animator(durationMs,
        new AccelerateDecelerateInterpolator(),
        new AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            v.setAlpha((float) valueAnimator.getAnimatedValue("alpha"));
          }
        },
        floatValue("alpha", startAlpha, 0)
    );
    fadeOut.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        v.setVisibility(View.INVISIBLE);
        if (onFinishRunnable != null) {
          onFinishRunnable.run();
        }
      }
    });
    fadeOut.start();
  }
  public static void fadeOutAndHide(final View v, long durationMs, float startAlpha) {
    fadeOutAndHide(v, durationMs, startAlpha, null);
  }

  public static void fadeOutAndHide(final View v, long durationMs) {
    fadeOutAndHide(v, durationMs, 1);
  }

  public static void showAndFadeIn(final View v, long durationMs, float endAlpha) {
    if (v.getVisibility() == View.VISIBLE) {
      return; // Already visible.
    }
    v.setAlpha(0);
    v.setVisibility(View.VISIBLE);

    ValueAnimator fadeIn = animator(durationMs,
        new AccelerateDecelerateInterpolator(),
        new AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            v.setAlpha((float) valueAnimator.getAnimatedValue("alpha"));
          }
        },
        floatValue("alpha", 0, endAlpha)
    );
    fadeIn.start();
  }

  public static void showAndFadeIn(final View v, long durationMs) {
    showAndFadeIn(v, durationMs, 1);
  }

  public static void fitToBounds(TextView textView, float widthPx, float heightPx) {
    textView.measure(0, 0);
    float currentWidthPx = textView.getMeasuredWidth();
    float currentHeightPx = textView.getMeasuredHeight();
    float textSize = textView.getTextSize();

    float scale = Math.min(widthPx / currentWidthPx, heightPx / currentHeightPx);
    textView.setTextSize(textSize * scale);
  }

  /**
   * Translates in Y from startPercent to endPercent (expecting 0 for 0%, 1 for 100%).
   * Hides at the end based on hideOnEnd.
   */
  public static void panUpAndHide(final View v, float startPercent, float endPercent,
      long durationMs, boolean hideOnEnd) {
    if (v.getVisibility() != View.VISIBLE) {
      return; // Already hidden.
    }
    ValueAnimator panUp = animator(durationMs,
        new AccelerateDecelerateInterpolator(),
        new AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            v.setY(((float) valueAnimator.getAnimatedValue()) * v.getHeight());
          }
        },
        floatValue("translateY", startPercent, endPercent)
    );
    if (hideOnEnd) {
      panUp.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          v.setVisibility(View.INVISIBLE);
        }
      });
    }
    panUp.start();
  }

}
