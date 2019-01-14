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

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test

/**
 * Unit tests for [LocaleMapper]
 */
class LocaleMapperTest {

    private val localeMapper = LocaleMapper()

    @Test
    fun testToServerLanguage_fil() {
        assertThat(localeMapper.toServerLanguage("fil"), `is`("tl"))
    }

    @Test
    fun testToServerLanguage_bg() {
        assertThat(localeMapper.toServerLanguage("bg"), `is`("bg"))
    }

    @Test
    fun testToServerLanguage_es_ES() {
        assertThat(localeMapper.toServerLanguage("es_ES"), `is`("es"))
    }

    @Test
    fun testToServerLanguage_es_dash_ES() {
        assertThat(localeMapper.toServerLanguage("es-ES"), `is`("es"))
    }

    @Test
    fun testToServerLanguage_ja_dash_JP() {
        assertThat(localeMapper.toServerLanguage("ja-JP"), `is`("ja"))
    }

    @Test
    fun testToServerLanguage_zh_Hans_CN() {
        assertThat(localeMapper.toServerLanguage("zh-Hans-CN"), `is`("zh-CN"))
    }

    @Test
    fun testToServerLanguage_zh_Hant_TW() {
        assertThat(localeMapper.toServerLanguage("zh-Hant-TW"), `is`("zh-TW"))
    }

    @Test
    fun testToServerLanguage_nonExistent() {
        assertThat(localeMapper.toServerLanguage("random"), `is`("en"))
    }
}
