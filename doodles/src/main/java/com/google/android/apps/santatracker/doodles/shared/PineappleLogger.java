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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.Builder;

import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.DEFAULT_DOODLE_NAME;
import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.DISTINCT_GAMES_PLAYED;

/**
 * Interface for logging DoodleEvents within the pineapple games.
 */
public abstract class PineappleLogger {
  private static final String PREFS_NAME = "PineappleLoggerPrefs";

  public abstract void logEvent(PineappleLogEvent event);

  public void logGameLaunchEvent(
      final Context context, final String gameType, final String eventName) {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... voids) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        int gamePlays = sharedPreferences.getInt(gameType, 0);
        int distinctGamesPlayed = sharedPreferences.getInt(DISTINCT_GAMES_PLAYED, 0);
        if (gamePlays == 0) {
          // If this is our first time playing this game, increment distinct games played.
          editor.putInt(DISTINCT_GAMES_PLAYED, ++distinctGamesPlayed);
        }
        editor.putInt(gameType, ++gamePlays);
        editor.commit();

        logEvent(
            new Builder(DEFAULT_DOODLE_NAME, eventName)
                .withEventSubType(gameType)
                .withEventValue1(gamePlays)
                .withEventValue2(distinctGamesPlayed)
                .build());

        return null;
      }
    }.execute();
  }
}
