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

package com.google.android.apps.santatracker.games.jetpack;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.gamebase.GameConfig;

public class JetpackConfig {

    // game constants
    public static class Keys {
        public static final String JETPACK_PREFERENCES = "JETPACK_PREFERENCES";
        public static final String JETPACK_MUTE_KEY = "JETPACK_MUTE_KEY";
    }

    // player settings
    public static class Player {

        public static final float WIDTH = 0.15f;
        public static final float INJURED_WIDTH = 0.26f;
        public static final float COLLIDER_WIDTH = 0.15f;
        public static final float COLLIDER_HEIGHT = 0.20f;
        public static final float MAX_SPEED = 20.0f;

        // how large is the "no fly zone" near the edges of the screen, where
        // the player can't go
        public static final float HORIZ_MOVEMENT_MARGIN = COLLIDER_WIDTH / 2;
        public static final float VERT_MOVEMENT_MARGIN = COLLIDER_HEIGHT / 2 + 0.08f;

        public static class SpriteAngle {

            // the sprite angle is proportional to how far the player is from the target position
            public static final float ANGLE_CONST = 5000.0f;
                    // per 1.0 units of distance from target
            public static final float MAX_ANGLE = 22.0f;
            public static final float MAX_CHANGE_RATE = 600.0f;
            public static final int FILTER_SAMPLES = 2;
        }

        // jetpack fire animation
        public static class Fire {

            public static final float WIDTH = 0.4f * Player.WIDTH;
            public static final float ANIM_PERIOD = 0.5f;
            public static final float ANIM_AMPLITUDE = 0.05f;
        }
    }

    // input settings
    public static class Input {

        public static final float SENSOR_SENSITIVITY = 0.002f;
        public static final float TOUCH_SENSIVITY = 1.2f;
        public static final float KEY_SENSIVITY = 0.15f;
        public static final float X_ZERO_BUFFER_ZONE = 0.1f;
        public static class Sensor {
            public static float transformX(float x) {
                x = Math.max(Math.min(6, x), -6);
                if(Math.abs(x) < X_ZERO_BUFFER_ZONE) {
                    x = 0;
                } else if(x > 0) {
                    x -= X_ZERO_BUFFER_ZONE;
                } else if(x < 0) {
                    x += X_ZERO_BUFFER_ZONE;
                }
                return x * JetpackConfig.Input.SENSOR_SENSITIVITY;
            }

            public static float transformY(float y) {
                y = Math.max(Math.min(1.5f, y), -10.5f) + 4.5f ;
                return y * JetpackConfig.Input.SENSOR_SENSITIVITY;
            }
        }
    }

    // item settings
    public static class Items {

        public static final float CANDY_WIDTH = 0.05f;
        public static final float CANDY_COLLIDER_HEIGHT = CANDY_WIDTH * 2;
        public static final float CANDY_COLLIDER_WIDTH = 0.1f;
        public static final float PRESENT_WIDTH = 0.12f;
        public static final float PRESENT_HEIGHT = PRESENT_WIDTH * 2;
        public static final float PRESENT_COLLIDER_WIDTH = 0.10f;
        public static final float PRESENT_COLLIDER_HEIGHT = PRESENT_COLLIDER_WIDTH * 2;
        public static final float SMALL_WIDTH = 0.05f;
        public static final float SMALL_HEIGHT = SMALL_WIDTH * 2;
        public static final float SMALL_COLLIDER_WIDTH = 0.05f;
        public static final float SMALL_COLLIDER_HEIGHT = SMALL_COLLIDER_WIDTH * 2;

        // item spawn settings
        public static final float SPAWN_INTERVAL = 0.4f;
        public static final float SPAWN_Y = 0.8f;
        public static final float FALL_SPEED_MIN = 0.2f;
        public static final float FALL_SPEED_MAX = 0.35f;
        public static final float FALL_SPEED_LEVEL_MULT = 1.05f;
        public static final float DELETE_Y = -0.8f;

        // what's the initial value for the small items?
        public static final int BASE_VALUE = 1;

