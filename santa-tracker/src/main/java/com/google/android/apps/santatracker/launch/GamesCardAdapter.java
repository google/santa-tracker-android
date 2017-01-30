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

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.doodles.shared.LaunchDecisionMaker;

/**
 * RecyclerView adapter for list of Games in the village.
 */

public class GamesCardAdapter extends CardAdapter {
    public static final int MINIGAME_SWIMMING_CARD = 0;
    public static final int CITY_QUIZ_CARD = 1;
    public static final int MINIGAME_RUNNING_CARD = 2;
    public static final int GUMBALL_CARD = 3;
    public static final int MEMORY_CARD = 4;
    public static final int ROCKET_CARD = 5;
    public static final int DANCER_CARD = 6;
    public static final int SNOWDOWN_CARD = 7;
    public static final int NUM_CARDS = 8;


    public GamesCardAdapter(SantaContext santaContext) {
        super(santaContext, new AbstractLaunch[NUM_CARDS]);
    }

    @Override
    public void initializeLaunchers(SantaContext santaContext) {
        mAllLaunchers[GUMBALL_CARD] = new LaunchGumball(santaContext, this);
        mAllLaunchers[MEMORY_CARD] = new LaunchMemory(santaContext, this);
        mAllLaunchers[ROCKET_CARD] = new LaunchRocket(santaContext, this);
        mAllLaunchers[DANCER_CARD] = new LaunchDancer(santaContext, this);
        mAllLaunchers[SNOWDOWN_CARD] = new LaunchSnowdown(santaContext, this);

        mAllLaunchers[MINIGAME_SWIMMING_CARD] = new LaunchDoodle(santaContext, this,
                LaunchDecisionMaker.SWIMMING_GAME_VALUE,
                R.string.swimming, R.drawable.android_game_cards_penguin_swim);
        mAllLaunchers[MINIGAME_RUNNING_CARD] = new LaunchDoodle(santaContext, this,
                LaunchDecisionMaker.RUNNING_GAME_VALUE,
                R.string.running, R.drawable.android_game_cards_snowballrunner);
        
        mAllLaunchers[CITY_QUIZ_CARD] = new LaunchCityQuiz(santaContext, this);
    }
}
