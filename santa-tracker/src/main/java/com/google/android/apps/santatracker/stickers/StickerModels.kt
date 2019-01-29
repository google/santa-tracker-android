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

package com.google.android.apps.santatracker.stickers

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class StickerConfig(
    @SerializedName("stickerpacks") val stickerPacks: List<StickerPack>?
)

@Keep
data class StickerPack(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("image") val imageUrl: String,
    @SerializedName("stickers") val stickers: List<Sticker>
)

@Keep
data class Sticker(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("image") val imageUrl: String,
    @SerializedName("keywords") val keywords: List<String>?
)