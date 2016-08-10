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

package com.google.android.apps.santatracker.util;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

/** Handles communication with Firebase Analytics. */
public class MeasurementManager {

    private static final String TAG = "MeasurementManager";

    private static final String GAME_TITLE = "game_title";
    private static final String TYPE_SCREEN = "type_screen";

    public static void recordCustomEvent(FirebaseAnalytics measurement,
                                         @NonNull String name,
                                         @NonNull String action,
                                         @Nullable String label) {
        Log.d(TAG, "recordCustomEvent:" + name + ":" + action + ":" + label);

        Bundle params = new Bundle();
        params.putString("action", action);
        if (label != null) {
            params.putString("label", label);
        }
        measurement.logEvent(name, params);
    }

    public static void recordCustomEvent(FirebaseAnalytics measurement,
                                         @NonNull String name,
                                         @NonNull String action) {
        Log.d(TAG, "recordCustomEvent:" + name + ":" + action);

        recordCustomEvent(measurement, name, action, null);
    }

    public static void recordScreenView(FirebaseAnalytics measurement,
                                        @NonNull String id) {
        Log.d(TAG, "recordScreenView:" + id);

        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, TYPE_SCREEN);
        params.putString(FirebaseAnalytics.Param.ITEM_ID, id);
        measurement.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params);
    }

    public static void recordInvitationReceived(FirebaseAnalytics measurement,
                                                @NonNull String deepLink) {
        Log.d(TAG, "recordInvitationReceived:" + deepLink);

        Bundle params = new Bundle();
        params.putString("deepLink", deepLink);
        measurement.logEvent(FirebaseAnalytics.Event.APP_OPEN, params);
    }

    public static void recordInvitationSent(FirebaseAnalytics measurement,
                                            @NonNull String type,
                                            @NonNull String deepLink) {
        Log.d(TAG, "recordInvitationSent:" + type + ":" + deepLink);

        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, type);
        params.putSerializable(FirebaseAnalytics.Param.ITEM_ID, deepLink);
        measurement.logEvent(FirebaseAnalytics.Event.SHARE, params);
    }

    public static void recordLogin(FirebaseAnalytics measurement) {
        Log.d(TAG, "recordLogin");
        measurement.logEvent(FirebaseAnalytics.Event.LOGIN, null);
    }

    public static void recordAchievement(FirebaseAnalytics measurement,
                                         @NonNull String achId,
                                         @Nullable String gameTitle) {
        Log.d(TAG, "recordAchievement:" + achId + ":" + gameTitle);

        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, achId);
        if (gameTitle != null) {
            params.putString(GAME_TITLE, gameTitle);
        }
        measurement.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, params);
    }

    public static void recordGameScore(FirebaseAnalytics measurement,
                                       @NonNull Long score,
                                       @Nullable Long level,
                                       @Nullable String gameTitle) {
        Log.d(TAG, "recordGameEnd:" + gameTitle + ":" + score + ":" + level);

        Bundle params = new Bundle();
        params.putLong(FirebaseAnalytics.Param.SCORE, score);
        if (level != null) {
            params.putLong(FirebaseAnalytics.Param.LEVEL, level);
        }
        if (gameTitle != null) {
            params.putString(GAME_TITLE, gameTitle);
        }
        measurement.logEvent(FirebaseAnalytics.Event.POST_SCORE, params);
    }

}
