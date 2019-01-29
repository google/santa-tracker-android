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

package com.google.android.apps.santatracker.tracker.time

import com.google.android.apps.santatracker.config.Config
import com.google.android.apps.santatracker.util.SantaLog
import com.instacart.library.truetime.TrueTime
import java.io.IOException
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.concurrent.Executor

/**
 * Implementation of [Clock] whose time can be adjusted by Firebase Remote Config
 * (through [Config] class).
 */
open class OffsettableClock(private val config: Config, executor: Executor) : Clock {

    companion object {
        private const val TAG = "OffsettableClock"
        private var cachedCalendar: Calendar? = null // Optimization
        private val TIME_FORMAT = "%02d:%02d"
    }

    init {
        // The app has only 1 instance of this class (because this is provided as @Singleton in
        // AppModule). So init is going to be executed once.
        executor.execute {
            try {
                TrueTime.build().initialize()
                SantaLog.d(TAG, "TrueTime: initialized")
            } catch (e: IOException) {
                SantaLog.w(TAG, "TrueTime: initialization failed", e)
            }
        }
    }

    open val timeOffset: Long
        get() = config.get(Config.TIME_OFFSET)

    override fun nowMillis(): Long {
        val nowMillis = if (TrueTime.isInitialized())
            TrueTime.now().time
        else
            System.currentTimeMillis()
        return nowMillis + timeOffset
    }

    override fun formatTime(timestamp: Long): String? {
        if (cachedCalendar == null) {
            cachedCalendar = GregorianCalendar.getInstance() as GregorianCalendar
        }
        return cachedCalendar?.let { calendar ->
            calendar.timeInMillis = adjustedTime(timestamp)
            String.format(Locale.US, TIME_FORMAT,
                    calendar.get(GregorianCalendar.HOUR),
                    calendar.get(GregorianCalendar.MINUTE))
        }
    }

    override fun adjustedTime(time: Long): Long {
        return time - timeOffset
    }
}
