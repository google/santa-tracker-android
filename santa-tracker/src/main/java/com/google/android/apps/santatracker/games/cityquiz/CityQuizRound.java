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

/**
 * Represents a round in a City Quiz Game.
 */
public class CityQuizRound {

    private City mCity;

    // Location status indicates whether each location has been touched.
    private boolean[] locationStatus;

    public CityQuizRound(City city) {
        this.mCity = city;
        // position 0 = location, 1 = firstFakeLocation, 2 = secondFakeLocation
        locationStatus = new boolean[3];
    }

    public boolean[] getLocationStatus() {
        return locationStatus;
    }

    public void updateLocationStatus(int pos, boolean value) {
        locationStatus[pos] = value;
    }

    public boolean isSolved() {
        return locationStatus[0];
    }

    /**
     * Calculates the score of this round. Only solved rounds are worth points. Correct guess is worth 5 points,
     * incorrect guesses are worth -1 point.
     *
     * @return The points gained in this round. 0 if round is not yet solved.
     */
    public int calculateRoundScore() {
        int score = 0;
        // 5 points for getting the correct city
        if (locationStatus[0]) {
            score = 5;

            if (locationStatus[1]) {
                score--;
            }

            if (locationStatus[2]) {
                score--;
            }
        }
        return score;
    }

    public City getCity() {
        return mCity;
    }
}
