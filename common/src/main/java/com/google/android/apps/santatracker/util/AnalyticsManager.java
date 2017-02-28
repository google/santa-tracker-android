/*
 * Copyright 2016 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.util;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.apps.santatracker.common.R;

/**
 * Handles communication with Google Analytics.
 * Based on implementation in iosched: com.google.samples.apps.iosched.util.AnalyticsManager
 */
public class AnalyticsManager {

    private static Context sAppContext = null;
    private static Tracker mTracker;

    private final static String TAG = "AnalyticsManager";

    public static synchronized void setTracker(Tracker tracker) {
        mTracker = tracker;
    }

    private static boolean canSend() {
        return mTracker != null;
    }

    /**
     * Sends a screen view with the string resource loaded as its label.
     */
    public static void sendScreenView(int resourceId) {
        sendScreenView(getString(resourceId));
    }

    private static String getString(int id){
        if(sAppContext != null) {
            return sAppContext.getString(id);
        }
        return null;
    }
    /**
     * Sends a screen vie for a screen label.
     */
    public static void sendScreenView(String screenName) {
        if (canSend()) {
            mTracker.setScreenName(screenName);
            mTracker.send(new HitBuilders.AppViewBuilder().build());
            Log.d(TAG, "Screen View recorded: " + screenName);
        } else {
            Log.d(TAG, "Screen View NOT recorded (analytics disabled or not ready).");
        }
    }

    /**
     * Sends an event to the tracker with string resources loaded as parameters.
     */
    public static void sendEvent(int category, int action, int label, long value) {
        sendEvent(getString(category), getString(action),
                getString(label), value);
    }

    /**
     * Sends an event to the tracker with string resources loaded as parameters.
     */
    public static void sendEvent(int category, int action) {
        sendEvent(getString(category), getString(action));
    }

    /**
     * Sends an event to the tracker with string resources loaded as parameters.
     */
    public static void sendEvent(int category, int action, String label) {
        sendEvent(getString(category), getString(action), label);
    }

    public static void sendEvent(String category, String action) {
        if (canSend()) {
            mTracker.send(new HitBuilders.EventBuilder(category,action).build());

            Log.d(TAG, "Event recorded:");
            Log.d(TAG, "\tCategory: " + category);
            Log.d(TAG, "\tAction: " + action);
        } else {
            Log.d(TAG, "Analytics event ignored (analytics disabled or not ready).");
        }
    }

    public static void sendEvent(String category, String action, String label, long value) {
        if (canSend()) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(category)
                    .setAction(action)
                    .setLabel(label)
                    .setValue(value)
                    .build());

            Log.d(TAG, "Event recorded:");
            Log.d(TAG, "\tCategory: " + category);
            Log.d(TAG, "\tAction: " + action);
            Log.d(TAG, "\tLabel: " + label);
            Log.d(TAG, "\tValue: " + value);
        } else {
            Log.d(TAG, "Analytics event ignored (analytics disabled or not ready).");
        }
    }

    /**
     * Sends an event to the tracker with string resources loaded as parameters.
     */
    public static void sendEvent(int category, int action, int label) {
        sendEvent(getString(category), getString(action),
                getString(label));
    }

    public static void sendEvent(String category, String action, String label) {
        sendEvent(category, action, label, 0);
    }

    public Tracker getTracker() {
        return mTracker;
    }

    public static synchronized void initializeAnalyticsTracker(Context context) {
        // To avoid Activity life cycle related memory leaks, assigning the application context
        sAppContext = context.getApplicationContext();
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
            mTracker = analytics.newTracker(R.xml.config_analytics_tracker);
            Log.d(TAG, "Analytics tracker initialised.");
        }
    }
}