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
import android.preference.PreferenceManager
import androidx.annotation.WorkerThread

object UpgradeDetector {
    private const val PREF_KEY_VERSION_CODE = "last_installed_version"

    @JvmStatic
    @WorkerThread
    fun hasVersionCodeChanged(context: Context): Boolean {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val version = pInfo.versionCode

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        if (prefs.getInt(PREF_KEY_VERSION_CODE, 0) != version) {
            prefs.edit().putInt(PREF_KEY_VERSION_CODE, version).apply()
            return true
        }
        return false
    }
}