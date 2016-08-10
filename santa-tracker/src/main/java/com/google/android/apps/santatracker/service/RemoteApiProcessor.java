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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * ApiProcessor that loads data from the remote Santa API.
 */
public class RemoteApiProcessor extends APIProcessor {

    private static final String TAG = "RemoteApiProcessor";

    private static final String SUPPORTED_SANTA_HEADER_API = "2";
    private static final String API_VERSION_FIELD = "X-Santa-Version";
    private static final long CACHE_EXPIRY_S = 60 * 12; // This gets us 5 requests / hr

    private final FirebaseRemoteConfig mConfig = FirebaseRemoteConfig.getInstance();

    public RemoteApiProcessor(SantaPreferences mPreferences, DestinationDbHelper mDBHelper,
            StreamDbHelper streamDbHelper, APICallback callback) {
        super(mPreferences, mDBHelper, streamDbHelper, callback);
        initializeRemoteConfigApi(callback);
    }

    @Override
    public JSONObject loadApi(String url) {
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

    private void initializeRemoteConfigApi(final APICallback callback) {
        FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();

        mConfig.setConfigSettings(settings);
        mConfig.setDefaults(R.xml.remote_config_defaults);
        Log.i(TAG, "Fetching Santa config");
        mConfig.fetch(CACHE_EXPIRY_S).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.i(TAG, "Santa config result: Success");
                    mConfig.activateFetched();
                    callback.onNewApiDataAvailable();
                } else {
                    Log.w(TAG, "Santa config result: Failed", task.getException());
                }
            }
        });
    }

    @Override
    protected Switches getSwitches() {
        Switches switches = new Switches();

        switches.disableCastButton = mConfig.getBoolean(FIELD_DISABLE_CASTBUTTON);
        switches.disableDestinationPhoto = mConfig.getBoolean(FIELD_DISABLE_PHOTO);
        switches.disableGumballGame = mConfig.getBoolean(FIELD_DISABLE_GUMBALLGAME);
        switches.disableJetpackGame = mConfig.getBoolean(FIELD_DISABLE_JETPACKGAME);
        switches.disableMemoryGame = mConfig.getBoolean(FIELD_DISABLE_MEMORYGAME);
        switches.disableRocketGame = mConfig.getBoolean(FIELD_DISABLE_ROCKETGAME);
        switches.disableDancerGame = mConfig.getBoolean(FIELD_DISABLE_DANCERGAME);
        switches.disableSnowdownGame = mConfig.getBoolean(FIELD_DISABLE_SNOWDOWNGAME);

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
        return version != null && version.equals(SUPPORTED_SANTA_HEADER_API);

    }

}
