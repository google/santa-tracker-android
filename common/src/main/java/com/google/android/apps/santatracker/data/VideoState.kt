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

/** Configuration for video URLs.  */
data class VideoState(
    val video1: String? = null,
    val video15: String? = null,
    val video23: String? = null
)

fun videoStateFromConfig(config: Config) = VideoState(
        Config.VIDEO_1.getValue(config),
        Config.VIDEO_15.getValue(config),
        Config.VIDEO_23.getValue(config)
)

fun Config.videoState() = videoStateFromConfig(this)
