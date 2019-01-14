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
import androidx.annotation.Keep
import com.google.android.apps.santatracker.tracker.R
import com.google.android.apps.santatracker.tracker.ui.TrackerActivity
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions

/**
 * Sets up the Cast configuration. Casting the Santa Tracker app only opens a website, there are no
 * controls or other options.
 */
@Keep
class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {

        // Create a notification with the default options and no actions.
        val notificationOptions = NotificationOptions.Builder()
                .setTargetActivityClassName(TrackerActivity::class.java.name)
                .build()

        val mediaOptions = CastMediaOptions.Builder()
                .setNotificationOptions(notificationOptions)
                .build()

        return CastOptions.Builder()
                .setReceiverApplicationId(context.getString(R.string.cast_app_id))
                .setCastMediaOptions(mediaOptions)
                .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}
