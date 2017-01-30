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

public class Elf implements Character {

	private static final long[] sDurations = new long[]{
            1000, 1000, 1000, 1000, 1000,
            1000, 1000, 1000, 1333
	};
	
	private static final int[][] sFrames = new int[][]{
		{
			R.drawable.elf_idle0001, R.drawable.elf_idle0002, R.drawable.elf_idle0003, R.drawable.elf_idle0004,
			R.drawable.elf_idle0005, R.drawable.elf_idle0006, R.drawable.elf_idle0007, R.drawable.elf_idle0008,
			R.drawable.elf_idle0009, R.drawable.elf_idle0010, R.drawable.elf_idle0011, R.drawable.elf_idle0012,
			R.drawable.elf_idle0013, R.drawable.elf_idle0014, R.drawable.elf_idle0015, R.drawable.elf_idle0016,
			R.drawable.elf_idle0017, R.drawable.elf_idle0018, R.drawable.elf_idle0019, R.drawable.elf_idle0020,
			R.drawable.elf_idle0021, R.drawable.elf_idle0022, R.drawable.elf_idle0023, R.drawable.elf_idle0024}, //idle
		{R.drawable.elf_tap0001, R.drawable.elf_tap0002, R.drawable.elf_tap0003, R.drawable.elf_tap0004,
				R.drawable.elf_tap0005, R.drawable.elf_tap0006, R.drawable.elf_tap0007, R.drawable.elf_tap0008,
				R.drawable.elf_tap0009, R.drawable.elf_tap0010, R.drawable.elf_tap0011, R.drawable.elf_tap0012,
				R.drawable.elf_tap0013, R.drawable.elf_tap0014, R.drawable.elf_tap0015, R.drawable.elf_tap0016,
				R.drawable.elf_tap0017, R.drawable.elf_tap0018, R.drawable.elf_tap0019, R.drawable.elf_tap0020,
				R.drawable.elf_tap0021, R.drawable.elf_tap0022, R.drawable.elf_tap0023, R.drawable.elf_tap0024},//tap
		{R.drawable.elf_shake0001, R.drawable.elf_shake0002, R.drawable.elf_shake0003, R.drawable.elf_shake0004,
				R.drawable.elf_shake0005, R.drawable.elf_shake0006, R.drawable.elf_shake0007, R.drawable.elf_shake0008,
				R.drawable.elf_shake0009, R.drawable.elf_shake0010, R.drawable.elf_shake0011, R.drawable.elf_shake0012,
				R.drawable.elf_shake0013, R.drawable.elf_shake0014, R.drawable.elf_shake0015, R.drawable.elf_shake0016,
				R.drawable.elf_shake0017, R.drawable.elf_shake0018, R.drawable.elf_shake0019, R.drawable.elf_shake0020,
				R.drawable.elf_shake0021, R.drawable.elf_shake0022, R.drawable.elf_shake0023, R.drawable.elf_shake0024},//shake
		{R.drawable.elf_swipedown0001,
			R.drawable.elf_swipedown0002,
			R.drawable.elf_swipedown0003,
			R.drawable.elf_swipedown0004,
			R.drawable.elf_swipedown0005,
			R.drawable.elf_swipedown0006,
			R.drawable.elf_swipedown0007,
			R.drawable.elf_swipedown0008,
			R.drawable.elf_swipedown0009,
			R.drawable.elf_swipedown0010,
			R.drawable.elf_swipedown0011,
			R.drawable.elf_swipedown0012,
			R.drawable.elf_swipedown0013,
			R.drawable.elf_swipedown0014,
			R.drawable.elf_swipedown0015,
			R.drawable.elf_swipedown0016,
			R.drawable.elf_swipedown0017,
			R.drawable.elf_swipedown0018,
			R.drawable.elf_swipedown0019,
			R.drawable.elf_swipedown0020,
			R.drawable.elf_swipedown0021,
			R.drawable.elf_swipedown0022,
			R.drawable.elf_swipedown0023,
			R.drawable.elf_swipedown0024},//swipe down
		{R.drawable.elf_swipeup0002,
			R.drawable.elf_swipeup0003,
			R.drawable.elf_swipeup0004,
			R.drawable.elf_swipeup0005,
			R.drawable.elf_swipeup0006,
			R.drawable.elf_swipeup0007,
			R.drawable.elf_swipeup0008,
			R.drawable.elf_swipeup0009,
			R.drawable.elf_swipeup0010,
			R.drawable.elf_swipeup0011,
			R.drawable.elf_swipeup0012,
			R.drawable.elf_swipeup0013,
			R.drawable.elf_swipeup0014,
			R.drawable.elf_swipeup0015,
			R.drawable.elf_swipeup0016,
			R.drawable.elf_swipeup0017,
			R.drawable.elf_swipeup0018,
			R.drawable.elf_swipeup0019,
			R.drawable.elf_swipeup0020,
			R.drawable.elf_swipeup0021,
			R.drawable.elf_swipeup0022,
			R.drawable.elf_swipeup0023,
			R.drawable.elf_swipeup0024},//swipe up
		{R.drawable.elf_swipeleft0001,
			R.drawable.elf_swipeleft0002,
			R.drawable.elf_swipeleft0003,
			R.drawable.elf_swipeleft0004,
			R.drawable.elf_swipeleft0005,
			R.drawable.elf_swipeleft0006,
			R.drawable.elf_swipeleft0007,
			R.drawable.elf_swipeleft0008,
			R.drawable.elf_swipeleft0009,
			R.drawable.elf_swipeleft0010,
			R.drawable.elf_swipeleft0011,
			R.drawable.elf_swipeleft0012,
			R.drawable.elf_swipeleft0013,
			R.drawable.elf_swipeleft0014,
			R.drawable.elf_swipeleft0015,
			R.drawable.elf_swipeleft0016,
			R.drawable.elf_swipeleft0017,
			R.drawable.elf_swipeleft0018,
			R.drawable.elf_swipeleft0019,
			R.drawable.elf_swipeleft0020,
			R.drawable.elf_swipeleft0021,
			R.drawable.elf_swipeleft0022,
			R.drawable.elf_swipeleft0023,
			R.drawable.elf_swipeleft0024},//swipe left 
		{R.drawable.elf_swiperight0002,
				R.drawable.elf_swiperight0003,
				R.drawable.elf_swiperight0004,
				R.drawable.elf_swiperight0005,
				R.drawable.elf_swiperight0006,
				R.drawable.elf_swiperight0007,
				R.drawable.elf_swiperight0008,
				R.drawable.elf_swiperight0009,
				R.drawable.elf_swiperight0010,
				R.drawable.elf_swiperight0011,
				R.drawable.elf_swiperight0012,
				R.drawable.elf_swiperight0013,
				R.drawable.elf_swiperight0014,
				R.drawable.elf_swiperight0015,
				R.drawable.elf_swiperight0016,
				R.drawable.elf_swiperight0017,
				R.drawable.elf_swiperight0018,
				R.drawable.elf_swiperight0019,
				R.drawable.elf_swiperight0020,
				R.drawable.elf_swiperight0021,
				R.drawable.elf_swiperight0022,
				R.drawable.elf_swiperight0023,
				R.drawable.elf_swiperight0024},//swipe right
		{R.drawable.elf_pinchout0001,
					R.drawable.elf_pinchout0002,
					R.drawable.elf_pinchout0003,
					R.drawable.elf_pinchout0004,
					R.drawable.elf_pinchout0005,
					R.drawable.elf_pinchout0006,
					R.drawable.elf_pinchout0007,
					R.drawable.elf_pinchout0008,
					R.drawable.elf_pinchout0009,
					R.drawable.elf_pinchout0010,
					R.drawable.elf_pinchout0011,
					R.drawable.elf_pinchout0012,
					R.drawable.elf_pinchout0013,
					R.drawable.elf_pinchout0014,
					R.drawable.elf_pinchout0015,
					R.drawable.elf_pinchout0016,
					R.drawable.elf_pinchout0017,
					R.drawable.elf_pinchout0018,
					R.drawable.elf_pinchout0019,
					R.drawable.elf_pinchout0020,
					R.drawable.elf_pinchout0021,
					R.drawable.elf_pinchout0022,
					R.drawable.elf_pinchout0023,
					R.drawable.elf_pinchout0024},//pinch in
		{R.drawable.elf_pinchin_ball0001, R.drawable.elf_pinchin_ball0002, R.drawable.elf_pinchin_ball0003, R.drawable.elf_pinchin_ball0004,
                R.drawable.elf_pinchin_ball0005, R.drawable.elf_pinchin_ball0006, R.drawable.elf_pinchin_ball0007, R.drawable.elf_pinchin_ball0008,
                R.drawable.elf_pinchin_ball0009, R.drawable.elf_pinchin_ball0010, R.drawable.elf_pinchin_ball0011, R.drawable.elf_pinchin_ball0012,
                R.drawable.elf_pinchin_ball0013, R.drawable.elf_pinchin_ball0014, R.drawable.elf_pinchin_ball0015, R.drawable.elf_pinchin_ball0016,
                R.drawable.elf_pinchin_ball0017, R.drawable.elf_pinchin_ball0018, R.drawable.elf_pinchin_ball0019, R.drawable.elf_pinchin_ball0020,
                R.drawable.elf_pinchin_ball0021, R.drawable.elf_pinchin_ball0022, R.drawable.elf_pinchin_ball0023, R.drawable.elf_pinchin_ball0024,
                R.drawable.elf_pinchin_ball0025, R.drawable.elf_pinchin_ball0026, R.drawable.elf_pinchin_ball0027, R.drawable.elf_pinchin_ball0028,
                R.drawable.elf_pinchin_ball0029, R.drawable.elf_pinchin_ball0030, R.drawable.elf_pinchin_ball0031, R.drawable.elf_pinchin_ball0032}//pinch out
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
                19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31}//pinch out
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
				return R.raw.elf_pinchin_ball;
			case Character.ANIM_PINCH_OUT:
				return R.raw.elf_pinchout;
			case Character.ANIM_SHAKE:
				return R.raw.elf_shake2;
			case Character.ANIM_SWIPE_DOWN:
				return R.raw.elf_swipedown2;
			case Character.ANIM_SWIPE_UP:
				return R.raw.elf_swipeup2;
			case Character.ANIM_SWIPE_LEFT:
				return R.raw.elf_swipeleft;
			case Character.ANIM_SWIPE_RIGHT:
				return R.raw.elf_swiperight;
			case Character.ANIM_TAP:
				return R.raw.elf_tap3;
		}

		return -1;
	}

	@Override
    public String getCharacterName() {
        return "h";
    }
}
