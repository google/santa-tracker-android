/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

import org.json.JSONException;

import android.util.Log;

public abstract class SantaLog {

    private static final boolean LOG_ENABLED = false;

    public static void v(String tag, String msg) {
        if (LOG_ENABLED) {
            Log.v(tag, msg);
        }
    }
    public static void e(String tag, String msg) {
        if (LOG_ENABLED) {
            Log.e(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (LOG_ENABLED) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (LOG_ENABLED) {
            Log.i(tag, msg);
        }
    }

    public static void d(String tag, String s, JSONException e) {
        if (LOG_ENABLED) {
            Log.d(tag, s, e);
        }
    }
}
