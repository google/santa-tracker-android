/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.dasherdancer;

public class Reindeer implements Character {

    private static final long[] sDurations = new long[]{
            1000, 1000, 1000, 1000, 1000,
            1000, 1000, 1000, 1000
    };

    private static final int[][] sFrames = new int[][]{
            {
                    R.drawable.reindeer_tap0001}, //idle
            {R.drawable.reindeer_tap0001, R.drawable.reindeer_tap0002, R.drawable.reindeer_tap0003, R.drawable.reindeer_tap0004,
                    R.drawable.reindeer_tap0005, R.drawable.reindeer_tap0006, R.drawable.reindeer_tap0007, R.drawable.reindeer_tap0008,
                    R.drawable.reindeer_tap0009, R.drawable.reindeer_tap0010, R.drawable.reindeer_tap0011, R.drawable.reindeer_tap0012,
                    R.drawable.reindeer_tap0013, R.drawable.reindeer_tap0014, R.drawable.reindeer_tap0015, R.drawable.reindeer_tap0016,
                    R.drawable.reindeer_tap0017, R.drawable.reindeer_tap0018, R.drawable.reindeer_tap0019, R.drawable.reindeer_tap0020,
                    R.drawable.reindeer_tap0021, R.drawable.reindeer_tap0022, R.drawable.reindeer_tap0023, R.drawable.reindeer_tap0024},//tap
            {R.drawable.reindeer_shake0001, R.drawable.reindeer_shake0002, R.drawable.reindeer_shake0003, R.drawable.reindeer_shake0004,
                    R.drawable.reindeer_shake0005, R.drawable.reindeer_shake0006, R.drawable.reindeer_shake0007, R.drawable.reindeer_shake0008,
                    R.drawable.reindeer_shake0009, R.drawable.reindeer_shake0010, R.drawable.reindeer_shake0011, R.drawable.reindeer_shake0012,
                    R.drawable.reindeer_shake0013, R.drawable.reindeer_shake0014, R.drawable.reindeer_shake0015, R.drawable.reindeer_shake0016,
                    R.drawable.reindeer_shake0017, R.drawable.reindeer_shake0018, R.drawable.reindeer_shake0019, R.drawable.reindeer_shake0020,
                    R.drawable.reindeer_shake0021, R.drawable.reindeer_shake0022, R.drawable.reindeer_shake0023, R.drawable.reindeer_shake0024},//shake
            {R.drawable.reindeer_swipedown0001,
                    R.drawable.reindeer_swipedown0002,
                    R.drawable.reindeer_swipedown0003,
                    R.drawable.reindeer_swipedown0004,
                    R.drawable.reindeer_swipedown0005,
                    R.drawable.reindeer_swipedown0006,
                    R.drawable.reindeer_swipedown0007,
                    R.drawable.reindeer_swipedown0008,
                    R.drawable.reindeer_swipedown0009,
                    R.drawable.reindeer_swipedown0010,
                    R.drawable.reindeer_swipedown0011,
                    R.drawable.reindeer_swipedown0012,
                    R.drawable.reindeer_swipedown0013,
                    R.drawable.reindeer_swipedown0014,
                    R.drawable.reindeer_swipedown0015,
                    R.drawable.reindeer_swipedown0016,
                    R.drawable.reindeer_swipedown0017,
                    R.drawable.reindeer_swipedown0018,
                    R.drawable.reindeer_swipedown0019,
                    R.drawable.reindeer_swipedown0020,
                    R.drawable.reindeer_swipedown0021,
                    R.drawable.reindeer_swipedown0022,
                    R.drawable.reindeer_swipedown0023,
                    R.drawable.reindeer_swipedown0024},//swipe down
            {R.drawable.reindeer_swipeup0002,
                    R.drawable.reindeer_swipeup0003,
                    R.drawable.reindeer_swipeup0004,
                    R.drawable.reindeer_swipeup0005,
                    R.drawable.reindeer_swipeup0006,
                    R.drawable.reindeer_swipeup0007,
                    R.drawable.reindeer_swipeup0008,
                    R.drawable.reindeer_swipeup0009,
                    R.drawable.reindeer_swipeup0010,
                    R.drawable.reindeer_swipeup0011,
                    R.drawable.reindeer_swipeup0012,
                    R.drawable.reindeer_swipeup0013,
                    R.drawable.reindeer_swipeup0014,
                    R.drawable.reindeer_swipeup0015,
                    R.drawable.reindeer_swipeup0016,
                    R.drawable.reindeer_swipeup0017,
                    R.drawable.reindeer_swipeup0018,
                    R.drawable.reindeer_swipeup0019,
                    R.drawable.reindeer_swipeup0020,
                    R.drawable.reindeer_swipeup0021,
                    R.drawable.reindeer_swipeup0022,
                    R.drawable.reindeer_swipeup0023,
                    R.drawable.reindeer_swipeup0024},//swipe up
            {R.drawable.reindeer_swipeleft0001,
                    R.drawable.reindeer_swipeleft0002,
                    R.drawable.reindeer_swipeleft0003,
                    R.drawable.reindeer_swipeleft0004,
                    R.drawable.reindeer_swipeleft0005,
                    R.drawable.reindeer_swipeleft0006,
                    R.drawable.reindeer_swipeleft0007,
                    R.drawable.reindeer_swipeleft0008,
                    R.drawable.reindeer_swipeleft0009,
                    R.drawable.reindeer_swipeleft0010,
                    R.drawable.reindeer_swipeleft0011,
                    R.drawable.reindeer_swipeleft0012,
                    R.drawable.reindeer_swipeleft0013,
                    R.drawable.reindeer_swipeleft0014,
                    R.drawable.reindeer_swipeleft0015,
                    R.drawable.reindeer_swipeleft0016,
                    R.drawable.reindeer_swipeleft0017,
                    R.drawable.reindeer_swipeleft0018,
                    R.drawable.reindeer_swipeleft0019,
                    R.drawable.reindeer_swipeleft0020,
                    R.drawable.reindeer_swipeleft0021,
                    R.drawable.reindeer_swipeleft0022,
                    R.drawable.reindeer_swipeleft0023,
                    R.drawable.reindeer_swipeleft0024},//swipe left
            {R.drawable.reindeer_swiperight0002,
                    R.drawable.reindeer_swiperight0003,
                    R.drawable.reindeer_swiperight0004,
                    R.drawable.reindeer_swiperight0005,
                    R.drawable.reindeer_swiperight0006,
                    R.drawable.reindeer_swiperight0007,
                    R.drawable.reindeer_swiperight0008,
                    R.drawable.reindeer_swiperight0009,
                    R.drawable.reindeer_swiperight0010,
                    R.drawable.reindeer_swiperight0011,
                    R.drawable.reindeer_swiperight0012,
                    R.drawable.reindeer_swiperight0013,
                    R.drawable.reindeer_swiperight0014,
                    R.drawable.reindeer_swiperight0015,
                    R.drawable.reindeer_swiperight0016,
                    R.drawable.reindeer_swiperight0017,
                    R.drawable.reindeer_swiperight0018,
                    R.drawable.reindeer_swiperight0019,
                    R.drawable.reindeer_swiperight0020,
                    R.drawable.reindeer_swiperight0021,
                    R.drawable.reindeer_swiperight0022,
                    R.drawable.reindeer_swiperight0023,
                    R.drawable.reindeer_swiperight0024},//swipe right
            {R.drawable.reindeer_pinchout0001,
                    R.drawable.reindeer_pinchout0002,
                    R.drawable.reindeer_pinchout0003,
                    R.drawable.reindeer_pinchout0004,
                    R.drawable.reindeer_pinchout0005,
                    R.drawable.reindeer_pinchout0006,
                    R.drawable.reindeer_pinchout0007,
                    R.drawable.reindeer_pinchout0008,
                    R.drawable.reindeer_pinchout0009,
                    R.drawable.reindeer_pinchout0010,
                    R.drawable.reindeer_pinchout0011,
                    R.drawable.reindeer_pinchout0012,
                    R.drawable.reindeer_pinchout0013,
                    R.drawable.reindeer_pinchout0014,
                    R.drawable.reindeer_pinchout0015,
                    R.drawable.reindeer_pinchout0016,
                    R.drawable.reindeer_pinchout0017,
                    R.drawable.reindeer_pinchout0018,
                    R.drawable.reindeer_pinchout0019,
                    R.drawable.reindeer_pinchout0020,
                    R.drawable.reindeer_pinchout0021,
                    R.drawable.reindeer_pinchout0022,
                    R.drawable.reindeer_pinchout0023,
                    R.drawable.reindeer_pinchout0024},//pinch in
            {R.drawable.reindeer_pinchin0001,
                    R.drawable.reindeer_pinchin0002,
                    R.drawable.reindeer_pinchin0003,
                    R.drawable.reindeer_pinchin0004,
                    R.drawable.reindeer_pinchin0005,
                    R.drawable.reindeer_pinchin0006,
                    R.drawable.reindeer_pinchin0007,
                    R.drawable.reindeer_pinchin0008,
                    R.drawable.reindeer_pinchin0009,
                    R.drawable.reindeer_pinchin0010,
                    R.drawable.reindeer_pinchin0011,
                    R.drawable.reindeer_pinchin0012,
                    R.drawable.reindeer_pinchin0013,
                    R.drawable.reindeer_pinchin0014,
                    R.drawable.reindeer_pinchin0015,
                    R.drawable.reindeer_pinchin0016,
                    R.drawable.reindeer_pinchin0017,
                    R.drawable.reindeer_pinchin0018,
                    R.drawable.reindeer_pinchin0019,
                    R.drawable.reindeer_pinchin0020,
                    R.drawable.reindeer_pinchin0021,
                    R.drawable.reindeer_pinchin0022,
                    R.drawable.reindeer_pinchin0023,
                    R.drawable.reindeer_pinchin0024}//pinch in
    };

