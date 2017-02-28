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

package com.google.android.apps.santatracker.service;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.apps.santatracker.data.DestinationDbHelper;
import com.google.android.apps.santatracker.data.GameDisabledState;
import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.apps.santatracker.data.StreamDbHelper;
import com.google.android.apps.santatracker.data.Switches;
import com.google.android.apps.santatracker.util.SantaLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Abstracts access to the Santa API.
 * This class handles the processing and interpretation of received data from the API, including
 * parsing the result into {@link org.json.JSONObject}s and calling the
 * {@link com.google.android.apps.santatracker.service.APIProcessor.APICallback}
 * with any changed values.
 */
public abstract class APIProcessor {

    private static final String TAG = "SantaCommunicator";

    // JSON field names for parsing
    protected final static String FIELD_ROUTEOFFSET = "routeOffset";
    protected final static String FIELD_NOW = "now";
    protected final static String FIELD_TIMEOFFSET = "timeOffset";
    protected final static String FIELD_FINGERPRINT = "fingerprint";
    protected final static String FIELD_SWITCHOFF = "switchOff";
    protected final static String FIELD_REFRESH = "refresh";
    protected final static String FIELD_DISABLE_CASTBUTTON = "DisableCastButton";
    protected final static String FIELD_DISABLE_PHOTO = "DisableDestinationPhoto";
    protected final static String FIELD_DISABLE_GUMBALLGAME = "DisableGumballGame";
    protected final static String FIELD_DISABLE_JETPACKGAME = "DisableJetpackGame";
    protected final static String FIELD_DISABLE_MEMORYGAME = "DisableMemoryGame";
    protected final static String FIELD_DISABLE_ROCKETGAME = "DisableRocketGame";
    protected final static String FIELD_DISABLE_DANCERGAME = "DisableDancerGame";
    protected final static String FIELD_DISABLE_SNOWDOWNGAME = "DisableSnowdownGame";
    protected final static String FIELD_DISABLE_SWIMMINGGAME = "DisableSwimmingGame";
    protected final static String FIELD_DISABLE_BMXGAME = "DisableBmxGame";
    protected final static String FIELD_DISABLE_RUNNINGGAME = "DisableRunningGame";
    protected final static String FIELD_DISABLE_TENNISGAME = "DisableTennisGame";
    protected final static String FIELD_DISABLE_WATERPOLOGAME = "DisableWaterpoloGame";
    protected final static String FIELD_DISABLE_CITY_QUIZ = "DisableCityQuiz";
    protected final static String FIELD_DISABLE_PRESENTQUEST = "DisablePresentQuest";
    protected final static String FIELD_VIDEO_1 = "Video1";
    protected final static String FIELD_VIDEO_15 = "Video15";
    protected final static String FIELD_VIDEO_23 = "Video23";

    protected static final String FIELD_DESTINATIONS = "destinations";
    protected static final String FIELD_STATUS = "status";

    protected static final String FIELD_IDENTIFIER = "id";
    protected static final String FIELD_ARRIVAL = "arrival";
    protected static final String FIELD_DEPARTURE = "departure";

    protected static final String FIELD_DETAILS_CITY = "city";
    protected static final String FIELD_DETAILS_REGION = "region";
    protected static final String FIELD_DETAILS_COUNTRY = "country";

    protected static final String FIELD_DETAILS_LOCATION = "location";
    protected static final String FIELD_DETAILS_LOCATION_LAT = "lat";
    protected static final String FIELD_DETAILS_LOCATION_LNG = "lng";
    protected static final String FIELD_DETAILS_PRESENTSDELIVERED = "presentsDelivered";

    protected static final String FIELD_DETAILS_DETAILS = "details";
    protected static final String FIELD_DETAILS_ALTITUDE = "altitude";
    protected static final String FIELD_DETAILS_TIMEZONE = "timezone";
    protected static final String FIELD_DETAILS_PHOTOS = "photos";
    protected static final String FIELD_DETAILS_WEATHER = "weather";
    protected static final String FIELD_DETAILS_STREETVIEW = "streetView";
    protected static final String FIELD_DETAILS_GMMSTREETVIEW = "gmmStreetView";

    public static final String FIELD_PHOTO_URL = "url";
    public static final String FIELD_PHOTO_ATTRIBUTIONHTML= "attributionHtml";

    public static final String FIELD_WEATHER_URL = "url";
    public static final String FIELD_WEATHER_TEMPC = "tempC";
    public static final String FIELD_WEATHER_TEMPF = "tempF";

    public static final String FIELD_STREETVIEW_ID = "id";
    public static final String FIELD_STREETVIEW_LATITUDE = "latitude";
    public static final String FIELD_STREETVIEW_LONGITUDE = "longitude";
    public static final String FIELD_STREETVIEW_HEADING = "heading";

