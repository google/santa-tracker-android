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

package com.google.android.apps.santatracker.games.matching;

import com.google.android.apps.santatracker.R;

/**
 * Constants for the memory match and gumball games.
 */
public class MatchingGameConstants {

    /**
     * Name of the preferences file for the gumball and memory match games.
     */
    public static final String PREFERENCES_FILENAME = "match_gumball_games";
    /**
     * Key of the preference indicating that the memory match instructions have been viewed.
     */
    public static final String MATCH_INSTRUCTIONS_VIEWED = "MATCH_INSTRUCTIONS_VIEWED";
    /**
     * Key of the preference indicating that the gumball instructions have been viewed.
     */
    public static final String GUMBALL_INSTRUCTIONS_VIEWED = "GUMBALL_INSTRUCTIONS_VIEWED";

    /**
     * ID of the string resource pointing to the Play Games leaderboard game ID for the memory match
     * game.
     */
    public static final int LEADERBOARDS_MATCH = R.string.leaderboard_memory;
    /**
     * ID of the string resource pointing to the Play Games leaderboard game ID for the gumball
     * game.
     */
    public static final int LEADERBOARDS_GUMBALL = R.string.leaderboard_gumball;

    /**
     * Initial time for the gumball game.
     */
    public static final long GUMBALL_INIT_TIME = 60000;
    /**
     * Time to add to the countdown when a gumball is dropped.
     */
    public static final long GUMBALL_ADDED_TIME = 5000;
    /**
     * Initial time for the memory match game.
     */
    public static final long MATCH_INIT_TIME = 60000;
    /**
     * Time to add to the countdown for each successful match in the memory match game.
     */
    public static final long MATCH_ADD_TIME_NEXT_LEVEL = 5000;
}
