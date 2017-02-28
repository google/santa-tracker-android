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

import android.graphics.Color;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Manages the Santa Marker on a {@link SantaMapFragment}.
 *
 */
public class SantaMarker {

    // private static final String TAG = "SantaMarker";

    /**
     * Snippet used by all markers that make up a santa marker (including all
     * animation frame markers).
     */
    static final String TITLE = "santa-marker";

    private static final Object TOKEN = new Object();

    // The santa marker
    private Marker[] mMovementMarkers;

    // The map to which this marker is attached
    private final SantaMapFragment mMap;

    // The movement thread
    private SantaMarkerMovementThread mMovementThread = null;

    // The animation thread (marker icon)
    private SantaMarkerAnimationThread mAnimationThread = null;
    private Marker[] mAnimationMarkers;

    // line colour
    private static final int mLineColour = Color.parseColor("#AA109f65");

    // Santa's path
    private Polyline mPath = null;

    // 2D array: for each present type, 4 types of presents, 0=100
    private static final int[][] PRESENTS = {
            {R.drawable.blue_100, R.drawable.blue_75, R.drawable.blue_50,
                    R.drawable.blue_25},
            {R.drawable.purple_100, R.drawable.purple_75,
                    R.drawable.purple_50, R.drawable.purple_25},
            {R.drawable.yellow_100, R.drawable.yellow_75,
                    R.drawable.yellow_50, R.drawable.yellow_25},
            {R.drawable.red_100, R.drawable.red_75,
                    R.drawable.red_50, R.drawable.red_25},
            {R.drawable.green_100, R.drawable.green_75,
                    R.drawable.green_50, R.drawable.green_25}};

    /**
     * Markers for santa movement
     */
    private static final int[] MOVEMENT_MARKERS = new int[]{
            R.drawable.santa_n, R.drawable.santa_ne, R.drawable.santa_e,
            R.drawable.santa_se, R.drawable.santa_s, R.drawable.santa_sw,
            R.drawable.santa_w, R.drawable.santa_nw, R.drawable.santa_n,};

    // orientation of camera
    private double mCameraOrientation;
    // Santa's heading when moving
    private double mHeading = -1;
    // current movement marker
    private int mMovingMarker = 0;

    private PresentMarker[] mPresentMarkers;

    // State of Santa Marker - visiting or travelling
    private boolean mVisiting = false;

    // Flag to indicate whether draw presents or not.
    private boolean mPresentsDrawingPaused = false;

    /**
     * Santa's position.
     */
    private LatLng mPosition = new LatLng(0, 0);

    SantaMarker(SantaMapFragment map) {
        super();
        mMap = map;

        LatLng tempLocation = new LatLng(0, 0);

        // setup array of Santa animation markers and make them invisible
        final int[] animationIcons = new int[]{
                R.drawable.marker_santa_presents1,
                R.drawable.marker_santa_presents2,
                R.drawable.marker_santa_presents3,
                R.drawable.marker_santa_presents4,
                R.drawable.marker_santa_presents5,
                R.drawable.marker_santa_presents6,
                R.drawable.marker_santa_presents7,
                R.drawable.marker_santa_presents8};
        mAnimationMarkers = new Marker[animationIcons.length];
        for (int i = 0; i < animationIcons.length; i++) {
            Marker m = addSantaMarker(animationIcons[i], 0.5f, 1f,
                    tempLocation);
            m.setVisible(false);
            mAnimationMarkers[i] = m;
        }

        // Present marker
        View v = map.getView();
        if (v != null) {
            mPresentMarkers = new PresentMarker[PRESENTS.length];
            for (int i = 0; i < mPresentMarkers.length; i++) {
                mPresentMarkers[i] = new PresentMarker(map.getMap(), this,
                        new Handler(), PRESENTS[i], v.getWidth(), v.getHeight());
            }
        }

        // Movement markers
        mMovementMarkers = new Marker[MOVEMENT_MARKERS.length];
        for (int i = 0; i < MOVEMENT_MARKERS.length; i++) {
            mMovementMarkers[i] = addSantaMarker(MOVEMENT_MARKERS[i], 0.5f,
                    0.5f, tempLocation);
            mMovementMarkers[i].setVisible(false);
        }

        mMovingMarker = 0;

    }