    public static final String FIELD_STREAM = "stream";
    public static final String FIELD_STREAMOFFSET = "streamOffset";
    public static final String FIELD_NOTIFICATIONSTREAM = "notificationStream";
    public static final String FIELD_STREAM_TIMESTAMP = "timestamp";
    public static final String FIELD_STREAM_STATUS = "status";
    public static final String FIELD_STREAM_DIDYOUKNOW = "didyouknow";
    public static final String FIELD_STREAM_IMAGEURL = "imageUrl";
    public static final String FIELD_STREAM_YOUTUBEID = "youtubeId";

    protected static final String FIELD_STATUS_OK = "OK";

    public static final long ERROR_CODE = Long.MIN_VALUE;

    private static final String EMPTY_STRING = "";

    // Preferences
    protected SantaPreferences mPreferences;
    // DB helpers
    protected DestinationDbHelper mDestinationDBHelper = null;
    protected StreamDbHelper mStreamDBHelper = null;

    // Callback
    private APICallback mCallback;

    public APIProcessor(SantaPreferences mPreferences, DestinationDbHelper mDBHelper,
            StreamDbHelper mStreamDBHelper, APICallback mCallback) {
        this.mPreferences = mPreferences;
        this.mDestinationDBHelper = mDBHelper;
        this.mStreamDBHelper = mStreamDBHelper;
        this.mCallback = mCallback;
    }

    /**
     * Load data from the API from the given URL and parse the returned data into a JSONObject.
     *
     * Implementations may ignore the URL parameter.
     */
    protected abstract JSONObject loadApi(String url);

    /** Loads remotely configurable switches and flags. */
    protected abstract Switches getSwitches();

    /**
     * Access the API from a URL and process its data.
     * If any values have changed, the appropriate callbacks in
     * {@link com.google.android.apps.santatracker.service.APIProcessor.APICallback} are called.
     * Returns {@link #ERROR_CODE} if the data could not be loaded or processed.
     * Returns the delay to the next API access if the access was successful.
     */
    public long accessAPI(String url) {
        SantaLog.d(TAG, "URL=" + url);
        // Get current values from mPreferences
        long offsetPref = mPreferences.getOffset();
        String fingerprintPref = mPreferences.getFingerprint();
        boolean switchOffPref = mPreferences.getSwitchOff();

        // load data as JSON
        JSONObject json = loadApi(url);

        if (json == null) {
            Log.d(TAG, "Santa Communication Error 3");
            return ERROR_CODE;
        }

        try {
            // Error if the status is not OK
            if (!FIELD_STATUS_OK.equals(json.getString(FIELD_STATUS))) {
                Log.d(TAG, "Santa Communication Error 4");
                return ERROR_CODE;
            }

            final int routeOffset = json.getInt(FIELD_ROUTEOFFSET);
            final long now = json.getLong(FIELD_NOW);
            final long offset = json.getLong(FIELD_TIMEOFFSET);
            final String fingerprint = json.getString(FIELD_FINGERPRINT);
            final long refresh = json.getLong(FIELD_REFRESH);
            final boolean switchOff = json.getBoolean(FIELD_SWITCHOFF);
            final JSONArray locations = json.getJSONArray(FIELD_DESTINATIONS);

            final int streamOffset = json.getInt(FIELD_STREAMOFFSET);
            final JSONArray stream = json.getJSONArray(FIELD_STREAM);

            // Notification stream parameters are optional
            final JSONArray notificationStream =
                    json.has(FIELD_NOTIFICATIONSTREAM) ?
                            json.getJSONArray(FIELD_NOTIFICATIONSTREAM) : null;

            // Fingerprint has changed, remove route and stream from db
            if (!fingerprint.equals(fingerprintPref)) {
                mCallback.notifyRouteUpdating();
                //empty the database and reset preferences
                mDestinationDBHelper.emptyDestinationTable();
                mStreamDBHelper.emptyCardTable();
                mPreferences.invalidateData();
            }

            // Destinations
            if (locations != null && locations.length() > 0) {
                int processedLocations = processRoute(locations);
                if (processedLocations > 0) {
                    final int newOffset = routeOffset + processedLocations;
                    mCallback.onNewRouteLoaded();
                    mPreferences.setFingerprint(fingerprint);
                    mPreferences.setRouteOffset(newOffset);
                    SantaLog.d(TAG,
                            "Processed route - new details: " + newOffset + ", " + fingerprint);
                }
            }

            // Stream
            if (stream != null && stream.length() > 0) {
                // process non-notification cards
                int processedCards = processStream(stream, false);
                if (processedCards > 0) {
                    final int newOffset = streamOffset + processedCards;
                    mCallback.onNewStreamLoaded();
                    mPreferences.setStreamOffset(newOffset);
                    SantaLog.d(TAG,
                            "Processed stream - new details: " + newOffset);
                }
            }

            // Notification Stream
            if (notificationStream != null && notificationStream.length() > 0) {
                // process notification cards
                int processedCards = processStream(notificationStream, true);
                if (processedCards > 0) {
                    mCallback.onNewNotificationStreamLoaded();
                    SantaLog.d(TAG,
                            "Processed notification stream - count: " + processedCards);
                }
            }

            // Offset
            final long newOffset = now - System.currentTimeMillis() + offset;
            if (offsetPref != newOffset) {
                mPreferences.setOffset(newOffset);

                SantaLog.d(TAG,
                        "New offset: " + newOffset + ", current=" + System.currentTimeMillis()
                                + ", new Santa=" + SantaPreferences.getCurrentTime());

                // Log.d(TAG, "new offset: new="+newOffset+", now="+now+", offset="+offset+",
                // prefOffset="+offsetPref+", time="+System.currentTimeMillis());
                // Notify only if offset varies significantly
                if ((newOffset > offsetPref + SantaPreferences.OFFSET_ACCEPTABLE_RANGE_DIFFERENCE
                        || newOffset
                        < offsetPref - SantaPreferences.OFFSET_ACCEPTABLE_RANGE_DIFFERENCE)) {
                    mCallback.onNewOffset();
                }

            }

            if (switchOffPref != switchOff) {
                mPreferences.setSwitchOff(switchOff);
                mCallback.onNewSwitchOffState(switchOff);

            }

            // Check Switches for Changes
            checkSwitchesDiff(getSwitches());

            if (!fingerprint.equals(fingerprintPref)) {
                // new data has been processed and locations have been stored
                mCallback.onNewFingerprint();
            }

            return refresh;
        } catch (JSONException e) {
            Log.d(TAG, "Santa Communication Error 5");
            SantaLog.d(TAG, "JSON Exception", e);
            return ERROR_CODE;
        }

    }

