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

package com.google.android.apps.santatracker.tracker.api

class LocaleMapper {

    /**
     * @return the matched language available in the server from the device's locale.
     */
    fun toServerLanguage(locale: String): String {
        if (locale == "fil") {
            return "tl"
        } else if (locale in SERVER_LANGUAGES) {
            return locale
        }
        if (locale == "zh-Hant-TW") {
            return "zh-TW"
        }
        if (locale == "zh-Hans-CN") {
            return "zh-CN"
        }
        val lowerCase = locale.toLowerCase()
        if (lowerCase in SERVER_LANGUAGES) {
            return lowerCase
        }
        val sep = lowerCase.indexOfAny(charArrayOf('-', '_', ';'))
        if (sep != -1 && lowerCase.substring(0, sep) in SERVER_LANGUAGES) {
            return lowerCase.substring(0, sep)
        }
        return "en" // Fall back to "en" if the match isn't found
    }

    companion object {
        val SERVER_LANGUAGES = listOf("af", "bg", "ca", "da", "de", "en", "en-GB", "es",
                "es-419", "et", "fi", "fr", "fr-CA", "hr", "id", "it", "ja", "lt", "lv", "ml", "no",
                "pl", "pt-BR", "pt-PT", "ro", "sl", "sv", "ta", "th", "tl", "uk", "vi", "zh-CN",
                "zh-TW")
    }
}
