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
package com.google.android.apps.santatracker.data

import com.google.android.apps.santatracker.config.Config

/** Disabled state of each game.  */
data class GameState(
    var disableGumballGame: Boolean = false,
    var disableJetpackGame: Boolean = false,
    var disableMemoryGame: Boolean = false,
    var disableRocketGame: Boolean = false,
    var disableDancerGame: Boolean = false,
    var disableSwimmingGame: Boolean = false,
    var disableBmxGame: Boolean = false,
    var disableRunningGame: Boolean = false,
    var disableTennisGame: Boolean = false,
    var disableWaterpoloGame: Boolean = false,
    var disableCityQuizGame: Boolean = false,
    var disablePresentQuest: Boolean = false,
    var disableSantaSnap: Boolean = false,
    var disablePresentThrow: Boolean = false,
    var featureGumballGame: Boolean = false,
    var featureJetpackGame: Boolean = false,
    var featureMemoryGame: Boolean = false,
    var featureRocketGame: Boolean = false,
    var featureDancerGame: Boolean = false,
    var featureSwimmingGame: Boolean = false,
    var featureBmxGame: Boolean = false,
    var featureRunningGame: Boolean = false,
    var featureTennisGame: Boolean = false,
    var featureWaterpoloGame: Boolean = false,
    var featureCityQuizGame: Boolean = false,
    var featurePresentQuest: Boolean = false,
    var featureSantaSnap: Boolean = false,
    var featurePresentThrow: Boolean = false
)

fun gameStateFromConfig(config: Config) = GameState(
        disableGumballGame = Config.DISABLE_GUMBALLGAME.getValue(config),
        disableJetpackGame = Config.DISABLE_JETPACKGAME.getValue(config),
        disableMemoryGame = Config.DISABLE_MEMORYGAME.getValue(config),
        disableRocketGame = Config.DISABLE_ROCKETGAME.getValue(config),
        disableDancerGame = Config.DISABLE_DANCERGAME.getValue(config),
        disableSwimmingGame = Config.DISABLE_SWIMMINGGAME.getValue(config),
        disableBmxGame = Config.DISABLE_BMXGAME.getValue(config),
        disableRunningGame = Config.DISABLE_RUNNINGGAME.getValue(config),
        disableTennisGame = Config.DISABLE_TENNISGAME.getValue(config),
        disableWaterpoloGame = Config.DISABLE_WATERPOLOGAME.getValue(config),
        disableCityQuizGame = Config.DISABLE_CITY_QUIZ.getValue(config),
        disablePresentQuest = Config.DISABLE_PRESENTQUEST.getValue(config),
        disableSantaSnap = Config.DISABLE_SANTA_SNAP.getValue(config),
        disablePresentThrow = Config.DISABLE_PRESENT_THROW.getValue(config),
        featureGumballGame = Config.FEATURE_GUMBALLGAME.getValue(config),
        featureJetpackGame = Config.FEATURE_JETPACKGAME.getValue(config),
        featureMemoryGame = Config.FEATURE_MEMORYGAME.getValue(config),
        featureRocketGame = Config.FEATURE_ROCKETGAME.getValue(config),
        featureDancerGame = Config.FEATURE_DANCERGAME.getValue(config),
        featureSwimmingGame = Config.FEATURE_SWIMMINGGAME.getValue(config),
        featureBmxGame = Config.FEATURE_BMXGAME.getValue(config),
        featureRunningGame = Config.FEATURE_RUNNINGGAME.getValue(config),
        featureTennisGame = Config.FEATURE_TENNISGAME.getValue(config),
        featureWaterpoloGame = Config.FEATURE_WATERPOLOGAME.getValue(config),
        featureCityQuizGame = Config.FEATURE_CITY_QUIZ.getValue(config),
        featurePresentQuest = Config.FEATURE_PRESENTQUEST.getValue(config),
        featureSantaSnap = Config.FEATURE_SANTA_SNAP.getValue(config),
        featurePresentThrow = Config.FEATURE_PRESENT_THROW.getValue(config)
)

fun Config.gameState() = gameStateFromConfig(this)