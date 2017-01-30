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

import java.lang.*;

public class Santa implements Character {

	private static final long[] sDurations = new long[]{
            2400, 1000, 1000, 1000, 1000,
            1000, 1000, 1000, 1000
	};
	
	private static final int[][] sFrames = new int[][]{
		{R.drawable.santa_idle0001,
			R.drawable.santa_idle0002, R.drawable.santa_idle0003,
			R.drawable.santa_idle0004, R.drawable.santa_idle0005,
			R.drawable.santa_idle0006, R.drawable.santa_idle0007,
			R.drawable.santa_idle0008,
			R.drawable.santa_idle0009,
			R.drawable.santa_idle0010, // index is 9
			R.drawable.santa_idle0035, R.drawable.santa_idle0036,
			R.drawable.santa_idle0037, R.drawable.santa_idle0038},//idle
		{R.drawable.santa_tap0001, R.drawable.santa_tap0002, R.drawable.santa_tap0003,
                R.drawable.santa_tap0004, R.drawable.santa_tap0005, R.drawable.santa_tap0006,
                R.drawable.santa_tap0007, R.drawable.santa_tap0008, R.drawable.santa_tap0009,
                R.drawable.santa_tap0010, R.drawable.santa_tap0011, R.drawable.santa_tap0012,
                R.drawable.santa_tap0013, R.drawable.santa_tap0014, R.drawable.santa_tap0015,
                R.drawable.santa_tap0016, R.drawable.santa_tap0017, R.drawable.santa_tap0018,
                R.drawable.santa_tap0019, R.drawable.santa_tap0020, R.drawable.santa_tap0021,
                R.drawable.santa_tap0022, R.drawable.santa_tap0023, R.drawable.santa_tap0024},//tap
		{R.drawable.santa_shake0001,
				R.drawable.santa_shake0002,
				R.drawable.santa_shake0003,
				R.drawable.santa_shake0004,
				R.drawable.santa_shake0005,
				R.drawable.santa_shake0006,
				R.drawable.santa_shake0007,
				R.drawable.santa_shake0008,
				R.drawable.santa_shake0009,
				R.drawable.santa_shake0010,
				R.drawable.santa_shake0011,
				R.drawable.santa_shake0012,
				R.drawable.santa_shake0013,
				R.drawable.santa_shake0014,
				R.drawable.santa_shake0015,
				R.drawable.santa_shake0016,
				R.drawable.santa_shake0017,
				R.drawable.santa_shake0018,
				R.drawable.santa_shake0019,
				R.drawable.santa_shake0020,
				R.drawable.santa_shake0021,
				R.drawable.santa_shake0022,
				R.drawable.santa_shake0023,
				R.drawable.santa_shake0024,
				R.drawable.santa_idle0001},//shake
		{R.drawable.santa_swipedown0001,
			R.drawable.santa_swipedown0002,
			R.drawable.santa_swipedown0003,
			R.drawable.santa_swipedown0004,
			R.drawable.santa_swipedown0005,
			R.drawable.santa_swipedown0006,
			R.drawable.santa_swipedown0007,
			R.drawable.santa_swipedown0008,
			R.drawable.santa_swipedown0009,
			R.drawable.santa_swipedown0010,
			R.drawable.santa_swipedown0011,
			R.drawable.santa_swipedown0012,
			R.drawable.santa_swipedown0013,
			R.drawable.santa_swipedown0014,
			R.drawable.santa_swipedown0015,
			R.drawable.santa_swipedown0016,
			R.drawable.santa_swipedown0017,
			R.drawable.santa_swipedown0018,
			R.drawable.santa_swipedown0019,
			R.drawable.santa_swipedown0020,
			R.drawable.santa_swipedown0021,
			R.drawable.santa_swipedown0022,
			R.drawable.santa_swipedown0023,
			R.drawable.santa_swipedown0024,
			R.drawable.santa_idle0001},//swipe down
		{R.drawable.santa_swipeup0002,
			R.drawable.santa_swipeup0003,
			R.drawable.santa_swipeup0004,
			R.drawable.santa_swipeup0005,
			R.drawable.santa_swipeup0006,
			R.drawable.santa_swipeup0007,
			R.drawable.santa_swipeup0008,
			R.drawable.santa_swipeup0009,
			R.drawable.santa_swipeup0010,
			R.drawable.santa_swipeup0011,
			R.drawable.santa_swipeup0012,
			R.drawable.santa_swipeup0013,
			R.drawable.santa_swipeup0014,
			R.drawable.santa_swipeup0015,
			R.drawable.santa_swipeup0016,
			R.drawable.santa_swipeup0017,
			R.drawable.santa_swipeup0018,
			R.drawable.santa_swipeup0019,
			R.drawable.santa_swipeup0020,
			R.drawable.santa_swipeup0021,
			R.drawable.santa_swipeup0022,
			R.drawable.santa_swipeup0023,
			R.drawable.santa_swipeup0024,
			R.drawable.santa_idle0001},//swipe up
		{R.drawable.santa_swipeleft0001,
			R.drawable.santa_swipeleft0002,
			R.drawable.santa_swipeleft0003,
			R.drawable.santa_swipeleft0004,
			R.drawable.santa_swipeleft0005,
			R.drawable.santa_swipeleft0006,
			R.drawable.santa_swipeleft0007,
			R.drawable.santa_swipeleft0008,
			R.drawable.santa_swipeleft0009,
			R.drawable.santa_swipeleft0010,
			R.drawable.santa_swipeleft0011,
			R.drawable.santa_swipeleft0012,
			R.drawable.santa_swipeleft0013,
			R.drawable.santa_swipeleft0014,
			R.drawable.santa_swipeleft0015,
			R.drawable.santa_swipeleft0016,
			R.drawable.santa_swipeleft0017,
			R.drawable.santa_swipeleft0018,
			R.drawable.santa_swipeleft0019,
			R.drawable.santa_swipeleft0020,
			R.drawable.santa_swipeleft0021,
			R.drawable.santa_swipeleft0022,
			R.drawable.santa_swipeleft0023,
			R.drawable.santa_swipeleft0024,
			R.drawable.santa_idle0001},//swipe left 
		{R.drawable.santa_swipe_right20002,
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
				R.drawable.santa_idle0001},//swipe right
		{R.drawable.santa_pinchout20001,
					R.drawable.santa_pinchout20002,
					R.drawable.santa_pinchout20003,
					R.drawable.santa_pinchout20004,
					R.drawable.santa_pinchout20005,
					R.drawable.santa_pinchout20006,
					R.drawable.santa_pinchout20007,
					R.drawable.santa_pinchout20008,
					R.drawable.santa_pinchout20009,
					R.drawable.santa_pinchout20010,
					R.drawable.santa_pinchout20011,
					R.drawable.santa_pinchout20012,
					R.drawable.santa_pinchout20013,
					R.drawable.santa_pinchout20014,
					R.drawable.santa_pinchout20015,
					R.drawable.santa_pinchout20016,
					R.drawable.santa_pinchout20017,
					R.drawable.santa_pinchout20018,
					R.drawable.santa_pinchout20019,
					R.drawable.santa_pinchout20020,
					R.drawable.santa_pinchout20021,
					R.drawable.santa_pinchout20022,
					R.drawable.santa_pinchout20023,
					R.drawable.santa_pinchout20024,
					R.drawable.santa_idle0001},//pinch in
		{R.drawable.santa_pinchin0001,
						R.drawable.santa_pinchin0002,
						R.drawable.santa_pinchin0003,
						R.drawable.santa_pinchin0004,
						R.drawable.santa_pinchin0005,
						R.drawable.santa_pinchin0006,
						R.drawable.santa_pinchin0007,
						R.drawable.santa_pinchin0008,
						R.drawable.santa_pinchin0009,
						R.drawable.santa_pinchin0010,
						R.drawable.santa_pinchin0011,
						R.drawable.santa_pinchin0012,
						R.drawable.santa_pinchin0013,
						R.drawable.santa_pinchin0014,
						R.drawable.santa_pinchin0015,
						R.drawable.santa_pinchin0016,
						R.drawable.santa_pinchin0017,
						R.drawable.santa_pinchin0018,
						R.drawable.santa_pinchin0019,
						R.drawable.santa_pinchin0020,
						R.drawable.santa_pinchin0021,
						R.drawable.santa_pinchin0022,
						R.drawable.santa_pinchin0023,
						R.drawable.santa_pinchin0024,
						R.drawable.santa_idle0001}//pinch out
	};
	
