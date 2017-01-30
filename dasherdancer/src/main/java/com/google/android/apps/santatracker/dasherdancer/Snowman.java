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

public class Snowman implements Character {

	private static final long[] sDurations = new long[]{
            1000, 1000, 1000, 960, 1000,
            1000, 1000, 1000, 1000
	};
	
	private static final int[][] sFrames = new int[][]{
		{
			R.drawable.snowman_idle0001, R.drawable.snowman_idle0002, R.drawable.snowman_idle0003, R.drawable.snowman_idle0004,
			R.drawable.snowman_idle0005, R.drawable.snowman_idle0006, R.drawable.snowman_idle0007, R.drawable.snowman_idle0008,
			R.drawable.snowman_idle0009, R.drawable.snowman_idle0010, R.drawable.snowman_idle0011, R.drawable.snowman_idle0012,
			R.drawable.snowman_idle0013, R.drawable.snowman_idle0014, R.drawable.snowman_idle0015, R.drawable.snowman_idle0016,
			R.drawable.snowman_idle0017, R.drawable.snowman_idle0018, R.drawable.snowman_idle0019, R.drawable.snowman_idle0020,
			R.drawable.snowman_idle0021, R.drawable.snowman_idle0022, R.drawable.snowman_idle0023, R.drawable.snowman_idle0024}, //idle
		{R.drawable.snowman_tap0001, R.drawable.snowman_tap0002, R.drawable.snowman_tap0003, R.drawable.snowman_tap0004,
				R.drawable.snowman_tap0005, R.drawable.snowman_tap0006, R.drawable.snowman_tap0007, R.drawable.snowman_tap0008,
				R.drawable.snowman_tap0009, R.drawable.snowman_tap0010, R.drawable.snowman_tap0011, R.drawable.snowman_tap0012,
				R.drawable.snowman_tap0013, R.drawable.snowman_tap0014, R.drawable.snowman_tap0015, R.drawable.snowman_tap0016,
				R.drawable.snowman_tap0017, R.drawable.snowman_tap0018, R.drawable.snowman_tap0019, R.drawable.snowman_tap0020,
				R.drawable.snowman_tap0021, R.drawable.snowman_tap0022, R.drawable.snowman_tap0023, R.drawable.snowman_tap0024},//tap
		{R.drawable.snowman_shake0001, R.drawable.snowman_shake0002, R.drawable.snowman_shake0003, R.drawable.snowman_shake0004,
				R.drawable.snowman_shake0005, R.drawable.snowman_shake0006, R.drawable.snowman_shake0007, R.drawable.snowman_shake0008,
				R.drawable.snowman_shake0009, R.drawable.snowman_shake0010, R.drawable.snowman_shake0011, R.drawable.snowman_shake0012,
				R.drawable.snowman_shake0013, R.drawable.snowman_shake0014, R.drawable.snowman_shake0015, R.drawable.snowman_shake0016,
				R.drawable.snowman_shake0017, R.drawable.snowman_shake0018, R.drawable.snowman_shake0019, R.drawable.snowman_shake0020,
				R.drawable.snowman_shake0021, R.drawable.snowman_shake0022, R.drawable.snowman_shake0023, R.drawable.snowman_shake0024},//shake
		{R.drawable.snowman_swipedown0001,
			R.drawable.snowman_swipedown0002,
			R.drawable.snowman_swipedown0003,
			R.drawable.snowman_swipedown0004,
			R.drawable.snowman_swipedown0005,
			R.drawable.snowman_swipedown0006,
			R.drawable.snowman_swipedown0007,
			R.drawable.snowman_swipedown0008,
			R.drawable.snowman_swipedown0009,
			R.drawable.snowman_swipedown0010,
			R.drawable.snowman_swipedown0011,
			R.drawable.snowman_swipedown0012,
			R.drawable.snowman_swipedown0013,
			R.drawable.snowman_swipedown0014,
			R.drawable.snowman_swipedown0015,
			R.drawable.snowman_swipedown0016,
			R.drawable.snowman_swipedown0017,
			R.drawable.snowman_swipedown0018,
			R.drawable.snowman_swipedown0019,
			R.drawable.snowman_swipedown0020,
			R.drawable.snowman_swipedown0021,
			R.drawable.snowman_swipedown0022,
			R.drawable.snowman_swipedown0023},//swipe down
		{R.drawable.snowman_swipeup0002,
			R.drawable.snowman_swipeup0003,
			R.drawable.snowman_swipeup0004,
			R.drawable.snowman_swipeup0005,
			R.drawable.snowman_swipeup0006,
			R.drawable.snowman_swipeup0007,
			R.drawable.snowman_swipeup0008,
			R.drawable.snowman_swipeup0009,
			R.drawable.snowman_swipeup0010,
			R.drawable.snowman_swipeup0011,
			R.drawable.snowman_swipeup0012,
			R.drawable.snowman_swipeup0013,
			R.drawable.snowman_swipeup0014,
			R.drawable.snowman_swipeup0015,
			R.drawable.snowman_swipeup0016,
			R.drawable.snowman_swipeup0017,
			R.drawable.snowman_swipeup0018,
			R.drawable.snowman_swipeup0019,
			R.drawable.snowman_swipeup0020,
			R.drawable.snowman_swipeup0021,
			R.drawable.snowman_swipeup0022,
			R.drawable.snowman_swipeup0023,
			R.drawable.snowman_swipeup0024},//swipe up
		{R.drawable.snowman_swipeleft0001,
			R.drawable.snowman_swipeleft0002,
			R.drawable.snowman_swipeleft0003,
			R.drawable.snowman_swipeleft0004,
			R.drawable.snowman_swipeleft0005,
			R.drawable.snowman_swipeleft0006,
			R.drawable.snowman_swipeleft0007,
			R.drawable.snowman_swipeleft0008,
			R.drawable.snowman_swipeleft0009,
			R.drawable.snowman_swipeleft0010,
			R.drawable.snowman_swipeleft0011,
			R.drawable.snowman_swipeleft0012,
			R.drawable.snowman_swipeleft0013,
			R.drawable.snowman_swipeleft0014,
			R.drawable.snowman_swipeleft0015,
			R.drawable.snowman_swipeleft0016,
			R.drawable.snowman_swipeleft0017,
			R.drawable.snowman_swipeleft0018,
			R.drawable.snowman_swipeleft0019,
			R.drawable.snowman_swipeleft0020,
			R.drawable.snowman_swipeleft0021,
			R.drawable.snowman_swipeleft0022,
			R.drawable.snowman_swipeleft0023,
			R.drawable.snowman_swipeleft0024},//swipe left 
		{R.drawable.snowman_swiperight0002,
				R.drawable.snowman_swiperight0003,
				R.drawable.snowman_swiperight0004,
				R.drawable.snowman_swiperight0005,
				R.drawable.snowman_swiperight0006,
				R.drawable.snowman_swiperight0007,
				R.drawable.snowman_swiperight0008,
				R.drawable.snowman_swiperight0009,
				R.drawable.snowman_swiperight0010,
				R.drawable.snowman_swiperight0011,
				R.drawable.snowman_swiperight0012,
				R.drawable.snowman_swiperight0013,
				R.drawable.snowman_swiperight0014,
				R.drawable.snowman_swiperight0015,
				R.drawable.snowman_swiperight0016,
				R.drawable.snowman_swiperight0017,
				R.drawable.snowman_swiperight0018,
				R.drawable.snowman_swiperight0019,
				R.drawable.snowman_swiperight0020,
				R.drawable.snowman_swiperight0021,
				R.drawable.snowman_swiperight0022,
				R.drawable.snowman_swiperight0023,
				R.drawable.snowman_swiperight0024},//swipe right
		{R.drawable.snowman_pinchin0001, R.drawable.snowman_pinchin0002, R.drawable.snowman_pinchin0003, R.drawable.snowman_pinchin0004,
                R.drawable.snowman_pinchin0005, R.drawable.snowman_pinchin0006, R.drawable.snowman_pinchin0007, R.drawable.snowman_pinchin0008,
                R.drawable.snowman_pinchin0009, R.drawable.snowman_pinchin0010, R.drawable.snowman_pinchin0011, R.drawable.snowman_pinchin0012,
                R.drawable.snowman_pinchin0013, R.drawable.snowman_pinchin0014, R.drawable.snowman_pinchin0015, R.drawable.snowman_pinchin0016,
                R.drawable.snowman_pinchin0017, R.drawable.snowman_pinchin0018, R.drawable.snowman_pinchin0019, R.drawable.snowman_pinchin0020,
                R.drawable.snowman_pinchin0021, R.drawable.snowman_pinchin0022, R.drawable.snowman_pinchin0023, R.drawable.snowman_pinchin0024},//pinch out
            {R.drawable.snowman_pinchout0001,
                    R.drawable.snowman_pinchout0002,
                    R.drawable.snowman_pinchout0003,
                    R.drawable.snowman_pinchout0004,
                    R.drawable.snowman_pinchout0005,
                    R.drawable.snowman_pinchout0006,
                    R.drawable.snowman_pinchout0007,
                    R.drawable.snowman_pinchout0008,
                    R.drawable.snowman_pinchout0009,
                    R.drawable.snowman_pinchout0010,
                    R.drawable.snowman_pinchout0011,
                    R.drawable.snowman_pinchout0012,
                    R.drawable.snowman_pinchout0013,
                    R.drawable.snowman_pinchout0014,
                    R.drawable.snowman_pinchout0015,
                    R.drawable.snowman_pinchout0016,
                    R.drawable.snowman_pinchout0017,
                    R.drawable.snowman_pinchout0018,
                    R.drawable.snowman_pinchout0019,
                    R.drawable.snowman_pinchout0020,
                    R.drawable.snowman_pinchout0021,
                    R.drawable.snowman_pinchout0022,
                    R.drawable.snowman_pinchout0023,
                    R.drawable.snowman_pinchout0024}//pinch in
	};
	
