/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.cityquiz

/** Represents a round in a City Quiz Game.  */
class CityQuizRound(val city: City) {

    // Location status indicates whether each location has been touched.
    // position 0 = location, 1 = incorrectLocationOne, 2 = incorrectLocationTwo
    private val locationStatus: BooleanArray = BooleanArray(3)

    val isSolved: Boolean
        get() = locationStatus[0]

    fun updateLocationStatus(pos: Int, value: Boolean) {
        locationStatus[pos] = value
    }

    /**
     * Calculates the score of this round. Only solved rounds are worth points. Correct guess is
     * worth 5 points, incorrect guesses are worth -1 point.
     *
     * @return The points gained in this round. 0 if round is not yet solved.
     */
    fun calculateRoundScore(): Int {
        var score = 0
        // 5 points for getting the correct city
        if (locationStatus[0]) {
            score = 5
            if (locationStatus[1]) {
                score--
            }
            if (locationStatus[2]) {
                score--
            }
        }
        return score
    }
}
