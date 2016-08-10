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

package com.google.android.apps.santatracker;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

import com.google.android.apps.santatracker.cast.NotificationDataCastManager;
import com.google.android.apps.santatracker.util.AnalyticsManager;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.DataCastConsumerImpl;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * The {@link android.app.Application} for this Santa application.
 */
public class SantaApplication extends MultiDexApplication {

    private static final String TAG = "SantaApplication";

    private static NotificationDataCastManager sCastManager = null;

    /*
     * (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialise the Google Analytics Tracker
        AnalyticsManager.initializeAnalyticsTracker(this);
    }

    /**
     * Returns the CastManager and adds tracking for connection events.
     * Note that before calling this method you need to verify that Play Services is up to date
     * using <code>BaseCastManager.checkGooglePlayServices(..)</code>.
     */
    public static NotificationDataCastManager getCastManager(final Context context) {
        //TODO: Support cast config from Santa API, including kill switch!

        final FirebaseAnalytics measurement = FirebaseAnalytics.getInstance(context);

        if (sCastManager == null) {
            sCastManager = NotificationDataCastManager.initialize(context,
                    context.getString(R.string.cast_app_id),
                    context.getString(R.string.cast_namespace));
            //sCastManager.enableFeatures(DataCastManager.FEATURE_DEBUGGING);

            DataCastConsumerImpl consumer = new DataCastConsumerImpl() {
                @Override
                public void onConnected() {
                    SantaLog.d(TAG, "Cast device connected");

                    // App measurement event
                    MeasurementManager.recordCustomEvent(measurement,
                            context.getString(R.string.analytics_event_category_cast),
                            context.getString(R.string.analytics_cast_action_connection),
                            context.getString(R.string.analytics_cast_connected));

                    // [ANALYTICS EVENT]: Cast connected
                    AnalyticsManager.sendEvent(R.string.analytics_event_category_cast,
                            R.string.analytics_cast_action_connection,
                            R.string.analytics_cast_connected);
                }

                @Override
                public void onDisconnected() {
                    SantaLog.d(TAG, "Cast device disconnected");
                    // App measurement event
                    MeasurementManager.recordCustomEvent(measurement,
                            context.getString(R.string.analytics_event_category_cast),
                            context.getString(R.string.analytics_cast_action_connection),
                            context.getString(R.string.analytics_cast_disconnected));

                    // [ANALYTICS EVENT]: Cast disconnected
                    AnalyticsManager.sendEvent(R.string.analytics_event_category_cast,
                            R.string.analytics_cast_action_connection,
                            R.string.analytics_cast_disconnected);
                }
            };
            sCastManager.addDataCastConsumer(consumer);
        }
        return sCastManager;
    }

    public static void toogleCast(Context context, boolean turnOffCast) {
        NotificationDataCastManager castManager = SantaApplication.getCastManager(context);
        if (castManager == null) {
            return;
        }

        if (turnOffCast) {
            // Disable cast
            castManager.disconnect();
            castManager.stopCastDiscovery();
            SantaLog.d(TAG, "Disabled cast.");
        } else {
            // Enable cast
            castManager.startCastDiscovery();
            SantaLog.d(TAG, "Enabled cast.");
        }
    }
}