        // index of the "base value" variable of an item
        public static final int IVAR_BASE_VALUE = 0;

        // index of the "item type" integer variable of an item
        public static final int IVAR_TYPE = 1;

        // candy rotational speed
        public static final float CANDY_ROTATE_SPEED = 180.0f;

        // maximum interval between two item collections for them to be
        // considered a combo
        public static final float COMBO_INTERVAL = 0.25f;
    }

    public static class ComboPopup {

        public static final float SIZE = 0.1f;
        public static final float VEL_Y = GameConfig.ScorePopup.POPUP_VEL_Y * 0.3f;
    }

    public static final int SKY_COLOR = 0xff91d2f2;

    public static class Clouds {

        public static final int COUNT = 6;
        public static final float WIDTH = 0.2f;
        public static final float SPAWN_Y = 0.8f;
        public static final float SPEED_MIN = 0.3f;
        public static final float SPEED_MAX = 0.5f;
        public static final float DELETE_Y = -0.8f;
    }

    public static class Time {

        // how much time the player has at the beginning
        public static final float INITIAL = 30.0f;

        // maximum remaining time player can have
        public static final float MAX = INITIAL * 2;

        // how many seconds are recovered by picking up an item, at the beginning of the game
        public static final float RECOVERED_BY_ITEM = 2.0f;

        // by how many seconds the time reward decreases per level gained
        public static final float RECOVERED_DECREASE_PER_LEVEL = 0.5f;

        // the minimum # of seconds recovered by catching a present
        public static final float RECOVERED_MIN = 1.0f;

        // the # of seconds that hitting coal reduces the time by
        public static final float COAL_TIME_PENALTY = 5.0f;

        // the # of seconds that a player looks injured after getting hit by a bad item
        public static final float HIT_TIME = 0.5f;

        // the # of millis that device vibrates - SMALL
        public static final long VIBRATE_SMALL = 40;
    }

    public static class Progression {

        // how many items must be collected to go up a level?
        public static final int ITEMS_PER_LEVEL = 10;
        // by how much the score multiplier increases when we go up a level?
        public static final float SCORE_LEVEL_MULT = 1.5f;
    }

    // background music asset file
    public static final String BGM_ASSET_FILE = "jetpack_music.mp3";

    // Achievements
    public static class Achievements {

        // combo-based achievements
        public static final int[] COMBO_ACHS = {
                R.string.achievement_jetpack_2_combo,
                R.string.achievement_jetpack_3_combo,
                R.string.achievement_jetpack_4_combo
        };

        // score-based
        public static final int[] SCORE_ACHS = {
                R.string.achievement_jetpack_beginner_score_500,
                R.string.achievement_jetpack_intermediate_score_1000,
                R.string.achievement_jetpack_pro_score_5000,
                R.string.achievement_jetpack_advanced_score_10000,
                R.string.achievement_jetpack_expert_score_50000
        };

        // score necessary for the corresponding SCORE_ACHS achievement
        public static final int[] SCORE_FOR_ACH = {500, 1000, 5000, 10000, 50000};

        // flight time achievements (one increment = one second)
        public static final int[] TOTAL_TIME_ACHS = {
                R.string.achievement_jetpack_flight_time_15,
                R.string.achievement_jetpack_flight_time_30,
                R.string.achievement_jetpack_flight_time_60
        };

        // total presents achievements
        public static final int[] TOTAL_PRESENTS_ACHS = {
                R.string.achievement_jetpack_a_dozen_presents,
                R.string.achievement_jetpack_a_dozen_dozen_presents
        };

        // total candy achievements
        public static final int[] TOTAL_CANDY_ACHS = {
                R.string.achievement_jetpack_candy_for_one_month_30,
                R.string.achievement_jetpack_candy_for_one_year_365
        };

        // interval between consecutive sending of incremental achievements
        public static final float INC_ACH_SEND_INTERVAL = 15.0f;
    }

    // leaderboard
    public static final int LEADERBOARD = R.string.leaderboard_jetpack_high_scores;
}
