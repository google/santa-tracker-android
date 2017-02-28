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

package com.google.android.apps.santatracker.launch;

import android.content.Intent;

import com.google.android.gms.actions.SearchIntents;

import java.util.Arrays;

/**
 * Support for Google Now (Voice) Actions
 * See http://android-developers.blogspot.com/2014/10/the-fastest-route-between-voice-search.html
 */

public class VoiceAction {

    public static final String ACTION_SHOW_SANTA
            = "com.google.android.apps.santatracker.SHOW_SANTA";
    /**
     * When the system detects ACTION_SHOW_SANTA, the name is passed as this parameter
     */
    public static final String ACTION_SHOW_SANTA_EXTRA = "santa";

    public static final String ACTION_PLAY_GAME
            = "com.google.android.apps.santatracker.PLAY_GAME";
    /**
     * When the system detects ACTION_PLAY_GAME_EXTRA, the game name is passed as this parameter
     */
    public static final String ACTION_PLAY_GAME_EXTRA = "game";

    /**
     * Pick one of the available games
     */
    public static final String ACTION_PLAY_RANDOM_GAME
            = "com.google.android.apps.santatracker.PLAY_RANDOM_GAME";

    private static final String[] SUPPORTED_ACTIONS = {SearchIntents.ACTION_SEARCH,
            ACTION_SHOW_SANTA, ACTION_PLAY_GAME, ACTION_PLAY_RANDOM_GAME};


    public static boolean isVoiceAction(Intent intent) {
        return Arrays.asList(SUPPORTED_ACTIONS).contains(intent.getAction());
    }

    public interface VoiceActionHandler {

        /**
         * Callback method for handling voice actions
         *
         * @param intent Google Now Actions intent.
         * @return true if the action was handled or will be handled
         */
        boolean handleVoiceAction(Intent intent);
    }
}
