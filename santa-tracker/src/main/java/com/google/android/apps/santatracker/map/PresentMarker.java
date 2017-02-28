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

package com.google.android.apps.santatracker.map;

import android.graphics.Point;
import android.os.Handler;

import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

class PresentMarker {

    private static final int ANIMATION_FRAMES_FADEOUT = 4; // per marker
    private static final int ANIMATION_FRAMES_MOVING_MAX = 275;
    private static final int ANIMATION_FRAMES_MOVING_MIN = 175;
    private static final int ANIMATION_FRAMES_WAIT = 500;
    private static final double MAXIMUM_ZOOM_LEVEL = 8.7f;

    static final String MARKER_TITLE = "PresentMarker";

    private Marker[] mAnimationMarkers;
    private Marker mMovementMarker;
    private int mIndex = 0;
    private GoogleMap mMap;
    private SantaMarker mSantaMarker;
    private int mSizeX, mSizeY;

    private LatLng mDestination = null;
    private int mFrame = 0;
    private double mDirectionLat, mDirectionLng;
    private int mTotalAnimationLength;

    private LatLng mLocation;
    private int mAnimationDuration;
    private Projection mProjection;
    private boolean mWaitingForProjection = false;
    private LatLng mSantaPosition;
    private Handler mHandler;

    private static boolean VALID_CAMERA;

    PresentMarker(GoogleMap map, SantaMarker santa, Handler handler,
                  int[] animIcons, int screenWidth, int screenHeight) {
        this.mMap = map;
        this.mSantaMarker = santa;
        this.mHandler = handler;

        // setup markers, one per icon
        mAnimationMarkers = new Marker[animIcons.length - 1];
        LatLng position = new LatLng(0f, 0f);
        for (int i = 1; i < animIcons.length; i++) {
            mAnimationMarkers[i - 1] = mMap.addMarker(new MarkerOptions()
                    .title(MARKER_TITLE)
                    .icon(BitmapDescriptorFactory.fromResource(animIcons[i]))
                    .position(position).visible(false));
            mAnimationMarkers[i - 1].setVisible(false);
        }
        mMovementMarker = mMap.addMarker(new MarkerOptions()
                .title(MARKER_TITLE)
                .icon(BitmapDescriptorFactory.fromResource(animIcons[0]))
                .position(position).visible(false));
        mMovementMarker.setVisible(false);

        mSizeX = screenWidth;
        mSizeY = screenHeight;

        // Wait before start
        mFrame = SantaPreferences.getRandom(-ANIMATION_FRAMES_WAIT, 0);

        reset();
    }

    private void setProjection(Projection p, LatLng santaPosition) {
        this.mProjection = p;
        this.mSantaPosition = santaPosition;
        this.mWaitingForProjection = false;
    }

    static void setViewParameters(double zoom, boolean inSantaCam) {
        VALID_CAMERA = zoom > MAXIMUM_ZOOM_LEVEL || inSantaCam;

    }

    void draw() {

        // 5 States: waiting for valid camera for new present location, waiting
        // for start,
        // New present, moving, animating/disappearing
        if (!VALID_CAMERA && (mDestination == null && mProjection == null)) {

        } else if (mAnimationDuration < 0 || mWaitingForProjection) {
            // wait to start and until projection has been set

            // need to initialise the projection
        } else if (VALID_CAMERA && mDestination == null && mProjection == null) {
            // Log.d(TAG,"getting projection - zoom: "+ZOOM_LEVEL);
            mWaitingForProjection = true;
            mHandler.post(mGetProjectionRunnable);

        } else if (mDestination == null && mProjection != null) {
            // pick a new destination from screen coordinates
            int y = SantaPreferences.getRandom(0, mSizeY);
            int x = SantaPreferences.getRandom(0, mSizeX);

            mDestination = mProjection.fromScreenLocation(new Point(x, y));
            if (mDestination == null) {
                SantaLog.d("SantaPresents", "Point = " + new Point(x, y));
            }

            mAnimationDuration = SantaPreferences.getRandom(
                    ANIMATION_FRAMES_MOVING_MIN, ANIMATION_FRAMES_MOVING_MAX);
            mTotalAnimationLength = mAnimationDuration
                    + (ANIMATION_FRAMES_FADEOUT * mAnimationMarkers.length);
            // calculate speed
            mDirectionLat = (mDestination.latitude - mSantaPosition.latitude)
                    / mAnimationDuration;
            mDirectionLng = (mDestination.longitude - mSantaPosition.longitude)
                    / mAnimationDuration;
            mLocation = mSantaPosition;
            mHandler.post(mSetVisibleLocationRunnable);

            mFrame = 0;
            // Log.d(TAG,
            // "New present Marker position: "+mLocation+" movement: "+mDirectionLat+", "+mDirectionLng);
            mProjection = null;

        } else if (mFrame < mAnimationDuration) {
            // Moving animation

            mLocation = new LatLng(mLocation.latitude + mDirectionLat,
                    mLocation.longitude + mDirectionLng);
            mHandler.post(mSetLocationRunnable);

            // animate out if frames left for all animation markers
        } else if (mFrame >= mAnimationDuration
                && mFrame <= mTotalAnimationLength) {

            if ((mFrame - mAnimationDuration) % ANIMATION_FRAMES_FADEOUT == 0) {
                // switch to the next marker
                mHandler.post(mSwapIconRunnable);

            }

        } else if (mFrame > mTotalAnimationLength) {
            // animation finished, reset and start again after wait
            mDestination = null;
            mFrame = SantaPreferences
                    .getRandom(-ANIMATION_FRAMES_MOVING_MAX, 0);
        }

        // Wait
        if (!mWaitingForProjection) {
            mFrame++;
        }
    }

    /**
     * Hides the previous animation marker and marks the given marker visible.
     * If this is is the first marker, only it will be set visible. If this is
     * not a marker, nothing will be done.
     */
    private void showAnimationMarker(int i) {
        if (i >= 0 && i < mAnimationMarkers.length) {
            mAnimationMarkers[i].setPosition(mLocation);
            mAnimationMarkers[i].setVisible(true);
        }

        // hide the previous marker
        if (i - 1 < 0) {
            mMovementMarker.setVisible(false);
        } else if (i - 1 < mAnimationMarkers.length) {
            mAnimationMarkers[i - 1].setVisible(false);
        }

    }

    public void reset() {
        mAnimationMarkers[mIndex].setVisible(false);
        mIndex = 0;
    }

    public void hide() {
        mAnimationMarkers[mIndex].setVisible(false);
    }

    private Runnable mGetProjectionRunnable = new Runnable() {

        public void run() {
            setProjection(mMap.getProjection(), mSantaMarker.getPosition());
        }
    };

    private Runnable mSwapIconRunnable = new Runnable() {
        public void run() {
            showAnimationMarker((mFrame - mAnimationDuration)
                    / ANIMATION_FRAMES_FADEOUT);
        }
    };

    private Runnable mSetVisibleLocationRunnable = new Runnable() {
        public void run() {
            mMovementMarker.setPosition(mLocation);
            mMovementMarker.setVisible(true);
        }
    };

    private Runnable mSetLocationRunnable = new Runnable() {
        public void run() {
            mMovementMarker.setPosition(mLocation);
        }
    };

}
