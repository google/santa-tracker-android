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

package com.google.android.apps.santatracker.cast;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.util.AnalyticsManager;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.firebase.analytics.FirebaseAnalytics;

import android.content.Context;
import android.support.annotation.StringRes;

public class LoggingCastStateListener implements CastStateListener {

    final private Context mContext;

    final private FirebaseAnalytics mMeasurement;

    private int mCategory;

    public LoggingCastStateListener(Context context, @StringRes int category) {
        mContext = context;
        mMeasurement = FirebaseAnalytics.getInstance(context);
        mCategory = category;
    }

    @Override
    public void onCastStateChanged(int newState) {
        // App measurement event
        MeasurementManager.recordCustomEvent(mMeasurement,
                mContext.getString(R.string.analytics_event_category_cast),
                mContext.getString(mCategory),
                Integer.toString(newState));
    }
}
