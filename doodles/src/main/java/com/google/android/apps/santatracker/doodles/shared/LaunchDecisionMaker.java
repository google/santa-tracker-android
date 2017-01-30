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
package com.google.android.apps.santatracker.doodles.shared;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.android.apps.santatracker.doodles.pursuit.PursuitFragment;
import com.google.android.apps.santatracker.doodles.tilt.SwimmingFragment;
import com.google.android.apps.santatracker.doodles.waterpolo.WaterPoloFragment;

import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.DOODLE_LAUNCHED;
import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.RUNNING_GAME_TYPE;
import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.SWIMMING_GAME_TYPE;
import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.WATERPOLO_GAME_TYPE;

/**
 * Determines which Fragment we should start with based on the DoodleConfig data.  For example,
 * starting with the Swimming game versus going to the Main Menu.
 */
public class LaunchDecisionMaker {

  // Key for value of final Present Drop score.
  public static final String EXTRA_PRESENT_DROP_SCORE = "presentDropScore";
  public static final String EXTRA_PRESENT_DROP_STARS = "presentDropStars";

  // These are the keys and values we expect from the DoodleConfig setup in doodle canon.
  public static final String START_GAME_KEY = "startUp";
  public static final String RUNNING_GAME_VALUE = "running";
  public static final String WATERPOLO_GAME_VALUE = "waterpolo";
  public static final String SWIMMING_GAME_VALUE = "swimming";

  public static void finishActivity(Context context) {
    AndroidUtils.getActivityFromContext(context).finish();
  }

  public static void finishActivityWithResult(Context context, int resultCode, Bundle extras) {
    Activity activity = AndroidUtils.getActivityFromContext(context);
    Intent intent = activity.getIntent();
    intent.putExtras(extras);
    activity.setResult(resultCode, intent);
    activity.finish();
  }
  public static Fragment makeFragment(@Nullable Context context,
      @Nullable DoodleConfig doodleConfig, PineappleLogger logger) {
    String gameType = null;
    Fragment gameFragment = null;
    if (doodleConfig != null && doodleConfig.extraData != null) {
      // Check if we have a startup value.
      CharSequence startUp = doodleConfig.extraData.getCharSequence(START_GAME_KEY);
      if (startUp != null) {
        // Launch the right game if so.
         if (RUNNING_GAME_VALUE.equals(startUp)) {
          gameType = RUNNING_GAME_TYPE;
          gameFragment = new PursuitFragment(context, doodleConfig, logger);
        } else if (WATERPOLO_GAME_VALUE.equals(startUp)) {
          gameType = WATERPOLO_GAME_TYPE;
          gameFragment = new WaterPoloFragment(context, doodleConfig, logger);
        } else if (SWIMMING_GAME_VALUE.equals(startUp)) {
          gameType = SWIMMING_GAME_TYPE;
          gameFragment = new SwimmingFragment(context, doodleConfig, logger, false);
        }
      }
    }

    if (gameFragment != null) {
      logger.logGameLaunchEvent(context, gameType, DOODLE_LAUNCHED);
      PineappleLogTimer.getInstance().reset();
      return gameFragment;
    } else {
      throw new IllegalArgumentException("Invalid DoodleConfig: " + doodleConfig);
    }
  }

}
