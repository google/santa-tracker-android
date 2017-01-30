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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

/**
 * Camera animation that centers on Santa, then follows his position.
 *
 */
class MoveAroundSanta extends SantaCamAnimation {

    private static final int ANIMATION_DURATION = 10000;
    private static final int ANIMATION_CATCHUP_DURATION = 5000;

    private static final float MAX_ZOOM = 10f;
    private static final float MIN_ZOOM = 7.8f;
    private static final float MAX_TILT = 40f;
    private static final float MIN_TILT = 0f;

    private static final int SCROLL_FRAME_DURATION = 300;

    private static final int STATE_FULL = 1;
    private static final int STATE_CATCHUP = 2;
    private static final int STATE_CATCHUP_IN = 3;
    private static final int STATE_SCROLL = 4;
    private static final int STATE_SMALL = 5;
    private static final int STATE_IN_ANIMATION = 6;

    // Order: Full, catchup, scroll, small, catchup, scroll
    private static final int[] ORDER = new int[]{
            STATE_FULL, STATE_IN_ANIMATION, STATE_CATCHUP, STATE_CATCHUP_IN, STATE_SCROLL,
            /*STATE_SMALL, STATE_IN_ANIMATION, */ STATE_CATCHUP, STATE_CATCHUP_IN, STATE_SCROLL
    };

    private int mState = 0;
    private long mScrollFrames = 0;

    private CameraUpdate mAnimateCameraUpdate;
    private CameraUpdate mMoveCameraUpdate;
    private int mAnimationDuration = ANIMATION_DURATION;

    MoveAroundSanta(Handler handler, GoogleMap map, SantaMarker santa) {
        super(handler, map, santa);

        reset();
    }

    @Override
    public void reset() {
        super.reset();
        mState = 0;
    }

    private long mAnimationStart;
    private float mAnimationBearingChange;
    private float mInitialBearing;

    void onSantaMoving(LatLng position) {
        // only execute animation if not cancelled
        // (required so that scroll won't be animated)
        if (!mIsCancelled) {
            switch (ORDER[mState]) {
                case STATE_CATCHUP:
                    catchupAnimation();
                    nextState();
                    break;
                case STATE_FULL:
                    fullAnimation();
                    nextState();
                    break;
                case STATE_SMALL:
                    smallAnimation();
                    nextState();
                    break;
                case STATE_CATCHUP_IN:
                    // ignore  during catchup animation, heading does not change
                    break;
                case STATE_IN_ANIMATION:
                    if (mAnimationStart > 0) {
                        updateHeading();
                    }
                    break;
                case STATE_SCROLL:
                    if (mScrollFrames > SCROLL_FRAME_DURATION) {
                        nextState();
                    } else {
                        scrollAnimation(position);
                        mScrollFrames++;
                    }
                    break;
            }
        }
    }

    private void updateHeading() {

        // never exceed progress, could be called with off-timings.
        float p = Math.min(
                ((float) (System.currentTimeMillis() - mAnimationStart))
                        / (float) ANIMATION_DURATION, 1f);

        float b = mInitialBearing + (mAnimationBearingChange * p);
        if (b < 0f) {
            b += 360f;
        }
        mSanta.setCameraOrientation(b);
    }

    private void nextState() {
        mState = (mState + 1) % ORDER.length;
        mScrollFrames = 0;
    }

    private void catchupAnimation() {
        LatLng position = mSanta.getFuturePosition(SantaPreferences.getCurrentTime()
                + ANIMATION_CATCHUP_DURATION);

        mAnimationDuration = ANIMATION_CATCHUP_DURATION;
        mAnimateCameraUpdate = CameraUpdateFactory.newLatLng(position);

        executeRunnable(mThreadAnimate);
    }

    private void smallAnimation() {
        LatLng pos = mSanta.getFuturePosition(SantaPreferences.getCurrentTime()
                + ANIMATION_DURATION);
        float tilt = SantaPreferences.getRandom(MIN_TILT, MAX_TILT);
        float bearing = SantaPreferences.getRandom(0f, 306f);

        CameraPosition camera = new CameraPosition.Builder().target(pos)
                .tilt(tilt).zoom(mMap.getCameraPosition().zoom)
                .bearing(bearing).build();

        saveBearing(bearing);

        mAnimationDuration = ANIMATION_DURATION;
        mAnimateCameraUpdate = CameraUpdateFactory.newCameraPosition(camera);
        executeRunnable(mThreadAnimate);
    }

    private void scrollAnimation(LatLng position) {
        mMoveCameraUpdate = CameraUpdateFactory.newLatLng(position);
        executeRunnable(mThreadMove);

    }

    private boolean skipScroll = false;

    private Runnable mThreadMove = new Runnable() {

        public void run() {
            if (mMap != null && mMoveCameraUpdate != null && !mIsCancelled && !skipScroll) {
                mMap.moveCamera(mMoveCameraUpdate);
                mAnimationStart = System.currentTimeMillis();
            }
            skipScroll = false;
        }
    };

    private void fullAnimation() {
        // get position in future so that camera is centered when camera
        // animation is finished
        LatLng pos = mSanta.getFuturePosition(SantaPreferences.getCurrentTime()
                + ANIMATION_DURATION);
        float tilt = SantaPreferences.getRandom(MIN_TILT, MAX_TILT);
        float zoom = SantaPreferences.getRandom(MIN_ZOOM, MAX_ZOOM);
        float bearing = SantaPreferences.getRandom(0f, 306f);

        // store animation heading changes
        saveBearing(bearing);

        CameraPosition camera = new CameraPosition.Builder().target(pos)
                .tilt(tilt).zoom(zoom).bearing(bearing).build();

        mAnimationDuration = ANIMATION_DURATION;
        mAnimateCameraUpdate = CameraUpdateFactory.newCameraPosition(camera);
        executeRunnable(mThreadAnimate);

    }

    private void saveBearing(float endBearing) {
        float startBearing = mMap.getCameraPosition().bearing;
        if (mInitialBearing > endBearing) {
            if (startBearing - endBearing > 180) {
                startBearing -= 360f;
            }
        } else {
            if (endBearing - startBearing > 180) {
                endBearing -= 360f;
            }
        }
        mInitialBearing = startBearing;
        mAnimationBearingChange = endBearing - startBearing;

    }

    void triggerPaddingAnimation() {
        // Cancel the scroll animation
        if (ORDER[mState] == STATE_SCROLL) {
            nextState();
            skipScroll = true;
        }

    }

    private Runnable mThreadAnimate = new Runnable() {

        public void run() {
            if (mAnimateCameraUpdate != null) {
                mMap.animateCamera(mAnimateCameraUpdate, mAnimationDuration,
                        mCancelListener);
                mAnimationStart = System.currentTimeMillis();

            }
        }
    };

    private CancelableCallback mCancelListener = new GoogleMap.CancelableCallback() {

        public void onFinish() {
            nextState();
        }

        public void onCancel() {

        }
    };
}
