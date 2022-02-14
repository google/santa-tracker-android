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

package com.google.android.apps.santatracker.dasherdancer

import androidx.annotation.RawRes

/**
 * Interface for characters. To create a character, implement this interface. The animationKey
 * passed to this interface's methods is called with one of the ANIM_* static values defined in this
 * interface. Generally implementing classes should use an array to store their durations, frame
 * indices arrays, and frame arrays.
 */
interface Character {

    // The initial release used getClass().getSimpleName(), which was ProGuarded out.
    // These strings are the pro-guarded names from the released version. In future releases
    // these should be changed to the names of the characters.
    val characterName: String

    fun getDuration(animationKey: Int): Long

    fun getFrameIndices(animationKey: Int): IntArray

    fun getFrames(animationKey: Int): IntArray

    @RawRes
    fun getSoundResource(animationid: Int): Int

    companion object {

        const val ANIM_IDLE = 0
        const val ANIM_TAP = 1
        const val ANIM_SHAKE = 2
        const val ANIM_SWIPE_DOWN = 3
        const val ANIM_SWIPE_UP = 4
        const val ANIM_SWIPE_LEFT = 5
        const val ANIM_SWIPE_RIGHT = 6
        const val ANIM_PINCH_IN = 7
        const val ANIM_PINCH_OUT = 8

        val ALL_ANIMS = intArrayOf(ANIM_IDLE, ANIM_TAP, ANIM_SHAKE, ANIM_SWIPE_DOWN, ANIM_SWIPE_UP, ANIM_SWIPE_LEFT, ANIM_SWIPE_RIGHT, ANIM_PINCH_IN, ANIM_PINCH_OUT)
    }
}
