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

package com.google.android.apps.santatracker.util

import android.util.Log
import com.google.android.apps.santatracker.common.BuildConfig

object SantaLog {
    private val LOG_ENABLED = BuildConfig.BUILD_TYPE != "release"

    @JvmStatic
    fun v(tag: String, msg: String) {
        if (LOG_ENABLED) {
            Log.v(tag, msg)
        }
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        if (LOG_ENABLED) {
            Log.w(tag, msg)
        }
    }

    @JvmStatic
    fun w(tag: String, msg: String, t: Throwable?) {
        if (LOG_ENABLED) {
            Log.w(tag, msg, t)
        }
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        Log.e(tag, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String, t: Throwable?) {
        Log.e(tag, msg, t)
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (LOG_ENABLED) {
            Log.d(tag, msg)
        }
    }

    @JvmStatic
    fun i(tag: String, msg: String) {
        if (LOG_ENABLED) {
            Log.i(tag, msg)
        }
    }

    @JvmStatic
    fun d(tag: String, s: String, t: Throwable?) {
        if (LOG_ENABLED) {
            Log.d(tag, s, t)
        }
    }
}
