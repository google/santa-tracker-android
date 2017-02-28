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

package com.google.android.apps.santatracker.map;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.util.AnalyticsManager;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Automatically re-enable SantaCam after a timeout has expired.
 */
class SantaCamTimeout {

    private SantaMapFragment mMap;

    // timestamp at which SC is to be enabled without user interaction
    private long mCamReEnableTime;
    private int mCamTimeout = -1;

    // ms to wait without activity before enabling SC again
    private static final int SANTACAM_AUTO_ENABLE_TIMEOUT = 30000; // 30s

    // s timeout countdown
    private static final int SANTACAM_AUTO_ENABLE_COUNTDOWN = 5;

    private SantaCamButton mSantaCamButton;

    SantaCamTimeout(SantaMapFragment map, SantaCamButton santaCamButton) {
        mMap = map;
        mSantaCamButton = santaCamButton;
    }

    public void check() {
        if (mCamTimeout > 0) {
            mCamTimeout--;
            if (mSantaCamButton != null) {
                mSantaCamButton.showMessage(String.valueOf(mCamTimeout), 500);
            }
        } else if (mCamTimeout == 0) {
            mMap.enableSantaCam(true);
            mCamTimeout--;

            // App Measurement
            FirebaseAnalytics measurement = FirebaseAnalytics.getInstance(mMap.getContext());
            MeasurementManager.recordCustomEvent(measurement,
                    mMap.getString(R.string.analytics_event_category_tracker),
                    mMap.getString(R.string.analytics_tracker_action_cam),
                    mMap.getString(R.string.analytics_tracker_cam_timeout));

            // [ANALYTICS EVENT]: SantaCamEnabled Timeout
            AnalyticsManager.sendEvent(R.string.analytics_event_category_tracker,
                    R.string.analytics_tracker_action_cam,
                    R.string.analytics_tracker_cam_timeout);
        } else if (mCamReEnableTime > 0
                && System.currentTimeMillis() >= mCamReEnableTime) {
            mCamTimeout = SANTACAM_AUTO_ENABLE_COUNTDOWN;
        }

    }

    public void cancel() {
        mCamReEnableTime = -1;
    }

    public void reset() {
        mCamReEnableTime = System.currentTimeMillis() + SANTACAM_AUTO_ENABLE_TIMEOUT;
        mCamTimeout = -1;
    }

}
