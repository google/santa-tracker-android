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

package com.google.android.apps.santatracker.map.cameraAnimations;

import android.os.Handler;

import com.google.android.apps.santatracker.map.SantaMarker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

public class SantaCamAnimator {

    private Handler mHandler;

    private MoveAroundSanta mTravellingAnimation;

    private CurrentPathAnimation mPathAnimation;

    /**
     * Frames to stay in departing animation
     */
    private static final int DEPARTING_TIME = 10000;

    // Frames to wait for the current animation
    private static final long ARRIVING_TRIGGER = 15000;

    private boolean mStartedDeparting, mStartedArriving;

    private boolean mPaused = false;

    public SantaCamAnimator(GoogleMap map, SantaMarker santaMarker) {
        super();
        mHandler = new Handler();

        // setup animations
        mPathAnimation = new CurrentPathAnimation(mHandler, map, santaMarker);

        mTravellingAnimation = new MoveAroundSanta(mHandler, map, santaMarker);

    }

    public void triggerPaddingAnimation() {
        mTravellingAnimation.triggerPaddingAnimation();
    }

    public void animate(LatLng position, long remainingTime, long elapsedTime) {
        if (!mPaused && position != null) {
            if (!mStartedDeparting && elapsedTime < DEPARTING_TIME) {
                // reset variables and show first departing animation
                mPathAnimation.start();
                mTravellingAnimation.reset();
                mStartedArriving = false;
                mStartedDeparting = true;
            } else if (!mStartedArriving && remainingTime < ARRIVING_TRIGGER) {
                // arriving animation
                mPathAnimation.start();
                mStartedArriving = true;

            } else if (remainingTime >= ARRIVING_TRIGGER
                    && elapsedTime >= DEPARTING_TIME) {
                // between departing and arriving times, animate travelling animation
                mTravellingAnimation.onSantaMoving(position);

            }
        }
    }

    public void pause() {
        mPaused = true;
    }

    public void resume() {
        mPaused = false;
    }

    public void cancel() {
        mTravellingAnimation.cancel();
        mPathAnimation.cancel();
        mHandler.removeCallbacksAndMessages(null);
    }

    public void reset() {
        mPathAnimation.reset();
        mTravellingAnimation.reset();
        mPaused = false;
        mStartedDeparting = false;
        mStartedArriving = false;
    }

}
