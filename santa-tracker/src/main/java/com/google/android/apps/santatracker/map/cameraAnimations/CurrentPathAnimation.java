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

import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.apps.santatracker.map.SantaMarker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Camera animation that shows the path from Santa's current position to the
 * destination. The camera is animated independently of calls to
 * {@link MoveAroundSanta#onSantaMoving(LatLng)} until the {@link #MAX_ZOOM} level is reached.
 *
 */
class CurrentPathAnimation extends SantaCamAnimation {

    private static final int ANIMATION_DURATION = 2000;
    private static final int PADDING = 50; // TODO: move to constructor

    private static final float MAX_ZOOM = 10f;

    private CameraUpdate mCameraUpdate;

    CurrentPathAnimation(Handler handler, GoogleMap map, SantaMarker santa) {
        super(handler, map, santa);
    }

    public void start() {

        // Start the first animation - zoom to capture the current position and
        // destination
        animateShowSantaDestination(mSanta.getPosition());

    }

    // animate to a new bounds with santa and his destination
    private Runnable mThreadAnimate = new Runnable() {

        public void run() {
            if (mCameraUpdate != null && mMap != null) {
                mMap.animateCamera(mCameraUpdate, ANIMATION_DURATION,
                        mCancelCallback);
            }
        }
    };

    /**
     * Animate showing the destination and the position.
     */
    private void animateShowSantaDestination(LatLng futurePosition) {
        final LatLng santaDestination = (mSanta != null) ? mSanta.getDestination() : null;

        // Only construct a camera update if both positions are valid
        if (futurePosition == null || santaDestination == null) {
            return;
        }

        mCameraUpdate = CameraUpdateFactory.newLatLngBounds(
                new LatLngBounds.Builder().include(futurePosition)
                        .include(santaDestination).build(), PADDING);
        executeRunnable(mThreadAnimate);

    }

    /**
     * Animate at current zoom level to center on the position.
     */
    private void animateFollowSanta(LatLng futurePosition) {
        if (futurePosition == null) {
            return;
        }

        mCameraUpdate = CameraUpdateFactory.newLatLng(futurePosition);
        executeRunnable(mThreadAnimate);

    }

    private CancelableCallback mCancelCallback = new GoogleMap.CancelableCallback() {

        public void onFinish() {

            // only zoom until max zoom level, after that only move camera
            LatLng futurePosition = mSanta.getFuturePosition(SantaPreferences
                    .getCurrentTime() + ANIMATION_DURATION);

            if (futurePosition == null
                    || mMap.getCameraPosition().zoom <= MAX_ZOOM) {
                animateShowSantaDestination(futurePosition);

            } else {
                // Animate to where Santa is going to be
                animateFollowSanta(futurePosition);
            }

            executeRunnable(mThreadAnimate);
        }

        public void onCancel() {
        }
    };
}
