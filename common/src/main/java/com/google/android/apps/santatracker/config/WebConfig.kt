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
package com.google.android.apps.santatracker.config

class WebConfig {
    companion object {
        const val WEBCONFIG = "WEBCONFIG"
        const val AIRPORT = "AIRPORT"
        const val BOATLOAD = "BOATLOAD"
        const val CLAUSDRAWS = "CLAUSDRAWS"
        const val CODEBOOGIE = "CODEBOOGIE"
        const val CODELAB = "CODELAB"
        const val ELFSKI = "ELFSKI"
        const val GUMBALL = "GUMBALL"
        const val JAMBAND = "JAMBAND"
        const val PENGUINDASH = "PENGUINDASH"
        const val PRESENTBOUNCE = "PRESENTBOUNCE"
        const val PRESENTDROP = "PRESENTDROP"
        const val RACER = "RACER"
        const val RUNNER = "RUNNER"
        const val SANTASEARCH = "SANTASEARCH"
        const val SANTASELFIE = "SANTASELFIE"
        const val SEASONOFGIVING = "SEASONOFGIVING"
        const val SNOWBALL = "SNOWBALL"
        const val SNOWFLAKE = "SNOWFLAKE"
        const val SPEEDSKETCH = "SPEEDSKETCH"
        const val WRAPBATTLE = "WRAPBATTLE"
        const val ELFMAKER = "ELFMAKER"
    }

    val SCENE_CONFIG = hashMapOf(
            AIRPORT to WebConfigScene("WebAirport"),
            BOATLOAD to WebConfigScene("WebBoatload"),
            CLAUSDRAWS to WebConfigScene("WebClausdraws"),
            CODEBOOGIE to WebConfigScene("WebCodeboogie"),
            CODELAB to WebConfigScene("WebCodelab"),
            ELFSKI to WebConfigScene("WebElfski"),
            GUMBALL to WebConfigScene("WebGumball"),
            JAMBAND to WebConfigScene("WebJamband"),
            PENGUINDASH to WebConfigScene("WebPenguindash"),
            PRESENTBOUNCE to WebConfigScene("WebPresentbounce"),
            PRESENTDROP to WebConfigScene("WebPresentdrop"),
            RACER to WebConfigScene("WebRacer"),
            RUNNER to WebConfigScene("WebRunner"),
            SANTASEARCH to WebConfigScene("WebSantasearch"),
            SANTASELFIE to WebConfigScene("WebSantaselfie"),
            SEASONOFGIVING to WebConfigScene("WebSeasonofgiving"),
            SNOWBALL to WebConfigScene("WebSnowball"),
            SNOWFLAKE to WebConfigScene("WebSnowflake"),
            SPEEDSKETCH to WebConfigScene("WebSpeedsketch"),
            WRAPBATTLE to WebConfigScene("WebWrapbattle"),
            ELFMAKER to WebConfigScene("WebElfMaker")
    )
}

class WebConfigScene(
    featuredFlagName: String,
    disabledFlagName: String,
    landscapeFlagName: String,
    urlFlagName: String,
    cardImageUrlFlagName: String? = null
) {
    constructor(prefix: String) : this(
            featuredFlagName = "${prefix}Featured",
            disabledFlagName = "${prefix}Disabled",
            landscapeFlagName = "${prefix}Landscape",
            urlFlagName = "${prefix}Url",
            cardImageUrlFlagName = "${prefix}CardImageUrl"
    )

    val configFeatured = BooleanConfigParam(featuredFlagName)
    val configDisabled = BooleanConfigParam(disabledFlagName)
    val configLandscape = BooleanConfigParam(landscapeFlagName)
    val configUrl = StringConfigParam(urlFlagName)
    val configCardImageUrl = cardImageUrlFlagName?.let { StringConfigParam(it) }
}
