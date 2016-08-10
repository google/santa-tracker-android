/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

package com.google.android.apps.santatracker.games.jetpack;

import android.os.Bundle;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.gamebase.SceneActivity;
import com.google.android.apps.santatracker.games.simpleengine.Scene;
import com.google.android.apps.santatracker.games.simpleengine.SceneManager;
import com.google.android.apps.santatracker.launch.StartupActivity;
import com.google.android.apps.santatracker.util.AnalyticsManager;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.firebase.analytics.FirebaseAnalytics;

public class JetpackActivity extends SceneActivity {

    private FirebaseAnalytics mMeasurement;

    public JetpackActivity() {
        super(R.layout.activity_jetpack, StartupActivity.class);

        // [ANALYTICS SCREEN]: Jetpack
        AnalyticsManager.sendScreenView(R.string.analytics_screen_jetpack);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // App Measurement
        mMeasurement = FirebaseAnalytics.getInstance(this);
        MeasurementManager.recordScreenView(mMeasurement,
                getString(R.string.analytics_screen_jetpack));
    }

    @Override
    protected Scene getGameScene() {
        return new JetpackScene();
    }

    @Override
    public void onBackPressed() {
        boolean handled = false;
        Scene scene = SceneManager.getInstance().getCurrentScene();
        if (scene instanceof JetpackScene) {
            handled = ((JetpackScene) scene).onBackKeyPressed();
        }
        if (!handled) {
            super.onBackPressed();
        }
    }

    @Override
    public String getGameId() {
        return getResources().getString(R.string.jetpack_game_id);
    }

    @Override
    public String getGameTitle() {
        return getString(R.string.elf_jetpack);
    }
}
