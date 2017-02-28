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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.graphics.drawable.VectorDrawableCompat;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.data.Destination;
import com.google.android.apps.santatracker.data.DestinationDbHelper;
import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.apps.santatracker.map.SantaMarker.SantaMarkerInterface;
import com.google.android.apps.santatracker.map.cameraAnimations.AtLocation;
import com.google.android.apps.santatracker.map.cameraAnimations.SantaCamAnimator;
import com.google.android.apps.santatracker.util.AnalyticsManager;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * A specialised {@link MapFragment} that displays Santa's destinations and
 * holds a {@link SantaMarker}. The attaching activity MUST implement {@link SantaMapInterface}.
 *
 */
public class SantaMapFragment extends SupportMapFragment implements SantaMarkerInterface {

    // The map
    private GoogleMap mMap = null;

    // Interface
    private SantaMapInterface mCallback;

    // visited location
    private BitmapDescriptor mMarkerIconVisited;

    // Identify different types of markers for info window
    public static final String MARKER_PAST = "MARKER_PAST";
    public static final String MARKER_NEXT = "MARKER_NEXT";
    public static final String MARKER_ACTIVE = "MARKER_ACTIVE";

    // Next location marker
    private Marker mNextMarker = null;

    // info window for marker pop-up bubbles
    private DestinationInfoWindowAdapter mInfoWindowAdapter;

    // Marker used for active marker
    private Marker mActiveMarker = null;
    private Marker mCurrentInfoMarker = null;
    private Marker mPendingInfoMarker = null;

    protected static final LatLng BOGUS_LOCATION = new LatLng(0f, 0f);

    // Santa
    private SantaMarker mSantaMarker = null;

    // duration of camera animation to santa when SC is enabled
    public static final int SANTACAM_MOVE_TO_SANTA_DURATION = 2000;

    // duration of camera animation to user destination
    public static final int SANTACAM_MOVE_TO_DEST_DURATION = 2000;

    // zoom level of MOVE TO DEST destination animation
    public static final float SANTACAM_MOVE_TO_DEST_ZOOM = 12.f;


    // is SantaCam enabled?
    private boolean mSantaCam = false;

    // Manages audio playback
    private TrackerSoundPlayer mTrackerSoundPlayer;

    private SantaCamAnimator mSantaCamAnimator;

    private Handler mHandler = new Handler();

    private FirebaseAnalytics mMeasurement;

