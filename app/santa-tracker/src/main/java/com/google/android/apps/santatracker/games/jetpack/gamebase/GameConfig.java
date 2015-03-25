/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

package com.google.android.apps.santatracker.games.jetpack.gamebase;

import com.google.android.apps.santatracker.games.simpleengine.Renderer;

public class GameConfig {

    // type code for decorative objects (HUD, etc)
    public static final int TYPE_DECOR = 9999;

    // score popup settings
    public class ScorePopup {

        public static final float DIGIT_SIZE = 0.04f;
        public static final float DIGIT_SPACING = 0.022f;
        public static final float POPUP_VEL_Y = 0.1f;
        public static final float POPUP_EXPIRE = 0.8f;
    }

    // score bar settings
    public class ScoreBar {

        public static final float WIDTH = 0.7f;
        public static final int X_REL = Renderer.REL_RIGHT;
        public static final float X_DELTA = -WIDTH / 2;
        public static final int Y_REL = Renderer.REL_BOTTOM;
        public static final float Y_DELTA = 0.06f;

        public class PauseButton {

            public static final int X_REL = Renderer.REL_RIGHT;
            public static final float X_DELTA = -0.1f;
            public static final int Y_REL = Renderer.REL_BOTTOM;
            public static final float Y_DELTA = 0.06f;
            public static final float WIDTH = 0.2f;
            public static final float HEIGHT = 0.2f;
            public static final float SPRITE_WIDTH = 0.1f;
            public static final float SPRITE_HEIGHT = 0.1f;
        }
    }

    // score display settings
    public class ScoreDisplay {

        public static final float DIGIT_SIZE = 0.06f;
        public static final float DIGIT_SPACING = DIGIT_SIZE * 0.5f;
        public static final int DIGIT_COUNT = 6;
        public static final int POS_X_REL = Renderer.REL_RIGHT;
        public static final float POS_X_DELTA = -0.62f;
        public static final int POS_Y_REL = Renderer.REL_BOTTOM;
        public static final float POS_Y_DELTA = 0.062f;
        public static final float UPDATE_SPEED = 1000.0f;
    }

    // time display settings
    public class TimeDisplay {

        public static final float ICON_SIZE = 0.06f;
        public static final float DIGIT_SIZE = ScoreDisplay.DIGIT_SIZE;
        public static final float DIGIT_SPACING = ScoreDisplay.DIGIT_SPACING;
        public static final int DIGIT_COUNT = 2;
        public static final int POS_X_REL = Renderer.REL_RIGHT;
        public static final float POS_X_DELTA = -0.33f;
        public static final int POS_Y_REL = Renderer.REL_BOTTOM;
        public static final float POS_Y_DELTA = ScoreDisplay.POS_Y_DELTA;
    }

    // podium (level end) screen settings
    public class Podium {

        public static final float WIDTH = 0.8f;
        public static final int X_REL = Renderer.REL_CENTER;
        public static final float X_DELTA = 0.0f;
        public static final int Y_REL = Renderer.REL_CENTER;
        public static final float Y_DELTA = 0.1f;

        // score label (the static text that says "Score")
        public class ScoreLabel {

            public static final int X_REL = Renderer.REL_CENTER;
            public static final float X_DELTA = 0.15f;
            public static final int Y_REL = Renderer.REL_CENTER;
            public static final float Y_DELTA = 0.2f;
            public static final float FONT_SIZE = 25.0f;
        }

        // where do we display the score in the podium screen
        public class ScoreDisplay {

            public static final int X_REL = Renderer.REL_CENTER;
            public static final float X_DELTA = 0.07f;
            public static final int Y_REL = Renderer.REL_CENTER;
            public static final float Y_DELTA = 0.1f;
        }

        // "play again" button
        public class ReplayButton {

            public static final float FONT_SIZE = 25.0f;
            public static final int X_REL = Renderer.REL_CENTER;
            public static final float X_DELTA = 0.0f;
            public static final int Y_REL = Renderer.REL_CENTER;
            public static final float Y_DELTA = -0.13f;
            public static final float WIDTH = 0.6f;
            public static final float HEIGHT = 0.12f;
            public static final int NORMAL_COLOR = 0xff269e43;
            public static final int HIGHLIGHT_COLOR = 0xff2db04b;
        }
    }

    // Sign in bar
    public class SignInBar {

        public static final int COLOR = 0x80ffffff;
        public static final int X_REL = Renderer.REL_CENTER;
        public static final float X_DELTA = 0.0f;
        public static final int Y_REL = Renderer.REL_BOTTOM;
        public static final float HEIGHT = 0.2f;
        public static final float WIDTH = 10.0f;
        public static final float Y_DELTA = 0.5f * HEIGHT;
    }

    // Sign in button
    public class SignInButton {

        public static final float WIDTH = 0.4f;
        // (120/402 is the height/width of the image asset)
        public static final float HEIGHT = WIDTH * (120.0f / 402.0f);
        public static final int X_REL = Renderer.REL_LEFT;
        public static final float X_DELTA = WIDTH * 0.5f + 0.05f;
        public static final int Y_REL = Renderer.REL_BOTTOM;
        public static final float Y_DELTA = 0.1f;

        public static final float TEXT_DELTA_X = 0.05f;

        public static final float FONT_SIZE = 20.0f;
    }

    // Sign in encouragement text
    public class SignInText {

        public static final int COLOR = 0xffdf4a32;
        public static final int X_REL = Renderer.REL_LEFT;
        public static final float X_DELTA = SignInButton.X_DELTA + SignInButton.WIDTH * 0.5f
                + 0.05f;
        public static final int Y_REL = Renderer.REL_BOTTOM;
        public static final float Y_DELTA = 0.1f;
        public static final int ANCHOR = Renderer.TEXT_ANCHOR_MIDDLE | Renderer.TEXT_ANCHOR_LEFT;
        public static final float FONT_SIZE = 20.0f;
    }

    // pause screen settings
    public class PauseScreen {

        public static final int CURTAIN_COLOR = 0x80ffffff;

        public class BigPlayButton {

            public static final int X_REL = Renderer.REL_CENTER;
            public static final float X_DELTA = 0.0f;
            public static final int Y_REL = Renderer.REL_CENTER;
            public static final float Y_DELTA = 0.0f;
            public static final float WIDTH = 0.4f;
            public static final float HEIGHT = 0.4f;
            public static final float SPRITE_WIDTH = 0.4f;
        }

        public class QuitBar {

            public static final int X_REL = Renderer.REL_LEFT;
            public static final float X_DELTA = 0.05f;
            public static final int Y_REL = Renderer.REL_BOTTOM;
            public static final float Y_DELTA = 0.058f;
            public static final float WIDTH = 0.25f;
            public static final float HEIGHT = WIDTH * 0.5f;
            public static final float SPRITE_WIDTH = WIDTH;
        }
    }
}
