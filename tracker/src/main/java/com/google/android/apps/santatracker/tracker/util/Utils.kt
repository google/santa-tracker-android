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

package com.google.android.apps.santatracker.tracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.view.View
import android.view.ViewTreeObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.android.apps.santatracker.tracker.vo.DestinationStreetView
import java.util.Locale

object Utils {

    fun getRandom(min: Int, max: Int): Int = (min + Math.random() * (max - min)).toInt()

    fun getRandom(min: Float, max: Float): Float = min + Math.random().toFloat() * (max - min)

    /**
     * @param langTags the string representation of languages, delimited by a comma
     * @return the primary locale from the string representation of languages
     */
    fun extractLocale(langTags: String): String {
        val comma = langTags.indexOf(",")
        return if (comma != -1) {
            langTags.substring(0, comma)
        } else {
            langTags
        }
    }
}

fun <T> LiveData<T>.observeNonNull(owner: LifecycleOwner, observer: (T) -> Unit) {
    this.observe(owner, Observer {
        if (it != null) {
            observer(it)
        }
    })
}

fun View.watchLayoutOnce(body: (View) -> Unit) {
    if (this.isLaidOut) {
        body(this)
    } else {
        viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        body(this@watchLayoutOnce)
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
    }
}

object HtmlCompat {

    fun fromHtml(source: String, flags: Int): Spanned? =
            if (Build.VERSION.SDK_INT >= 24) {
                Html.fromHtml(source, flags)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(source)
            }
}

object TrackerIntents {

    private const val VIDEO_URL = "https://www.youtube.com/watch?v=%s"

    private const val STREETVIEW_URI = "google.streetview:panoid=%s&amp;cbp=0,%f"
    private const val GMM_PACKAGE = "com.google.android.apps.maps"
    private const val GMM_ACTIVITY = "com.google.android.maps.MapsActivity"

    /**
     * Constructs an Intent that plays back a YouTube video.
     * <p>
     * If the YouTube app is installed, the video will be played back directly in full screen mode.
     * If the YouTube app is not available (e.g. not installed or disabled), the video is launched
     * in a browser instead.
     *
     * @param context
     * @param videoId YouTube Video id.
     * @return
     */
    fun getYoutubeIntent(context: Context, videoId: String): Intent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://" + videoId)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("force_fullscreen", true)
        }

        val resolvers = context.packageManager.queryIntentActivities(intent, 0)
        return if (resolvers != null && resolvers.size > 0) {
            // Devices with YouTube installed will get the native full-screen player
            intent
        } else {
            // If YouTube is not available, load open the video in the browser
            Intent(Intent.ACTION_VIEW, Uri.parse(String.format(VIDEO_URL, videoId))).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun getStreetViewIntent(streetView: DestinationStreetView) =
            Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Locale.US,
            STREETVIEW_URI, streetView.id, streetView.heading))).apply {
        setClassName(GMM_PACKAGE, GMM_ACTIVITY)
    }
}
