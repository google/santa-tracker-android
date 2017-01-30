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

package com.google.android.apps.santatracker.games;

import android.os.Bundle;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.common.PlayGamesActivity;
import com.google.android.apps.santatracker.games.gumball.TiltGameFragment;
import com.google.android.apps.santatracker.launch.StartupActivity;
import com.google.android.apps.santatracker.util.AnalyticsManager;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.firebase.analytics.FirebaseAnalytics;

public class GumballActivity extends PlayGamesActivity {

    private static final String TAG = GumballActivity.class
            .getSimpleName();

    private TiltGameFragment mGumballFragment;
    private FirebaseAnalytics mMeasurement;

    public GumballActivity() {
        super(R.layout.activity_gumball, StartupActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGumballFragment = TiltGameFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainFragmentContainer, mGumballFragment)
                .commit();

        // App Measurement
        mMeasurement = FirebaseAnalytics.getInstance(this);
        MeasurementManager.recordScreenView(mMeasurement,
                getString(R.string.analytics_screen_gumball));

        // [ANALYTICS SCREEN]: Gumball
        AnalyticsManager.sendScreenView(R.string.analytics_screen_gumball);
    }

    @Override
    public void onBackPressed() {
        if (mGumballFragment != null) {
            mGumballFragment.onBackKeyPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onSignInSucceeded() {
        super.onSignInSucceeded();
        mGumballFragment.onSignInSucceeded();
    }

    @Override
    public String getGameId() {
        return getResources().getString(R.string.gumball_game_id);
    }

    @Override
    public String getGameTitle() {
        return getString(R.string.gumball);
    }

    @Override
    public void onSignInFailed() {
        super.onSignInFailed();
        mGumballFragment.onSignInFailed();
    }
}
