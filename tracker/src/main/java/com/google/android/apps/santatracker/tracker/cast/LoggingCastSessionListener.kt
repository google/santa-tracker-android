/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.tracker.cast

import android.content.Context
import androidx.annotation.StringRes

import com.google.android.apps.santatracker.util.MeasurementManager
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.firebase.analytics.FirebaseAnalytics

class LoggingCastSessionListener(
    private val context: Context,
    @param:StringRes private val category: Int,
    private val measurement: FirebaseAnalytics
)
    : SessionManagerListener<CastSession> {

    private fun logEvent(messageText: String) {
        SantaLog.d(TAG, String.format("Cast: %s", messageText))

        // App measurement event
        MeasurementManager.recordCustomEvent(measurement,
                context.getString(
                        com.google.android.apps.santatracker.common.R.string
                                .analytics_event_category_cast),
                context.getString(category),
                messageText)
    }

    override fun onSessionStarting(session: CastSession) {
        logEvent("onSessionStarting")
    }

    override fun onSessionStarted(session: CastSession, s: String) {
        logEvent("onSessionStarted")
    }

    override fun onSessionStartFailed(session: CastSession, i: Int) {
        logEvent("onSessionStartFailed")
    }

    override fun onSessionEnding(session: CastSession) {
        logEvent("onSessionEnding")
    }

    override fun onSessionEnded(session: CastSession, i: Int) {
        logEvent("onSessionEnded")
    }

    override fun onSessionResuming(session: CastSession, s: String) {
        logEvent("onSessionResuming")
    }

    override fun onSessionResumed(session: CastSession, b: Boolean) {
        logEvent("onSessionResumed")
    }

    override fun onSessionResumeFailed(session: CastSession, i: Int) {
        logEvent("onSessionResumeFailed")
    }

    override fun onSessionSuspended(session: CastSession, i: Int) {
        logEvent("onSessionSuspended")
    }

    companion object {

        private val TAG = "CastSessionManagerListener"
    }
}
