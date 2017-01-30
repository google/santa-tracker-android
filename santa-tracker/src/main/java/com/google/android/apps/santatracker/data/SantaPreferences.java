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

package com.google.android.apps.santatracker.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * Singleton that manages access to internal data stored as preferences.
 */
@SuppressLint("CommitPrefEdits")
public class SantaPreferences {

    private static final int PREFERENCE_VERSION = 3;

    // for a +/- this value (ms) retrieved offset, the difference is simply
    // ignored and not adjusted
    public static final int OFFSET_ACCEPTABLE_RANGE_DIFFERENCE = 120000;
    private static final String TAG = "SantaPreferences";
    // Shared time offset
    private static long TIME_OFFSET = 0L;

    private SharedPreferences settings;

    private static final String PREFERENCES_FILENAME = "SantaTracker";

    // Preference parameters
    private static final String PREF_TIMESTAMP = "PREF_TIMESTAMP";
    private static final String PREF_NEXT_INFO_ACCESS_TIMESTAMP = "PREF_INFO_API_TIMESTAMP";
    private static final String PREF_FINGERPRINT = "PREF_FINGERPRINT";
    private static final String PREF_ROUTEOFFSET = "PREF_ROUTEOFFSET";
    private static final String PREF_STREAMOFFSET = "PREF_STREAMOFFSET";
    private static final String PREF_TOTALPRESENTS = "PREF_TOTALPRESENTS";

    private static final String PREF_OFFSET = "PREF_OFFSET";
    private static final String PREF_SWITCHOFF = "PREF_SWITCHOFF";
    private static final String PREF_VIDEO1 = "PREF_VIDEO1";
    private static final String PREF_VIDEO15 = "PREF_VIDEO15";
    private static final String PREF_VIDEO23 = "PREF_VIDEO23";

    private static final String PREF_CASTDISABLED = "PREF_CASTDISABLED";
    private static final String PREF_PHOTODISABLED = "PREF_PHOTODISABLED";
    private static final String PREF_GUMBALLDISABLED = "PREF_GUMBALLDISABLED";
    private static final String PREF_JETPACKDISABLED = "PREF_JETPACKDISABLED";
    private static final String PREF_MEMORYDISABLED = "PREF_MEMORYDISABLED";
    private static final String PREF_ROCKETDISABLED = "PREF_ROCKETDISABLED";
    private static final String PREF_DANCERDISABLED = "PREF_DANCERDISABLED";
    private static final String PREF_SNOWDOWNDISABLED = "PREF_SNOWDOWNDISABLED";
    private final static String PREF_SWIMMING_DISABLED = "PREF_SWIMMING_DISABLED";
    private final static String PREF_BMX_DISABLED = "PREF_BMX_DISABLED";
    private final static String PREF_RUNNING_DISABLED = "PREF_RUNNING_DISABLED";
    private final static String PREF_TENNIS_DISABLED = "PREF_TENNIS_DISABLED";
    private final static String PREF_WATERPOLO_DISABLED = "PREF_WATERPOLO_DISABLED";
    private final static String PREF_CITY_QUIZ_DISABLED = "PREF_CITY_QUIZ_DISABLED";
    private final static String PREF_PRESENT_QUEST_DISABLED = "PREF_PRESENT_QUEST_DISABLED";
    private static final String PREF_RANDVALUE = "PREF_RANDVALUE";
    private static final String PREF_DBVERSION_DEST = "PREF_DBVERSION";
    private static final String PREF_DBVERSION_STREAM = "PREF_DBSTREAMVERSION";
    private static final String PREF_PREFVERSION = "PREF_PREFERENCEVERSION";

    // cached rand value
    private static float rand;

    public SantaPreferences(Context context) {
        settings = context.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE);

        // update the cached rand value if it has been set, otherwise it will be
        // overwritten later
        rand = settings.getFloat(PREF_RANDVALUE, -1f);

        // initialise time offset
        SantaPreferences.TIME_OFFSET = getOffset();

