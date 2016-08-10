/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.libraries.cast.companionlibrary.utils;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * An AsyncTask to fetch an image over HTTP and scale it to the desired size. Clients need to extend
 * this and implement their own {@code onPostExecute(Bitmap bitmap)} method. It provides a uniform
 * treatment of ThreadPool across various versions of Android.
 */
public abstract class FetchBitmapTask extends AsyncTask<Uri, Void, Bitmap> {
    private final int mPreferredWidth;
    private final int mPreferredHeight;

    /**
     * Constructs a new FetchBitmapTask that will do scaling.
     *
     * @param preferredWidth The preferred image width.
     * @param preferredHeight The preferred image height.
     */
    public FetchBitmapTask(int preferredWidth, int preferredHeight) {
        mPreferredWidth = preferredWidth;
        mPreferredHeight = preferredHeight;
    }

    /**
     * Constructs a new FetchBitmapTask. No scaling will be performed if you use this constructor.
     */
    public FetchBitmapTask() {
        this(0, 0);
    }

    @Override
    protected Bitmap doInBackground(Uri... uris) {
        if (uris.length != 1 || uris[0] == null) {
            return null;
        }

        Bitmap bitmap = null;
        URL url;
        try {
            url = new URL(uris[0].toString());
        } catch (MalformedURLException e) {
            return null;
        }
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);

            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream stream = new BufferedInputStream(urlConnection.getInputStream());
                bitmap = BitmapFactory.decodeStream(stream);
                if ((mPreferredWidth > 0) && (mPreferredHeight > 0)) {
                    bitmap = scaleBitmap(bitmap);
                }
            }
        } catch (IOException e) { /* ignore */
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return bitmap;
    }

    /**
     * Executes the task.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void execute(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uri);
        } else {
            execute(new Uri[] {uri});
        }
    }

    /*
     * Scales the bitmap to the preferred width and height.
     *
     * @param bitmap The bitmap to scale.
     * @return The scaled bitmap.
     */
    private Bitmap scaleBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Calculate deltas.
        int dw = width - mPreferredWidth;
        int dh = height - mPreferredHeight;

        if ((dw == 0) && (dh == 0)) {
            return bitmap;
        }

        float scaleFactor;
        if ((dw > 0) || (dh > 0)) {
            // Icon is too big; scale down.
            float scaleWidth = (float) mPreferredWidth / width;
            float scaleHeight = (float) mPreferredHeight / height;
            scaleFactor = Math.min(scaleHeight, scaleWidth);
        } else {
            // Icon is too small; scale up.
            float scaleWidth = width / (float) mPreferredWidth;
            float scaleHeight = height / (float) mPreferredHeight;
            scaleFactor = Math.min(scaleHeight, scaleWidth);
        }

        int finalWidth = (int) ((width * scaleFactor) + 0.5f);
        int finalHeight = (int) ((height * scaleFactor) + 0.5f);

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, false);
    }

}