    /**
     * Compare Switches to SharedPreferences and notify clients of any changes.
     */
    protected void checkSwitchesDiff(Switches s) {
        if (mPreferences.getCastDisabled() != s.disableCastButton) {
            // set cast preference
            mPreferences.setCastDisabled(s.disableCastButton);
            mCallback.onNewCastState(s.disableCastButton);
        }

        if (mPreferences.getDestinationPhotoDisabled() != s.disableDestinationPhoto) {
            // set destination photo preference
            mPreferences.setDestinationPhotoDisabled(s.disableDestinationPhoto);
            mCallback.onNewDestinationPhotoState(s.disableDestinationPhoto);
        }

        // Games
        if (!mPreferences.gameDisabledStateConsistent(s.gameState)) {
            // Overwrite game disabled state from Switches
            mPreferences.setGamesDisabled(s.gameState);

            // Notify of new game state
            mCallback.onNewGameState(s.gameState);
        }

        // Videos
        boolean videosConsistent = Arrays.equals(
                new String[]{s.video1, s.video15, s.video23},
                mPreferences.getVideos());
        if (!videosConsistent) {
            mPreferences.setVideos(s.video1, s.video15, s.video23);
            mCallback.onNewVideos(s.video1, s.video15, s.video23);
        }
    }

    private int processRoute(JSONArray json) {
        SQLiteDatabase db = mDestinationDBHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            // loop over each destination
            long previousPresents = mPreferences.getTotalPresents();

            int i;
            for (i = 0; i < json.length(); i++) {
                JSONObject dest = json.getJSONObject(i);

                JSONObject location = dest
                        .getJSONObject(FIELD_DETAILS_LOCATION);

                long presentsTotal = dest
                        .getLong(FIELD_DETAILS_PRESENTSDELIVERED);
                long presents = presentsTotal - previousPresents;
                previousPresents = presentsTotal;

                // Name
                String city = dest.getString(FIELD_DETAILS_CITY);
                String region = null;
                String country = null;

                if (dest.has(FIELD_DETAILS_REGION)) {
                    region = dest.getString(FIELD_DETAILS_REGION);
                    if (region.length() < 1) {
                        region = null;
                    }
                }
                if (dest.has(FIELD_DETAILS_COUNTRY)) {
                    country = dest.getString(FIELD_DETAILS_COUNTRY);
                    if (country.length() < 1) {
                        country = null;
                    }
                }

//                if (mDebugLog) {
//                    Log.d(TAG, "Location: " + city);
//                }

                // Detail fields
                JSONObject details = dest.getJSONObject(FIELD_DETAILS_DETAILS);
                long timezone = details.isNull(FIELD_DETAILS_TIMEZONE) ? 0L
                        : details.getLong(FIELD_DETAILS_TIMEZONE);
                long altitude = details.getLong(FIELD_DETAILS_ALTITUDE);
                String photos = details.has(FIELD_DETAILS_PHOTOS) ? details
                        .getString(FIELD_DETAILS_PHOTOS) : EMPTY_STRING;
                String weather = details.has(FIELD_DETAILS_WEATHER) ? details
                        .getString(FIELD_DETAILS_WEATHER) : EMPTY_STRING;
                String streetview = details.has(FIELD_DETAILS_STREETVIEW) ? details
                        .getString(FIELD_DETAILS_STREETVIEW) : EMPTY_STRING;
                String gmmStreetview = details.has(FIELD_DETAILS_GMMSTREETVIEW) ? details
                        .getString(FIELD_DETAILS_GMMSTREETVIEW) : EMPTY_STRING;

                try {
                    // All parsed, insert into DB
                    mDestinationDBHelper.insertDestination(db,
                            dest.getString(FIELD_IDENTIFIER),
                            dest.getLong(FIELD_ARRIVAL),
                            dest.getLong(FIELD_DEPARTURE),

                            city, region, country,

                            location.getDouble(FIELD_DETAILS_LOCATION_LAT),
                            location.getDouble(FIELD_DETAILS_LOCATION_LNG),
                            presentsTotal, presents, timezone, altitude, photos, weather,
                            streetview, gmmStreetview);
                } catch (android.database.sqlite.SQLiteConstraintException e) {
                    // ignore duplicate locations
                }
            }

            db.setTransactionSuccessful();
            // Update mPreferences
            mPreferences.setDBTimestamp(System.currentTimeMillis());
            mPreferences.setTotalPresents(previousPresents);
            return i;
        } catch (JSONException e) {
            Log.d(TAG, "Santa location tracking error 30");
            SantaLog.d(TAG, "JSON Exception", e);
        } finally {
            db.endTransaction();
        }