        // Handle Preference upgrades
        checkUpgrade();
    }

    private void checkUpgrade() {
        final int storedVersion = settings.getInt(PREF_PREFVERSION, 1);

        if (PREFERENCE_VERSION > storedVersion) {
            onUpgrade(storedVersion, PREFERENCE_VERSION);
        }
    }

    private void onUpgrade(int oldVersion, int preferenceVersion) {
        Editor editor = settings.edit();

        Log.d(TAG, String.format("Upgrading from %d to %d.", oldVersion, preferenceVersion));

        if (oldVersion >= 1) {
            // Delete all unused cast flags from v1
            for (String key : LegacyPreferences.CastFlags_1.ALL_FLAGS) {
                editor.remove(key);
            }
            // Delete all other unused flags
            for (String key : LegacyPreferences.PREFERENCES){
                editor.remove(key);
            }
        }

        // store new preference version
        editor.putInt(PREF_PREFVERSION, preferenceVersion);

        editor.commit();
    }

    /**
     * Returns true if the preferences have been initialised and the database contains valid data.
     */
    public boolean hasValidData() {
        return getFingerprint() != null && getRandValue() >= 0
                && getDBTimestamp() > 1;

    }

    /**
     * Returns the hash of the current data stored in the database. Returns {@link Long#MIN_VALUE}
     * if no hash has been set.
     */
    public String getFingerprint() {
        return settings.getString(PREF_FINGERPRINT, null);
    }

    public boolean getSwitchOff() {
        return settings.getBoolean(PREF_SWITCHOFF, false);
    }

    public void setSwitchOff(boolean switchOff) {
        Editor editor = settings.edit();
        editor.putBoolean(PREF_SWITCHOFF, switchOff);
        editor.commit();
    }

    /**
     * Returns the timestamp at which the INFO-API should next be accessed or 0 if not set.
     */
    public long getNextInfoAPIAccess() {
        return settings.getLong(PREF_NEXT_INFO_ACCESS_TIMESTAMP, 0L);
    }

    public void setNextInfoAPIAccess(long time) {
        Editor editor = settings.edit();
        editor.putLong(PREF_NEXT_INFO_ACCESS_TIMESTAMP, time);
        editor.commit();
    }

    public int getRouteOffset() {
        return settings.getInt(PREF_ROUTEOFFSET, 0);
    }

    public void setRouteOffset(int routeOffset) {
        Editor editor = settings.edit();
        editor.putInt(PREF_ROUTEOFFSET, routeOffset);
        editor.commit();
    }

    public int getStreamOffset() {
        return settings.getInt(PREF_STREAMOFFSET, 0);
    }

    public void setStreamOffset(int streamOffset) {
        Editor editor = settings.edit();
        editor.putInt(PREF_STREAMOFFSET, streamOffset);
        editor.commit();
    }

    public long getTotalPresents() {
        return settings.getLong(PREF_TOTALPRESENTS, 0L);
    }

    public void setTotalPresents(long presents) {
        Editor editor = settings.edit();
        editor.putLong(PREF_TOTALPRESENTS, presents);
        editor.commit();
    }

    public void invalidateData() {
        Editor editor = settings.edit();
        editor.putLong(PREF_TIMESTAMP, Long.MIN_VALUE);
        editor.putString(PREF_FINGERPRINT, null);
        editor.putInt(PREF_ROUTEOFFSET, 0);
        editor.putInt(PREF_STREAMOFFSET, 0);
        editor.putLong(PREF_TOTALPRESENTS, 0L);
        editor.commit();
    }

    public long getOffset() {
        return settings.getLong(PREF_OFFSET, 0);
    }

    public void setOffset(long offset) {
        Editor editor = settings.edit();
        editor.putLong(PREF_OFFSET, offset);
        editor.commit();
        SantaPreferences.TIME_OFFSET = offset;
    }

    private long getDBTimestamp() {
        return settings.getLong(PREF_TIMESTAMP, 0);
    }

    public void setDBTimestamp(long time) {
        Editor editor = settings.edit();
        editor.putLong(PREF_TIMESTAMP, time);
        editor.commit();
    }

    public float getRandValue() {
        return rand;
    }

    public void setRandValue(float value) {
        Editor editor = settings.edit();
        editor.putFloat(PREF_RANDVALUE, value);
        editor.commit();

        rand = value;
    }

    /**
     * Stores the fingerprint of the data stored in the database.
     */
    public void setFingerprint(String fingerprint) {
        Editor editor = settings.edit();
        editor.putString(PREF_FINGERPRINT, fingerprint);
        editor.commit();
    }

    /**
     * Checks if all values in Switches are consistent with stored prefs.
     */
    public boolean gameDisabledStateConsistent(GameDisabledState state) {
        return getGumballDisabled() == state.disableGumballGame
                && getJetpackDisabled() == state.disableJetpackGame
                && getMemoryDisabled() == state.disableMemoryGame
                && getRocketDisabled() == state.disableRocketGame
                && getDancerDisabled() == state.disableDancerGame
                && getSnowdownDisabled() == state.disableSnowdownGame
                && getSwimmingDisabled() == state.disableSwimmingGame
                && getBmxDisabled() == state.disableBmxGame
                && getRunningDisabled() == state.disableRunningGame
                && getTennisDisabled() == state.disableTennisGame
                && getWaterpoloDisabled() == state.disableWaterpoloGame
                && getCityQuizDisabled() == state.disableCityQuizGame
                && getPresentQuestDisabled() == state.disablePresentQuest;
    }

    /**
     * Overwrite all game disabled prefs from Switches.
     */
    public void setGamesDisabled(GameDisabledState state) {
        Editor editor = settings.edit();
        editor.putBoolean(PREF_GUMBALLDISABLED, state.disableGumballGame);
        editor.putBoolean(PREF_JETPACKDISABLED, state.disableJetpackGame);
        editor.putBoolean(PREF_MEMORYDISABLED, state.disableMemoryGame);
        editor.putBoolean(PREF_ROCKETDISABLED, state.disableRocketGame);
        editor.putBoolean(PREF_DANCERDISABLED, state.disableDancerGame);
        editor.putBoolean(PREF_SNOWDOWNDISABLED, state.disableSnowdownGame);
        editor.putBoolean(PREF_SWIMMING_DISABLED, state.disableSwimmingGame);
        editor.putBoolean(PREF_BMX_DISABLED, state.disableBmxGame);
        editor.putBoolean(PREF_RUNNING_DISABLED, state.disableRunningGame);
        editor.putBoolean(PREF_TENNIS_DISABLED, state.disableTennisGame);
        editor.putBoolean(PREF_WATERPOLO_DISABLED, state.disableWaterpoloGame);
        editor.putBoolean(PREF_CITY_QUIZ_DISABLED, state.disableCityQuizGame);
        editor.putBoolean(PREF_PRESENT_QUEST_DISABLED, state.disablePresentQuest);
        editor.commit();
    }

    boolean getGumballDisabled() {
        return settings.getBoolean(PREF_GUMBALLDISABLED, false);
    }

    boolean getJetpackDisabled() {
        return settings.getBoolean(PREF_JETPACKDISABLED, false);
    }

    boolean getMemoryDisabled() {
        return settings.getBoolean(PREF_MEMORYDISABLED, false);
    }

    boolean getRocketDisabled() {
        return settings.getBoolean(PREF_ROCKETDISABLED, false);
    }

    boolean getDancerDisabled() {
        return settings.getBoolean(PREF_DANCERDISABLED, false);
    }

    boolean getSnowdownDisabled() {
        return settings.getBoolean(PREF_SNOWDOWNDISABLED, false);
    }

    boolean getSwimmingDisabled() {
        return settings.getBoolean(PREF_SWIMMING_DISABLED, false);
    }

    boolean getBmxDisabled() {
        return settings.getBoolean(PREF_BMX_DISABLED, false);
    }

    boolean getRunningDisabled() {
        return settings.getBoolean(PREF_RUNNING_DISABLED, false);
    }

    boolean getTennisDisabled() {
        return settings.getBoolean(PREF_TENNIS_DISABLED, false);
    }

    boolean getWaterpoloDisabled() {
        return settings.getBoolean(PREF_WATERPOLO_DISABLED, false);
    }

    boolean getCityQuizDisabled() {
        return settings.getBoolean(PREF_CITY_QUIZ_DISABLED, false);
    }

    boolean getPresentQuestDisabled() {
        return settings.getBoolean(PREF_PRESENT_QUEST_DISABLED, false);
    }

    public void setVideos(String video1, String video15, String video23) {
        Editor editor = settings.edit();
        editor.putString(PREF_VIDEO1, video1);
        editor.putString(PREF_VIDEO15, video15);
        editor.putString(PREF_VIDEO23, video23);
        editor.commit();
    }

    public String[] getVideos() {
        return new String[] {
                settings.getString(PREF_VIDEO1, null),
                settings.getString(PREF_VIDEO15, null),
                settings.getString(PREF_VIDEO23, null)
        };
    }

    public void setCastDisabled(boolean disableCast) {
        Editor editor = settings.edit();
        editor.putBoolean(PREF_CASTDISABLED, disableCast);
        editor.commit();
    }

    public boolean getCastDisabled() {
        return settings.getBoolean(PREF_CASTDISABLED, false);
    }

    public void setDestinationPhotoDisabled(boolean disablePhoto) {
        Editor editor = settings.edit();
        editor.putBoolean(PREF_PHOTODISABLED, disablePhoto);
        editor.commit();
    }

    public boolean getDestinationPhotoDisabled() {
        return settings.getBoolean(PREF_PHOTODISABLED, false);
    }

    public void setDestDBVersion(int version) {
        Editor editor = settings.edit();
        editor.putInt(PREF_DBVERSION_DEST, version);
        editor.commit();
    }

    public int getDestDBVersion() {
        return settings.getInt(PREF_DBVERSION_DEST, 0);
    }

    public void setStreamDBVersion(int version) {
        Editor editor = settings.edit();
        editor.putInt(PREF_DBVERSION_STREAM, version);
        editor.commit();
    }

    public int getStreamDBVersion() {
        return settings.getInt(PREF_DBVERSION_STREAM, 0);
    }

    /**
     * Returns the current time in milliseconds with the offset applied.
     */
    public static long getCurrentTime() {
        return System.currentTimeMillis() + SantaPreferences.TIME_OFFSET;
    }

    /**
     * Returns the time adjusted for the offset
     */
    public static long getAdjustedTime(long time) {
        return time - SantaPreferences.TIME_OFFSET;
    }

    public static int getRandom(int min, int max) {
        return (int) (min + (Math.random() * ((max - min))));
    }

    public static float getRandom(float min, float max) {
        return (min + ((float) Math.random() * ((max - min))));
    }

    public static void cacheOffset(long offset) {
        SantaPreferences.TIME_OFFSET = offset;
    }
}