    /**
     * Move all Markers used for the present animation to the given position
     */
    private void moveAnimationMarkers(LatLng position) {
        for (Marker m : mAnimationMarkers) {
            m.setPosition(position);
        }
    }

    /**
     * Adds a new marker at the given position. u, describes the anchor
     * position.
     */
    private Marker addSantaMarker(int iconDrawable, float u, float v, LatLng position) {
        return mMap.addMarker(new MarkerOptions().position(position)
                .anchor(u, v) // anchor in center
                .title(TITLE)
                .icon(BitmapDescriptorFactory.fromResource(iconDrawable)));
    }

    /**
     * Sets the camera orientation and update the marker if moving.
     */
    public void setCameraOrientation(float bearing) {
        mCameraOrientation = (bearing + 360.0f) % 360.0f;

        if (mMovementThread != null && mMovementThread.isMoving()) {
            setMovingIcon();
        }
    }

    /**
     * Update the movement marker.
     */
    private void setMovingIcon() {

        double angle = ((mHeading - mCameraOrientation + 360.0)) % 360.0;
        int index = ((int) (Math.round((Math.abs(angle) / 360f)
                * (mMovementMarkers.length - 1)))) % mMovementMarkers.length;

        setMovingMarker(index);

        // Log.d("SantaMarker", "Moving icon = camera:" + mCameraOrientation
        // + ", heading:" + mHeading + ",angle=" + angle + ", index="+index);
    }

    /**
     * Hides the previous marker, moves the new marker and makes it visible.
     */
    private void setMovingMarker(int i) {
        if (mMovingMarker != i) {
            LatLng pos = mMovementMarkers[mMovingMarker].getPosition();
            mMovementMarkers[i].setPosition(pos);
            mMovementMarkers[i].setVisible(true);
            mMovementMarkers[mMovingMarker].setVisible(false);
            mMovingMarker = i;
        }
    }

    /**
     * Sets the position of the current movement marker.
     */
    private void setMovingPosition(LatLng pos) {
        setCachedPosition(pos);
        mMovementMarkers[mMovingMarker].setPosition(pos);
    }

    /**
     * Hides the current movement marker.
     */
    private void hideMovingMarker() {
        mMovementMarkers[mMovingMarker].setVisible(false);
    }

    /**
     * Santa is visiting this location, display animation.
     */
    public void setVisiting(LatLng pos) {
        mVisiting = true;

        setCachedPosition(pos);

        // stopAnimations();
        removePath();

        mAnimationThread = new SantaMarkerAnimationThread(this, mAnimationMarkers);

        mAnimationThread.startAnimation(pos);

        hideMovingMarker();

        // reset heading
        mHeading = -1;

    }

    public boolean isVisiting() {
        return mVisiting;
    }

    /**
     * Returns the current position of this marker.
     */
    public synchronized LatLng getPosition() {
        return mPosition;
    }

    /**
     * Saves a location as Santa's current location. This makes it available to other classes in
     * {@link #getPosition()}.
     *
     * @param position The location to be saved.
     */
    private synchronized void setCachedPosition(LatLng position) {
        mPosition = position;
    }

    /**
     * Returns the destination position if the marker is moving, null otherwise.
     */
    public LatLng getDestination() {
        if (mMovementThread != null) {
            return mMovementThread.getDestination();
        } else {
            return null;
        }
    }

    /**
     * Animate this marker to the given position for the timestamps.
     */
    void animateTo(LatLng originLocation, LatLng destinationLocation,
                   long departure, long arrival) {

        mVisiting = false;

        setMovingIcon();
        // create new animation runnable and post to handler
        mMovementThread = new SantaMarkerMovementThread(this, departure, arrival,
                destinationLocation, originLocation, true);
        mMovementThread.startAnimation();

        if (mAnimationThread != null && mAnimationThread.isAlive()) {
            mAnimationThread.stopAnimation();
        }

    }

