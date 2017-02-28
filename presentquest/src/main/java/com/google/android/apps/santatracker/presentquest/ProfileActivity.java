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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.santatracker.presentquest.model.Avatars;
import com.google.android.apps.santatracker.presentquest.model.User;
import com.google.android.apps.santatracker.presentquest.model.Workshop;
import com.google.android.apps.santatracker.presentquest.ui.BounceInAnimator;
import com.google.android.apps.santatracker.presentquest.util.Config;
import com.google.android.apps.santatracker.presentquest.util.FuzzyLocationUtil;
import com.google.android.apps.santatracker.presentquest.util.PreferencesUtil;
import com.google.android.apps.santatracker.util.FontHelper;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.List;

public class ProfileActivity extends AppCompatActivity implements
        View.OnClickListener {

    private static final String TAG = "PQ(ProfileActivity)";

    private static final int MAX_WORKSHOPS = 3;
    private static final int RC_MOVE_WORKSHOP = 9001;

    public static final String EXTRA_LEVELED_UP = "extra_leveled_up";
    public static final String EXTRA_LAST_LOCATION = "extra_last_location";

    private ImageView mAvatarView;
    private TextView mLevelTextView;

    private Workshop[] mWorkshops = new Workshop[MAX_WORKSHOPS];
    private SupportMapFragment[] mMaps = new SupportMapFragment[MAX_WORKSHOPS];
    private WorkshopMapListener[] mListeners = new WorkshopMapListener[MAX_WORKSHOPS];

    private int mAvatarIndex;

    private User mUser;

    private PreferencesUtil mPrefs;

    private FirebaseAnalytics mAnalytics;

    private Config mConfig;

    private LatLng mLatLng;

    public static Intent getIntent(Context context, boolean leveledUp, LatLng location) {
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.putExtra(EXTRA_LEVELED_UP, leveledUp);
        intent.putExtra(EXTRA_LAST_LOCATION, location);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // [ANALYTICS]
        mAnalytics = FirebaseAnalytics.getInstance(this);
        MeasurementManager.recordScreenView(mAnalytics,
                getString(R.string.analytics_screen_pq_profile));

        // Config
        mConfig = new Config();

        // Get the current user
        mUser = User.get();

        // Preferences
        mPrefs = new PreferencesUtil(this);

        mAvatarView = (ImageView) findViewById(R.id.profile_user_image);
        mLevelTextView = (TextView) findViewById(R.id.text_current_level);

        // Make text santa-style
        FontHelper.makeLobster((TextView) findViewById(R.id.text_workshops), false);

        // Set up lock text
        ((TextView) findViewById(R.id.workshop_2_lock_text))
                .setText(getString(R.string.unlock_at_level, User.WORKSHOP_2_LEVEL));
        ((TextView) findViewById(R.id.workshop_3_lock_text))
                .setText(getString(R.string.unlock_at_level, User.WORKSHOP_3_LEVEL));

        // Get position passed in
        if (getIntent() != null && getIntent().getParcelableExtra(EXTRA_LAST_LOCATION) != null) {
            mLatLng = getIntent().getParcelableExtra(EXTRA_LAST_LOCATION);
        }

        // Click listeners
        mAvatarView.setOnClickListener(this);
        findViewById(R.id.button_edit_1).setOnClickListener(this);
        findViewById(R.id.button_edit_2).setOnClickListener(this);
        findViewById(R.id.button_edit_3).setOnClickListener(this);
        findViewById(R.id.arrow_left).setOnClickListener(this);
        findViewById(R.id.arrow_right).setOnClickListener(this);

        // Get map fragments
        FragmentManager fm = getSupportFragmentManager();
        mMaps[0] = (SupportMapFragment) fm.findFragmentById(R.id.map_workshop_1);
        mMaps[1] = (SupportMapFragment) fm.findFragmentById(R.id.map_workshop_2);
        mMaps[2] = (SupportMapFragment) fm.findFragmentById(R.id.map_workshop_3);

        // Load workshops
        loadWorkshops();

        // Load map fragments
        for (int i = 0; i < MAX_WORKSHOPS; i++) {
            mListeners[i] = new WorkshopMapListener(this, mWorkshops[i]);
            mMaps[i].getMapAsync(mListeners[i]);
        }

        setUserProgress();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Special animation for leveling up
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_LEVELED_UP, false)) {
            bounceInAvatar();
        }
    }

    private void loadWorkshops() {
        // Get a list of all workshops in order of ascending ID (aka new to old).
        List<Workshop> all = Workshop.listAll(Workshop.class, "id ASC");
        for (int i = 0; i < MAX_WORKSHOPS; i++) {
            if (all.size() > i) {
                mWorkshops[i] = all.get(i);
            } else {
                mWorkshops[i] = null;
            }
        }
    }

    private void refreshWorkshops() {
        loadWorkshops();
        for (int i = 0; i < MAX_WORKSHOPS; i++) {
            mListeners[i].setWorkshop(mWorkshops[i]);
        }
    }

    private boolean isAnyWorkshopNearby() {
        for (Workshop workshop : mWorkshops) {
            if (workshop != null && Distance.between(
                    mLatLng, workshop.getLatLng()) < mConfig.REACHABLE_RADIUS_METERS) {
                return true;
            }
        }

        return false;
    }

    private void setUserProgress() {
        mAvatarIndex = mUser.getLevel() - 1;
        mLevelTextView.setText(String.valueOf(mUser.getLevel()));
        mAvatarView.setImageDrawable(ContextCompat.getDrawable(this, mUser.getAvatar()));

        // Can't edit workshop 1 location until you've at least collected a present
        setEditButtonVisible(findViewById(R.id.button_edit_1), mPrefs.getHasCollectedPresent());

        // Lock workshop 2
        boolean secondWorkshopUnlocked = mUser.getMaxPresentsCollected() >= 2;
        if (secondWorkshopUnlocked) {
            findViewById(R.id.workshop_2_scrim).setVisibility(View.GONE);
        } else {
            findViewById(R.id.workshop_2_scrim).setVisibility(View.VISIBLE);
        }
        setEditButtonVisible(findViewById(R.id.button_edit_2), secondWorkshopUnlocked);

        // Lock workshop 3
        boolean thirdWorkshopUnlocked = mUser.getMaxPresentsCollected() >= 3;
        if (thirdWorkshopUnlocked) {
            findViewById(R.id.workshop_3_scrim).setVisibility(View.GONE);
        } else {
            findViewById(R.id.workshop_3_scrim).setVisibility(View.GONE);
        }
        setEditButtonVisible(findViewById(R.id.button_edit_3), thirdWorkshopUnlocked);

        // Allow or deny workshop "magnet"-ing if we don't know the location or if the user
        // is already near a workshop
        if (mLatLng == null || isAnyWorkshopNearby()) {
            setEditButtonVisible(findViewById(R.id.button_edit_1), false);
            setEditButtonVisible(findViewById(R.id.button_edit_2), false);
            setEditButtonVisible(findViewById(R.id.button_edit_3), false);
        }
    }

    private void setEditButtonVisible(View button, boolean visible) {
        if (visible) {
            // Show the button at full strength
            button.setVisibility(View.VISIBLE);
            button.setAlpha(1.0f);
        } else if (isDebug()) {
            // When debugging never hide the buttons, just dim them
            button.setAlpha(0.5f);
        } else {
            // When not debugging, hide the button fully
            button.setVisibility(View.GONE);
        }
    }

    private void bounceInAvatar() {
        mAvatarView.postDelayed(new Runnable() {
            @Override
            public void run() {
                BounceInAnimator.animate(mAvatarView);
            }
        }, 750);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_MOVE_WORKSHOP) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, getString(R.string.workshop_moved), Toast.LENGTH_SHORT).show();
                refreshWorkshops();

                // [ANALYTICS]
                MeasurementManager.recordWorkshopMoved(mAnalytics);
            } else {
                Toast.makeText(this, getString(R.string.move_canceled), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public boolean needsNearbyWorkshop() {
        if (mLatLng != null && mUser.getBagFillPercentage() >= 100f) {
            // Check if any workshop is within a certain radius (default 1km)
            for (Workshop workshop : mWorkshops) {
                if(Distance.between(workshop.getLatLng(), mLatLng) < mConfig.NEARBY_WORKSHOP_RADIUS) {
                    return false;
                }
            }

            // No close workshops
            return true;
        }

        return false;
    }

    public void onMagnetWorkshopClick(Workshop workshop) {
        // Allow the user to move the workshop if they have not moved it today or if their bag
        // is full and they don't have any workshops within 1km
        if (workshop != null && !(workshop.isMovable() || needsNearbyWorkshop())) {
            Toast.makeText(this, getString(R.string.workshop_move_too_soon),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Move workshop to current location
        if (mLatLng != null) {
            // Create workshop if necessary
            if (workshop == null) {
                workshop = new Workshop();
            }

            // Update workshop
            LatLng fuzzyLocation = FuzzyLocationUtil.fuzz(mLatLng);
            workshop.setLatLng(fuzzyLocation);
            workshop.saveWithTimestamp();

            // [ANALYTICS]
            MeasurementManager.recordWorkshopMoved(mAnalytics);
        }

        // Refresh workshops
        refreshWorkshops();
    }

    public void onEditWorkshopClick(Workshop workshop) {
        if (workshop != null && !workshop.isMovable()) {
            Toast.makeText(this, getString(R.string.workshop_move_too_soon),
                    Toast.LENGTH_SHORT).show();
        } else {
            // May need to create workshop first
            if (workshop == null) {
                workshop = new Workshop();
                workshop.setLatLng(Workshop.NULL_LATLNG);

                workshop.save();
            }

            Intent intent = new Intent(this, MapsActivity.class);
            intent.setAction(MapsActivity.ACTION_MOVE_WORKSHOP);
            intent.putExtra(MapsActivity.EXTRA_MOVE_WORKSHOP_ID, workshop.getId());

            startActivityForResult(intent, RC_MOVE_WORKSHOP);
        }
    }

    private void scrollAvatar(int delta) {
        mAvatarIndex = mAvatarIndex + delta;

        // Wrap around 0 --> last
        if (mAvatarIndex < 0) {
            mAvatarIndex = Avatars.NUM_AVATARS - 1;
        }

        // Wrap around last --> 0
        if (mAvatarIndex >= Avatars.NUM_AVATARS) {
            mAvatarIndex = 0;
        }

        // Set image
        int levelNumber = mAvatarIndex + 1;
        boolean locked = levelNumber > mUser.getLevel();
        if (locked) {
            mAvatarView.setImageResource(Avatars.AVATARS_LOCKED[mAvatarIndex]);
        } else {
            mAvatarView.setImageResource(Avatars.AVATARS_UNLOCKED[mAvatarIndex]);
        }

        // Set level text
        mLevelTextView.setText(String.valueOf(levelNumber));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_edit_1) {
            onMagnetWorkshopClick(mWorkshops[0]);
        }

        if (v.getId() == R.id.button_edit_2) {
            // Check if user has unlocked second workshop
            if (mUser.getMaxWorkshops() < 2) {
                return;
            }

            onMagnetWorkshopClick(mWorkshops[1]);
        }

        if (v.getId() == R.id.button_edit_3) {
            // Check if user has unlocked third workshop
            if (mUser.getMaxWorkshops() < 3) {
                return;
            }

            onMagnetWorkshopClick(mWorkshops[2]);
        }

        if (v.getId() == R.id.profile_user_image) {
            supportFinishAfterTransition();
        }

        if (v.getId() == R.id.arrow_left) {
            scrollAvatar(-1);
        }

        if (v.getId() == R.id.arrow_right) {
            scrollAvatar(1);
        }
    }

    private boolean isDebug() {
        return getPackageName().contains("debug");
    }

    private class WorkshopMapListener implements OnMapReadyCallback {

        Context context;
        Workshop workshop;

        GoogleMap googleMap;

        WorkshopMapListener(Context context, Workshop workshop) {
            this.context = context;
            this.workshop = workshop;
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            this.googleMap = googleMap;

            // Santa Style
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style));

            // Control map UI
            UiSettings settings = googleMap.getUiSettings();
            settings.setMapToolbarEnabled(false);
            settings.setMyLocationButtonEnabled(false);

            // Prevent Google Maps from launching
            googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    // TODO: No-op
                }
            });

            // Show workshop
            setWorkshop(workshop);
        }

        public void setWorkshop(Workshop workshop) {
            this.workshop = workshop;

            if (workshop != null && googleMap != null) {
                LatLng workshopLatLng = workshop.getLatLng();

                googleMap.clear();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(workshopLatLng, 17f));
                googleMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.workshop))
                        .position(workshopLatLng));
            }
        }
    }
}