    private static final int[][] sFrameIndices = new int[][]{
            {0},//idle
            {0,1,2,3,4,5,6,7,
                    8,9,10,11,12,13,14,15,
                    16,17,18,19,20,21,22,23},//tap
            {0, 1, 2, 3, 4,
                    5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
                    22, 23},//shake
            {0,1,2,3,4,5,6,7,
                    8,9,10,11,12,13,14,15,
                    16,17,18,19,20,21,22,23},//swipe down
            {0,1,2,3,4,5,6,7,
                    8,9,10,11,12,13,14,15,
                    16,17,18,19,20,21,22},//swipe up
            {0,1,2,3,4,5,6,7,
                    8,9,10,11,12,13,14,15,
                    16,17,18,19,20,21,22,23},//swipe left
            {0,1,2,3,4,5,6,7,
                    8,9,10,11,12,13,14,15,
                    16,17,18,19,20,21,22},//swipe right
            {0, 1, 2,
                    3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                    19, 20, 21, 22, 23},//pinch in
            {0, 1, 2,
                    3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
                    19, 20, 21, 22, 23}//pinch out
    };

    @Override
    public long getDuration(int animationKey) {
        return sDurations[animationKey];
    }

    @Override
    public int[] getFrameIndices(int animationKey) {
        return sFrameIndices[animationKey];
    }

    @Override
    public int[] getFrames(int animationKey) {
        return sFrames[animationKey];
    }

    @Override
    public int getSoundResource(int animationid) {
        switch (animationid) {
            case Character.ANIM_PINCH_IN:
                return R.raw.reindeer_pinchin;
            case Character.ANIM_PINCH_OUT:
                return R.raw.reindeer_pinchout;
            case Character.ANIM_SHAKE:
                return R.raw.reindeer_shake;
            case Character.ANIM_SWIPE_UP:
                return R.raw.reindeer_swipeup;
            case Character.ANIM_SWIPE_DOWN:
                return R.raw.reindeer_swipedown;
            case Character.ANIM_SWIPE_LEFT:
                return R.raw.reindeer_swipeleft;
            case Character.ANIM_SWIPE_RIGHT:
                return R.raw.reindeer_swiperight;
            case Character.ANIM_TAP:
                return R.raw.reindeer_tap2;
        }

        return -1;
    }

    @Override
    public String getCharacterName() {
        return "r";
    }

}
