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

package com.google.android.apps.santatracker

import android.content.Context
import android.content.Intent
import android.net.Uri

object Intents {
    private const val PACKAGE = "com.google.android.apps.santatracker.messaging"
    // These variable names below are usually named XXX_ACTION, but naming as XXX_INTENT to tell
    // the difference from the ACTION_XXXX for FCM
    const val SYNC_CONFIG_INTENT = "$PACKAGE.SYNC_CONFIG"
    const val SYNC_ROUTE_INTENT = "$PACKAGE.SYNC_ROUTE"
    const val FINISH_TRACKER_INTENT = "$PACKAGE.FINISH_TRACKER"

    /** URL for YouTube video IDs.  */
    private const val VIDEO_URL = "https://www.youtube.com/watch?v=%s"

    /**
     * Constructs an Intent that plays back a YouTube video. If the YouTube app is installed, the
     * video will be played back directly in full screen mode. if the YouTube app is not available
     * (e.g. not installed or disabled), the video is launched in a browser instead.
     *
     * @param videoId YouTube Video id.
     */
    @JvmStatic
    fun getYoutubeIntent(context: Context, videoId: String): Intent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("vnd.youtube://$videoId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("force_fullscreen", true)
        }

        val resolvers = context.packageManager.queryIntentActivities(intent, 0) ?: emptyList()
        return if (resolvers.isNotEmpty()) {
            // Devices with YouTube installed will get the native full-screen player
            intent
        } else {
            // If YouTube is not available, load open the video in the browser
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(VIDEO_URL.format(videoId))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }
}