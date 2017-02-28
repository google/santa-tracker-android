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

package com.google.android.apps.santatracker.util;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.data.Destination;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.List;
import java.util.Locale;

/**
 * Utility methods for Intents.
 */
public class Intents {

    private static final String GMM_PACKAGE = "com.google.android.apps.maps";
    private static final String GMM_ACTIVITY = "com.google.android.maps.MapsActivity";

    /**
     * URL for YouTube video IDs.
     */
    private static final String VIDEO_URL = "https://www.youtube.com/watch?v=%s";

    /**
     * Constructs an Intent that plays back a YouTube video.
     * If the YouTube app is installed, the video will be played back directly in full screen
     * mode.
     * if the YouTube app is not available (e.g. not installed or disabled), the video is launched
     * in a browser instead.
     *
     * @param context
     * @param videoId YouTube Video id.
     * @return
     */
    public static Intent getYoutubeIntent(Context context, String videoId) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("vnd.youtube://" + videoId));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("force_fullscreen", true);

        List<ResolveInfo> resolvers = context.getPackageManager().queryIntentActivities(intent, 0);
        if (resolvers != null && resolvers.size() > 0) {
            // Devices with YouTube installed will get the native full-screen player
            return intent;
        } else {
            // If YouTube is not available, load open the video in the browser
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(VIDEO_URL, videoId)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }
    }

    /**
     * Checks if the device can handle a GMM StreetView intent.
     */
    public static boolean canHandleStreetView(Context context) {
        // Construct a fake streetView intent
        Destination.StreetView sv = new Destination.StreetView();
        Intent intent = getStreetViewIntent(context.getString(R.string.streetview_uri), sv);

        List<ResolveInfo> resolvers = context.getPackageManager().queryIntentActivities(intent, 0);
        return resolvers != null && resolvers.size() > 0;
    }

    public static Intent getStreetViewIntent(String rawUri, Destination.StreetView streetView) {
        Uri gmmIntentUri = Uri
                .parse(String.format(Locale.US, rawUri, streetView.id, streetView.heading));
        Intent intent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        intent.setClassName(GMM_PACKAGE, GMM_ACTIVITY);

        return intent;

    }
}
