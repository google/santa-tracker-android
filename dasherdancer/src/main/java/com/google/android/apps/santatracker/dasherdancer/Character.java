/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

/**
 * Interface for characters.  To create a character, implement this interface.  The animationKey passed 
 * to this interface's methods is called with one of the ANIM_* static values defined in this interface. 
 * Generally implementing classes should use an array to store their durations, frame indices arrays, 
 * and frame arrays. 
 */
public interface Character {

	public static int ANIM_IDLE = 0;
	public static int ANIM_TAP = 1;
	public static int ANIM_SHAKE = 2;
	public static int ANIM_SWIPE_DOWN = 3;
	public static int ANIM_SWIPE_UP = 4;
	public static int ANIM_SWIPE_LEFT = 5;
	public static int ANIM_SWIPE_RIGHT = 6;
	public static int ANIM_PINCH_IN = 7;
	public static int ANIM_PINCH_OUT = 8;

	long getDuration(int animationKey);
	
	int[] getFrameIndices(int animationKey);
	
	int[] getFrames(int animationKey);

        // The initial release used getClass().getSimpleName(), which was ProGuarded out.
        // These strings are the pro-guarded names from the released version. In future releases
        // these should be changed to the names of the characters.
        String getCharacterName();
}
