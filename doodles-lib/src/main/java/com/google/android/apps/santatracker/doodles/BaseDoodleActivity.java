/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.apps.santatracker.doodles;

import static com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogEvent.DOODLE_LAUNCHED;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import com.google.android.apps.santatracker.doodles.shared.logging.DoodleDebugLogger;
import com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogTimer;
import com.google.android.apps.santatracker.doodles.shared.views.GameFragment;
import com.google.android.apps.santatracker.games.OnDemandActivity;
import com.google.android.apps.santatracker.invites.AppInvitesFragment;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.firebase.analytics.FirebaseAnalytics;

/** Base activity for doodle games */
public abstract class BaseDoodleActivity extends OnDemandActivity {

    private AppInvitesFragment appInvitesFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        // Setup Analytics
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        int stringResource = getAnalyticsStringResource();
        MeasurementManager.recordScreenView(analytics, getString(stringResource));

        appInvitesFragment = AppInvitesFragment.getInstance(this);

        // Setup Logging
        DoodleDebugLogger logger = new DoodleDebugLogger();
        String gameType = getGameType();

        // Setup Fragment
        Fragment fragment = makeFragment(logger);

        // Log fragment returned
        logger.logGameLaunchEvent(this, gameType, DOODLE_LAUNCHED);
        DoodleLogTimer.getInstance().reset();

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.activity_wrapper, fragment, "menu");
        fragmentTransaction.commit();
    }

    protected abstract String getGameType();

    protected abstract int getAnalyticsStringResource();

    protected abstract Fragment makeFragment(DoodleDebugLogger logger);

    @Override
    public void onBackPressed() {
        // Get the current game fragment
        final Fragment fragment = getFragmentManager().findFragmentById(R.id.activity_wrapper);

        if (fragment instanceof GameFragment) {
            GameFragment gameFragment = (GameFragment) fragment;

            // Pause the game, or go back to the home screen if the game is paused already
            if (gameFragment.isGamePaused()
                    || !gameFragment.isFinishedLoading()
                    || gameFragment.isGameOver()) {
                super.onBackPressed();
            } else {
                gameFragment.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    public AppInvitesFragment getAppInvitesFragment() {
        return appInvitesFragment;
    }
}
