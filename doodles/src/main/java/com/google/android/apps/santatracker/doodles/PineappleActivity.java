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
package com.google.android.apps.santatracker.doodles;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.apps.santatracker.doodles.shared.DoodleConfig;
import com.google.android.apps.santatracker.doodles.shared.GameFragment;
import com.google.android.apps.santatracker.doodles.shared.LaunchDecisionMaker;
import com.google.android.apps.santatracker.doodles.shared.PineappleDebugLogger;
import com.google.android.apps.santatracker.invites.AppInvitesFragment;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Main activity to route to the various doodle games.
 */
public class PineappleActivity extends FragmentActivity {
  private static final String TAG = PineappleActivity.class.getSimpleName();
  private PineappleDebugLogger logger;
  private AppInvitesFragment appInvitesFragment;
  private FirebaseAnalytics mAnalytics;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_view);

    // Check for game direct launch
    DoodleConfig config;
    if (getIntent() != null && getIntent().hasExtra(LaunchDecisionMaker.START_GAME_KEY)) {
      config = new DoodleConfig(getIntent().getExtras(), null);
    } else {
      throw new IllegalStateException("Extra START_GAME_KEY required");
    }

    // [ANALYTICS]
    mAnalytics = FirebaseAnalytics.getInstance(this);
    String gameKey = getIntent().getStringExtra(LaunchDecisionMaker.START_GAME_KEY);
    switch (gameKey) {
      case LaunchDecisionMaker.WATERPOLO_GAME_VALUE:
        MeasurementManager.recordScreenView(mAnalytics,
                getString(R.string.analytics_screen_waterpolo));
        break;
      case LaunchDecisionMaker.RUNNING_GAME_VALUE:
        MeasurementManager.recordScreenView(mAnalytics,
                getString(R.string.analytics_screen_running));
        break;
      case LaunchDecisionMaker.SWIMMING_GAME_VALUE:
        MeasurementManager.recordScreenView(mAnalytics,
                getString(R.string.analytics_screen_swimming));
        break;
    }

    appInvitesFragment = AppInvitesFragment.getInstance(this);

    logger = new PineappleDebugLogger();
    Fragment fragment = LaunchDecisionMaker.makeFragment(this, config, logger);
    FragmentManager fragmentManager = getFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.add(R.id.activity_wrapper, fragment, "menu");
    fragmentTransaction.commit();
  }

  @Override
  public void onBackPressed() {
    // Get the current game fragment
    final Fragment fragment = getFragmentManager().findFragmentById(R.id.activity_wrapper);

    if (fragment instanceof GameFragment) {
      GameFragment gameFragment = (GameFragment) fragment;

      // Pause the game, or go back to the home screen if the game is paused already
      if (gameFragment.isGamePaused() || !gameFragment.isFinishedLoading() || gameFragment.isGameOver()) {
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