    // Padding
    private int mPaddingCamLeft, mPaddingCamTop, mPaddingCamRight, mPaddingCamBottom;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mMap = null;
        mMeasurement = FirebaseAnalytics.getInstance(this.getContext());
    }

    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        super.onResume();
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                setupMap(map);
                mCallback.onMapInitialised();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        // Stop audio and release audio player
        stopAudio();
    }

    public void resumeAudio() {
        mTrackerSoundPlayer.resume();
        mTrackerSoundPlayer.unmute();
    }

    public void pauseAudio() {
        mTrackerSoundPlayer.pause();
        mTrackerSoundPlayer.mute();
    }

    public void stopAudio() {
        mTrackerSoundPlayer.release();
    }

    /**
     * Add the santa marker to the map.
     */
    private void addSanta() {
        // create Santa marker
        mSantaMarker = new SantaMarker(this);
    }

    /**
     * Animate the santa marker to destination to arrive at its arrival time.
     */
    public void setSantaTravelling(Destination origin,
        Destination destination, boolean moveCameraToSanta) {

        mTrackerSoundPlayer.sayHoHoHo();
        mTrackerSoundPlayer.startSleighBells();

        // display next destination marker
        mNextMarker.setSnippet(Integer.toString(destination.id));
        mNextMarker.setPosition(destination.position);
        mNextMarker.setVisible(true);

        if (moveCameraToSanta) {
            mSantaCamAnimator.reset();
        }
        mSantaMarker.animateTo(origin.position, destination.position,
                origin.departure, destination.arrival);
    }

    /**
     * Sets santa as visiting the given location.
     */
    public void setSantaVisiting(Destination destination, boolean playSound) {

        // move santa to this location
        mSantaMarker.setVisiting(destination.position);

        // stop bells and play 'hohoho'
        mTrackerSoundPlayer.stopSleighBells();
        if (playSound) {
            mTrackerSoundPlayer.sayHoHoHo();
        }

        // hide the next marker from this position, move it off-screen to
        // prevent touch events
        mNextMarker.setVisible(false);
        mNextMarker.setPosition(BOGUS_LOCATION);

        // if the info window for this position is open, dismiss it
        if (mActiveMarker != null && mActiveMarker.isVisible()
                && mActiveMarker.getSnippet().equals(Integer.toString(destination.id))) {
            hideInfoWindow();
        }

    }

    public boolean isInSantaCam() {
        return mSantaCam;
    }

    public void jumpToDestination(final LatLng position) {

        disableSantaCam();

        if (mSantaMarker != null) {
            // present drawing will be resumed when SantaCam is enabled again.
            mSantaMarker.pausePresentsDrawing();
        }

        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position,
                    SANTACAM_MOVE_TO_DEST_ZOOM),
                    SANTACAM_MOVE_TO_DEST_DURATION, null);
        }
    }

    /**
     * Enables the SantaCam and (if set) animates the camera to santa.
     */
    public void enableSantaCam(boolean animateToSanta) {

        if (mMap != null) {
            mMap.setPadding(mPaddingCamLeft, mPaddingCamTop, mPaddingCamRight, mPaddingCamBottom);
        }

        mSantaCam = true;
        mCallback.onSantacamStateChange(true);

        // hide current info window
        hideInfoWindow();

        mSantaCamAnimator.reset();

        if (animateToSanta) {
            // santa is already enroute, start animation to Santa and pause animator to speed up
            // camera animation
            if (!mSantaMarker.isVisiting()) {
                mSantaCamAnimator.pause();
                LatLng futurePosition = mSantaMarker.getFuturePosition(
                        SantaPreferences.getCurrentTime() + SANTACAM_MOVE_TO_SANTA_DURATION);
                if (futurePosition != null &&
                        !(futurePosition.latitude == 0 && futurePosition.longitude == 0)) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(futurePosition),
                            SANTACAM_MOVE_TO_SANTA_DURATION, mMovingCatchupCallback);
                } else {
                    mSantaCamAnimator.resume();
                    mSantaMarker.resumePresentsDrawing();
                }
            } else {
                // Santa is at a location
                onSantaReachedDestination(mSantaMarker.getPosition());
            }
        } else {
            mSantaMarker.resumePresentsDrawing();
        }

    }

    public void disableSantaCam() {
        if (mSantaCam) {
            mSantaCam = false;
            mCallback.onSantacamStateChange(false);

            mSantaCamAnimator.cancel();
        }
    }

    /**
     * Called when Santa has reached the given destination.
     */
    public void onSantaReachedDestination(final LatLng destination) {
        // hide the next marker from this position
        mNextMarker.setVisible(false);

        // Santa has reached destination - update camera
        // center on Santa's current position at lower zoom level
        if (mSantaCam) {
            // Post camera update through Handler to allow for subsequent camera animation in
            // CancellableCallback
            mHandler.post(mReachedDestinationRunnable);
        }
        mSantaCamAnimator.reset();

    }

    /**
     * Santa is currently moving, called with a progress update. If in SantaCam,
     * the camera is repositioned to capture santa.
     */
    public void onSantaIsMovingProgress(LatLng position, long remainingTime,
                                        long elapsedTime) {

        if (mSantaCam && mMap != null && mSantaMarker != null && position != null
                && mSantaMarker.getPosition() != null) {
            // use animator to update camera if in santa cam mode
            mSantaCamAnimator.animate(position, remainingTime, elapsedTime);
        }
    }

    /*
     * On map click - disable info window if it is displayed, otherwise disable
     * santa cam or do nothing
     */
    private OnMapClickListener mMapClickListener = new OnMapClickListener() {

        public void onMapClick(LatLng arg0) {
            if (mCallback == null) {
                // This can happen on orientation change
                return;
            }
            if (mCurrentInfoMarker != null) {
                // info window is displayed, hide it
                restoreClickedMarker();
                mCallback.onClearDestination();
            } else {
                mCallback.mapClickAction();
            }
        }
    };

    private OnCameraChangeListener mCameraChangeListener = new OnCameraChangeListener() {

        private float mPreviousBearing = Float.MIN_VALUE;

        public void onCameraChange(CameraPosition camera) {
            // Notify santa marker if new bearing
            if (mPreviousBearing != camera.bearing) {
                mSantaMarker.setCameraOrientation(camera.bearing);
                mPreviousBearing = camera.bearing;
            }
        }
    };

    /**
     * Marker click listener. Handles clicks on markers. When a destination
     * marker is clicked, the active marker is set to this position and the
     * corresponding info window is displayed. If santa cam is enabled, it is
     * disabled.
     */
    private OnMarkerClickListener mMarkerClickListener = new OnMarkerClickListener() {

        public boolean onMarkerClick(Marker marker) {

            // unsupported marker
            if (marker.getTitle() == null) {
                return false;
            }
            // Santa Marker
            else if (marker.getTitle().equals(SantaMarker.TITLE)) {

                mTrackerSoundPlayer.sayHoHoHo();

                hideInfoWindow();

                // App Measurement
                MeasurementManager.recordCustomEvent(mMeasurement,
                        getString(R.string.analytics_event_category_tracker),
                        getString(R.string.analytics_tracker_action_clicksanta), null);

                // [ANALYTICS EVENT]: SantaClicked
                AnalyticsManager.sendEvent(R.string.analytics_event_category_tracker,
                        R.string.analytics_tracker_action_clicksanta);

                // spin camera around if santa is not moving in SC
                // or at any other time if not in SC
                if (!mSantaCam || mSantaMarker.isVisiting()) {
                    // spin camera around to opposite side
                    CameraPosition oldCamera = mMap.getCameraPosition();
                    float bearing = oldCamera.bearing;
                    // calculate bearing, +1 so that the camera always moves in
                    // the
                    // same direction
                    bearing = (bearing + 181f) % 360f;

                    CameraPosition camera = CameraPosition.builder(oldCamera)
                            .bearing(bearing).build();
                    mMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(camera),
                            SANTACAM_MOVE_TO_SANTA_DURATION,
                            new CancelableCallback() {

                                public void onFinish() {

                                    // animate another 181 degrees around
                                    CameraPosition oldCamera = mMap
                                            .getCameraPosition();
                                    float bearing = oldCamera.bearing;
                                    bearing = (bearing + 181f) % 360f;
                                    CameraPosition camera = CameraPosition
                                            .builder(mMap.getCameraPosition())
                                            .bearing(bearing).build();
                                    mMap.animateCamera(CameraUpdateFactory
                                                    .newCameraPosition(camera),
                                            SANTACAM_MOVE_TO_SANTA_DURATION, null);
                                }

                                public void onCancel() {

                                }
                            });
                }
                return true;

                // Present Marker
            } else if (marker.getTitle().equals(PresentMarker.MARKER_TITLE)) {

                return true;

                // Pin marker (location)
            } else if (marker.getTitle().equals(MARKER_NEXT)
                    || marker.getTitle().equals(MARKER_PAST)) {

                showInfoWindow(marker);

                return true;

                // Active marker
            } else if (marker.getTitle().equals(MARKER_ACTIVE)) {

                hideInfoWindow();
                return true;
            } else {
                return false;
            }
        }

    };

    /**
     * Activity is attaching to this fragment, ensure it is implementing
     * {@link SantaMapInterface}.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mTrackerSoundPlayer = new TrackerSoundPlayer(activity);

        // ensure that attaching activity implements the required interface
        try {
            mCallback = (SantaMapInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SantaMapInterface");
        }

    }


    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
        mTrackerSoundPlayer.release();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (this.mMap != null) {
            this.mMap.clear();
        }

        // reset map to trigger new setup
        this.mMap = null;

        // stop santa's animation thread
        if (mSantaMarker != null) {
            mSantaMarker.stopAnimations();
        }
        if (this.mSantaCamAnimator != null) {
            this.mSantaCamAnimator.cancel();
        }

        pauseAudio();
    }

    /**
     * Sets up the map and member variables. This method should be called once
     * the map has been initialised.
     */
    private void setupMap(GoogleMap map) {
        mMap = map;

        mInfoWindowAdapter = new DestinationInfoWindowAdapter(getLayoutInflater(null),
                getActivity().getApplicationContext());

        // clear map in case it was restored
        mMap.clear();

        // Set map theme
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(),
                R.raw.map_style));

        // setup map UI - disable zoom controls
        UiSettings ui = this.mMap.getUiSettings();
        ui.setZoomControlsEnabled(false);
        ui.setCompassEnabled(false);

        mMap.setInfoWindowAdapter(mInfoWindowAdapter);
        mMap.setOnInfoWindowClickListener(mInfoWindowClickListener);
        mMap.setOnMapClickListener(mMapClickListener);
        mMap.setOnCameraChangeListener(mCameraChangeListener);
        mMap.setOnMarkerClickListener(mMarkerClickListener);

        // setup marker icons
        mMarkerIconVisited = createMarker(R.drawable.marker_pin);

        // add active marker
        mActiveMarker = mMap.addMarker(new MarkerOptions()
                .position(BOGUS_LOCATION)
                .icon(createMarker(R.drawable.marker_pin_active))
                .title(MARKER_ACTIVE)
                .visible(false)
                .snippet("0")
                .anchor(0.5f, 1f));
        mActiveMarker.setVisible(false); // required, visible in MarkerOptions
        // does not work

        // add next marker
        mNextMarker = mMap.addMarker(new MarkerOptions()
                .position(BOGUS_LOCATION)
                .icon(createMarker(R.drawable.marker_pin))
                .alpha(0.6f)
                .visible(false)
                .snippet("0")
                .title(MARKER_NEXT)
                .anchor(0.5f, 1f));
        mNextMarker.setVisible(false);

        addSanta();

        mSantaCamAnimator = new SantaCamAnimator(mMap, mSantaMarker);
    }

    private BitmapDescriptor createMarker(@DrawableRes int id) {
        final VectorDrawableCompat drawable =
                VectorDrawableCompat.create(getResources(), id, getActivity().getTheme());
        if (drawable == null) {
            return null;
        }
        final int width = drawable.getIntrinsicWidth();
        final int height = drawable.getIntrinsicHeight();
        drawable.setBounds(0, 0, width, height);
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public boolean isInitialised() {
        return this.mMap != null;
    }

    public GoogleMap getMap() {
        return mMap;
    }

    /**
     * Add a marker for a previous location.
     */
    public void addLocation(Destination destination) {
        mMap.addMarker(new MarkerOptions().position(destination.position)
                .icon(mMarkerIconVisited).anchor(0.5f, 1f).title(MARKER_PAST)
                .snippet(Integer.toString(destination.id)));
    }

    /**
     * If the active marker is set, hide its info window and restore the original
     * marker.
     */
    private void restoreClickedMarker() {
        if (this.mCurrentInfoMarker != null) {
            mActiveMarker.hideInfoWindow();
            mActiveMarker.setVisible(false);
            mCurrentInfoMarker.setPosition(mActiveMarker.getPosition());
            mActiveMarker.setPosition(BOGUS_LOCATION);
            mCurrentInfoMarker.setVisible(true);
            mCurrentInfoMarker = null;
        }
    }

    /**
     * Hides the current info window if it is displayed
     */
    public void hideInfoWindow() {
        if (this.mCurrentInfoMarker != null) {
            restoreClickedMarker();
            mCallback.onClearDestination();
        }
    }

    /**
     * Display the info window for a marker. The database is queried using a DestinationTask to
     * retrieve a Destination object.
     */
    private void showInfoWindow(Marker marker) {
        // disable santa cam mode
        if (mSantaCam) {
            disableSantaCam();
        }

        // store tapped marker as pending
        mPendingInfoMarker = marker;

        // hide window if it is currently displayed.
        hideInfoWindow();
        new DestinationTask().execute(Integer.parseInt(marker
                .getSnippet()));

        // App Measurement
        MeasurementManager.recordCustomEvent(mMeasurement,
                getString(R.string.analytics_event_category_tracker),
                getString(R.string.analytics_tracker_action_location),
                marker.getSnippet());

        // [ANALYTICS EVENT]: LocationSelected
        AnalyticsManager.sendEvent(R.string.analytics_event_category_tracker,
                R.string.analytics_tracker_action_location,
                marker.getSnippet());
    }

    private void showInfoWindow(Destination destination) {
        // ensure that destination data belongs to the pending info marker, ignore otherwise
        if (mPendingInfoMarker != null && destination != null &&
                mPendingInfoMarker.getSnippet() != null &&
                destination.id == Integer.parseInt(mPendingInfoMarker.getSnippet())) {
            // store selected marker
            mCurrentInfoMarker = mPendingInfoMarker;

            mPendingInfoMarker.setVisible(false);

            updateActiveDestination(destination, mCurrentInfoMarker);
            mInfoWindowAdapter.setData(destination);
            mActiveMarker.showInfoWindow();
            mPendingInfoMarker = null;

            mCallback.onShowDestination(destination);
        }
    }

    /**
     * Adds the Marker to the map.
     */
    public Marker addMarker(MarkerOptions m) {
        return this.mMap.addMarker(m);
    }

    /**
     * Sets the active marker to the given destination and makes it visible.
     */
    public void updateActiveDestination(Destination destination,
                                        Marker clickedMarker) {
        mActiveMarker.setPosition(destination.position);
        clickedMarker.setPosition(BOGUS_LOCATION);
        mActiveMarker.setVisible(true);
        mActiveMarker.setSnippet("" + destination.id);
    }

    /**
     * Info Window Click listener. When an info window is clicked, the displayed
     * info window is dismissed.
     */
    private OnInfoWindowClickListener mInfoWindowClickListener = new OnInfoWindowClickListener() {

        public void onInfoWindowClick(Marker arg0) {
            // dismiss the info window and restore original marker
            hideInfoWindow();
        }
    };

    public void setCamPadding(int left, int top, int right, int bottom) {
        mPaddingCamLeft = left;
        mPaddingCamTop = top;
        mPaddingCamRight = right;
        mPaddingCamBottom = bottom;
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.setPadding(mPaddingCamLeft, mPaddingCamTop,
                        mPaddingCamRight, mPaddingCamBottom);
            }
        });
        if (mSantaCam && mSantaMarker != null) {
            mSantaCamAnimator.triggerPaddingAnimation();
        }
    }

    private CancelableCallback mMovingCatchupCallback = new CancelableCallback() {
        @Override
        public void onFinish() {
            mSantaCamAnimator.resume();
            mSantaMarker.resumePresentsDrawing();
        }

        @Override
        public void onCancel() {
            mSantaCamAnimator.resume();
        }
    };

    private CancelableCallback mReachedAnimationCallback = new CancelableCallback() {

        @Override
        public void onFinish() {
            mHandler.post(mMoveToSantaRunnable);
        }

        @Override
        public void onCancel() {
            // ignore
        }
    };

    /**
     * Move camera: Reached destination in santa cam mode
     */
    Runnable mReachedDestinationRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMap != null) {
                mMap.animateCamera(AtLocation
                                .GetCameraUpdate(mSantaMarker.getPosition(),
                                        mMap.getCameraPosition().bearing),
                        SANTACAM_MOVE_TO_SANTA_DURATION, mReachedAnimationCallback);
            }
        }
    };

    /**
     * Move camera to center on Santa
     */
    public Runnable mMoveToSantaRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMap != null && mSantaMarker != null && mSantaMarker.getDestination() != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLng(mSantaMarker.getPosition()),
                        SANTACAM_MOVE_TO_SANTA_DURATION, null);
            }
        }
    };

    /**
     * AsyncTask that queries the database for a destination.
     */
    private class DestinationTask extends AsyncTask<Integer, Void, Destination> {

        @Override
        protected Destination doInBackground(Integer... params) {
            DestinationDbHelper dbHelper = DestinationDbHelper
                    .getInstance(getActivity().getApplicationContext());
            return dbHelper.getDestination(params[0]);
        }

        @Override
        protected void onPostExecute(Destination destination) {
            showInfoWindow(destination);
        }
    }

    /**
     * Interface for callbacks from this Fragment.
     *
     */
    public interface SantaMapInterface {

        /**
         * Called when the map has been initialised and is ready to be used.
         */
        void onMapInitialised();

        void mapClickAction();

        /**
         * Called when the santacam is enabled or disabled.
         */
        void onSantacamStateChange(boolean santacamEnabled);

        void onShowDestination(Destination destination);

        void onClearDestination();
    }

}
