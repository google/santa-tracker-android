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

import com.google.android.apps.playgames.simpleengine.Renderer

class GameConfig {

    // score popup settings
    object ScorePopup {
        const val DIGIT_SIZE = 0.04f
        const val DIGIT_SPACING = 0.022f
        const val POPUP_VEL_Y = 0.1f
        const val POPUP_EXPIRE = 0.8f
    }

    // score bar settings
    object ScoreBar {
        object PauseButton {
            const val X_REL = Renderer.REL_RIGHT
            const val X_DELTA = -0.23f
            const val Y_REL = Renderer.REL_TOP
            const val Y_DELTA = -0.09f
            const val WIDTH = 0.2f
            const val HEIGHT = 0.2f
            const val SPRITE_WIDTH = 0.15f
            const val SPRITE_HEIGHT = 0.15f
        }

        object ScoreBarLabel {
            const val WIDTH = ScoreBar.WIDTH
            const val X_REL = Renderer.REL_LEFT
            const val X_DELTA = 0.58f * WIDTH
            const val Y_REL = Renderer.REL_TOP
            const val Y_DELTA = -0.13f
            const val FONT_SIZE = 20.0f
        }

        const val WIDTH = 0.42f
        const val X_REL = Renderer.REL_LEFT
        const val X_DELTA = 0.6f * WIDTH
        const val Y_REL = Renderer.REL_TOP
        const val Y_DELTA = -0.08f
        const val MIN_DIGITS_VISIBLE = 2
    }

    object Countdown {
        const val TIME = 3
        const val DIGIT_SIZE = PauseScreen.BigPlayButton.WIDTH
    }

    object EndGame {
        const val DELAY = 2000
    }

    // sound display settings
    object Speaker {
        const val WIDTH = 0.2f
        const val HEIGHT = 0.2f
        const val X_REL = Renderer.REL_RIGHT
        const val X_DELTA = -.07f
        const val Y_REL = Renderer.REL_TOP
        const val Y_DELTA = ScoreBar.PauseButton.Y_DELTA
        const val SPRITE_WIDTH = 0.15f
        const val SPRITE_HEIGHT = 0.15f
    }

    // score display settings
    object ScoreDisplay {

        const val DIGIT_SIZE = 0.09f
        const val DIGIT_SPACING = DIGIT_SIZE * 0.5f
        const val DIGIT_COUNT = 6
        const val POS_X_REL = Renderer.REL_LEFT
        const val POS_X_DELTA = 0.04f
        const val POS_Y_REL = Renderer.REL_TOP
        const val POS_Y_DELTA = -0.062f

        const val POS_Y_REL_TV = Renderer.REL_TOP
        const val POS_Y_DELTA_TV = -0.093f

        const val UPDATE_SPEED = 1000.0f
    }

    // time display settings
    object TimeDisplay {

        const val ICON_SIZE = 0.06f
        const val DIGIT_SIZE = ScoreDisplay.DIGIT_SIZE
        const val DIGIT_SPACING = ScoreDisplay.DIGIT_SPACING
        const val DIGIT_COUNT = 2
        const val POS_X_REL = Renderer.REL_LEFT
        const val POS_X_DELTA = ScoreBar.WIDTH / 2 + ScoreBar.X_DELTA + 0.1f
        const val POS_Y_REL = Renderer.REL_TOP
        const val POS_Y_DELTA = ScoreDisplay.POS_Y_DELTA
        const val POS_Y_REL_TV = Renderer.REL_TOP
        const val POS_Y_DELTA_TV = ScoreDisplay.POS_Y_DELTA_TV
    }

    // podium (level end) screen settings
    object Podium {

        // score label (the static text that says "Score")
        object ScoreLabel {

            const val X_REL = Renderer.REL_CENTER
            const val X_DELTA = 0.15f
            const val Y_REL = Renderer.REL_CENTER
            const val Y_DELTA = 0.2f
            const val FONT_SIZE = 25.0f
        }

        // where do we display the score in the podium screen
        object ScoreDisplay {

            const val X_REL = Renderer.REL_CENTER
            const val X_DELTA = 0.07f
            const val Y_REL = Renderer.REL_CENTER
            const val Y_DELTA = 0.1f
        }

        // "play again" button
        object ReplayButton {

            const val FONT_SIZE = 25.0f
            const val X_REL = Renderer.REL_CENTER
            const val X_DELTA = 0.0f
            const val Y_REL = Renderer.REL_CENTER
            const val Y_DELTA = -0.13f
            const val WIDTH = 0.6f
            const val HEIGHT = 0.12f
            const val NORMAL_COLOR = -0xd961bd
            const val HIGHLIGHT_COLOR = -0xd24fb5
        }

        const val WIDTH = 0.8f
        const val X_REL = Renderer.REL_CENTER
        const val X_DELTA = 0.0f
        const val Y_REL = Renderer.REL_CENTER
        const val Y_DELTA = 0.1f
    }

    // Sign in bar
    object SignInBar {

        const val COLOR = -0x7f000001
        const val X_REL = Renderer.REL_CENTER
        const val X_DELTA = 0.0f
        const val Y_REL = Renderer.REL_BOTTOM
        const val HEIGHT = 0.2f
        const val WIDTH = 10.0f
        const val Y_DELTA = 0.5f * HEIGHT
    }

    // Sign in button
    object SignInButton {

        const val WIDTH = 0.4f
        // (120/402 is the height/width of the image asset)
        const val HEIGHT = WIDTH * (120.0f / 402.0f)
        const val X_REL = Renderer.REL_LEFT
        const val X_DELTA = WIDTH * 0.5f + 0.05f
        const val Y_REL = Renderer.REL_BOTTOM
        const val Y_DELTA = 0.1f

        const val TEXT_DELTA_X = 0.05f

        const val FONT_SIZE = 20.0f
    }

    // Sign in encouragement text
    object SignInText {

        const val COLOR = -0xda50cf
        const val X_REL = Renderer.REL_LEFT
        const val X_DELTA =
                SignInButton.X_DELTA + SignInButton.WIDTH * 0.5f + 0.05f
        const val Y_REL = Renderer.REL_BOTTOM
        const val Y_DELTA = 0.1f
        const val ANCHOR = Renderer.TEXT_ANCHOR_MIDDLE or Renderer.TEXT_ANCHOR_LEFT
        const val FONT_SIZE = 20.0f
    }

    // mute screen settings
    object PauseScreen {

        object BigPlayButton {

            const val X_REL = Renderer.REL_CENTER
            const val X_DELTA = 0.02f
            const val Y_REL = Renderer.REL_CENTER
            const val Y_DELTA = 0.0f
            const val WIDTH = 0.4f
            const val HEIGHT = 0.4f
            const val SPRITE_WIDTH = 0.4f
        }

        object QuitBar {

            object QuitBarLabel {
                const val X_REL = Renderer.REL_CENTER
                const val X_DELTA = 0.0f
                const val Y_REL = Renderer.REL_CENTER
                const val Y_DELTA = -0.35f
                const val WIDTH = .7f
                const val FONT_SIZE = 35.0f
            }

            const val X_REL = Renderer.REL_CENTER
            const val X_DELTA = 0.0f
            const val Y_REL = Renderer.REL_CENTER
            const val Y_DELTA = -0.35f
            const val WIDTH = .7f
            const val HEIGHT = WIDTH * 0.33f
            const val SPRITE_WIDTH = WIDTH
        }

        const val CURTAIN_COLOR = -0x7f000001
    }

    companion object {

        // type code for decorative objects (HUD, etc)
        const val TYPE_DECOR = 9999
    }
}
