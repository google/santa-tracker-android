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

package com.google.android.apps.santatracker.games.gumball;


import android.app.Activity;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.common.PlayGamesActivity;


public class Utils {

    private static final String TAG = "SantaTracker";

    /**
     * Checks if the user has at least API level 9
     */
    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    /**
     * Checks if the user has at least API level 11
     */
    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    /**
     * Checks if the user has at least API level 12
     */
    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    /**
     * Checks if the user has at least API level 16
     */
    public static boolean hasIceCreamSandwich() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    /**
     * Checks if the user has at least API level 16
     */
    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    /**
     * Checks if the user has at least API level 19
     */
    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static int getLevelRawFile(int levelNumber) {
        if (levelNumber > 13) {
            levelNumber = (levelNumber % 13) + 1;
        }
        switch (levelNumber) {
            case 1:
                return R.raw.level1;
            case 2:
                return R.raw.level2;
            case 3:
                return R.raw.level3;
            case 4:
                return R.raw.level4;
            case 5:
                return R.raw.level5;
            case 6:
                return R.raw.level6;
            case 7:
                return R.raw.level7;
            case 8:
                return R.raw.level8;
            case 9:
                return R.raw.level9;
            case 10:
                return R.raw.level10;
            case 11:
                return R.raw.level11;
            case 12:
                return R.raw.level12;
            case 13:
                return R.raw.level13;
            default:
                return R.raw.level1;
        }
    }

    public static PlayGamesActivity getPlayGamesActivity(Fragment fragment) {
        Activity act = fragment.getActivity();
        if (act == null || !(act instanceof PlayGamesActivity)) {
            Log.w(TAG, "Fragment is not in a PlayGamesActivity!");
            return null;
        }
        return (PlayGamesActivity) act;
    }

    public static boolean isSignedIn(Fragment fragment) {
        PlayGamesActivity gamesActivity = getPlayGamesActivity(fragment);
        return gamesActivity != null && gamesActivity.isSignedIn();
    }
}