    /**
     * Remove the path.
     */
    private void removePath() {
        if (mPath != null) {
            mPath.remove();
            mPath = null;
        }
    }

    /**
     * Stops all marker animations. Should be called by attached Activity in
     * lifecycle methods.
     */
    void stopAnimations() {
        if (mMovementThread != null) {
            mMovementThread.stopAnimation();
        }

        if (mAnimationThread != null) {
            mAnimationThread.stopAnimation();
        }
    }

    /**
     * If this marker is currently moving, calculate its future position at the
     * given timestamp. If this marker is not moving, return its current
     * position
     */
    public LatLng getFuturePosition(long timestamp) {
        if (mMovementThread != null && mMovementThread.isMoving()) {
            return mMovementThread.calculatePosition(timestamp);
        } else {
            return getPosition();
        }
    }

    void pausePresentsDrawing() {
        mPresentsDrawingPaused = true;
    }

    void resumePresentsDrawing() {
        mPresentsDrawingPaused = false;
    }

    /**
     * Thread that toggles visibility of the markers, making one marker at a
     * time visible.
     *
     */
    private static class SantaMarkerAnimationThread extends Thread {

        /*
         * The refresh rate is identical to the marker movement thread to
         * animate the presents
         */
        static final int REFRESH_RATE = SantaMarkerMovementThread.REFRESH_RATE;
        static final int ANIMATION_DELAY = 6; // should be equivalent to a post delay of 150ms
        private Marker[] mToggleMarkers;
        private int mCurrent = 0;
        private int mFrame = 0;
        private boolean mStopThread = false;
        private SwapMarkersRunnable mSwapRunnable;
        private final LatLng TEMP_POSITION = new LatLng(0f, 0f);
        private final WeakReference<SantaMarker> mSantaMarkerRef;

        SantaMarkerAnimationThread(SantaMarker santaMarker, Marker[] markers) {
            super();
            mToggleMarkers = markers;
            mSwapRunnable = new SwapMarkersRunnable();
            mSantaMarkerRef = new WeakReference<>(santaMarker);
        }

        public void run() {
            while (!this.mStopThread) {
                SantaMarker marker = mSantaMarkerRef.get();
                if (marker == null) {
                    mStopThread = true;
                    break;
                }
                if (mFrame == 0) {

                    final int currentMarker = mCurrent;
                    final int nextMarker = (++mCurrent % mToggleMarkers.length);
                    mCurrent = nextMarker;

                    mSwapRunnable.currentMarker = currentMarker;
                    mSwapRunnable.nextMarker = nextMarker;
                    View view = marker.mMap.getView();
                    if (view != null) {
                        view.getHandler().postAtTime(mSwapRunnable, TOKEN,
                                SystemClock.uptimeMillis());
                    }
                }
                mFrame = (mFrame + 1) % ANIMATION_DELAY;

                for (PresentMarker m : marker.mPresentMarkers) {
                    m.draw();
                }

                try {
                    Thread.sleep(REFRESH_RATE);
                } catch (InterruptedException e) {
                    // if interrupted, cancel
                    this.mStopThread = true;
                }
            }
        }

        /**
         * Hide and move markers, need to restart thread to make visible again.
         */
        void hideAll() {
            for (Marker m : mToggleMarkers) {
                m.setVisible(false);
                m.setPosition(TEMP_POSITION);
            }
        }

        public void cancel() {
            this.mStopThread = true;
        }

        /**
         * Start this thread. All animated markers (and the normal santa marker)
         * are hidden.
         */
        void startAnimation(LatLng position) {
            mStopThread = false;
            hideAll();

            SantaMarker marker = mSantaMarkerRef.get();
            if (marker == null) {
                return;
            }
            marker.setCachedPosition(position);

            for (PresentMarker m : marker.mPresentMarkers) {
                m.reset();
            }

            marker.moveAnimationMarkers(position);

            start();
        }

