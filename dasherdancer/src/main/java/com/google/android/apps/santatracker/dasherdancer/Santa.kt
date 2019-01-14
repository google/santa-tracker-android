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

class Santa : Character {

    override val characterName: String
        get() = "s"

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
            Character.ANIM_PINCH_IN -> return R.raw.santa_pinchin
            Character.ANIM_PINCH_OUT -> return R.raw.santa_pinchout
            Character.ANIM_SHAKE -> return R.raw.santa_shake
            Character.ANIM_SWIPE_UP -> return R.raw.santa_swipeup
            Character.ANIM_SWIPE_LEFT -> return R.raw.santa_swipeleft
            Character.ANIM_SWIPE_RIGHT -> return R.raw.santa_swiperight
            Character.ANIM_SWIPE_DOWN -> return R.raw.santa_swipedown
            Character.ANIM_TAP -> return R.raw.santa_tap
        }

        return -1
    }

    companion object {

        private val durations = longArrayOf(2400, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000)

        private val frames = arrayOf(
                intArrayOf(
                        R.drawable.santa_idle0001, R.drawable.santa_idle0002,
                        R.drawable.santa_idle0003, R.drawable.santa_idle0004,
                        R.drawable.santa_idle0005, R.drawable.santa_idle0006,
                        R.drawable.santa_idle0007, R.drawable.santa_idle0008,
                        R.drawable.santa_idle0009, R.drawable.santa_idle0010, // index is 9
                        R.drawable.santa_idle0012, R.drawable.santa_idle0036,
                        R.drawable.santa_idle0037, R.drawable.santa_idle0038), // idle
                intArrayOf(R.drawable.santa_pinchin0001, R.drawable.santa_pinchin0002,
                        R.drawable.santa_tap0003, R.drawable.santa_tap0004,
                        R.drawable.santa_tap0005, R.drawable.santa_tap0006,
                        R.drawable.santa_tap0007, R.drawable.santa_tap0008,
                        R.drawable.santa_tap0009, R.drawable.santa_tap0010,
                        R.drawable.santa_tap0011, R.drawable.santa_tap0012,
                        R.drawable.santa_tap0013, R.drawable.santa_tap0014,
                        R.drawable.santa_tap0015, R.drawable.santa_tap0016,
                        R.drawable.santa_tap0017, R.drawable.santa_tap0018,
                        R.drawable.santa_tap0019, R.drawable.santa_tap0016,
                        R.drawable.santa_tap0003, R.drawable.santa_tap0022,
                        R.drawable.santa_pinchin0001, R.drawable.santa_pinchin0001), // tap
                intArrayOf(R.drawable.santa_shake0001, R.drawable.santa_shake0002,
                        R.drawable.santa_shake0003, R.drawable.santa_shake0004,
                        R.drawable.santa_shake0005, R.drawable.santa_shake0006,
                        R.drawable.santa_shake0007, R.drawable.santa_shake0008,
                        R.drawable.santa_shake0009, R.drawable.santa_shake0010,
                        R.drawable.santa_shake0011, R.drawable.santa_shake0012,
                        R.drawable.santa_shake0013, R.drawable.santa_shake0014,
                        R.drawable.santa_shake0015, R.drawable.santa_shake0016,
                        R.drawable.santa_shake0017, R.drawable.santa_shake0018,
                        R.drawable.santa_shake0019, R.drawable.santa_shake0020,
                        R.drawable.santa_shake0021, R.drawable.santa_shake0022,
                        R.drawable.santa_shake0023, R.drawable.santa_shake0024,
                        R.drawable.santa_idle0001), // shake
                intArrayOf(R.drawable.santa_swipedown0001, R.drawable.santa_swipedown0002,
                        R.drawable.santa_swipedown0003, R.drawable.santa_swipedown0004,
                        R.drawable.santa_swipedown0005, R.drawable.santa_swipedown0006,
                        R.drawable.santa_swipedown0007, R.drawable.santa_swipedown0008,
                        R.drawable.santa_swipedown0009, R.drawable.santa_swipedown0010,
                        R.drawable.santa_swipedown0011, R.drawable.santa_swipedown0012,
                        R.drawable.santa_swipedown0013, R.drawable.santa_swipedown0014,
                        R.drawable.santa_swipedown0015, R.drawable.santa_swipedown0016,
                        R.drawable.santa_swipedown0017, R.drawable.santa_swipedown0018,
                        R.drawable.santa_swipedown0019, R.drawable.santa_swipedown0020,
                        R.drawable.santa_swipedown0021, R.drawable.santa_swipedown0022,
                        R.drawable.santa_swipedown0023, R.drawable.santa_swipedown0024,
                        R.drawable.santa_idle0001), // swipe down
                intArrayOf(R.drawable.santa_swipeup0002, R.drawable.santa_swipeup0003,
                        R.drawable.santa_swipeup0004, R.drawable.santa_swipeup0005,
                        R.drawable.santa_swipeup0006, R.drawable.santa_swipeup0007,
                        R.drawable.santa_swipeup0008, R.drawable.santa_swipeup0009,
                        R.drawable.santa_swipeup0010, R.drawable.santa_swipeup0011,
                        R.drawable.santa_swipeup0012, R.drawable.santa_swipeup0013,
                        R.drawable.santa_swipeup0014, R.drawable.santa_swipeup0015,
                        R.drawable.santa_swipeup0016, R.drawable.santa_swipeup0017,
                        R.drawable.santa_swipeup0018, R.drawable.santa_swipeup0019,
                        R.drawable.santa_swipeup0020, R.drawable.santa_swipeup0021,
                        R.drawable.santa_swipeup0022, R.drawable.santa_swipeup0023,
                        R.drawable.santa_swipeup0001,
                        R.drawable.santa_idle0001), // swipe up
                intArrayOf(R.drawable.santa_pinchin0001, R.drawable.santa_swipeleft0002,
                        R.drawable.santa_swipeleft0003, R.drawable.santa_swipeleft0004,
                        R.drawable.santa_swipeleft0005, R.drawable.santa_swipeleft0006,
                        R.drawable.santa_swipeleft0007, R.drawable.santa_swipeleft0008,
                        R.drawable.santa_swipeleft0009, R.drawable.santa_swipeleft0007,
                        R.drawable.santa_pinchin0001, R.drawable.santa_swipeleft0012,
                        R.drawable.santa_swipeleft0013, R.drawable.santa_swipeleft0014,
                        R.drawable.santa_swipeleft0015, R.drawable.santa_swipeleft0016,
                        R.drawable.santa_swipeleft0017, R.drawable.santa_swipeleft0018,
                        R.drawable.santa_swipeleft0019, R.drawable.santa_swipeleft0020,
                        R.drawable.santa_swipeleft0021, R.drawable.santa_swipeleft0022,
                        R.drawable.santa_swipeleft0023, R.drawable.santa_swipeleft0024,
                        R.drawable.santa_idle0001), // swipe left
                intArrayOf(R.drawable.santa_swipe_right20002,
                        R.drawable.santa_swipe_right20003,
                        R.drawable.santa_swipe_right20004,
                        R.drawable.santa_swipe_right20005,
                        R.drawable.santa_swipe_right20006,
                        R.drawable.santa_swipe_right20007,
                        R.drawable.santa_swipe_right20008,
                        R.drawable.santa_swipe_right20009,
                        R.drawable.santa_swipe_right20010,
                        R.drawable.santa_swipe_right20011,
                        R.drawable.santa_swipe_right20012,
                        R.drawable.santa_swipe_right20013,
                        R.drawable.santa_swipe_right20014,
                        R.drawable.santa_swipe_right20015,
                        R.drawable.santa_swipe_right20016,
                        R.drawable.santa_swipe_right20017,
                        R.drawable.santa_swipe_right20018,
                        R.drawable.santa_swipe_right20019,
                        R.drawable.santa_swipe_right20020,
                        R.drawable.santa_swipe_right20021,
                        R.drawable.santa_swipe_right20022,
                        R.drawable.santa_swipe_right20023,
                        R.drawable.santa_swipe_right20024,
                        R.drawable.santa_idle0001), // swipe right
                intArrayOf(R.drawable.santa_pinchout20001, R.drawable.santa_pinchout20002,
                        R.drawable.santa_pinchout20003, R.drawable.santa_pinchout20004,
                        R.drawable.santa_pinchout20005, R.drawable.santa_pinchout20006,
                        R.drawable.santa_pinchout20007, R.drawable.santa_pinchout20007,
                        R.drawable.santa_pinchout20007, R.drawable.santa_pinchout20007,
                        R.drawable.santa_pinchout20007, R.drawable.santa_pinchout20007,
                        R.drawable.santa_pinchout20007, R.drawable.santa_pinchout20007,
                        R.drawable.santa_pinchout20007, R.drawable.santa_pinchout20007,
                        R.drawable.santa_pinchout20017, R.drawable.santa_pinchout20018,
                        R.drawable.santa_pinchout20019, R.drawable.santa_pinchout20020,
                        R.drawable.santa_pinchout20021, R.drawable.santa_pinchout20001,
                        R.drawable.santa_pinchout20023, R.drawable.santa_pinchout20023,
                        R.drawable.santa_idle0001), // pinch in
                intArrayOf(R.drawable.santa_pinchin0001, R.drawable.santa_pinchin0002,
                        R.drawable.santa_pinchin0003, R.drawable.santa_pinchin0004,
                        R.drawable.santa_pinchin0005, R.drawable.santa_pinchin0006,
                        R.drawable.santa_pinchin0007, R.drawable.santa_pinchin0008,
                        R.drawable.santa_pinchin0009, R.drawable.santa_pinchin0010,
                        R.drawable.santa_pinchin0009, R.drawable.santa_pinchin0008,
                        R.drawable.santa_pinchin0013, R.drawable.santa_pinchin0014,
                        R.drawable.santa_pinchin0005, R.drawable.santa_pinchin0004,
                        R.drawable.santa_pinchin0017, R.drawable.santa_pinchin0018,
                        R.drawable.santa_pinchin0019, R.drawable.santa_pinchin0020,
                        R.drawable.santa_pinchin0017, R.drawable.santa_pinchin0022,
                        R.drawable.santa_pinchin0002, R.drawable.santa_pinchin0001,
                        R.drawable.santa_idle0001) // pinch out
        )

        private val frameIndices =
                arrayOf(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 9, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0,
                        0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2, 1,
                        0, 0), // idle
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23), // tap
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23, 24), // shake
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23, 24), // swipe down
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23), // swipe up
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23, 24), // swipe left
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23), // swipe right
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23, 24), // pinch in
                        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                                19, 20, 21, 22, 23, 24) // pinch out
                )
    }
}
