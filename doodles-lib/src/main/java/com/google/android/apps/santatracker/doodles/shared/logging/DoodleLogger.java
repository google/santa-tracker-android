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
package com.google.android.apps.santatracker.doodles.shared.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

/** Interface for logging DoodleEvents within the pineapple games. */
public abstract class DoodleLogger {
    private static final String PREFS_NAME = "PineappleLoggerPrefs";

    public abstract void logEvent(DoodleLogEvent event);

    public void logGameLaunchEvent(
            final Context context, final String gameType, final String eventName) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                int gamePlays = sharedPreferences.getInt(gameType, 0);
                int distinctGamesPlayed =
                        sharedPreferences.getInt(DoodleLogEvent.DISTINCT_GAMES_PLAYED, 0);
                if (gamePlays == 0) {
                    // If this is our first time playing this game, increment distinct games played.
                    editor.putInt(DoodleLogEvent.DISTINCT_GAMES_PLAYED, ++distinctGamesPlayed);
                }
                editor.putInt(gameType, ++gamePlays);
                editor.commit();

                logEvent(
                        new DoodleLogEvent.Builder(DoodleLogEvent.DEFAULT_DOODLE_NAME, eventName)
                                .withEventSubType(gameType)
                                .withEventValue1(gamePlays)
                                .withEventValue2(distinctGamesPlayed)
                                .build());

                return null;
            }
        }.execute();
    }
}
