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

package com.google.android.apps.santatracker.util

import android.media.SoundPool
import com.google.android.apps.santatracker.AudioConstants

/**
 * Extension function to play a sound using some sane defaults
 */
fun SoundPool.play(
    soundId: Int,
    volume: Float = AudioConstants.DEFAULT_SOUND_EFFECT_VOLUME,
    priority: Int = 0,
    loop: Int = 0,
    rate: Float = 1f
) = play(soundId, volume, volume, priority, loop, rate)

object SoundPoolUtils {
    /**
     * Util function to play a sound using some sane defaults. This is just for use from Java
     */
    @JvmOverloads
    @JvmStatic
    fun playSoundEffect(
        soundPool: SoundPool,
        soundId: Int,
        volume: Float = AudioConstants.DEFAULT_SOUND_EFFECT_VOLUME,
        priority: Int = 0,
        loop: Int = 0,
        rate: Float = 1f
    ) = soundPool.play(soundId, volume, volume, priority, loop, rate)
}