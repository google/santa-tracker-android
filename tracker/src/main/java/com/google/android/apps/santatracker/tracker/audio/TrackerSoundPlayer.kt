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

package com.google.android.apps.santatracker.tracker.audio

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import com.google.android.apps.santatracker.util.SantaLog

class TrackerSoundPlayer(private val context: Context) {

    private var sleighBellsPlayer: MediaPlayer? = null
    private var hoHoHoPlayer: MediaPlayer? = null
    private var muted: Boolean = false

    init {
        if (context is Activity) {
            context.volumeControlStream = AudioManager.STREAM_MUSIC
        }
    }

    fun release() {
        if (context is Activity) {
            context.volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE
        }
        sleighBellsPlayer?.let {
            it.stop()
            it.reset()
            it.release()
            sleighBellsPlayer = null
        }

        hoHoHoPlayer?.let {
            it.stop()
            it.reset()
            it.release()
            hoHoHoPlayer = null
        }
    }

    fun pause() {
        hoHoHoPlayer?.pause()
        sleighBellsPlayer?.pause()
    }

    fun resume() {
        hoHoHoPlayer?.start()
        sleighBellsPlayer?.start()
    }

    fun sayHoHoHo() {
        if (hoHoHoPlayer == null) {
            val player = MediaPlayer.create(context,
                    com.google.android.apps.santatracker.common.R.raw.ho_ho_ho)
            player.isLooping = false
            val volume = if (muted) 0f else VOLUME_MULTIPLIER
            player.setVolume(volume, volume)
            player.start()
            hoHoHoPlayer = player
        } else {
            hoHoHoPlayer?.let {
                if (!it.isPlaying) {
                    SantaLog.d(TAG, "sayHoHoHo: ready")
                    it.seekTo(0)
                    it.start()
                }
            }
        }
    }

    fun startSleighBells() {
        if (sleighBellsPlayer == null) {
            val player = MediaPlayer.create(context,
                    com.google.android.apps.santatracker.tracker.R.raw.sleighbells)
            player.isLooping = true
            val volume = if (muted) 0f else VOLUME_MULTIPLIER
            player.setVolume(volume, volume)
            player.start()
            sleighBellsPlayer = player
        } else {
            sleighBellsPlayer?.let {
                if (!it.isPlaying) {
                    it.seekTo(0)
                    it.start()
                }
            }
        }
    }

    fun stopSleighBells() {
        sleighBellsPlayer?.pause()
    }

    companion object {
        private val TAG = "TrackerSoundPlayer"
        private val VOLUME_MULTIPLIER = 0.25f
    }
}
