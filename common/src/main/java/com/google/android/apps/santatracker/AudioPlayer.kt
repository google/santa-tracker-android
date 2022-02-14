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

package com.google.android.apps.santatracker

import android.content.Context
import android.media.MediaPlayer
import android.util.SparseArray

class AudioPlayer(private val context: Context) {
    private val streams: SparseArray<MediaPlayer> = SparseArray()
    private var muted = false

    fun playTrack(resId: Int, loop: Boolean) {
        val mediaPlayer = MediaPlayer.create(context, resId)
        // Not all devices support audio (i.e. watches)
        if (mediaPlayer != null) {
            streams.put(resId, mediaPlayer)
            mediaPlayer.isLooping = loop
            mediaPlayer.setOnPreparedListener { mp -> startMedia(mp) }
            mediaPlayer.setOnCompletionListener { mp ->
                if (!mp.isLooping) {
                    mp.release()
                    streams.remove(resId)
                }
            }
        }
    }

    fun playTrackExclusive(resId: Int, loop: Boolean) {
        var restart = false
        val mp = streams.get(resId)
        try {
            if (mp == null || !mp.isPlaying) {
                restart = true
            }
        } catch (e: IllegalStateException) {
            // Media player was not initialised or was released
            restart = true
        }

        if (restart) {
            stopAll()
            playTrack(resId, loop)
        }
    }

    private fun startMedia(mp: MediaPlayer) {
        if (muted) {
            mp.setVolume(0f, 0f)
        } else {
            mp.setVolume(AudioConstants.DEFAULT_BACKGROUND_VOLUME,
                    AudioConstants.DEFAULT_BACKGROUND_VOLUME)
        }
        mp.start()
    }

    fun stop(resId: Int) {
        val mp = streams.get(resId)
        if (mp != null) {
            mp.stop()
            mp.release()
            streams.remove(resId)
        }
    }

    fun stopAll() {
        // Stop all audio
        for (i in 0 until streams.size()) {
            val mp = streams.valueAt(i)
            if (mp != null) {
                mp.stop()
                mp.release()
                streams.removeAt(i)
            }
        }
    }
}