	private static final int[][] sFrameIndices = new int[][]{
		{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 9, 9, 8, 7, 6, 5, 4,
			3, 2, 1, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 10,
			11, 12, 13, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0},//idle
		{0, 1, 2, 3, 4,
                5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
                22, 23},//tap
		{0, 1, 2, 3, 4,
				5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
				22, 23, 24},//shake
		{0,1,2,3,4,5,6,7,
			8,9,10,11,12,13,14,15,
			16,17,18,19,20,21,22,23, 24},//swipe down
		{0,1,2,3,4,5,6,7,
			8,9,10,11,12,13,14,15,
			16,17,18,19,20,21,22, 23},//swipe up
		{0,1,2,3,4,5,6,7,
			8,9,10,11,12,13,14,15,
			16,17,18,19,20,21,22,23,24},//swipe left 
		{0,1,2,3,4,5,6,7,
				8,9,10,11,12,13,14,15,
				16,17,18,19,20,21,22,23},//swipe right
		{0, 1, 2,
					3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
					19, 20, 21, 22, 23, 24},//pinch in
		{0, 1,
						2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
						19, 20, 21, 22, 23, 24}//pinch out
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
				return R.raw.santa_pinchin;
			case Character.ANIM_PINCH_OUT:
				return R.raw.santa_pinchout;
			case Character.ANIM_SHAKE:
				return R.raw.santa_shake;
			case Character.ANIM_SWIPE_UP:
				return R.raw.santa_swipeup;
			case Character.ANIM_SWIPE_LEFT:
				return R.raw.santa_swipeleft;
			case Character.ANIM_SWIPE_RIGHT:
				return R.raw.santa_swiperight;
			case Character.ANIM_SWIPE_DOWN:
				return R.raw.santa_swipedown;
			case Character.ANIM_TAP:
				return R.raw.santa_tap;
		}

		return -1;
	}

	@Override
    public String getCharacterName() {
        return "s";
    }

}
