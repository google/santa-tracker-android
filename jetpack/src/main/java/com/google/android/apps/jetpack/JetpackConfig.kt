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

package com.google.android.apps.jetpack

object JetpackConfig {

    // background music resource id
    const val BGM_RES_ID = com.google.android.apps.santatracker.R.raw.santatracker_musicloop

    // leaderboard
    const val LEADERBOARD = R.string.leaderboard_jetpack_high_scores

    // player settings
    object Player {
        const val WIDTH = 0.15f
        const val INJURED_WIDTH = 0.26f
        const val COLLIDER_WIDTH = 0.15f
        const val COLLIDER_HEIGHT = 0.20f
        const val MAX_SPEED = 20.0f

        // how large is the "no fly zone" near the edges of the screen, where
        // the player can't go
        const val HORIZ_MOVEMENT_MARGIN = COLLIDER_WIDTH / 2
        const val VERT_MOVEMENT_MARGIN = COLLIDER_HEIGHT / 2 + 0.08f

        object SpriteAngle {
            // the sprite angle is proportional to how far the player is from the target position
            const val ANGLE_CONST = 5000.0f
            // per 1.0 units of distance from target
            const val MAX_ANGLE = 22.0f
            const val MAX_CHANGE_RATE = 600.0f
            const val FILTER_SAMPLES = 2
        }

        // jetpack fire animation
        object Fire {
            const val WIDTH = 0.4f * Player.WIDTH
            const val ANIM_PERIOD = 0.5f
            const val ANIM_AMPLITUDE = 0.05f
        }
    }

    // input settings
    object Input {
        const val SENSOR_SENSITIVITY = 0.002f
        const val KEY_SENSIVITY = 0.05f
        const val X_ZERO_BUFFER_ZONE = 0.1f

        object Sensor {
            fun transformX(x: Float): Float {
                var sensorX = x
                sensorX = Math.max(Math.min(6f, sensorX), -6f)
                when {
                    Math.abs(sensorX) < X_ZERO_BUFFER_ZONE -> sensorX = 0f
                    sensorX > 0 -> sensorX -= X_ZERO_BUFFER_ZONE
                    sensorX < 0 -> sensorX += X_ZERO_BUFFER_ZONE
                }
                return sensorX * JetpackConfig.Input.SENSOR_SENSITIVITY
            }
        }
    }

    // item settings
    object Items {
        const val CANDY_WIDTH = 0.05f
        const val CANDY_COLLIDER_HEIGHT = CANDY_WIDTH * 2
        const val CANDY_COLLIDER_WIDTH = 0.1f
        const val PRESENT_WIDTH = 0.12f
        const val PRESENT_COLLIDER_WIDTH = 0.10f
        const val PRESENT_COLLIDER_HEIGHT = PRESENT_COLLIDER_WIDTH * 2
        const val SMALL_WIDTH = 0.05f
        const val SMALL_COLLIDER_WIDTH = 0.05f
        const val SMALL_COLLIDER_HEIGHT = SMALL_COLLIDER_WIDTH * 2

        // item spawn settings
        const val SPAWN_INTERVAL = 0.4f
        const val SPAWN_Y = 0.8f
        const val FALL_SPEED_MIN = 0.2f
        const val FALL_SPEED_MAX = 0.35f
        const val FALL_SPEED_LEVEL_MULT = 1.05f
        const val DELETE_Y = -0.8f

        // what's the initial value for the small items?
        const val BASE_VALUE = 1

        // index of the "base value" variable of an item
        const val IVAR_BASE_VALUE = 0

        // index of the "item type" integer variable of an item
        const val IVAR_TYPE = 1

        // candy rotational speed
        const val CANDY_ROTATE_SPEED = 180.0f

        // maximum interval between two item collections for them to be
        // considered a combo
        const val COMBO_INTERVAL = 0.25f
    }

    object ComboPopup {
        const val SIZE = 0.1f
        const val VEL_Y = GameConfig.ScorePopup.POPUP_VEL_Y * 0.3f
    }

    object Clouds {
        const val COUNT = 6
        const val WIDTH = 0.2f
        const val SPAWN_Y = 0.8f
        const val SPEED_MIN = 0.3f
        const val SPEED_MAX = 0.5f
        const val DELETE_Y = -0.8f
    }

    object Time {
        // how much time the player has at the beginning
        const val INITIAL = 10.0f

        // maximum remaining time player can have
        const val MAX = INITIAL * 3

        // how many seconds are recovered by picking up an item, at the beginning of the game
        const val RECOVERED_BY_ITEM = 2.0f

        // by how many seconds the time reward decreases per level gained
        const val RECOVERED_DECREASE_PER_LEVEL = 0.5f

        // the minimum # of seconds recovered by catching a present
        const val RECOVERED_MIN = 1.0f

        // the # of seconds that hitting coal reduces the time by
        const val COAL_TIME_PENALTY = 5.0f

        // the # of seconds that a player looks injured after getting hit by a bad item
        const val HIT_TIME = 0.5f

        // the # of millis that device vibrates - SMALL
        const val VIBRATE_SMALL: Long = 40
    }

    object Progression {
        // how many items must be collected to go up a level?
        const val ITEMS_PER_LEVEL = 10
        // by how much the score multiplier increases when we go up a level?
        const val SCORE_LEVEL_MULT = 1.5f
    }

    // Achievements
    object Achievements {

        // combo-based achievements
        val COMBO_ACHS = intArrayOf(R.string.achievement_jetpack_2_combo,
                R.string.achievement_jetpack_3_combo, R.string.achievement_jetpack_4_combo)

        // score-based
        val SCORE_ACHS = intArrayOf(R.string.achievement_jetpack_beginner_score_500,
                R.string.achievement_jetpack_intermediate_score_1000,
                R.string.achievement_jetpack_pro_score_5000,
                R.string.achievement_jetpack_advanced_score_10000,
                R.string.achievement_jetpack_expert_score_50000)

        // score necessary for the corresponding SCORE_ACHS achievement
        val SCORE_FOR_ACH = intArrayOf(500, 1000, 5000, 10000, 50000)

        // flight time achievements (one increment = one second)
        val TOTAL_TIME_ACHS = intArrayOf(R.string.achievement_jetpack_flight_time_15,
                R.string.achievement_jetpack_flight_time_30,
                R.string.achievement_jetpack_flight_time_60)

        // total presents achievements
        val TOTAL_PRESENTS_ACHS = intArrayOf(R.string.achievement_jetpack_a_dozen_presents,
                R.string.achievement_jetpack_a_dozen_dozen_presents)

        // total candy achievements
        val TOTAL_CANDY_ACHS = intArrayOf(R.string.achievement_jetpack_candy_for_one_month_30,
                R.string.achievement_jetpack_candy_for_one_year_365)

        // interval between consecutive sending of incremental achievements
        val INC_ACH_SEND_INTERVAL = 15.0f
    }
}
