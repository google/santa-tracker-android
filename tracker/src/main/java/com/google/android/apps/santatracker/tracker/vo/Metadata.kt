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

package com.google.android.apps.santatracker.tracker.vo

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = Metadata.TABLE_NAME)
data class Metadata(
    @PrimaryKey
    val key: String,
    val content: String
) {

    companion object {
        const val TABLE_NAME = "metadata"

        const val KEY_STATUS = "status"
        const val KEY_LANGUAGE = "language"
        const val KEY_FINGERPRINT = "fingerprint"
    }
}
