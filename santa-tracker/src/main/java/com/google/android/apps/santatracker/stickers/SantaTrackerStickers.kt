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

import com.google.android.apps.santatracker.config.Config
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.FirebaseAppIndexingInvalidArgumentException
import com.google.firebase.appindexing.Indexable
import com.google.firebase.appindexing.builders.Indexables
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject

class SantaTrackerStickers @Inject constructor(
    private val config: Config,
    private val okHttpClient: OkHttpClient,
    private val firebaseAppIndex: FirebaseAppIndex,
    private val gson: Gson
) {
    /**
     * Updates our sticker pack(s) in Firebase App Indexing
     *
     * @return true if successful
     */
    fun updateStickers(): Boolean {
        try {
            // First make sure we have a sync'd remote config
            config.syncConfig()
            // Now get the config url
            val url = Config.STICKERS_CONFIG_URL.getValue(config)

            if (url.isNullOrEmpty()) {
                // We don't have a stickers config url for some reason, fail fast
                SantaLog.d(TAG, "Stickers config URL not set")
                return false
            } else {
                SantaLog.d(TAG, "Got stickers config URL: $url")
            }

            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                // Failed to get sticker pack, fail fast
                SantaLog.e(TAG, "Failed to fetch stickers config: $response")
                return false
            }

            val stickerConfig = gson.fromJson(
                    response.body()!!.charStream(),
                    StickerConfig::class.java
            )

            val indexables = stickerConfig.stickerPacks?.map {
                toStickerPackBuilder(it)
            } ?: emptyList()

            val task = firebaseAppIndex.removeAll()
                    .continueWithTask { firebaseAppIndex.update(*indexables.toTypedArray()) }

            // We're already in a background thread, so lets await the result
            Tasks.await(task)
            // Return whether the task was successful
            return task.isSuccessful
        } catch (e: Exception) {
            SantaLog.e(TAG, "Unable to set stickers", e)
            return false
        }
    }

    @Throws(IOException::class, FirebaseAppIndexingInvalidArgumentException::class)
    private fun toStickerPackBuilder(stickerPack: StickerPack): Indexable {
        return Indexables.stickerPackBuilder()
                .setName(stickerPack.name)
                // Firebase App Indexing unique key that must match an intent-filter.
                // (e.g. mystickers://sticker/pack/0)
                .setUrl(STICKERPACK_URL_PATTERN.format(URLEncoder.encode(stickerPack.name)))
                .setImage(stickerPack.imageUrl)
                .setHasSticker(*toStickerBuilder(stickerPack.stickers).toTypedArray())
                .setDescription(stickerPack.description)
                .build()
    }

    @Throws(IOException::class)
    private fun toStickerBuilder(stickers: List<Sticker>) = stickers.map { sticker ->
        Indexables.stickerBuilder()
                .setName(sticker.name)
                // Firebase App Indexing unique key that must match an intent-filter
                // (e.g. mystickers://sticker/0)
                .setUrl(STICKER_URL_PATTERN.format(URLEncoder.encode(sticker.name)))
                .setImage(sticker.imageUrl)
                .setDescription(sticker.description ?: sticker.name)
                .put("keywords", *(sticker.keywords?.toTypedArray() ?: emptyArray()))
    }

    companion object {
        private const val TAG = "SantaTrackerStickers"

        private const val STICKERPACK_URL_PATTERN = "santa://stickerpack/%s"
        private const val STICKER_URL_PATTERN = "santa://sticker/%s"
    }
}