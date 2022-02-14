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

class Elf : Character {

    override val characterName: String
        get() = "h"

    override fun getDuration(animationKey: Int): Long {
        return durations[animationKey]
    }

    override fun getFrameIndices(animationKey: Int): IntArray {
        return frameIndices[animationKey]
    }

    override fun getFrames(animationKey: Int): IntArray {
        return frames[animationKey]
    }

    override fun getSoundResource(animationid: Int): Int {
        when (animationid) {
            Character.ANIM_PINCH_IN -> return R.raw.elf_pinchin_ball
            Character.ANIM_PINCH_OUT -> return R.raw.elf_pinchout
            Character.ANIM_SHAKE -> return R.raw.elf_shake2
            Character.ANIM_SWIPE_DOWN -> return R.raw.elf_swipedown2
            Character.ANIM_SWIPE_UP -> return R.raw.elf_swipeup2
            Character.ANIM_SWIPE_LEFT -> return R.raw.elf_swipeleft
            Character.ANIM_SWIPE_RIGHT -> return R.raw.elf_swiperight
            Character.ANIM_TAP -> return R.raw.elf_tap3
        }

        return -1
    }

    companion object {

        private val durations = longArrayOf(1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1333)

        private val frames = arrayOf(
                intArrayOf(R.drawable.elf_idle_0001, R.drawable.elf_idle_0002,
                        R.drawable.elf_idle_0003, R.drawable.elf_idle_0004, R.drawable.elf_idle_0005,
                        R.drawable.elf_idle_0006, R.drawable.elf_idle_0007, R.drawable.elf_idle_0008,
                        R.drawable.elf_idle_0009, R.drawable.elf_idle_0010, R.drawable.elf_idle_0011,
                        R.drawable.elf_idle_0012, R.drawable.elf_idle_0011, R.drawable.elf_idle_0010,
                        R.drawable.elf_idle_0015, R.drawable.elf_idle_0009, R.drawable.elf_idle_0017,
                        R.drawable.elf_idle_0007, R.drawable.elf_idle_0019, R.drawable.elf_idle_0020,
                        R.drawable.elf_idle_0021, R.drawable.elf_idle_0022, R.drawable.elf_idle_0002,
                        R.drawable.elf_idle_0024), // idle
                intArrayOf(R.drawable.elf_idle_0024, R.drawable.elf_tap_0002, R.drawable.elf_tap_0003,
                        R.drawable.elf_tap_0004, R.drawable.elf_tap_0005, R.drawable.elf_tap_0006,
                        R.drawable.elf_tap_0007, R.drawable.elf_tap_0008, R.drawable.elf_tap_0009,
                        R.drawable.elf_tap_0010, R.drawable.elf_tap_0011, R.drawable.elf_tap_0012,
                        R.drawable.elf_tap_0013, R.drawable.elf_tap_0014, R.drawable.elf_tap_0015,
                        R.drawable.elf_tap_0016, R.drawable.elf_tap_0017, R.drawable.elf_tap_0018,
                        R.drawable.elf_idle_0024, R.drawable.elf_idle_0024, R.drawable.elf_idle_0024,
                        R.drawable.elf_idle_0024, R.drawable.elf_idle_0024,
                        R.drawable.elf_idle_0024), // tap
                intArrayOf(R.drawable.elf_shake_0001, R.drawable.elf_shake_0002,
                        R.drawable.elf_shake_0003, R.drawable.elf_shake_0004,
                        R.drawable.elf_shake_0005, R.drawable.elf_shake_0006,
                        R.drawable.elf_shake_0007, R.drawable.elf_shake_0008,
                        R.drawable.elf_shake_0009, R.drawable.elf_shake_0010,
                        R.drawable.elf_shake_0011, R.drawable.elf_shake_0012,
                        R.drawable.elf_shake_0013, R.drawable.elf_shake_0014,
                        R.drawable.elf_shake_0015, R.drawable.elf_shake_0016,
                        R.drawable.elf_shake_0017, R.drawable.elf_shake_0018,
                        R.drawable.elf_shake_0019, R.drawable.elf_shake_0020,
                        R.drawable.elf_shake_0021, R.drawable.elf_shake_0022,
                        R.drawable.elf_shake_0023, R.drawable.elf_shake_0001), // shake
                intArrayOf(R.drawable.elf_swipedown_0001, R.drawable.elf_swipedown_0001,
                        R.drawable.elf_swipedown_0003, R.drawable.elf_swipedown_0004,
                        R.drawable.elf_swipedown_0005, R.drawable.elf_swipedown_0006,
                        R.drawable.elf_swipedown_0007, R.drawable.elf_swipedown_0008,
                        R.drawable.elf_swipedown_0009, R.drawable.elf_swipedown_0010,
                        R.drawable.elf_swipedown_0011, R.drawable.elf_swipedown_0012,
                        R.drawable.elf_swipedown_0013, R.drawable.elf_swipedown_0012,
                        R.drawable.elf_swipedown_0015, R.drawable.elf_swipedown_0016,
                        R.drawable.elf_swipedown_0017, R.drawable.elf_swipedown_0018,
                        R.drawable.elf_swipedown_0019, R.drawable.elf_swipedown_0020,
                        R.drawable.elf_swipedown_0021, R.drawable.elf_swipedown_0022,
                        R.drawable.elf_swipedown_0023, R.drawable.elf_swipedown_0001), // swipe down
                intArrayOf(R.drawable.elf_swipeup_0002, R.drawable.elf_swipeup_0003,
                        R.drawable.elf_swipeup_0004, R.drawable.elf_swipeup_0005,
                        R.drawable.elf_swipeup_0006, R.drawable.elf_swipeup_0007,
                        R.drawable.elf_swipeup_0008, R.drawable.elf_swipeup_0009,
                        R.drawable.elf_swipeup_0010, R.drawable.elf_swipeup_0011,
                        R.drawable.elf_swipeup_0012, R.drawable.elf_swipeup_0013,
                        R.drawable.elf_swipeup_0014, R.drawable.elf_swipeup_0015,
                        R.drawable.elf_swipeup_0016, R.drawable.elf_swipeup_0017,
                        R.drawable.elf_swipeup_0018, R.drawable.elf_swipeup_0019,
                        R.drawable.elf_swipeup_0020, R.drawable.elf_swipeup_0021,
                        R.drawable.elf_swipeup_0022, R.drawable.elf_swipeup_0023,
                        R.drawable.elf_swipedown_0001), // swipe up
                intArrayOf(R.drawable.elf_swipeleft_0001, R.drawable.elf_swipeleft_0002,
                        R.drawable.elf_swipeleft_0003, R.drawable.elf_swipeleft_0004,
                        R.drawable.elf_swipeleft_0005, R.drawable.elf_swipeleft_0006,
                        R.drawable.elf_swipeleft_0007, R.drawable.elf_swipeleft_0008,
                        R.drawable.elf_swipeleft_0009, R.drawable.elf_swipeleft_0010,
                        R.drawable.elf_swipeleft_0011, R.drawable.elf_swipeleft_0012,
                        R.drawable.elf_swipeleft_0013, R.drawable.elf_swipeleft_0014,
                        R.drawable.elf_swipeleft_0015, R.drawable.elf_swipeleft_0016,
                        R.drawable.elf_swipeleft_0017, R.drawable.elf_swipeleft_0018,
                        R.drawable.elf_swipeleft_0019, R.drawable.elf_swipeleft_0020,
                        R.drawable.elf_swipeleft_0021, R.drawable.elf_swipeleft_0022,
                        R.drawable.elf_swipeleft_0023, R.drawable.elf_swipeleft_0024), // swipe left
                intArrayOf(R.drawable.elf_swiperight_0002, R.drawable.elf_swiperight_0003,
                        R.drawable.elf_swiperight_0004, R.drawable.elf_swiperight_0005,
                        R.drawable.elf_swiperight_0006, R.drawable.elf_swiperight_0007,
                        R.drawable.elf_swiperight_0008, R.drawable.elf_swiperight_0009,
                        R.drawable.elf_swiperight_0010, R.drawable.elf_swiperight_0011,
                        R.drawable.elf_swiperight_0012, R.drawable.elf_swiperight_0013,
                        R.drawable.elf_swiperight_0014, R.drawable.elf_swiperight_0015,
                        R.drawable.elf_swiperight_0016, R.drawable.elf_swiperight_0017,
                        R.drawable.elf_swiperight_0018, R.drawable.elf_swiperight_0019,
                        R.drawable.elf_swiperight_0020, R.drawable.elf_swiperight_0021,
                        R.drawable.elf_swiperight_0022, R.drawable.elf_swiperight_0023,
                        R.drawable.elf_swipedown_0001), // swipe right
                intArrayOf(R.drawable.elf_pinchout_0001, R.drawable.elf_pinchout_0002,
                        R.drawable.elf_pinchout_0003, R.drawable.elf_pinchout_0004,
                        R.drawable.elf_pinchout_0005, R.drawable.elf_pinchout_0006,
                        R.drawable.elf_pinchout_0007, R.drawable.elf_pinchout_0007,
                        R.drawable.elf_pinchout_0007, R.drawable.elf_pinchout_0007,
                        R.drawable.elf_pinchout_0007, R.drawable.elf_pinchout_0007,
                        R.drawable.elf_pinchout_0007, R.drawable.elf_pinchout_0007,
                        R.drawable.elf_pinchout_0007, R.drawable.elf_pinchout_0007,
                        R.drawable.elf_pinchout_0007, R.drawable.elf_pinchout_0007,
                        R.drawable.elf_pinchout_0019, R.drawable.elf_pinchout_0020,
                        R.drawable.elf_pinchout_0021, R.drawable.elf_pinchout_0022,
                        R.drawable.elf_pinchout_0023, R.drawable.elf_pinchout_0024), // pinch in
                intArrayOf(R.drawable.elf_pinchin_0001, R.drawable.elf_pinchin_0002,
                        R.drawable.elf_pinchin_0003, R.drawable.elf_pinchin_0004,
                        R.drawable.elf_pinchin_0005, R.drawable.elf_pinchin_0006,
                        R.drawable.elf_pinchin_0007, R.drawable.elf_pinchin_0008,
                        R.drawable.elf_pinchin_0009, R.drawable.elf_pinchin_0010,
                        R.drawable.elf_pinchin_0011, R.drawable.elf_pinchin_0012,
                        R.drawable.elf_pinchin_0013, R.drawable.elf_pinchin_0014,
                        R.drawable.elf_pinchin_0015, R.drawable.elf_pinchin_0016,
                        R.drawable.elf_pinchin_0017, R.drawable.elf_pinchin_0018,
                        R.drawable.elf_pinchin_0019, R.drawable.elf_pinchin_0020,
                        R.drawable.elf_pinchin_0021, R.drawable.elf_pinchin_0022,
                        R.drawable.elf_pinchin_0023, R.drawable.elf_pinchin_0024,
                        R.drawable.elf_pinchin_0025, R.drawable.elf_pinchin_0026,
                        R.drawable.elf_pinchin_0027, R.drawable.elf_pinchin_0028,
                        R.drawable.elf_pinchin_0029, R.drawable.elf_pinchin_0030,
                        R.drawable.elf_pinchin_0031,
                        R.drawable.elf_pinchin_0032) // pinch out
        )

        private val frameIndices =
                arrayOf(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                        19, 20, 21, 22, 23), // idle
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23), // tap
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23), // shake
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23), // swipe down
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22), // swipe up
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23), // swipe left
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22), // swipe right
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23), // pinch in
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31) // pinch out
                )
    }
}
