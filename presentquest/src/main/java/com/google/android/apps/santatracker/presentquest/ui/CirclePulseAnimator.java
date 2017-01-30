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

import android.content.Context;
import android.os.Handler;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.model.Circle;

import com.google.android.apps.santatracker.presentquest.MapsActivity;
import com.google.android.apps.santatracker.presentquest.R;


/**
 * Animate a {@link Circle} that pulses and fades.
 */
public class CirclePulseAnimator {

    private static final int STEP_TIME_MS = 32;

    private static final double MIN_ACTION_RADIUS = 10.0f;
    private static final double ACTION_RADIUS_STEP = 1.2f;
    private final double MAX_ACTION_RADIUS;
    private static final int ACTION_OPACITY_STEP = 2;

    private Context mContext;
    private Circle mCircle;
    
    public CirclePulseAnimator(Context context, Circle circle, int reachableRadius) {
        mContext = context;
        mCircle = circle;
        // Multiply by 1.2 so that it appears to "reach" all reachable icons
        MAX_ACTION_RADIUS = reachableRadius * 1.2f;
    }

    private Handler mHorizonAnimationHandler = new Handler();
    private Runnable mPulseHorizonsRunnable = new Runnable() {
        @Override
        public void run() {
            // Calculate new radius
            double newRadius = mCircle.getRadius() + ACTION_RADIUS_STEP;

            // Calculate new colors (change opacity)
            int strokeColor = mCircle.getStrokeColor();
            int newStrokeOpacity = Math.max(getOpacity(strokeColor) - ACTION_OPACITY_STEP, 0);
            int newStrokeColor = setOpacity(strokeColor, newStrokeOpacity);

            int fillColor = mCircle.getFillColor();
            int newFillOpacity = Math.max(getOpacity(fillColor) - ACTION_OPACITY_STEP, 0);
            int newFillColor = setOpacity(fillColor, newFillOpacity);

            if (newRadius <= MAX_ACTION_RADIUS) {
                // Set to new values
                mCircle.setRadius(newRadius);
                mCircle.setStrokeColor(newStrokeColor);
                mCircle.setFillColor(newFillColor);
            } else {
                // Reset
                mCircle.setRadius(MIN_ACTION_RADIUS);
                mCircle.setStrokeColor(ContextCompat.getColor(mContext, R.color.action_horizon_stroke));
                mCircle.setFillColor(ContextCompat.getColor(mContext, R.color.action_horizon_fill));
            }

            // Reschedule
            mHorizonAnimationHandler.postDelayed(this, STEP_TIME_MS);
        }
    };

    public void start() {
        mHorizonAnimationHandler.removeCallbacksAndMessages(null);
        mHorizonAnimationHandler.post(mPulseHorizonsRunnable);
    }

    public void stop() {
        mHorizonAnimationHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Change the opacity of a hexadecimal color.
     * @param color the color.
     * @param opacity a new opacity value, 0 to 255.
     * @return the new color.
     */
    private int setOpacity(int color, int opacity) {
        // Remove opacity
        int currentOpacity = ((color >> 24) & 0xFF) << 24;
        color = (color - currentOpacity);

        // Add new opacity
        color = color + (opacity << 24);

        return color;
    }

    /**
     * Get the current opacity of a hexadecimal color.
     * @param color the color.
     * @return the opacity, 0 to 255.
     */
    private int getOpacity(int color) {
        return (color >> 24) & 0xFF;
    }
}
