/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.apps.santatracker.games.cityquiz;

import android.content.Context;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a City Quiz Game.
 */
public class CityQuizGame {

    private static final int DEFAULT_ROUND_COUNT = 5;

    private List<City> mCities;
    private List<CityQuizRound> mCityQuizRounds;
    private int mCurrentRoundCount;

    public CityQuizGame(Context context, int roundCount) {
        if (roundCount < 1) {
            roundCount = DEFAULT_ROUND_COUNT;
        }
        mCities = CityQuizUtil.getCities(context, roundCount);
        mCityQuizRounds = new ArrayList<>();
        for (City city : mCities) {
            mCityQuizRounds.add(new CityQuizRound(city));
        }
        mCurrentRoundCount = 0;
    }

    public void moveToNextRound() {
        mCurrentRoundCount++;
    }

    /**
     * Get the current round of the game. If the game is over null is returned.
     *
     * @return Current round or null if game is over.
     */
    @Nullable
    public CityQuizRound getCurrentRound() {
        if (mCurrentRoundCount < mCityQuizRounds.size()) {
            return mCityQuizRounds.get(mCurrentRoundCount);
        }
        return null;
    }

    public int getCurrentRoundCount() {
        return mCurrentRoundCount;
    }

    public int getTotalRoundCount() {
        return mCityQuizRounds.size();
    }

    public boolean isFinished() {
        return mCurrentRoundCount >= mCityQuizRounds.size();
    }

    /**
     * Calculates the sum of all solved rounds of the game.
     *
     * @return The current score of the game.
     */
    public int calculateScore() {
        int score = 0;
        for (CityQuizRound round : mCityQuizRounds) {
            if (round.isSolved()) {
                score += round.calculateRoundScore();
            }
        }
        return score;
    }

}