	private static final int[][] sFrameIndices = new int[][]{
		{0,1,2,3,4,5,6,7,
			8,9,10,11,12,13,14,15,
			16,17,18,19,20,21,22,23},//idle
		{0,1,2,3,4,5,6,7,
				8,9,10,11,12,13,14,15,
				16,17,18,19,20,21,22,23},//tap
		{0, 1, 2, 3, 4,
				5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
				22, 23},//shake
		{0,1,2,3,4,5,6,7,
			8,9,10,11,12,13,14,15,
			16,17,18,19,20,21,22},//swipe down
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
				return R.raw.snowman_pinchin;
			case Character.ANIM_PINCH_OUT:
				return R.raw.snowman_pinchout;
			case Character.ANIM_SHAKE:
				return R.raw.snowman_shake;
			case Character.ANIM_SWIPE_UP:
				return R.raw.snowman_swipeup;
			case Character.ANIM_SWIPE_DOWN:
				return R.raw.snowman_swipedown;
			case Character.ANIM_SWIPE_LEFT:
				return R.raw.snowman_swipeleft;
			case Character.ANIM_SWIPE_RIGHT:
				return R.raw.snowman_swiperight;
			case Character.ANIM_TAP:
				return R.raw.snowman_tap;
		}

		return -1;
	}

	@Override
    public String getCharacterName() {
        return "t";
    }

}