        /**
         * Stop this thread. All animated markers are hidden and the original
         * santa marker is made visible.
         */
        void stopAnimation() {
            // stop execution by removing all callbacks
            mStopThread = true;
            SantaMarker marker = mSantaMarkerRef.get();
            if (marker != null) {
                View view = marker.mMap.getView();
                if (view != null) {
                    view.getHandler().removeCallbacksAndMessages(TOKEN);
                }
            }
            hideAll();
        }

        class SwapMarkersRunnable implements Runnable {

            int currentMarker, nextMarker;

            public void run() {
                SantaMarker marker = mSantaMarkerRef.get();
                if (marker == null) {
                    return;
                }
                SantaMapFragment map = marker.mMap;
                if (map != null && map.getMap() != null) {
                    mToggleMarkers[currentMarker].setVisible(false);
                    mToggleMarkers[nextMarker].setVisible(true);

                    float zoom = map.getMap().getCameraPosition().zoom;
                    PresentMarker.setViewParameters(zoom, map.isInSantaCam());
                }
            }
        }

    }

    /**
     * Animation Thread for a Santa Marker. Animates the marker between two
     * locations.
     *
     */
    private static class SantaMarkerMovementThread extends Thread {

        /**
         * Refresh rate of this thread (it is called again every X ms.)
         */
        static final int REFRESH_RATE = 17;

        private boolean mStopThread = false;
        private long mStart, mArrival;
        private double mDuration;
        private LatLng mDestinationLocation, mStartLocation;
        private boolean mIsAnimated = false;
        private ArrayList<LatLng> mPathPoints;
        private final WeakReference<SantaMarker> mSantaMarkerRef;

        // Threads
        private MovementRunnable mMovementRunnable;

        SantaMarkerMovementThread(SantaMarker marker, long departureTime, long arrivalTime,
                                  LatLng destinationLocation, LatLng startLocation,
                                  boolean drawPath) {
            mStart = departureTime;
            mArrival = arrivalTime;
            mDestinationLocation = destinationLocation;
            mStartLocation = startLocation;
            mDuration = arrivalTime - departureTime;
            mSantaMarkerRef = new WeakReference<>(marker);

            marker.removePath();
            if (drawPath) {
                // set up path
                PolylineOptions line = new PolylineOptions().add(startLocation)
                        .add(startLocation).color(mLineColour);
                marker.mPath = marker.mMap.getMap().addPolyline(line);
                marker.mPath.setGeodesic(true);
                mPathPoints = new ArrayList<>(2);
                mPathPoints.add(startLocation); // origin
                mPathPoints.add(startLocation); // destination - updated in loop
            } else {
                marker.mPath = null; // already removed
            }

            mMovementRunnable = new MovementRunnable();

        }

        private void stopAnimation() {
            this.mStopThread = true;
            SantaMarker marker = mSantaMarkerRef.get();
            if (marker != null) {
                View view = marker.mMap.getView();
                if (view != null) {
                    view.getHandler().removeCallbacksAndMessages(TOKEN);
                }
            }
        }

        void startAnimation() {
            this.mStopThread = false;
            start();
        }

        private Runnable mSetIconRunnable = new Runnable() {
            public void run() {
                SantaMarker marker = mSantaMarkerRef.get();
                if (marker != null) {
                    marker.setMovingIcon();
                }
            }
        };
        private Runnable mReachedDestinationRunnable = new Runnable() {

            public void run() {
                SantaMarker marker = mSantaMarkerRef.get();
                if (marker == null) {
                    return;
                }
                marker.removePath();
                // notify callback
                marker.mMap.onSantaReachedDestination(mDestinationLocation);
            }
        };

