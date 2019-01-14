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
package com.google.android.apps.santatracker.doodles.shared;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;
import com.google.android.apps.santatracker.util.SantaLog;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Utility functions to make it easier to interact with Android APIs. */
public final class AndroidUtils {
    private static final String TAG = AndroidUtils.class.getSimpleName();

    private AndroidUtils() {
        // Don't instantiate this class.
    }

    public static Activity getActivityFromContext(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public static void allowScreenToTurnOff(Context context) {
        getActivityFromContext(context)
                .getWindow()
                .clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public static void forceScreenToStayOn(Context context) {
        getActivityFromContext(context)
                .getWindow()
                .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public static Point getScreenSize() {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        return new Point(displayMetrics.widthPixels, displayMetrics.heightPixels);
    }

    public static float dipToPixels(float dipValue) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    public static float pixelsToDips(float pixelValue) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float dip = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, metrics);
        return pixelValue / dip;
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    /** Handles loading text from our resources, including interpreting <b> and <i> tags. */
    public static CharSequence getText(Resources res, int id, Object... formatArgs) {
        try {
            return Html.fromHtml(res.getString(id, formatArgs));
        } catch (java.util.MissingFormatArgumentException e) {
            SantaLog.e(TAG, "unable to format string id: " + id, e);
        }
        return "";
    }

    /**
     * Re-orients a coordinate system based on default device rotation. Implementation based on:
     * http://goo.gl/kRajPd
     *
     * @param displayRotation Display rotation, from Display.getRotation()
     * @param eventValues Event values gathered from the raw sensor event.
     * @return The adjusted event values, with display rotation taken into account.
     */
    public static float[] getAdjustedAccelerometerValues(int displayRotation, float[] eventValues) {
        float[] adjustedValues = new float[3];
        final int axisSwap[][] = {
            {1, -1, 0, 1}, // ROTATION_0
            {-1, -1, 1, 0}, // ROTATION_90
            {-1, 1, 0, 1}, // ROTATION_180
            {1, 1, 1, 0} // ROTATION_270
        };

        final int[] axisFactors = axisSwap[displayRotation];
        adjustedValues[0] = ((float) axisFactors[0]) * eventValues[axisFactors[2]];
        adjustedValues[1] = ((float) axisFactors[1]) * eventValues[axisFactors[3]];
        adjustedValues[2] = eventValues[2];

        return adjustedValues;
    }

    /**
     * Reads all bytes from an input stream into a byte array. Does not close the stream.
     *
     * @param in the input stream to read from
     * @return a byte array containing all the bytes from the stream
     * @throws IOException if an I/O error occurs
     */
    public static byte[] toByteArray(InputStream in) throws IOException {
        // Presize the ByteArrayOutputStream since we know how large it will need
        // to be, unless that value is less than the default ByteArrayOutputStream
        // size (32).
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, in.available()));
        copy(in, out);
        return out.toByteArray();
    }

    /**
     * Copies all bytes from the input stream to the output stream. Does not close or flush either
     * stream.
     *
     * @param from the input stream to read from
     * @param to the output stream to write to
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs
     */
    private static long copy(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[8192];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    public static void finishActivity(Context context) {
        getActivityFromContext(context).finish();
    }

    public static void finishActivityWithResult(Context context, int resultCode, Bundle extras) {
        Activity activity = getActivityFromContext(context);
        Intent intent = activity.getIntent();
        intent.putExtras(extras);
        activity.setResult(resultCode, intent);
        activity.finish();
    }
}
