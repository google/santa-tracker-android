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

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.apps.santatracker.BuildConfig;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.data.DestinationDbHelper;
import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.apps.santatracker.data.StreamDbHelper;
import com.google.android.apps.santatracker.data.Switches;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigFetchThrottledException;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * ApiProcessor that loads data from the remote Santa API.
 */
public class RemoteApiProcessor extends APIProcessor {

    private static final String TAG = "RemoteApiProcessor";

    private static final Set<String> SUPPORTED_SANTA_HEADER_API = new HashSet<>(Arrays.asList("2", "2016"));
    private static final String API_VERSION_FIELD = "X-Santa-Version";
    private static final long DEFAULT_CACHE_EXPIRY_S = 60 * 12; // 5 requests / hr

    private final FirebaseRemoteConfig mConfig;
    private final FirebaseRemoteConfigSettings mConfigSettings;
    private long mConfigCacheExpiry;
    private long mThrottleEndTimeMillis = 0;

    public RemoteApiProcessor(SantaPreferences mPreferences, DestinationDbHelper mDBHelper,
            StreamDbHelper streamDbHelper, APICallback callback) {
        super(mPreferences, mDBHelper, streamDbHelper, callback);

        // Set Firebase remote config settings once
        mConfig = FirebaseRemoteConfig.getInstance();
        mConfigSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mConfig.setConfigSettings(mConfigSettings);
        mConfig.setDefaults(R.xml.remote_config_defaults);

        // Set cache expiration to 0s when debugging to allow easy testing, otherwise
        // use the default value
        mConfigCacheExpiry = mConfigSettings.isDeveloperModeEnabled() ? 0 : DEFAULT_CACHE_EXPIRY_S;
        SantaLog.d(TAG, "Config Cache Expiry: " + mConfigCacheExpiry);
    }

    @Override
    public JSONObject loadApi(String url) {
        // Check Firebase Remote Config
        long currentTime = System.currentTimeMillis();
        if (currentTime > mThrottleEndTimeMillis) {
            mConfig.fetch(mConfigCacheExpiry)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            SantaLog.d(TAG, "fetchConfig:SUCCESS");

                            // Activate config and notify clients of any changes
                            mConfig.activateFetched();
                            checkSwitchesDiff(getSwitches());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (e instanceof FirebaseRemoteConfigFetchThrottledException) {
                                // Store throttle end time
                                FirebaseRemoteConfigFetchThrottledException ex =
                                        (FirebaseRemoteConfigFetchThrottledException) e;
                                mThrottleEndTimeMillis = ex.getThrottleEndTimeMillis();
                                SantaLog.w(TAG, "fetchConfig:THROTTLED until " + mThrottleEndTimeMillis);
                            } else {
                                SantaLog.w(TAG, "fetchConfig:UNEXPECTED_ERROR", e);
                            }
                        }
                    });
        } else {
            long msRemaining = mThrottleEndTimeMillis - currentTime;
            Log.d(TAG, "Not trying config, throttled for " + msRemaining + "ms");
        }

        // Retrieve and parse the json data.
        String data;

        SantaLog.d(TAG, "Accessing API: "+url);

        try {
            data = downloadUrl(url);

        } catch (IOException e1) {
            Log.d(TAG, "Santa Communication Error 0");
            return null;
        }

        // Check that data was retrieved
        if (data == null) {
            Log.d(TAG, "Santa Communication Error 1");
            return null;
        }

        // parse data as json
        try {
            return new JSONObject(data);
        } catch (JSONException e) {
            Log.d(TAG, "Santa Communication Error 2");
        }
        return null;
    }

    @Override
    protected Switches getSwitches() {
        Switches switches = new Switches();

        switches.disableCastButton = mConfig.getBoolean(FIELD_DISABLE_CASTBUTTON);
        switches.disableDestinationPhoto = mConfig.getBoolean(FIELD_DISABLE_PHOTO);

        // Old games
        switches.gameState.disableGumballGame = mConfig.getBoolean(FIELD_DISABLE_GUMBALLGAME);
        switches.gameState.disableJetpackGame = mConfig.getBoolean(FIELD_DISABLE_JETPACKGAME);
        switches.gameState.disableMemoryGame = mConfig.getBoolean(FIELD_DISABLE_MEMORYGAME);
        switches.gameState.disableRocketGame = mConfig.getBoolean(FIELD_DISABLE_ROCKETGAME);
        switches.gameState.disableDancerGame = mConfig.getBoolean(FIELD_DISABLE_DANCERGAME);

        // Snowdown
        switches.gameState.disableSnowdownGame = mConfig.getBoolean(FIELD_DISABLE_SNOWDOWNGAME);

        // Doodles
        switches.gameState.disableSwimmingGame = mConfig.getBoolean(FIELD_DISABLE_SWIMMINGGAME);
        switches.gameState.disableBmxGame = mConfig.getBoolean(FIELD_DISABLE_BMXGAME);
        switches.gameState.disableRunningGame = mConfig.getBoolean(FIELD_DISABLE_RUNNINGGAME);
        switches.gameState.disableTennisGame = mConfig.getBoolean(FIELD_DISABLE_TENNISGAME);
        switches.gameState.disableWaterpoloGame = mConfig.getBoolean(FIELD_DISABLE_WATERPOLOGAME);

        // City Quiz
        switches.gameState.disableCityQuizGame = mConfig.getBoolean(FIELD_DISABLE_CITY_QUIZ);

        // Present Quest
        switches.gameState.disablePresentQuest = mConfig.getBoolean(FIELD_DISABLE_PRESENTQUEST);

        // Videos
        switches.video1 = mConfig.getString(FIELD_VIDEO_1);
        switches.video15 = mConfig.getString(FIELD_VIDEO_15);
        switches.video23 = mConfig.getString(FIELD_VIDEO_23);

        return switches;
    }

    /**
     * Downloads the given URL and return
     */
    protected String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first 500 characters of the retrieved
        // web page content.
        // int len = 500;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            if (!isValidHeader(conn)) {
                // not a valid header
                Log.d(TAG, "Santa communication failure.");
                return null;

            } else if (response != 200) {
                Log.d(TAG, "Santa communication failure " + response);
                return null;

            } else {
                is = conn.getInputStream();

                // Convert the InputStream into a string
                return read(is).toString();
            }

            // Makes sure that the InputStream is closed
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }


    /**
     * Returns true if this application can handle requests of this version,
     * false otherwise. The current API version can be retrieved through:
     * <code>curl -sI 'http://santa-api.appspot.com/info' | grep X-Santa</code>
     */
    protected boolean isValidHeader(HttpURLConnection connection) {

        String version = connection.getHeaderField(API_VERSION_FIELD);
        // if the version matches supported version, returns true, false if no
        // header is set or it is not recognised.
        return version != null && SUPPORTED_SANTA_HEADER_API.contains(version);

    }

}