        public void run() {
            while (!mStopThread) {
                SantaMarker marker = mSantaMarkerRef.get();
                if (marker == null) {
                    mStopThread = true;
                    break;
                }
                // need to initialise, marker not set as animated yet
                final View view = marker.mMap.getView();
                if (!mIsAnimated) {

                    mIsAnimated = true;

                    // calculate heading and update icon
                    marker.mHeading = SphericalUtil.computeHeading(mStartLocation,
                            mDestinationLocation);
                    marker.mHeading = (marker.mHeading + 360f) % 360f;

                    if (view != null) {
                        view.getHandler().postAtTime(mSetIconRunnable, TOKEN,
                                SystemClock.uptimeMillis());
                    }
                    // Log.d(TAG, "Starting animation thread: from: "
                    // + startLocation + " --to: " + destinationLocation
                    // + ", start=" + start + ", dep=" + arrival
                    // + ", duration=" + duration + ", head=" + mHeading);
                }

                double t = calculateProgress(SantaPreferences
                        .getCurrentTime());

                //SantaLog.d(TAG,"inSantaAnimateThread: t="+t+", position="+calculatePositionProgress(t)+", stopThread="+mStopThread);

                // Don't go backwards, but it could be negative if this thread is started too early
                t = Math.max(t, 0.0);
                // loop until finished or thread was notified to be stopped
                if (t < 1.0 && !mStopThread) {

                    mMovementRunnable.position = calculatePositionProgress(t);
                    // move marker and update path
                    if (view != null) {
                        view.getHandler().postAtTime(mMovementRunnable, TOKEN,
                                SystemClock.uptimeMillis());
                    }

                    if (!marker.mPresentsDrawingPaused) {
                        for (PresentMarker p : marker.mPresentMarkers) {
                            p.draw();
                        }
                    }

                    try {
                        Thread.sleep(REFRESH_RATE);
                    } catch (InterruptedException e) {
                        this.mStopThread = true;
                    }
                } else {
                    // reached final destination,stop moving
                    mIsAnimated = false;
                    mStopThread = true;
                    marker.setCachedPosition(mDestinationLocation);

                    if (view != null) {
                        view.getHandler().postAtTime(mReachedDestinationRunnable, TOKEN,
                                SystemClock.uptimeMillis());
                    }

                }

            }
        }

        /**
         * Calculate the position for the given future timestamp. If the
         * destination is reached before this timestamp, its destination is
         * returned.
         */
        LatLng calculatePosition(long timestamp) {
            double progress = calculateProgress(timestamp);
            return calculatePositionProgress(progress);
        }

        /**
         * Calculates the progress through the animation for the given timestamp
         */
        private double calculateProgress(long currentTimestamp) {
            return (currentTimestamp - mStart) / mDuration; // linear progress
        }

        /**
         * Calculate the position for the given progress (start at 0, finished
         * at 1).
         */
        private LatLng calculatePositionProgress(double progress) {

            return SphericalUtil.interpolate(mStartLocation, mDestinationLocation, progress);
        }

        private LatLng getDestination() {
            return mDestinationLocation;
        }

        private boolean isMoving() {
            return mIsAnimated;
        }

        class MovementRunnable implements Runnable {

            LatLng position;

            public void run() {
                SantaMarker marker = mSantaMarkerRef.get();
                if (marker == null) {
                    return;
                }
                marker.setMovingPosition(position);

                // update path if it is enabled
                if (marker.mPath != null) {
                    mPathPoints.set(1, position);
                    marker.mPath.setPoints(mPathPoints);
                }

                if (marker.mMap.getMap() != null) {
                    float zoom = marker.mMap.getMap().getCameraPosition().zoom;
                    PresentMarker.setViewParameters(zoom, marker.mMap.isInSantaCam());
                }

                long time = SantaPreferences.getCurrentTime();
                marker.mMap.onSantaIsMovingProgress(position, mArrival - time, time - mStart);

            }
        }
    }

    /**
     * Interface for callbacks from a {@link SantaMarker}.
     *
     */
    interface SantaMarkerInterface {

        void onSantaReachedDestination(LatLng location);

    }

}
