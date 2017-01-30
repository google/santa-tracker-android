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
package com.google.android.apps.santatracker.presentquest.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.apps.santatracker.presentquest.model.Messages;

/**
 * Utility to manage some small game data that is stored in Shared Prefs and not in the DB.
 */
public class PreferencesUtil {

    private static final String KEY_HAS_ONBOARDED = "has_onboarded";
    private static final String KEY_HAS_COLLECTED_PRESENT = "has_collected_present";
    private static final String KEY_HAS_RETURNED_PRESENT = "has_returned_present";
    private static final String KEY_HAS_VISITED_PROFILE = "has_visited_profile";
    private static final String KEY_LAST_PLACES_API_REQUEST = "last_places_api_request";
    private static final String KEY_PREFIX_MESSAGE = "key_message:";

    private SharedPreferences mPrefs;

    public PreferencesUtil(Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean getHasOnboarded() {
        return mPrefs.getBoolean(KEY_HAS_ONBOARDED, false);
    }

    public void setHasOnboarded(boolean onboarded) {
        mPrefs.edit()
                .putBoolean(KEY_HAS_ONBOARDED, onboarded)
                .apply();
    }

    public boolean getHasCollectedPresent() {
        return mPrefs.getBoolean(KEY_HAS_COLLECTED_PRESENT, false);
    }

    public void setHasCollectedPresent(boolean collectedPresent) {
        mPrefs.edit()
                .putBoolean(KEY_HAS_COLLECTED_PRESENT, collectedPresent)
                .apply();
    }

    public void setHasReturnedPresent(boolean returnedPresent) {
        mPrefs.edit()
                .putBoolean(KEY_HAS_RETURNED_PRESENT, returnedPresent)
                .apply();
    }

    public boolean getHasReturnedPresent() {
        return mPrefs.getBoolean(KEY_HAS_RETURNED_PRESENT, false);
    }

    public boolean getHasVisitedProfile() {
        return mPrefs.getBoolean(KEY_HAS_VISITED_PROFILE, false);
    }

    public void setHasVisitedProfile(boolean visitedProfile) {
        mPrefs.edit()
                .putBoolean(KEY_HAS_VISITED_PROFILE, visitedProfile)
                .apply();;
    }

    public void setLastPlacesApiRequest(long timestamp) {
        mPrefs.edit()
                .putLong(KEY_LAST_PLACES_API_REQUEST, timestamp)
                .apply();
    }

    public long getLastPlacesApiRequest() {
        return mPrefs.getLong(KEY_LAST_PLACES_API_REQUEST, 0);
    }

    public int getMessageTimesDisplayed(Messages.Message message) {
        return mPrefs.getInt(KEY_PREFIX_MESSAGE + message.key, 0);
    }

    public void incrementMessageTimesDisplayed(Messages.Message message) {
        int timesDisplayed = getMessageTimesDisplayed(message);
        mPrefs.edit()
                .putInt(KEY_PREFIX_MESSAGE + message.key, timesDisplayed + 1)
                .apply();
    }

    // DEBUG ONLY - Reset everything
    public void resetAll() {
        mPrefs.edit().clear().apply();
    }

}
