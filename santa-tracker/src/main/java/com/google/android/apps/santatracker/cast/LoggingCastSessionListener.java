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
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.firebase.analytics.FirebaseAnalytics;

import android.content.Context;
import android.support.annotation.StringRes;

public class LoggingCastSessionListener implements SessionManagerListener<CastSession> {

    private static final String TAG = "CastSessionManagerListener";
    final private Context mContext;
    final private FirebaseAnalytics mMeasurement;
    private int mCategory;

    public LoggingCastSessionListener(Context context, @StringRes int category) {
        mContext = context;
        mMeasurement = FirebaseAnalytics.getInstance(context);
        mCategory = category;
    }

    private void logEvent(String messageText) {
        SantaLog.d(TAG, String.format("Cast: %s", messageText));

        // App measurement event
        MeasurementManager.recordCustomEvent(mMeasurement,
                mContext.getString(R.string.analytics_event_category_cast),
                mContext.getString(mCategory),
                messageText);
    }

    @Override
    public void onSessionStarting(CastSession session) {
        logEvent("onSessionStarting");
    }

    @Override
    public void onSessionStarted(CastSession session, String s) {
        logEvent("onSessionStarted");
    }

    @Override
    public void onSessionStartFailed(CastSession session, int i) {
        logEvent("onSessionStartFailed");
    }

    @Override
    public void onSessionEnding(CastSession session) {
        logEvent("onSessionEnding");

    }

    @Override
    public void onSessionEnded(CastSession session, int i) {
        logEvent("onSessionEnded");

    }

    @Override
    public void onSessionResuming(CastSession session, String s) {
        logEvent("onSessionResuming");

    }

    @Override
    public void onSessionResumed(CastSession session, boolean b) {
        logEvent("onSessionResumed");

    }

    @Override
    public void onSessionResumeFailed(CastSession session, int i) {
        logEvent("onSessionResumeFailed");

    }

    @Override
    public void onSessionSuspended(CastSession session, int i) {
        logEvent("onSessionSuspended");

    }
}
