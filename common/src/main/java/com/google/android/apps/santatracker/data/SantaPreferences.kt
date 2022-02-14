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

package com.google.android.apps.santatracker.data

import android.annotation.SuppressLint
import android.content.Context

/** Singleton that manages access to internal data stored as preferences.  */
@SuppressLint("CommitPrefEdits")
class SantaPreferences(context: Context) {
    private val settings = context.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE)

    var offset: Long
        get() = dateOffset
        set(value) {
            // We have to use commit() here so that the value is updated immediately
            settings.edit().putLong(PREF_OFFSET, value).apply()
            dateOffset = value
        }

    var isMuted: Boolean = settings.getBoolean(PREF_MUTED, false)
        set(value) {
            settings.edit().putBoolean(PREF_MUTED, value).apply()
            field = value
        }

    fun toggleMuted() {
        isMuted = !isMuted
    }

    init {
        dateOffset = settings.getLong(PREF_OFFSET, 0)
    }

    companion object {
        // Shared time offset this is used only for the debug build
        private var dateOffset = 0L

        private const val PREFERENCES_FILENAME = "SantaTracker"
        private const val PREF_OFFSET = "PREF_OFFSET"
        private const val PREF_MUTED = "PREF_MUTED"
    }
}
