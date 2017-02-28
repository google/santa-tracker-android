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
package com.google.android.apps.santatracker.presentquest;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.santatracker.presentquest.model.Messages;
import com.google.android.apps.santatracker.presentquest.model.Place;
import com.google.android.apps.santatracker.presentquest.model.Present;
import com.google.android.apps.santatracker.presentquest.model.User;
import com.google.android.apps.santatracker.presentquest.model.Workshop;
import com.google.android.apps.santatracker.presentquest.ui.CirclePulseAnimator;
import com.google.android.apps.santatracker.presentquest.ui.ClickMeAnimator;
import com.google.android.apps.santatracker.presentquest.ui.OnboardingView;
import com.google.android.apps.santatracker.presentquest.ui.PlayGameDialog;
import com.google.android.apps.santatracker.presentquest.ui.PlayJetpackDialog;
import com.google.android.apps.santatracker.presentquest.ui.PlayWorkshopDialog;
import com.google.android.apps.santatracker.presentquest.ui.ScoreTextAnimator;
import com.google.android.apps.santatracker.presentquest.ui.SlideAnimator;
import com.google.android.apps.santatracker.presentquest.util.Config;
import com.google.android.apps.santatracker.presentquest.util.FuzzyLocationUtil;
import com.google.android.apps.santatracker.presentquest.util.MarkerCache;
import com.google.android.apps.santatracker.presentquest.util.PreferencesUtil;
import com.google.android.apps.santatracker.presentquest.util.VibrationUtil;
import com.google.android.apps.santatracker.util.MapHelper;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.apps.santatracker.util.NetworkHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MapsActivity extends AppCompatActivity implements
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        LocationListener,
        OnMapReadyCallback,
        View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        PlayGameDialog.GameDialogListener,
        EasyPermissions.PermissionCallbacks {

    private static final String TAG = "PQ(MapsActivity)";

    // RequestCode for launching the Workshop game
    public static final int RC_WORKSHOP_GAME = 9004;

    // RequestCode for launching the Jetpack game
    public static final int RC_JETPACK_GAME = 9005;

    // Intent action for moving workshop mode
    public static final String ACTION_MOVE_WORKSHOP = "ACTION_MOVE_WORKSHOP";

    // Extras for moving workshop
    public static final String EXTRA_MOVE_WORKSHOP_ID = "move_workshop_id";

    // Zoom when we're automatically moving the map as location changes.
    private static final int FOLLOWING_ZOOM = 16;

    // Location permissions
    private static final int RC_PERMISSIONS = 101;
    private static final int RC_SETTINGS = 102;
    private static final String[] PERMISSIONS_REQUIRED = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION};

    // Map and location
    private GoogleApiClient mGoogleApiClient;
    private SupportMapFragment mSupportMapFragment;
    private GoogleMap mMap;

    // User's current location.
    private LatLng mCurrentLatLng;

    // How far has the user walked since we last recorded it?
    private int mMetersWalked;

    // Are we moving the map as the user moves.
    private boolean mFollowing = true;

    // View elements for workshop moving flow
    private Marker mMovingWorkshopMarker;
    private View mWorkshopView;

    // Current game user
    private User mUser;

    // DialogFragment that can launch the Workshop game
    private PlayWorkshopDialog mWorkshopDialog = new PlayWorkshopDialog();

    // DialogFragment that can launch the Jetpack game
    private PlayJetpackDialog mJetpackDialog = new PlayJetpackDialog();

    // Marker showing current location
    private Marker mLocationMarker;

    // Circle that pulses around current location
    private CirclePulseAnimator mActionHorizonAnimator;
    private Circle mActionHorizon;

    // Text and animator for showing game scores
    private TextView mScoreTextView;
    private ScoreTextAnimator mScoreTextAnimator;

    // Views showing current level/progress.
    private ImageView mAvatarView;
    private ClickMeAnimator mAvatarAnimator;

    // Views showing bag fullness
    private ImageView mBagView;
    private ClickMeAnimator mBagAnimator;

    // Offline scrim
    private ViewGroup mMapScrim;

    // Snackbar
    private ViewGroup mSnackbar;
    private TextView mSnackbarText;
    private ImageView mSnackbarButton;

    // True when this activity has been launched for the purposes of moving the workshop
    private boolean mIsInWorkshopMoveMode = false;

    // Cache for Marker resources
    private MarkerCache mMarkerCache;

    // Map of workshop ID --> Marker
    private Map<Long, Marker> mWorkshopMarkers = new HashMap<>();

    // Map of present ID --> Marker
    private Map<Long, Marker> mPresentMarkers = new HashMap<>();

    // List of known presents
    private List<Present> mPresents;
    private List<Present> mReachablePresents = new ArrayList<>();

    // List of known workshops
    private List<Workshop> mWorkshops;
    private List<Workshop> mReachableWorkshops = new ArrayList<>();

    // Shared Prefs
    private PreferencesUtil mPreferences;

    // Firebase Analytics
    private FirebaseAnalytics mAnalytics;

    // Firebase Analytics
    private Config mConfig;

    // General-purpose handler
    private Handler mHandler = new Handler();

    // Receiver for results from PlacesIntentService
    private PlacesIntentService.NearbyResultReceiver mNearbyReceiver =
            new PlacesIntentService.NearbyResultReceiver() {
                @Override
                public void onResult(LatLng result) {
                    // Add present result
                    Log.d(TAG, "nearbyRecever:onResult");
                    addPresent(result, false);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // [ANALYTICS]
        mAnalytics = FirebaseAnalytics.getInstance(this);
        MeasurementManager.recordScreenView(mAnalytics,
                getString(R.string.analytics_screen_pq_map));

        // Init marker cache
        mMarkerCache = new MarkerCache(this);

        // Init prefs
        mPreferences = new PreferencesUtil(this);

        // Init views
        mAvatarView = (ImageView) findViewById(R.id.map_user_image);
        mBagView = (ImageView) findViewById(R.id.image_bag);
        mWorkshopView = findViewById(R.id.workshop);
        mScoreTextView = (TextView) findViewById(R.id.map_text_big_score);

        mMapScrim = (ViewGroup) findViewById(R.id.offline_map_scrim);

        mSnackbar = (ViewGroup) findViewById(R.id.snackbar_container);
        mSnackbarText = (TextView) findViewById(R.id.snackbar_text);
        mSnackbarButton = (ImageView) findViewById(R.id.close_snackbar);


        // Onboarding
        OnboardingView onboardingView = (OnboardingView) findViewById(R.id.container_onboarding);
        onboardingView.setOnFinishListener(new OnboardingView.OnFinishListener() {
            @Override
            public void onFinish() {
                mPreferences.setHasOnboarded(true);
            }
        });

        if (mPreferences.getHasOnboarded()) {
            onboardingView.setVisibility(View.GONE);
        } else {
            onboardingView.setVisibility(View.VISIBLE);
        }

        // Animator(s)
        mScoreTextAnimator = new ScoreTextAnimator(mScoreTextView);
        mAvatarAnimator = new ClickMeAnimator(mAvatarView);
        mBagAnimator = new ClickMeAnimator(mBagView);

        // Dialog listeners
        mJetpackDialog.setListener(this);
        mWorkshopDialog.setListener(this);

        // Debug UI
        initializeDebugUI();

        // Set current level
        mUser = User.get();
        setUserProgress();

        // Firebase config
        mConfig = new Config();

        // Click listeners
        mWorkshopView.setOnClickListener(this);
        mSnackbarButton.setOnClickListener(this);
        mMapScrim.setOnClickListener(this);
        findViewById(R.id.blue_bar).setOnClickListener(this);
        findViewById(R.id.map_user_image).setOnClickListener(this);
        findViewById(R.id.fab_location).setOnClickListener(this);
        findViewById(R.id.fab_accept_workshop_move).setOnClickListener(this);
        findViewById(R.id.fab_cancel_workshop_move).setOnClickListener(this);

        if (getIntent() != null) {
            mIsInWorkshopMoveMode = ACTION_MOVE_WORKSHOP.equals(getIntent().getAction());
        }

        initializeMap();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Update workshops and presents
        mWorkshops = Workshop.listAll(Workshop.class);
        mPresents = Present.listAll(Present.class);

        // Register result receiver for nearby places
        LocalBroadcastManager.getInstance(this).registerReceiver(mNearbyReceiver,
                PlacesIntentService.getNearbySearchIntentFilter());

        // Start animations
        if (mActionHorizonAnimator != null && mActionHorizon != null) {
            mActionHorizonAnimator.start();
        }

        // Nudge the user to go to the profile
        if (mPreferences.getHasCollectedPresent() && !mPreferences.getHasVisitedProfile()) {
            mAvatarAnimator.start();
        } else {
            mAvatarAnimator.stop();
        }

        initializeUI();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mMap != null && mCurrentLatLng != null) {
            mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    // Update workshops and draw markers
                    mWorkshops = Workshop.listAll(Workshop.class);
                    drawMarkers();
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister result receiver for nearby places
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mNearbyReceiver);

        // Stop animations
        if (mActionHorizonAnimator != null) {
            mActionHorizonAnimator.stop();
        }

        if (mAvatarAnimator != null) {
            mAvatarAnimator.stop();
        }

        if (mBagAnimator != null) {
            mBagAnimator.stop();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_JETPACK_GAME) {
            // Get score of the game
            int score = data != null ? data.getIntExtra("jetpack_score", 0) : 0;
            onPresentsCollected(score);
        }

        if (requestCode == RC_WORKSHOP_GAME) {
            // Get score of the game
            int stars = data != null ? data.getIntExtra("presentDropStars", 1) : 1;
            onPresentsReturned(stars);
        }

        // User has returned from being sent to the Settings screen to enable permissions
        if (requestCode == RC_SETTINGS) {
            if (EasyPermissions.hasPermissions(this, PERMISSIONS_REQUIRED)) {
                // We have the permissions we need, start location tracking
                startLocationTracking();
            } else {
                // User did not complete the task, quit the game
                onCompletePermissionDenial();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsInWorkshopMoveMode) {
            onCancelWorkshopMove();
            return;
        }

        super.onBackPressed();
    }

    private void initializeMap() {
        // Check for network and begin
        if (NetworkHelper.hasNetwork(this)) {
            // Kick off the map load, which kicks off the location update cycle which
            // is where all of the action happens
            mSupportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mSupportMapFragment.getMapAsync(this);

            // Hide the offline overlay
            mMapScrim.setVisibility(View.GONE);
        } else {
            // Show "offline" overlay
            mMapScrim.setVisibility(View.VISIBLE);
        }
    }

    private void onPresentsCollected(int score) {
        if (score > 0) {
            // Show the game score after a 500ms delay
            showScoreText(score, 500);

            // Mark that the user has done a successful present collection
            mPreferences.setHasCollectedPresent(true);

            // [ANALYTICS]
            MeasurementManager.recordPresentsCollected(mAnalytics, score);
        }

        // Update number of presents collected
        mUser.collectPresents(score);
        setUserProgress();

        // If the user's bag is full (or almost full) show a message
        if (mUser.getBagFillPercentage() >= 100) {
            showSnackbar(Messages.UNLOAD_BAG);
        } else if (mUser.getBagFillPercentage() >= 75) {
            showSnackbar(Messages.BAG_ALMOST_FULL);
        }
    }

    private void onPresentsReturned(int stars) {
        // Returning present works like this:
        // 0 stars - return 50% of bag
        // 1 stars - return 70% of bag
        // 2 stars - return 90% of bag
        // 3 stars - return 100% of bag

        float factor;
        int presentsReturned;
        switch (stars) {
            case 3:
                factor = 1.0f;
                break;
            case 2:
                factor = 0.90f;
                break;
            case 1:
                factor = 0.70f;
                break;
            default:
                factor = 0.50f;
                break;
        }

        // Multiply number of presents collected by return factor
        presentsReturned = (int) (mUser.presentsCollected * factor);

        if (presentsReturned > 0) {
            // Show the game score after a 500ms delay
            showScoreText(presentsReturned, 500);

            // Mark that the user has done a successful present return
            mPreferences.setHasReturnedPresent(true);

            // [ANALYTICS]
            MeasurementManager.recordPresentsReturned(mAnalytics, presentsReturned);
        }

        // Update number of presents returned
        int previousLevel = mUser.getLevel();
        mUser.returnPresentsAndEmpty(presentsReturned);
        setUserProgress();

        int currentLevel = mUser.getLevel();
        if (currentLevel != previousLevel) {
            // [ANALYTICS]
            MeasurementManager.recordPresentQuestLevel(mAnalytics, currentLevel);

            // Start level up animation
            onLevelUp();
        }
    }

    private void setUserProgress() {
        // Set level text
        ((TextView) findViewById(R.id.text_current_level)).setText(String.valueOf(mUser.getLevel()));

        // Set level progress
        ((ProgressBar) findViewById(R.id.progress_level)).setProgress(mUser.getLevelProgress());

        // Set avatar image
        mAvatarView.setImageDrawable(ContextCompat.getDrawable(this, mUser.getAvatar()));

        // Set bag fullness in text and image
        int percentFull = mUser.getBagFillPercentage();
        int bagImageId;
        if (percentFull >= 100) {
            bagImageId = R.drawable.bag_6;
        } else if (percentFull >= 75) {
            bagImageId = R.drawable.bag_5;
        } else if (percentFull >= 50) {
            bagImageId = R.drawable.bag_4;
        } else if (percentFull >= 25) {
            bagImageId = R.drawable.bag_3;
        } else if (percentFull > 0) {
            bagImageId = R.drawable.bag_2;
        } else {
            bagImageId = R.drawable.bag_1;
        }

        // Show fullness as percentage (0 to 100%)
        String percentString = String.valueOf(percentFull) + "%";
        ((TextView) findViewById(R.id.text_bag_level)).setText(percentString);

        // Show fullness image
        mBagView.setImageResource(bagImageId);

        // Pulse the bag if it's 100% full
        if (percentFull >= 100) {
            mBagAnimator.start();
        } else {
            mBagAnimator.stop();
        }
    }

    private void launchProfileActivity() {
        Intent intent = ProfileActivity.getIntent(MapsActivity.this, false, mCurrentLatLng);

        Pair<View, String> imagePair = new Pair<>(findViewById(R.id.map_user_image), "user_image");
        Pair<View, String> levelTextPair = new Pair<>(findViewById(R.id.layout_level_text), "level_text");

        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(MapsActivity.this, imagePair, levelTextPair);
        startActivity(intent, options.toBundle());
    }

    private void onLevelUp() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = ProfileActivity.getIntent(MapsActivity.this, true, mCurrentLatLng);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        }, 500);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setIndoorEnabled(false);
        mMap.setOnCameraMoveListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));

        // Min and max zoom
        mMap.setMaxZoomPreference(FOLLOWING_ZOOM + 1.0f);
        mMap.setMinZoomPreference(FOLLOWING_ZOOM - 3.0f);

        // Only enable showing user location if in workshop edit mode
        //noinspection MissingPermission
        mMap.setMyLocationEnabled(mIsInWorkshopMoveMode);

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(false);

        if (mIsInWorkshopMoveMode) {
            drawMarkerForWorkshop(getMovingWorkshopId());
            startWorkshopMove(getMovingWorkshopId());
        } else {
            startLocationTracking();
        }
    }

    @AfterPermissionGranted(RC_PERMISSIONS)
    private void startLocationTracking() {
        // Check for location permissions
        if (!EasyPermissions.hasPermissions(this, PERMISSIONS_REQUIRED)) {
            // Request permissions
            EasyPermissions.requestPermissions(this, getString(R.string.perm_location_rationale),
                    RC_PERMISSIONS, PERMISSIONS_REQUIRED);
        }

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .enableAutoManage(this, this)
                    .addApi(LocationServices.API)
                    .build();
        } else if (mGoogleApiClient.isConnected()) {
            requestLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @SuppressWarnings("MissingPermission")
    private void requestLocationUpdates() {
        if (!EasyPermissions.hasPermissions(this, PERMISSIONS_REQUIRED)) {
            return;
        }

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            onLocationChanged(location);
        }

        LocationRequest request = new LocationRequest();
        request.setInterval(mConfig.LOCATION_REQUEST_INTERVAL_MS);
        request.setFastestInterval(mConfig.LOCATION_REQUEST_INTERVAL_FASTEST_MS);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            Log.w(TAG, "onLocationChanged: null location");
            return;
        }

        // Check if no workshop exists, create one if the user has ever collected a present
        if (mWorkshops.isEmpty() && mUser.getPresentsCollectedAllTime() > 0) {
            Workshop workshop = new Workshop();

            // Put the workshop very close to current location but slightly offset
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            workshop.setLatLng(FuzzyLocationUtil.fuzz(latLng));
            workshop.save();

            // Reload workshops
            mWorkshops = Workshop.listAll(Workshop.class);
        }

        // If this is the first run, we'll draw markers.
        boolean firstRun = mCurrentLatLng == null;

        // Update our current location only if we've moved at least a metre, to avoid
        // jitter due to lack of accuracy in FusedLocationApi.
        LatLng newLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (firstRun || Distance.between(mCurrentLatLng, newLatLng) > 1) {
            updateMetersWalked(mCurrentLatLng, newLatLng);

            mCurrentLatLng = newLatLng;
            if (mFollowing) {
                if (firstRun) {
                    safeMoveCamera(mMap, CameraUpdateFactory.newLatLngZoom(mCurrentLatLng, FOLLOWING_ZOOM));
                } else {
                    safeAnimateCamera(mMap, CameraUpdateFactory.newLatLngZoom(mCurrentLatLng, FOLLOWING_ZOOM));
                }
            }
        }

        if (firstRun && !mPresents.isEmpty()) {
            animateToNearbyPresents();
        }

        // Draw all markers on the map
        drawMarkers();
        drawMarkerLabels();

        // Update reachable presents and workshops, nudge the user to click on things when appropriate
        int oldPresentsReachable = mReachablePresents.size();
        mReachablePresents = getPresentsReachable();
        if ((mReachablePresents.size() > oldPresentsReachable) && mUser.getBagFillPercentage() < 100) {
            // New present reachable and bag is not full, show prompt
            showSnackbar(Messages.CLICK_PRESENT);
        }

        int oldWorkshopsReachable = mReachableWorkshops.size();
        mReachableWorkshops = getWorkshopsReachable();
        if ((mReachableWorkshops.size() > oldWorkshopsReachable) && mUser.getBagFillPercentage() > 0) {
            // New workshop reachable and bag is not empty, show prompt
            showSnackbar(Messages.CLICK_WORKSHOP);
        }

        // Ask the places service for a new place if too few nearby.
        int near = getPresentsNearby().size();
        if (near < mConfig.MIN_NEARBY_PRESENTS) {
            // [DEBUG ONLY] log the user's position
            if (isDebug()) {
                Log.d(TAG, "Searching for places near: " + mCurrentLatLng);
            }
            PlacesIntentService.startNearbySearch(this, mCurrentLatLng, mConfig.NEARBY_RADIUS_METERS);
        }

        // Action horizon
        initCurrentLocationMarkers();

        // Update locations
        mLocationMarker.setPosition(mCurrentLatLng);
        mActionHorizon.setCenter(mCurrentLatLng);
    }

    private void initCurrentLocationMarkers() {
        // Show current location
        MarkerOptions options = mMarkerCache.getElfMarker().position(mCurrentLatLng);
        if (mLocationMarker == null) {
            mLocationMarker = mMap.addMarker(options);
        } else {
            MarkerCache.updateMarker(mLocationMarker, options);
        }

        // Create circle around current location
        if (mActionHorizon == null) {
            mActionHorizon = mMap.addCircle(new CircleOptions()
                    .center(mCurrentLatLng)
                    .radius(10.0f)
                    .strokeColor(ContextCompat.getColor(this, R.color.action_horizon_stroke))
                    .fillColor(ContextCompat.getColor(this, R.color.action_horizon_fill)));
        }

        // Create animations for circle around current location
        if (mActionHorizonAnimator == null) {
            mActionHorizonAnimator = new CirclePulseAnimator(this, mActionHorizon, mConfig.REACHABLE_RADIUS_METERS);
            mActionHorizonAnimator.start();
        }
    }

    private void addPresent(LatLng latLng, boolean force) {
        if (!isNearLatLng(latLng) || force) {  // Don't add if too close.

            float chanceIsLarge = mUser.getLargePresentChance();
            float randomChance = new Random().nextFloat();
            boolean isLarge = (randomChance <= chanceIsLarge);

            Present newPresent = new Present(latLng, isLarge);
            newPresent.save();

            // Show a message about the new present
            showSnackbar(Messages.NEW_PRESENT);

            // [ANALYTICS]
            MeasurementManager.recordPresentDropped(mAnalytics, newPresent.isLarge);

            // Reload the presents
            mPresents = getPresentsSorted();

            // If adding the present exceeds MAX_PRESENTS, delete the farthest.
            if (mPresents.size() > mConfig.MAX_PRESENTS) {
                Present removePresent = mPresents.get(mPresents.size() - 1);
                deletePresent(removePresent);
            }

            animateToNearbyPresents();
            drawMarkers();
            drawMarkerLabels();
        }
    }

    private void animateToNearbyPresents() {
        // Animate to bounds showing presents and current location.
        LatLngBounds.Builder bounds = LatLngBounds.builder().include(mCurrentLatLng);
        List<Present> nearbyPresents = getPresentsNearby();
        if (nearbyPresents.size() == 0) {
            // If no nearby presents, add last one
            nearbyPresents.add(Present.last(Present.class));
        } else {
            for (Present present : nearbyPresents) {
                bounds.include(present.getLatLng());
            }
        }
        safeAnimateCamera(mMap, CameraUpdateFactory.newLatLngBounds(bounds.build(),
                MapHelper.getMapPadding(mSupportMapFragment)));
    }

    private boolean isNearLatLng(LatLng latLng) {
        if (latLng == null) {
            return false;
        }

        return Distance.between(mCurrentLatLng, latLng) <= mConfig.REACHABLE_RADIUS_METERS;
    }

    private void initializeUI() {
        if (mIsInWorkshopMoveMode) {
            findViewById(R.id.fab_location).setVisibility(View.GONE);
            findViewById(R.id.fab_accept_workshop_move).setVisibility(View.VISIBLE);
            findViewById(R.id.fab_cancel_workshop_move).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.fab_location).setVisibility(View.VISIBLE);
            findViewById(R.id.fab_accept_workshop_move).setVisibility(View.GONE);
            findViewById(R.id.fab_cancel_workshop_move).setVisibility(View.GONE);
        }
    }

    private void initializeDebugUI() {
        if (!isDebug()) {
            return;
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_debug);
        fab.setVisibility(View.VISIBLE);

        final PopupMenu popup = new PopupMenu(this, fab);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_debug_popup, popup.getMenu());

        // Show the menu when clicked
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popup.show();
            }
        });

        // Handle debug menu clicks
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final int id = item.getItemId();
                if (id == R.id.item_drop_present) {
                    // This should be about 20m away, so within the action radius
                    LatLng fuzzyCurrent = FuzzyLocationUtil.fuzz(mCurrentLatLng);
                    addPresent(fuzzyCurrent, true);
                }

                if (id == R.id.item_delete_present) {
                    if (!mPresents.isEmpty()) {
                        deletePresent(mPresents.get(0));
                        drawMarkers();
                        drawMarkerLabels();
                    }
                }

                if (id == R.id.item_play_jetpack) {
                    showPlayJetpackDialog(null);
                }

                if (id == R.id.item_play_present_toss) {
                    showPlayWorkshopDialog();
                }

                if (id == R.id.item_collect_20) {
                    onPresentsCollected(20);
                }

                if (id == R.id.item_return) {
                    onPresentsReturned(3);
                }

                if (id == R.id.item_downlevel) {
                    mUser.downlevel();
                    setUserProgress();
                }

                if (id == R.id.reset_prefs) {
                    // Reset prefs
                    mPreferences.resetAll();

                    // Reset user
                    mUser.presentsCollected = 0;
                    mUser.presentsReturned = 0;
                    mUser.save();

                    // Delete all workshops
                    Workshop.deleteAll(Workshop.class);

                    // Delete all places and presents
                    Present.deleteAll(Present.class);
                    Place.deleteAll(Place.class);

                    // Restart the activity
                    recreate();
                }

                return true;
            }
        });
    }

    private void drawMarkers() {
        // Workshop markers
        for (Workshop workshop : mWorkshops) {
            boolean isNear = isNearLatLng(workshop.getLatLng());
            Marker workshopMarker = mWorkshopMarkers.get(workshop.getId());

            // Get workshop marker options
            MarkerOptions options = mMarkerCache.getWorkshopMarker(isNear)
                    .position(workshop.getLatLng())
                    .visible(false);

            // Add or update marker
            if (workshopMarker == null) {
                workshopMarker = mMap.addMarker(options);
            } else {
                MarkerCache.updateMarker(workshopMarker, options);
            }

            // Tag marker with workshop
            workshopMarker.setTag(workshop);

            // Hide workshop markers for moving workshop
            workshopMarker.setVisible(getMovingWorkshopId() != workshop.getId());

            // Cache
            mWorkshopMarkers.put(workshop.getId(), workshopMarker);
        }

        // Present markers, only drawn when not in workshop moving mode
        if (!mIsInWorkshopMoveMode) {
            for (Present present : mPresents) {

                boolean isNear = isNearLatLng(present.getLatLng());
                Marker presentMarker = mPresentMarkers.get(present.getId());

                // Get present marker options
                MarkerOptions options = mMarkerCache.getPresentMarker(present, isNear)
                        .position(present.getLatLng());

                // Add or update marker
                if (presentMarker == null) {
                    presentMarker = mMap.addMarker(options);
                } else {
                    MarkerCache.updateMarker(presentMarker, options);
                }

                // Tag marker with present
                presentMarker.setTag(present);

                // Cache
                mPresentMarkers.put(present.getId(), presentMarker);
            }
        }
    }

    /**
     * Draws a single Workshop marker, always in non-pin mode.
     * @param workshopId the ID of the workshop to draw.
     */
    private void drawMarkerForWorkshop(long workshopId) {
        Workshop workshop = Workshop.findById(Workshop.class, workshopId);

        Marker workshopMarker = mMap.addMarker(mMarkerCache.getWorkshopMarker(true)
                .position(workshop.getLatLng())
                .visible(false));

        workshopMarker.setTag(workshop);
        workshopMarker.setVisible(getMovingWorkshopId() != workshop.getId());

        // Cache
        mWorkshopMarkers.put(workshopId, workshopMarker);
    }

    /**
     * Draw helpful labels on top of map markers to nudge the player in the right direction.
     */
    private void drawMarkerLabels() {
        // Show marker label if the user has never collected a present
        if (!mPreferences.getHasCollectedPresent()) {
            // Get first present marker
            long presentId = mPresents.isEmpty() ? -1 : mPresents.get(0).getId();
            Marker presentMarker = mPresentMarkers.get(presentId);

            // Show marker label
            if (presentMarker != null) {
                presentMarker.setTitle(getString(R.string.go_here));
                presentMarker.showInfoWindow();
            }
        } else {
            // If the user has collected a present, hide all marker labels on presents
            for (Marker presentMarker : mPresentMarkers.values()) {
                presentMarker.setTitle(null);
                presentMarker.hideInfoWindow();
            }
        }

        // Show marker label if the user has collected presents but never returned
        if (mUser.getBagFillPercentage() > 0 && !mPreferences.getHasReturnedPresent()) {
            // Get workshop marker
            long workshopId = mWorkshops.isEmpty() ? -1 : mWorkshops.get(0).getId();
            Marker workshopMarker = mWorkshopMarkers.get(workshopId);

            // Show marker label
            if (workshopMarker != null) {
                workshopMarker.setTitle(getString(R.string.go_here));
                workshopMarker.showInfoWindow();
            }
        } else {
            // Hide the workshop marker labels
            for (Marker workshopMarker : mWorkshopMarkers.values()) {
                workshopMarker.setTitle(null);
                workshopMarker.hideInfoWindow();
            }
        }
    }

    private long getMovingWorkshopId() {
        if (!mIsInWorkshopMoveMode) {
            return 0;
        } else {
            return getIntent().getLongExtra(EXTRA_MOVE_WORKSHOP_ID, 1L);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Move the camera.  If the map is not yet loaded, catch the exception and try the move again
     * after it loads. This helps to avoid a rare race condition on very low-spec devices.
     */
    private void safeMoveCamera(final GoogleMap map, final CameraUpdate cameraUpdate) {
        try {
            map.moveCamera(cameraUpdate);
        } catch (IllegalStateException e) {
            map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    map.moveCamera(cameraUpdate);
                }
            });
        }
    }

    /**
     * Animate the camera.  If the map is not yet loaded, catch the exception and try the animation
     * again after it loads. This helps to avoid a rare race condition on very low-spec devices.
     */
    private void safeAnimateCamera(final GoogleMap map, final CameraUpdate cameraUpdate) {
        try {
            map.animateCamera(cameraUpdate);
        } catch (IllegalStateException e) {
            map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    map.animateCamera(cameraUpdate);
                }
            });
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Object object = marker.getTag();
        if (object instanceof Workshop) {
            return onWorkshopMarkerClick((Workshop) object, marker);
        } else if (object instanceof Present) {
            return onPresentMarkerClick((Present) object, marker);
        } else {
            return false;
        }
    }

    private boolean onWorkshopMarkerClick(Workshop workshop, Marker marker) {
        if (isDebug() && !workshop.isMovable()) {
            workshop.updated = 0;
            workshop.save();
            toast("DEBUG: Workshop now movable");
        }

        if (!isNearLatLng(marker.getPosition())) {
            showSnackbar(Messages.WORKSHOP_TOO_FAR);
            VibrationUtil.vibratePattern(this, VibrationUtil.PATTERN_BAD);
        } else if (mUser.presentsCollected == 0) {
            showSnackbar(Messages.NO_PRESENTS_COLLECTED);
            VibrationUtil.vibratePattern(this, VibrationUtil.PATTERN_BAD);
        } else {
            showPlayWorkshopDialog();
        }

        return true;
    }

    private void startWorkshopMove(long workshopId) {
        if (mMovingWorkshopMarker != null) {
            return;
        }

        Workshop workshop = Workshop.findById(Workshop.class, workshopId);
        Marker workshopMarker = mWorkshopMarkers.get(workshopId);

        // Go to workshop location, or near the last workshops if this is a new workshop
        LatLng target = workshop.getLatLng();
        if (Workshop.NULL_LATLNG.equals(target)) {
            Workshop firstWorkshop = Workshop.first(Workshop.class);
            LatLng firstLatLng = firstWorkshop.getLatLng();
            target = FuzzyLocationUtil.fuzz(firstLatLng);
        }

        safeAnimateCamera(mMap, CameraUpdateFactory.newLatLngZoom(target, FOLLOWING_ZOOM));
        mWorkshopView.setVisibility(View.VISIBLE);
        mMovingWorkshopMarker = workshopMarker;
        mMovingWorkshopMarker.setVisible(false);
    }

    private boolean onPresentMarkerClick(Present present, Marker marker) {
        if (isNearLatLng(present.getLatLng())) {
            if (mUser.getBagFillPercentage() >= 100) {
                // Bag is full, warn the user but allow them to get more presents
                showSnackbar(Messages.BAG_IS_FULL);
                VibrationUtil.vibratePattern(this, VibrationUtil.PATTERN_BAD);
            }

            // Near the present, play the jetpack game
            showPlayJetpackDialog(present);

            // TODO: Only delete if they play the game
            deletePresent(present);
        } else {
            // Too far from the presents
            showSnackbar(Messages.PRESENT_TOO_FAR);
            VibrationUtil.vibratePattern(this, VibrationUtil.PATTERN_BAD);
        }

        return true;
    }

    private void showPlayWorkshopDialog() {
        mWorkshopDialog.show(getSupportFragmentManager(), PlayWorkshopDialog.TAG);
    }

    private void showPlayJetpackDialog(Present present) {
        mJetpackDialog.show(getSupportFragmentManager(), PlayJetpackDialog.TAG);
        if (present != null) {
            mJetpackDialog.setPresent(present);
        }
    }

    private void showScoreText(final int score, long delayMs) {
        mScoreTextView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScoreTextAnimator.start("+" + String.valueOf(score));
            }
        }, delayMs);
    }

    private void showSnackbar(Messages.Message message) {
        // Don't show messages if already showing the snackbar
        if (mSnackbar.getVisibility() == View.VISIBLE) {
            return;
        }

        // Record how many times a message is displayed, and don't display it too many times.
        int numTimesDisplayed = mPreferences.getMessageTimesDisplayed(message);
        if (numTimesDisplayed >= message.timesToShow) {
            Log.d(TAG, "showSnackbar: not showing " + message.key);
            return;
        } else {
            mPreferences.incrementMessageTimesDisplayed(message);
        }

        // Hide location button, it can get in the way
        findViewById(R.id.fab_location).setVisibility(View.GONE);

        // Set text and slide up
        mSnackbarText.setText(getString(message.stringId));
        SlideAnimator.slideUp(mSnackbar);

        // Slide back down after some delay
        mSnackbar.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideSnackbar();
            }
        }, 3000);
    }

    private void hideSnackbar() {
        // Don't play the hide animation if the snackbar is already invisible
        if (mSnackbar.getVisibility() != View.VISIBLE) {
            return;
        }

        // Show location button
        findViewById(R.id.fab_location).setVisibility(View.VISIBLE);

        // Hide snackbar
        SlideAnimator.slideDown(mSnackbar);
    }

    private void onCancelWorkshopMove() {
        Workshop workshop = (Workshop) mMovingWorkshopMarker.getTag();
        if (Workshop.NULL_LATLNG.equals(workshop.getLatLng())) {
            workshop.delete();
        }

        setResult(RESULT_CANCELED);
        finish();
    }

    private void endWorkshopMove() {
        // Workshop view was clicked - workshop placement mode exited, so save the new
        // workshop location, hide the workshop view, and redraw markers (to reflect the
        // new workshop location).
        LatLng latLng = mMap.getCameraPosition().target;
        Workshop workshop = (Workshop) mMovingWorkshopMarker.getTag();
        workshop.setLatLng(latLng);
        workshop.saveWithTimestamp();

        mWorkshopView.setVisibility(View.INVISIBLE);
        mMovingWorkshopMarker = null;
    }

    @Override
    public boolean onMyLocationButtonClick() {
        if (!mFollowing && mCurrentLatLng != null) {
            safeAnimateCamera(mMap, CameraUpdateFactory.newLatLngZoom(mCurrentLatLng, FOLLOWING_ZOOM));
            mFollowing = true;
        }
        return true;
    }

    @Override
    public void onCameraMove() {
        mFollowing = false;
    }

    private void updateMetersWalked(LatLng oldLocation, LatLng newLocation) {
        if (oldLocation == null || newLocation == null) {
            return;
        }

        int distance = Distance.between(oldLocation, newLocation);
        mMetersWalked += distance;

        // Record distances in increments of 100 meters walked
        while (mMetersWalked >= 100) {
            MeasurementManager.recordHundredMetersWalked(mAnalytics);
            mMetersWalked -= 100;
        }
    }

    private List<Present> getPresentsSorted() {
        List<Present> presents = Present.listAll(Present.class);
        Collections.sort(presents, new PresentComparator());
        return presents;
    }

    private List<Present> getPresentsNearby() {
        return getPresentsInRadius(mConfig.NEARBY_RADIUS_METERS);
    }

    private List<Present> getPresentsReachable() {
        return getPresentsInRadius(mConfig.REACHABLE_RADIUS_METERS);
    }

    private List<Present> getPresentsInRadius(int radius) {
        if (mCurrentLatLng == null) {
            return new ArrayList<>();
        }

        List<Present> presents = new ArrayList<>();
        for (Present present : mPresents) {
            if (Distance.between(mCurrentLatLng, present.getLatLng()) <= radius) {
                presents.add(present);
            }
        }
        return presents;
    }

    private List<Workshop> getWorkshopsReachable() {
        if (mCurrentLatLng == null) {
            return new ArrayList<>();
        }

        List<Workshop> workshops = new ArrayList<>();
        for (Workshop workshop : mWorkshops) {
            if (Distance.between(mCurrentLatLng, workshop.getLatLng()) <= mConfig.REACHABLE_RADIUS_METERS) {
                workshops.add(workshop);
            }
        }
        return workshops;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.map_user_image || id == R.id.blue_bar) {
            mAvatarAnimator.stop();
            mPreferences.setHasVisitedProfile(true);
            launchProfileActivity();
        } else if (id == R.id.workshop) {
            // No-op
        } else if (id == R.id.fab_location){
            onMyLocationButtonClick();
        } else if (id == R.id.fab_accept_workshop_move) {
            endWorkshopMove();
            setResult(RESULT_OK);
            finish();
        } else if (id == R.id.fab_cancel_workshop_move) {
            onCancelWorkshopMove();
        } else if (id == R.id.close_snackbar) {
            hideSnackbar();
        } else if (id == R.id.offline_map_scrim) {
            initializeMap();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    public void onDialogShow() {
        if (mActionHorizonAnimator != null) {
            mActionHorizonAnimator.stop();
        }
    }

    @Override
    public void onDialogDismiss() {
        if (mActionHorizonAnimator != null) {
            mActionHorizonAnimator.start();
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + perms);
        startLocationTracking();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + perms);

        if (!EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            // If they deny it but non-permanently, we have to ask again
            startLocationTracking();
        } else {
            // This game will not work without location permissions, so we need to detect
            // if permissions are permanently denied and send the user to the settings screen
            new AppSettingsDialog.Builder(this, getString(R.string.perm_go_to_settings))
                    .setTitle(getString(R.string.perm_required))
                    .setPositiveButton(getString(android.R.string.ok))
                    .setNegativeButton(getString(android.R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    onCompletePermissionDenial();
                                }
                            })
                    .setRequestCode(RC_SETTINGS)
                    .build()
                    .show();
        }
    }

    private void onCompletePermissionDenial() {
        // Show Toast message
        Toast.makeText(this,
                getString(R.string.required_permissions_missing),
                Toast.LENGTH_SHORT).show();

        // Finish the Activity
        finish();
    }

    private class PresentComparator implements Comparator<Present> {

        @Override
        public int compare(Present a, Present b) {
            int distA = Distance.between(mCurrentLatLng, a.getLatLng());
            int distB = Distance.between(mCurrentLatLng, b.getLatLng());
            return distA - distB;
        }
    }

    private void deletePresent(Present present) {
        Marker marker = mPresentMarkers.remove(present.getId());
        if (marker != null) {
            marker.remove();
        }

        present.delete();
        mPresents = getPresentsSorted();  // Reload.
    }

    @Override
    public void onMapLongClick(LatLng location) {
        if (isDebug()) {
            // Add a present where the click was
            addPresent(location, true);
        }
    }

    private boolean isDebug() {
        return getPackageName().contains("debug");
    }

}
