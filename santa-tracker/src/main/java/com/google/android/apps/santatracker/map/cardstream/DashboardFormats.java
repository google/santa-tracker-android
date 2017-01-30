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

package com.google.android.apps.santatracker.map.cardstream;

import android.content.Context;

import com.google.android.apps.santatracker.data.Destination;
import com.google.android.apps.santatracker.data.SantaPreferences;

import java.text.NumberFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

public class DashboardFormats {

    private static final NumberFormat PRESENTS_COUNT_FORMAT = NumberFormat.getIntegerInstance();
    private static final String TIME_FORMAT = "%02d:%02d";
    private static final String COUNTDOWN_HMS = "%d:%02d:%02d";
    private static final String COUNTDOWN_MS = "%02d:%02d";
    private static GregorianCalendar sCalendar;
    private static Long sOffset;

    public static String formatDestination(Destination d) {
        if (d != null) {
            return d.getPrintName();
        }
        return null;
    }

    public static String formatPresents(long presents) {
        return PRESENTS_COUNT_FORMAT.format(presents);
    }

    /**
     * Format the given unix time. This is not thread-safe.
     *
     * @param context   The context.
     * @param timestamp The unix time.
     * @return A formatted string.
     */
    static String formatTime(Context context, long timestamp) {
        if (sCalendar == null) {
            sCalendar = (GregorianCalendar) GregorianCalendar.getInstance();
        }
        if (sOffset == null) {
            sOffset = new SantaPreferences(context).getOffset();
        }
        sCalendar.setTimeInMillis(timestamp - sOffset);
        return String.format(Locale.US, TIME_FORMAT, sCalendar.get(GregorianCalendar.HOUR),
                sCalendar.get(GregorianCalendar.MINUTE));
    }

    /**
     * @param time The time in milliseconds
     * @return A string representation of the countdown time.
     */
    public static String formatCountdown(long time) {
        final int iHours = (int) Math.floor(time / (60 * 60 * 1000) % 24);
        final int iMinutes = (int) Math.floor(time / (60 * 1000) % 60);
        final int iSeconds = (int) Math.floor(time / (1000) % 60);
        if (iHours > 0) {
            return String.format(Locale.US, COUNTDOWN_HMS, iHours, iMinutes, iSeconds);
        } else {
            return String.format(Locale.US, COUNTDOWN_MS, iMinutes, iSeconds);
        }
    }

}
