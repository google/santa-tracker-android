/*
 * Copyright (C) 2017 Google Inc. All Rights Reserved.
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

package com.google.android.apps.santatracker.presentquest.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.apps.santatracker.presentquest.R;
import com.google.android.apps.santatracker.presentquest.ui.components.BounceInAnimator;
import com.google.android.apps.santatracker.presentquest.vo.User;
import com.google.android.apps.santatracker.presentquest.vo.Workshop;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

import static com.google.android.apps.santatracker.presentquest.ui.profile.ProfileViewModel.EXTRA_LAST_LOCATION;
import static com.google.android.apps.santatracker.presentquest.ui.profile.ProfileViewModel.EXTRA_LEVELED_UP;

// TODO turn back to AppCompatActivity
public class ProfileActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "PQ(ProfileActivity)";
    private ImageView mAvatarView;
    private TextView mLevelTextView;
    private SupportMapFragment[] mMaps = new SupportMapFragment[ProfileViewModel.MAX_WORKSHOPS];
    private WorkshopMapListener[] mListeners =
            new WorkshopMapListener[ProfileViewModel.MAX_WORKSHOPS];
    private ProfileViewModel mViewModel;

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

        mAvatarView = (ImageView) findViewById(R.id.profile_user_image);
        mLevelTextView = (TextView) findViewById(R.id.text_current_level);

        // ViewModel setup
        mViewModel = ViewModelProviders.of(this).get(ProfileViewModel.class);

        // Get position passed in
        if (getIntent() != null && getIntent().getParcelableExtra(EXTRA_LAST_LOCATION) != null) {
            LatLng latLng = getIntent().getParcelableExtra(EXTRA_LAST_LOCATION);
            mViewModel.setLatLng(latLng);
        }

        // Set up lock text
        // TODO update this
        ((TextView) findViewById(R.id.workshop_2_lock_text))
                .setText(getString(R.string.unlock_at_level, User.WORKSHOP_2_LEVEL));
        ((TextView) findViewById(R.id.workshop_3_lock_text))
                .setText(getString(R.string.unlock_at_level, User.WORKSHOP_3_LEVEL));

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

        // Setup workshops
        mViewModel
                .getWorkshops()
                .observe(
                        this,
                        new Observer<List<Workshop>>() {
                            @Override
                            public void onChanged(@Nullable List<Workshop> workshops) {
                                // Load map fragments
                                if (workshops != null) {
                                    for (int i = 0; i < ProfileViewModel.MAX_WORKSHOPS; i++) {
                                        Workshop currentWorkshop = null;
                                        if (i < workshops.size())
                                            currentWorkshop = workshops.get(i);

                                        if (mListeners[i] == null) {
                                            mListeners[i] =
                                                    new WorkshopMapListener(
                                                            ProfileActivity.this, currentWorkshop);
                                            mMaps[i].getMapAsync(mListeners[i]);
                                        } else {
                                            mListeners[i].setWorkshop(currentWorkshop);
                                        }
                                    }
                                }
                            }
                        });

        // Setup ProfileActivity based on user
        mViewModel
                .getUserViewState()
                .observe(
                        this,
                        new Observer<UserViewState>() {
                            @Override
                            public void onChanged(@Nullable UserViewState userViewState) {
                                bindUserViewState(userViewState);
                            }
                        });

        mViewModel
                .getLevelViewState()
                .observe(
                        this,
                        new Observer<LevelViewState>() {
                            @Override
                            public void onChanged(@Nullable LevelViewState levelViewState) {
                                bindLevelViewState(levelViewState);
                            }
                        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Special animation for leveling up
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_LEVELED_UP, false)) {
            bounceInAvatar();
        }
    }

    private void bindUserViewState(final UserViewState viewState) {

        setEditButtonVisible(findViewById(R.id.button_edit_1), viewState.isMagnet1Visible());
        setEditButtonVisible(findViewById(R.id.button_edit_2), viewState.isMagnet2Visible());
        setEditButtonVisible(findViewById(R.id.button_edit_3), viewState.isMagnet3Visible());

        // Lock workshop 2
        if (viewState.isSecondWorkshopUnlocked()) {
            findViewById(R.id.workshop_2_scrim).setVisibility(View.GONE);
        } else {
            findViewById(R.id.workshop_2_scrim).setVisibility(View.VISIBLE);
        }

        // Lock workshop 3
        if (viewState.isThirdWorkshopUnlocked()) {
            findViewById(R.id.workshop_3_scrim).setVisibility(View.GONE);
        } else {
            findViewById(R.id.workshop_3_scrim).setVisibility(View.VISIBLE);
        }
    }

    private void bindLevelViewState(LevelViewState levelViewState) {
        if (levelViewState != null) {
            mLevelTextView.setText(levelViewState.currentLevel);
            mAvatarView.setImageDrawable(
                    ContextCompat.getDrawable(this, levelViewState.currentAvatarResourceId));
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
        mAvatarView.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        BounceInAnimator.animate(mAvatarView);
                    }
                },
                750);
    }

    private void scrollAvatar(int delta) {
        mViewModel.scrollAvatar(delta);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_edit_1) {
            mViewModel.onMagnetWorkshipClick(1, this);

        } else if (v.getId() == R.id.button_edit_2) {
            mViewModel.onMagnetWorkshipClick(2, this);

        } else if (v.getId() == R.id.button_edit_3) {
            mViewModel.onMagnetWorkshipClick(3, this);

        } else if (v.getId() == R.id.profile_user_image) {
            supportFinishAfterTransition();

        } else if (v.getId() == R.id.arrow_left) {
            scrollAvatar(-1);

        } else if (v.getId() == R.id.arrow_right) {
            scrollAvatar(1);
        }
    }

    private boolean isDebug() {
        return getPackageName().contains("debug");
    }

    private static class WorkshopMapListener implements OnMapReadyCallback {

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
            googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            context, com.google.android.apps.santatracker.common.R.raw.map_style));

            // Control map UI
            UiSettings settings = googleMap.getUiSettings();
            settings.setMapToolbarEnabled(false);
            settings.setMyLocationButtonEnabled(false);

            // Prevent Google Maps from launching
            googleMap.setOnMapClickListener(
                    new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(LatLng latLng) {
                            // TODO: No-op
                        }
                    });

            // Show workshop
            moveMapToWorkshop();
        }

        public void setWorkshop(Workshop workshop) {
            this.workshop = workshop;
            moveMapToWorkshop();
        }

        private void moveMapToWorkshop() {
            if (this.workshop != null && googleMap != null) {
                LatLng workshopLatLng = workshop.getLatLng();

                googleMap.clear();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(workshopLatLng, 17f));
                googleMap.addMarker(
                        new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.workshop))
                                .position(workshopLatLng));
            }
        }
    }
}
