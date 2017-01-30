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

package com.google.android.apps.santatracker;

import com.google.android.apps.santatracker.presentquest.model.Place;
import com.google.android.apps.santatracker.util.AnalyticsManager;

import com.orm.SugarContext;
import com.squareup.leakcanary.LeakCanary;

import android.support.multidex.MultiDexApplication;
import android.util.Log;

/**
 * The {@link android.app.Application} for this Santa application.
 */
public class SantaApplication extends MultiDexApplication {

    /*
     * (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        // Initialise the Google Analytics Tracker
        AnalyticsManager.initializeAnalyticsTracker(this);
        SugarContext.init(this);
        try {
            Place.executeQuery("CREATE INDEX IF NOT EXISTS IDX_PLACE ON PLACE (LAT,LNG)");
        } catch (Exception e) {
            Log.e("SantaApplication", "Error creating places index: " + e);
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Application#onTerminate()
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        SugarContext.terminate();
    }

}
