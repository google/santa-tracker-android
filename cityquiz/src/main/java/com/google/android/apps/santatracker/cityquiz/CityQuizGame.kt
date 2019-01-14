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

import android.content.Context

/** Represents a City Quiz Game.  */
class CityQuizGame(context: Context, roundCount: Int = DEFAULT_ROUND_COUNT) {
    private val cityQuizRounds = CityQuizUtil.getCities(context, roundCount).map(::CityQuizRound)

    var currentRoundCount: Int = 0
        private set

    /**
     * Get the current round of the game. If the game is over null is returned.
     *
     * @return Current round or null if game is over.
     */
    val currentRound: CityQuizRound?
        get() = cityQuizRounds.getOrNull(currentRoundCount)

    val totalRoundCount: Int
        get() = cityQuizRounds.size

    val isFinished: Boolean
        get() = currentRoundCount >= cityQuizRounds.size

    fun moveToNextRound() {
        currentRoundCount++
    }

    /**
     * Calculates the sum of all solved rounds of the game.
     *
     * @return The current score of the game.
     */
    fun calculateScore() = cityQuizRounds.sumBy(CityQuizRound::calculateRoundScore)

    companion object {
        private const val DEFAULT_ROUND_COUNT = 5
    }
}
