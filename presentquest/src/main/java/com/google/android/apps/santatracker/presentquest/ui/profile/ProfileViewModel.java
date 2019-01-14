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

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.google.android.apps.santatracker.presentquest.R;
import com.google.android.apps.santatracker.presentquest.repository.PQRepository;
import com.google.android.apps.santatracker.presentquest.util.Config;
import com.google.android.apps.santatracker.presentquest.util.Distance;
import com.google.android.apps.santatracker.presentquest.util.FuzzyLocationUtil;
import com.google.android.apps.santatracker.presentquest.util.PreferencesUtil;
import com.google.android.apps.santatracker.presentquest.vo.Avatars;
import com.google.android.apps.santatracker.presentquest.vo.User;
import com.google.android.apps.santatracker.presentquest.vo.Workshop;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.List;

public class ProfileViewModel extends AndroidViewModel {
    static final int MAX_WORKSHOPS = 3;
    static final String EXTRA_LEVELED_UP = "extra_leveled_up";
    static final String EXTRA_LAST_LOCATION = "extra_last_location";

    private MutableLiveData<Integer> mAvatarIndex = new MutableLiveData<>();

    private LiveData<LevelViewState> mLevelViewState;

    private LiveData<User> mUser;

    private PreferencesUtil mPrefs;

    private Config mConfig;
    private FirebaseAnalytics mAnalytics;

    private LatLng mLatLng;

    private PQRepository repository;

    private LiveData<List<Workshop>> mWorkshops;
    private MediatorLiveData<UserViewState> mUserViewState = new MediatorLiveData<>();

    public ProfileViewModel(final Application application) {
        super(application);

        // Config
        mConfig = new Config();

        // [ANALYTICS]
        mAnalytics = FirebaseAnalytics.getInstance(application);
        MeasurementManager.recordScreenView(
                mAnalytics, application.getString(R.string.analytics_screen_pq_profile));

        // Preferences
        mPrefs = new PreferencesUtil(application);

        // Repository
        // TODO inject this
        repository = PQRepository.getInstance(application);

        // Get the current user
        mUser = repository.getUser();
        mWorkshops = repository.getAllWorkshopsNewToOld();

        mUserViewState.addSource(
                mUser,
                new Observer<User>() {
                    @Override
                    public void onChanged(@Nullable User user) {
                        if (user != null && mWorkshops.getValue() != null) {
                            initializeUI(user);
                        }
                    }
                });

        mUserViewState.addSource(
                mWorkshops,
                new Observer<List<Workshop>>() {
                    @Override
                    public void onChanged(@Nullable List<Workshop> workshops) {
                        if (mUser.getValue() != null && workshops != null) {
                            initializeUI(mUser.getValue());
                        }
                    }
                });

        // Sets up the mapping from the avatar index to the LevelViewState
        mLevelViewState =
                Transformations.map(
                        mAvatarIndex,
                        new Function<Integer, LevelViewState>() {
                            @Override
                            public LevelViewState apply(Integer avatarIndex) {
                                int currentLevel = avatarIndex + 1;
                                int currentAvatarResId = Avatars.AVATARS_UNLOCKED[avatarIndex];

                                boolean locked = currentLevel > mUser.getValue().getLevel();
                                if (locked) {
                                    currentAvatarResId = Avatars.AVATARS_LOCKED[avatarIndex];
                                }

                                return new LevelViewState(
                                        String.valueOf(currentLevel), currentAvatarResId);
                            }
                        });
    }

