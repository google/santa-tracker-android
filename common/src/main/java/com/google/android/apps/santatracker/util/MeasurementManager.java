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

package com.google.android.apps.santatracker.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.apps.santatracker.common.BuildConfig;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Locale;

/** Handles communication with Firebase Analytics. */
public class MeasurementManager {

    private static final String TAG = "MeasurementManager";

    private static final String GAME_TITLE = "game_title";
    private static final String TYPE_SCREEN = "type_screen";

    /** User properties **/
    private static final String BUILD_DEBUG = "BUILD_DEBUG";
    private static final String BUILD_VERSION_NAME = "BUILD_VERSION_NAME";
    private static final String DEVICE_BOARD = "DEVICE_BOARD";
    private static final String DEVICE_BRAND = "DEVICE_BRAND";
    private static final String DEVICE_LOCALE = "DEVICE_LOCALE";
    private static final String API_LEVEL = "API_LEVEL";

    public static void recordDeviceProperties(Context context) {
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(
                context.getApplicationContext());

        // Set some user properties based on the device, this can be used for Analytics or
        // for Remote Config
        analytics.setUserProperty(BUILD_DEBUG, String.valueOf(BuildConfig.DEBUG));
        analytics.setUserProperty(DEVICE_BOARD, Build.BOARD);
        analytics.setUserProperty(DEVICE_BRAND, Build.BRAND);
        analytics.setUserProperty(DEVICE_LOCALE, Locale.getDefault().getLanguage());
        analytics.setUserProperty(API_LEVEL, String.valueOf(Build.VERSION.SDK_INT));

        try {
            // Set version name, if we can get it
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            analytics.setUserProperty(BUILD_VERSION_NAME, info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Could not get package info", e);
        }
    }

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

    public static void recordVillageTabClick(FirebaseAnalytics measurement,
                                             @NonNull String tabName) {
        Log.d(TAG, "recordVillageTabClick:" + tabName);

        Bundle params = new Bundle();
        params.putString("tab_name", tabName);

        measurement.logEvent("village_tab_clicked", params);
    }

    public static void recordVillageSantaClick(FirebaseAnalytics measurement) {
        Log.d(TAG, "recordVillageSantaClick");
        measurement.logEvent("village_santa_clicked", new Bundle());
    }

    public static void recordSwimmingEnd(FirebaseAnalytics measurement,
                                         int numStars,
                                         int score,
                                         @NonNull String end_reason) {
        Log.d(TAG, "recordSwimmingEnd:" + numStars + ":" + score + ":" + end_reason);

        Bundle params = new Bundle();
        params.putInt("num_stars", numStars);
        params.putInt("score", score);
        params.putString("end_reason", end_reason);

        // Log custom swimming event
        measurement.logEvent("swimming_game_end", params);

        // Log generic game score event
        recordGameScore(measurement, (long) score, null, "swimming");
    }

    public static void recordRunningEnd(FirebaseAnalytics measurement,
                                        int numStars,
                                        int score) {
        Log.d(TAG, "recordRunningEnd:" + numStars + ":" + score);

        Bundle params = new Bundle();
        params.putInt("num_stars", numStars);
        params.putInt("score", score);

        // Log custom swimming event
        measurement.logEvent("running_game_end", params);

        // Log generic game score event
        recordGameScore(measurement, (long) score, null, "running");
    }

    public static void recordPresentDropped(FirebaseAnalytics analytics,
                                            boolean isLarge) {
        Log.d(TAG, "recordPresentDropped:" + isLarge);

        Bundle params = new Bundle();
        if (isLarge) {
            params.putString("size", "large");
        } else {
            params.putString("size", "small");
        }

        analytics.logEvent("pq_present_dropped", params);
    }

    public static void recordPresentsCollected(FirebaseAnalytics analytics,
                                               int numPresents) {
        Log.d(TAG, "recordPresentsCollected:" + numPresents);

        Bundle params = new Bundle();
        params.putInt("num_presents", numPresents);

        analytics.logEvent("pq_presents_collected", params);
    }

    public static void recordPresentsReturned(FirebaseAnalytics analytics,
                                               int numPresents) {
        Log.d(TAG, "recordPresentsReturned:" + numPresents);

        Bundle params = new Bundle();
        params.putInt("num_presents", numPresents);

        analytics.logEvent("pq_presents_returned", params);
    }

    public static void recordPresentQuestLevel(FirebaseAnalytics analytics,
                                               int level) {
        Log.d(TAG, "recordPresentQuestLevel:" + level);

        Bundle params = new Bundle();
        params.putInt("level", level);

        // Log custom event
        analytics.logEvent("pq_level_unlocked", params);

        // Log standard LEVEL_UP event
        Bundle params2 = new Bundle();
        params2.putLong(FirebaseAnalytics.Param.LEVEL, (long) level);
        analytics.logEvent(FirebaseAnalytics.Event.LEVEL_UP, params2);
    }

    public static void recordWorkshopMoved(FirebaseAnalytics analytics) {
        Log.d(TAG, "recordWorkshopMoved");

        analytics.logEvent("pq_workshop_moved", new Bundle());
    }

    public static void recordHundredMetersWalked(FirebaseAnalytics analytics) {
        Log.d(TAG, "recordHundredMetersWalked");

        analytics.logEvent("pq_hundred_meters_walked", new Bundle());
    }

    public static void recordCorrectCitySelected(FirebaseAnalytics analytics,
                                                 @NonNull String cityId,
                                                 int numIncorrectAttempts) {
        Log.d(TAG, "recordCorrectCitySelected:" + cityId + ":" + numIncorrectAttempts);

        Bundle params = new Bundle();
        params.putString("city_id", cityId);
        params.putInt("incorrect_attempts", numIncorrectAttempts);

        analytics.logEvent("cq_select_correct", params);
    }

    public static void recordIncorrectCitySelected(FirebaseAnalytics analytics,
                                                   @NonNull String cityId) {
        Log.d(TAG, "recordIncorrectCitySelected:" + cityId);

        Bundle params = new Bundle();
        params.putString("city_id", cityId);

        analytics.logEvent("cq_select_incorrect", params);
    }

}
