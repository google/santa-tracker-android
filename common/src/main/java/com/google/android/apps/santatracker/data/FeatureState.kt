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

import com.google.android.apps.santatracker.config.Config

/** State for non-video non-game feature flags.  */
data class FeatureState(
    val santaDisabled: Boolean,
    val castDisabled: Boolean,
    val photoDisabled: Boolean
)

fun featureStateFromConfig(config: Config) = FeatureState(
        Config.DISABLE_SANTA.getValue(config),
        Config.DISABLE_CASTBUTTON.getValue(config),
        Config.DISABLE_PHOTO.getValue(config)
)

fun Config.featureState() = featureStateFromConfig(this)