    private void initializeUI(User user) {

        UserViewState state = new UserViewState();

        mAvatarIndex.setValue(user.getLevel() - 1);

        state.setSecondWorkshopUnlocked(user.isWorkshopUnlocked(2));
        state.setThirdWorkshopUnlocked(user.isWorkshopUnlocked(3));

        // Allow or deny workshop "magnet"-ing if we don't know the location or if the user
        // is already near a workshop
        if (mLatLng == null || isAnyWorkshopNearby()) {
            state.setMagnet1Visible(false);
            state.setMagnet2Visible(false);
            state.setMagnet3Visible(false);
        } else {
            // Can't edit workshop 1 location until you've at least collected a present
            state.setMagnet1Visible(mPrefs.getHasCollectedPresent());
            state.setMagnet2Visible(user.isWorkshopUnlocked(2));
            state.setMagnet3Visible(user.isWorkshopUnlocked(3));
        }
        mUserViewState.setValue(state);

        mUserViewState.removeSource(mUser);
        mUserViewState.removeSource(mWorkshops);
    }

    private boolean isAnyWorkshopNearby() {

        for (Workshop workshop : mWorkshops.getValue()) {
            if (workshop != null
                    && Distance.between(mLatLng, workshop.getLatLng())
                            < mConfig.REACHABLE_RADIUS_METERS) {
                return true;
            }
        }
        return false;
    }

    // UI interactions
    void scrollAvatar(int delta) {
        // Check to make sure initial value is loaded
        if (mAvatarIndex.getValue() != null) {
            int tempIndex = mAvatarIndex.getValue() + delta;

            // Wrap around 0 --> last
            if (tempIndex < 0) {
                tempIndex = Avatars.NUM_AVATARS - 1;
            }

            // Wrap around last --> 0
            if (tempIndex >= Avatars.NUM_AVATARS) {
                tempIndex = 0;
            }

            mAvatarIndex.setValue(tempIndex);
        }
    }

    void onMagnetWorkshipClick(int workshopNumber, ProfileActivity activity) {

        // Make sure that the user and workshops have been loaded
        if (mUser.getValue() != null && mWorkshops.getValue() != null) {
            // If the workshop is unlocked...
            if (mUser.getValue().isWorkshopUnlocked(workshopNumber)) {
                int workshopIndexNumber = workshopNumber - 1;

                Workshop workshop = null;
                if (mWorkshops.getValue().size() >= workshopNumber) {
                    workshop = mWorkshops.getValue().get(workshopIndexNumber);
                }

                // Allow the user to move the workshop if they have not moved it today or if their
                // bag
                // is full and they don't have any workshops within 1km
                if (workshop != null && !(workshop.isMovable() || needsNearbyWorkshop())) {
                    // TODO not sure inserting the activity is kosher
                    Toast.makeText(
                                    activity,
                                    activity.getString(R.string.workshop_move_too_soon),
                                    Toast.LENGTH_SHORT)
                            .show();
                } else if (mLatLng != null) {
                    // Move workshop to current location
                    // Create workshop if necessary
                    if (workshop == null) {
                        workshop = new Workshop();
                    }

                    // Update workshop
                    LatLng fuzzyLocation = FuzzyLocationUtil.fuzz(mLatLng);
                    workshop.setLatLng(fuzzyLocation);
                    repository.saveWorkshopWithTimestamp(workshop);

                    // [ANALYTICS]
                    MeasurementManager.recordWorkshopMoved(mAnalytics);
                }
            }
        }
    }

    private boolean needsNearbyWorkshop() {
        if (mLatLng != null && mUser.getValue().getBagFillPercentage() >= 100f) {
            // Check if any workshop is within a certain radius (default 1km)
            for (Workshop workshop : mWorkshops.getValue()) {
                if (Distance.between(workshop.getLatLng(), mLatLng)
                        < mConfig.NEARBY_WORKSHOP_RADIUS) {
                    return false;
                }
            }

            // No close workshops
            return true;
        }

        return false;
    }

    // Getters and Setters

    void setLatLng(LatLng latLng) {
        mLatLng = latLng;
    }

    public LiveData<User> getUser() {
        return mUser;
    }

    public LiveData<List<Workshop>> getWorkshops() {
        return mWorkshops;
    }

    LiveData<UserViewState> getUserViewState() {
        return mUserViewState;
    }

    LiveData<LevelViewState> getLevelViewState() {
        return mLevelViewState;
    }
}