        return 0;
    }

    private int processStream(JSONArray json, boolean isWear) {
        SQLiteDatabase db = mStreamDBHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            // loop over each card

            int i;
            for (i = 0; i < json.length(); i++) {
                JSONObject card = json.getJSONObject(i);

                final long timestamp = card
                        .getLong(FIELD_STREAM_TIMESTAMP);
                final String status = getExistingJSONString(card, FIELD_STREAM_STATUS);
                final String didYouKnow = getExistingJSONString(card, FIELD_STREAM_DIDYOUKNOW);
                final String imageUrl = getExistingJSONString(card, FIELD_STREAM_IMAGEURL);
                final String youtubeId = getExistingJSONString(card, FIELD_STREAM_YOUTUBEID);

//                if (mDebugLog) {
//                    Log.d(TAG, "Notification: " + timestamp);
//                }

                try {
                    // All parsed, insert into DB
                    mStreamDBHelper.insert(db, timestamp, status, didYouKnow, imageUrl, youtubeId,
                            isWear);
                } catch (android.database.sqlite.SQLiteConstraintException e) {
                    // ignore duplicate cards
                }
            }

            db.setTransactionSuccessful();
            return i;
        } catch (JSONException e) {
            Log.d(TAG, "Santa location tracking error 31");
            SantaLog.d(TAG, "JSON Exception", e);
        } finally {
            db.endTransaction();
        }

        return 0;
    }

    // Reads an InputStream and converts it to a String.
    protected static StringBuilder read(InputStream stream) throws IOException {
        BufferedReader reader;
        StringBuilder builder = new StringBuilder();
        reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

        for (String line; (line = reader.readLine()) != null; ) {
            builder.append(line);
        }

        return builder;
    }

    /**
     * Returns the value of the JSON field identified by the name. Returns null if the field does
     * not exist.
     */
    public static String getExistingJSONString(JSONObject json, String name) throws JSONException {
        if (json.has(name)) {
            return json.getString(name);
        } else {
            return null;
        }
    }

    /**
     * Returns the double of the JSON object identified by the name. Returns {@link
     * java.lang.Double#MAX_VALUE} if the field does not exist.
     */
    public static double getExistingJSONDouble(JSONObject json, String name) throws JSONException {
        if (json.has(name)) {
            return json.getDouble(name);
        } else {
            return Double.MAX_VALUE;
        }
    }

    interface APICallback {

        void onNewSwitchOffState(boolean isOn);

        /**
         * Called when a new fingerprint has been detected and stored data
         * will be cleared to process the new route.
         *
         * @see #onNewRouteLoaded()
         */
        void onNewFingerprint();

        void onNewOffset();

        /**
         * Called when new data has been processed.
         */
        void onNewRouteLoaded();

        void onNewStreamLoaded();

        void onNewNotificationStreamLoaded();

        void notifyRouteUpdating();

        void onNewCastState(boolean disableCast);

        void onNewGameState(GameDisabledState state);

        void onNewVideos(String video1, String video15, String video23);

        void onNewDestinationPhotoState(boolean disableDestinationPhoto);

        void onNewApiDataAvailable();
    }

}
