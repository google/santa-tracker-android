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

import android.content.Context
import com.google.android.apps.santatracker.config.Config
import com.google.android.apps.santatracker.data.SantaPreferences
import java.util.concurrent.Executor

/**
 * Implementation of [Clock] whose time can be adjusted locally (using SharedPreferences)
 */
class LocalOffsettableClock(context: Context, config: Config, executor: Executor)
    : OffsettableClock(config, executor) {

    private val santaPreferences = SantaPreferences(context)

    override val timeOffset: Long
        get() = santaPreferences.offset
}
