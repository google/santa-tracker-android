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

import android.os.Environment;
import android.util.Log;

import com.google.android.apps.santatracker.data.DestinationDbHelper;
import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.apps.santatracker.data.StreamDbHelper;
import com.google.android.apps.santatracker.data.Switches;
import com.google.android.apps.santatracker.util.SantaLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * ApiProcessor that loads data from a local file (/sdcard/santa.json).
 *
 * If the <code>now</code> parameter in the file is negative, the current system time is used
 * instead.
 *
 * <b><NOTE: It is best to leave the "refresh" value high (days).
 * If the fingerprint is unchanged and the file contains the same locations the next time it is
 * read in (and data has not been cleared), the application will fail.
 * </b> (The application expects correct behavior, ie. the API will return following locations if
 * the fingerprint is unchanged.
 */
public class LocalApiProcessor extends APIProcessor {

    private static final String FILENAME = "santa.json";
    private static final String TAG = "SantaLocalApiProcessor";
    protected final static String FIELD_CLIENT_SPECIFIC = "clientSpecific";

    private File mFile;
    private JSONObject mClientConfig;

    public LocalApiProcessor(SantaPreferences mPreferences, DestinationDbHelper mDBHelper,
            StreamDbHelper streamDBHelper, APICallback mCallback) {
        super(mPreferences, mDBHelper, streamDBHelper, mCallback);
        mFile = new File(Environment.getExternalStorageDirectory(), FILENAME);
    }

    @Override
    public JSONObject loadApi(String url) {

        SantaLog.d(TAG, "Loading local data.");
        // read the santa json file from the SD card
        String data = null;
        try {
            data = loadLocalFile();
        } catch (IOException e) {
            Log.d(TAG, "Communication Error 101");
            return null;
        }

        if (data == null) {
            Log.d(TAG, "Communication Error 102");
            return null;
        }

        SantaLog.d(TAG, "Local File accessed, old data removed.");

        // Parse data as JSON
        try {
            JSONObject result = parseData(data);
            mClientConfig = result.getJSONObject(FIELD_CLIENT_SPECIFIC);
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "Communication Error 103");
        }

        return null;
    }

    @Override
    protected Switches getSwitches() {
        if (mClientConfig == null) {
            throw new IllegalStateException("Can't call getSwitches() before successful loadApi()");
        }

        try {
            Switches config = new Switches();

            config.disableCastButton = mClientConfig.getBoolean(FIELD_DISABLE_CASTBUTTON);
            config.disableDestinationPhoto = mClientConfig.getBoolean(FIELD_DISABLE_PHOTO);
            config.disableGumballGame = mClientConfig.getBoolean(FIELD_DISABLE_GUMBALLGAME);
            config.disableJetpackGame = mClientConfig.getBoolean(FIELD_DISABLE_JETPACKGAME);
            config.disableMemoryGame = mClientConfig.getBoolean(FIELD_DISABLE_MEMORYGAME);
            config.disableRocketGame = mClientConfig.getBoolean(FIELD_DISABLE_ROCKETGAME);
            config.disableDancerGame = mClientConfig.getBoolean(FIELD_DISABLE_DANCERGAME);
            config.disableSnowdownGame = mClientConfig.getBoolean(FIELD_DISABLE_SNOWDOWNGAME);

            config.video1 = mClientConfig.getString(FIELD_VIDEO_1);
            config.video15 = mClientConfig.getString(FIELD_VIDEO_15);
            config.video23 = mClientConfig.getString(FIELD_VIDEO_23);

            return config;
        } catch (JSONException e) {
            Log.d(TAG, "Communication Error 104");
            return null;
        }
    }

    private JSONObject parseData(String data) throws JSONException {
        JSONObject json = new JSONObject(data);

        // Replace the offset with a hardcoded offset

        // Set the current time to now if the now timestamp is negative
        if (json.getLong(FIELD_NOW) < 0L) {
            json.put(FIELD_NOW, System.currentTimeMillis());
        }

        // Reset the fingerprint with each request for testing
        // json.put(FIELD_FINGERPRINT, Long.toString(System.currentTimeMillis()));

        return json;
    }

    private String loadLocalFile() throws IOException {
        if (!mFile.isFile() || !mFile.canRead()) {
            // Could not open file for reading
            return null;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(mFile);
            return read(fis).toString();
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
}
