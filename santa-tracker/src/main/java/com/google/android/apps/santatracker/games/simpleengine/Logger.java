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

package com.google.android.apps.santatracker.games.simpleengine;

import android.util.Log;

public class Logger {

    private static final String TAG = "seng";
    private static boolean debugEnabled = false;

    public static void enableDebugLog(boolean enable) {
        debugEnabled = enable;
        if (debugEnabled) {
            d("Debug logs enabled.");
        }
    }

    public static void d(String msg) {
        if (debugEnabled) {
            Log.d(TAG, msg);
        }
    }

    public static void w(String msg) {
        Log.w(TAG, "!!! WARNING: " + msg);
    }

    public static void e(String msg) {
        Log.e(TAG, "*** ERROR: " + msg);
    }
}
