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

data class WebSceneState(val scenes: Map<String, WebScene> = emptyMap()) {
    data class WebScene(
        val featured: Boolean,
        val disabled: Boolean,
        val landscape: Boolean,
        val url: String,
        val cardImageUrl: String?
    )
}

fun webSceneStateFromConfig(config: Config) = WebSceneState(
        Config.WEB_SCENES.SCENE_CONFIG.mapValues { (_, value) ->
            WebSceneState.WebScene(
                    value.configFeatured.getValue(config),
                    value.configDisabled.getValue(config),
                    value.configLandscape.getValue(config),
                    value.configUrl.getValue(config),
                    value.configCardImageUrl?.getValue(config)
            )
        }
)

fun Config.webSceneState() = webSceneStateFromConfig(this)