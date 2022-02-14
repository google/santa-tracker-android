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
import com.google.android.apps.playgames.simpleengine.game.GameObject
import com.google.android.apps.playgames.simpleengine.game.World
import com.google.android.apps.playgames.simpleengine.ui.Button
import com.google.android.apps.playgames.simpleengine.ui.Widget
import java.util.Arrays

class GameObjectFactory(private var renderer: Renderer, private var world: World) {

    // Textures
    private var texClock: Int = 0
    private var texPodium: Int = 0
    private var playAgainTex: Int = 0
    private var scoreLabelTex: Int = 0
    private var signInLabelTex: Int = 0
    private var signInNormalTex: Int = 0
    private var signInHighlightTex: Int = 0
    private var signInTextTex: Int = 0
    private var scoreBarTex: Int = 0
    private var scoreBarLabelTex: Int = 0
    private var pauseIconTex: Int = 0
    private var pauseIconPressedTex: Int = 0
    private var speakerMuteIconTex: Int = 0
    private var speakerOnIconTex: Int = 0
    private var bigPlayButtonNormalTex: Int = 0
    private var bigPlayButtonHighlightTex: Int = 0
    private var quitBarTex: Int = 0
    private var quitBarPressedTex: Int = 0
    private var quitBarLabelTex: Int = 0
    private var inviteBarTex: Int = 0
    private var inviteBarPressedTex: Int = 0

    private var tmpDigits = arrayOfNulls<GameObject>(5)

    fun makeScorePopup(x: Float, y: Float, score: Int, df: DigitObjectFactory) {
        val digits: Int = if (score >= 0) {
            when {
                score >= 10000 -> 5
                score >= 1000 -> 4
                score >= 100 -> 3
                score >= 10 -> 2
                else -> 1
            }
        } else {
            when {
                score <= -10000 -> 6
                score <= -1000 -> 5
                score <= -100 -> 4
                score <= -10 -> 3
                else -> 2
            }
        }

        Arrays.fill(tmpDigits, null)
        df.makeDigitObjects(
                digits,
                GameConfig.TYPE_DECOR,
                x,
                y,
                GameConfig.ScorePopup.DIGIT_SIZE,
                GameConfig.ScorePopup.DIGIT_SPACING,
                tmpDigits)
        df.setDigits(score, tmpDigits, 0, digits)

        var i = 0
        while (i < digits) {
            val o = tmpDigits[i]
            o?.velY = GameConfig.ScorePopup.POPUP_VEL_Y
            o?.timeToLive = GameConfig.ScorePopup.POPUP_EXPIRE
            i++
        }
    }

    fun requestTextures() {
        texClock = renderer.requestImageTex(
                R.drawable.jetpack_clock,
                "jetpack_clock",
                Renderer.DIM_WIDTH,
                GameConfig.TimeDisplay.ICON_SIZE)
        texPodium = renderer.requestImageTex(
                R.drawable.jetpack_podium,
                "jetpack_podium",
                Renderer.DIM_WIDTH,
                GameConfig.Podium.WIDTH)

        playAgainTex = renderer.requestTextTex(
                com.google.android.apps.santatracker.common.R.string.return_to_map,
                "return_to_map",
                GameConfig.Podium.ReplayButton.FONT_SIZE)
        scoreLabelTex = renderer.requestTextTex(
                com.google.android.apps.santatracker.common.R.string.score,
                "score",
                GameConfig.Podium.ScoreLabel.FONT_SIZE)

        signInLabelTex = renderer.requestTextTex(
                com.google
                        .android
                        .apps
                        .santatracker
                        .common
                        .R
                        .string
                        .common_signin_button_text,
                "jetpack_sign_in",
                GameConfig.SignInButton.FONT_SIZE)
        signInNormalTex = renderer.requestImageTex(
                R.drawable.jetpack_signin,
                "jetpack_siginin",
                Renderer.DIM_WIDTH,
                GameConfig.SignInButton.WIDTH)
        signInHighlightTex = renderer.requestImageTex(
                R.drawable.jetpack_signin_pressed,
                "jetpack_signin_pressed",
                Renderer.DIM_WIDTH,
                GameConfig.SignInButton.WIDTH)
        signInTextTex = renderer.requestTextTex(
                com.google.android.apps.santatracker.common.R.string.why_sign_in,
                "jetpack_why_sign_in",
                GameConfig.SignInText.FONT_SIZE,
                GameConfig.SignInText.ANCHOR,
                GameConfig.SignInText.COLOR)
        scoreBarTex = renderer.requestImageTex(
                com.google
                        .android
                        .apps
                        .santatracker
                        .common
                        .R
                        .drawable
                        .icon_ribbon_upsidedown_short,
                "games_scorebar",
                Renderer.DIM_WIDTH,
                GameConfig.ScoreBar.WIDTH)
        scoreBarLabelTex = renderer.requestTextTex(
                com.google.android.apps.santatracker.common.R.string.score,
                "score_bar_label",
                GameConfig.ScoreBar.ScoreBarLabel.FONT_SIZE)
        pauseIconTex = renderer.requestImageTex(
                com.google.android.apps.santatracker.common.R.drawable.common_btn_pause,
                "games_pause",
                Renderer.DIM_WIDTH,
                GameConfig.ScoreBar.PauseButton.SPRITE_WIDTH)
        pauseIconPressedTex = renderer.requestImageTex(
                com.google.android.apps.santatracker.common.R.drawable.common_btn_pause,
                "games_pause_pressed",
                Renderer.DIM_WIDTH,
                GameConfig.ScoreBar.PauseButton.SPRITE_WIDTH)
        speakerMuteIconTex = renderer.requestImageTex(
                com.google
                        .android
                        .apps
                        .santatracker
                        .common
                        .R
                        .drawable
                        .common_btn_speaker_off,
                "speaker_mute",
                Renderer.DIM_WIDTH,
                GameConfig.Speaker.SPRITE_WIDTH)
        speakerOnIconTex = renderer.requestImageTex(
                com.google
                        .android
                        .apps
                        .santatracker
                        .common
                        .R
                        .drawable
                        .common_btn_speaker_on,
                "speaker_on",
                Renderer.DIM_WIDTH,
                GameConfig.Speaker.SPRITE_WIDTH)
        bigPlayButtonNormalTex = renderer.requestImageTex(
                com.google.android.apps.santatracker.common.R.drawable.btn_play_yellow,
                "btn_play_yellow",
                Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.BigPlayButton.SPRITE_WIDTH)
        bigPlayButtonHighlightTex = renderer.requestImageTex(
                com.google.android.apps.santatracker.common.R.drawable.btn_play_yellow,
                "btn_play_pressed",
                Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.BigPlayButton.SPRITE_WIDTH)
        quitBarTex = renderer.requestImageTex(
                com.google
                        .android
                        .apps
                        .santatracker
                        .common
                        .R
                        .drawable
                        .purple_rectangle_button,
                "quit_button",
                Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH)
        quitBarLabelTex = renderer.requestTextTex(
                com.google.android.apps.santatracker.common.R.string.back_to_village,
                "quit_bar_label",
                GameConfig.PauseScreen.QuitBar.QuitBarLabel.FONT_SIZE)
        quitBarPressedTex = renderer.requestImageTex(
                com.google
                        .android
                        .apps
                        .santatracker
                        .common
                        .R
                        .drawable
                        .purple_rectangle_button,
                "quit_button_pressed",
                Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH)
        inviteBarTex = renderer.requestImageTex(
                com.google.android.apps.santatracker.common.R.drawable.games_share,
                "games_share",
                Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH)
        inviteBarPressedTex = renderer.requestImageTex(
                com.google.android.apps.santatracker.common.R.drawable.games_share_pressed,
                "games_share_pressed",
                Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH)
    }

    fun makePodium(): GameObject {
        val x = renderer.getRelativePos(GameConfig.Podium.X_REL, GameConfig.Podium.X_DELTA)
        val y = renderer.getRelativePos(GameConfig.Podium.Y_REL, GameConfig.Podium.Y_DELTA)
        return world.newGameObjectWithImage(
                GameConfig.TYPE_DECOR, x, y, texPodium, GameConfig.Podium.WIDTH,
                java.lang.Float.NaN)
    }

    fun makeScoreBar(): GameObject {
        val x = renderer.getRelativePos(GameConfig.ScoreBar.X_REL, GameConfig.ScoreBar.X_DELTA)
        val y = renderer.getRelativePos(GameConfig.ScoreBar.Y_REL, GameConfig.ScoreBar.Y_DELTA)
        return world.newGameObjectWithImage(
                GameConfig.TYPE_DECOR, x, y, scoreBarTex, GameConfig.ScoreBar.WIDTH,
                java.lang.Float.NaN)
    }

    fun makeScoreBarLabel(): GameObject {
        val x = renderer.getRelativePos(
                GameConfig.ScoreBar.ScoreBarLabel.X_REL,
                GameConfig.ScoreBar.ScoreBarLabel.X_DELTA)
        val y = renderer.getRelativePos(
                GameConfig.ScoreBar.ScoreBarLabel.Y_REL,
                GameConfig.ScoreBar.ScoreBarLabel.Y_DELTA)
        return world.newGameObjectWithImage(
                GameConfig.TYPE_DECOR,
                x,
                y,
                scoreBarLabelTex,
                GameConfig.ScoreBar.ScoreBarLabel.WIDTH,
                java.lang.Float.NaN)
    }

    fun makeQuitBarLabel(): GameObject {
        val x = renderer.getRelativePos(
                GameConfig.PauseScreen.QuitBar.QuitBarLabel.X_REL,
                GameConfig.PauseScreen.QuitBar.QuitBarLabel.X_DELTA)
        val y = renderer.getRelativePos(
                GameConfig.PauseScreen.QuitBar.QuitBarLabel.Y_REL,
                GameConfig.PauseScreen.QuitBar.QuitBarLabel.Y_DELTA)
        return world.newGameObjectWithImage(
                GameConfig.TYPE_DECOR,
                x,
                y,
                quitBarLabelTex,
                GameConfig.PauseScreen.QuitBar.QuitBarLabel.WIDTH,
                java.lang.Float.NaN)
    }

    fun makeScoreLabel(): GameObject {
        // create the "score" static label
        val x = renderer.getRelativePos(
                GameConfig.Podium.ScoreLabel.X_REL, GameConfig.Podium.ScoreLabel.X_DELTA)
        val y = renderer.getRelativePos(
                GameConfig.Podium.ScoreLabel.Y_REL, GameConfig.Podium.ScoreLabel.Y_DELTA)
        return world.newGameObjectWithImage(
                GameConfig.TYPE_DECOR, x, y, scoreLabelTex, java.lang.Float.NaN,
                java.lang.Float.NaN)
    }

    fun makeReturnToMapButton(listener: Widget.WidgetTriggerListener, message: Int): Button {
        val x = renderer.getRelativePos(
                GameConfig.Podium.ReplayButton.X_REL,
                GameConfig.Podium.ReplayButton.X_DELTA)
        val y = renderer.getRelativePos(
                GameConfig.Podium.ReplayButton.Y_REL,
                GameConfig.Podium.ReplayButton.Y_DELTA)
        val returnToMapButton = Button(
                renderer,
                x,
                y,
                GameConfig.Podium.ReplayButton.WIDTH,
                GameConfig.Podium.ReplayButton.HEIGHT)
        returnToMapButton.addFlatBackground(
                GameConfig.Podium.ReplayButton.NORMAL_COLOR,
                GameConfig.Podium.ReplayButton.HIGHLIGHT_COLOR)
        returnToMapButton.addTex(playAgainTex)
        returnToMapButton.setClickListener(listener, message)
        return returnToMapButton
    }

    fun makeSignInBar(): GameObject {
        val x = renderer.getRelativePos(GameConfig.SignInBar.X_REL, GameConfig.SignInBar.X_DELTA)
        val y = renderer.getRelativePos(GameConfig.SignInBar.Y_REL, GameConfig.SignInBar.Y_DELTA)
        return world.newGameObjectWithColor(
                GameConfig.TYPE_DECOR,
                x,
                y,
                GameConfig.SignInBar.COLOR,
                GameConfig.SignInBar.WIDTH,
                GameConfig.SignInBar.HEIGHT)
    }

    fun makeSignInText(): GameObject {
        val x = renderer.getRelativePos(
                GameConfig.SignInText.X_REL, GameConfig.SignInText.X_DELTA)
        val y = renderer.getRelativePos(
                GameConfig.SignInText.Y_REL, GameConfig.SignInText.Y_DELTA)
        return world.newGameObjectWithImage(
                GameConfig.TYPE_DECOR, x, y, signInTextTex, java.lang.Float.NaN,
                java.lang.Float.NaN)
    }

    fun makeSignInButton(listener: Widget.WidgetTriggerListener, message: Int): Button {
        val x = renderer.getRelativePos(
                GameConfig.SignInButton.X_REL, GameConfig.SignInButton.X_DELTA)
        val y = renderer.getRelativePos(
                GameConfig.SignInButton.Y_REL, GameConfig.SignInButton.Y_DELTA)
        val signInButton = Button(
                renderer,
                x,
                y,
                GameConfig.SignInButton.WIDTH,
                GameConfig.SignInButton.HEIGHT)
        signInButton.addNormalTex(signInNormalTex)
        signInButton.addHighlightTex(signInHighlightTex)
        signInButton.addTex(
                signInLabelTex, GameConfig.SignInButton.TEXT_DELTA_X, 0.0f, java.lang.Float.NaN,
                java.lang.Float.NaN)
        signInButton.setClickListener(listener, message)
        return signInButton
    }

    private fun makeSpeakerOnOrMuteButton(
        isMute: Boolean,
        listener: Widget.WidgetTriggerListener,
        message: Int
    ): Button {
        val x = renderer.getRelativePos(GameConfig.Speaker.X_REL, GameConfig.Speaker.X_DELTA)
        val y = renderer.getRelativePos(GameConfig.Speaker.Y_REL, GameConfig.Speaker.Y_DELTA)
        val button = Button(renderer, x, y, GameConfig.Speaker.WIDTH, GameConfig.Speaker.HEIGHT)
        button.addNormalTex(
                if (isMute) speakerMuteIconTex else speakerOnIconTex,
                0.0f,
                0.0f,
                GameConfig.Speaker.SPRITE_WIDTH,
                GameConfig.Speaker.SPRITE_HEIGHT)
        button.setClickListener(listener, message)
        return button
    }

    fun makePauseButton(listener: Widget.WidgetTriggerListener, message: Int): Button {
        val x = renderer.getRelativePos(
                GameConfig.ScoreBar.PauseButton.X_REL,
                GameConfig.ScoreBar.PauseButton.X_DELTA)
        val y = renderer.getRelativePos(
                GameConfig.ScoreBar.PauseButton.Y_REL,
                GameConfig.ScoreBar.PauseButton.Y_DELTA)
        val button = Button(
                renderer,
                x,
                y,
                GameConfig.ScoreBar.PauseButton.WIDTH,
                GameConfig.ScoreBar.PauseButton.HEIGHT)
        button.addNormalTex(
                pauseIconTex,
                0.0f,
                0.0f,
                GameConfig.ScoreBar.PauseButton.SPRITE_WIDTH,
                GameConfig.ScoreBar.PauseButton.SPRITE_HEIGHT)
        button.addHighlightTex(
                pauseIconPressedTex,
                0.0f,
                0.0f,
                GameConfig.ScoreBar.PauseButton.SPRITE_WIDTH,
                GameConfig.ScoreBar.PauseButton.SPRITE_HEIGHT)
        button.setClickListener(listener, message)
        return button
    }

    fun makeSpeakerOnButton(listener: Widget.WidgetTriggerListener, message: Int): Button {
        return makeSpeakerOnOrMuteButton(false, listener, message)
    }

    fun makeSpeakerMuteButton(listener: Widget.WidgetTriggerListener, message: Int): Button {
        return makeSpeakerOnOrMuteButton(true, listener, message)
    }

    fun makePauseCurtain(): GameObject {
        val o = world.newGameObject(GameConfig.TYPE_DECOR, 0.0f, 0.0f)
        val sp = o.getSprite(o.addSprite())
        sp!!.width = renderer.width + 0.1f // safety margin
        sp.height = renderer.height + 0.1f // safety margin
        sp.color = GameConfig.PauseScreen.CURTAIN_COLOR
        sp.tintFactor = 0.0f
        return o
    }

    fun makeBigPlayButton(listener: Widget.WidgetTriggerListener, message: Int): Button {
        val x = renderer.getRelativePos(
                GameConfig.PauseScreen.BigPlayButton.X_REL,
                GameConfig.PauseScreen.BigPlayButton.X_DELTA)
        val y = renderer.getRelativePos(
                GameConfig.PauseScreen.BigPlayButton.Y_REL,
                GameConfig.PauseScreen.BigPlayButton.Y_DELTA)
        val button = Button(
                renderer,
                x,
                y,
                GameConfig.PauseScreen.BigPlayButton.WIDTH,
                GameConfig.PauseScreen.BigPlayButton.HEIGHT)
        button.addNormalTex(
                bigPlayButtonNormalTex,
                0.0f,
                0.0f,
                GameConfig.PauseScreen.BigPlayButton.SPRITE_WIDTH,
                java.lang.Float.NaN)
        button.addHighlightTex(
                bigPlayButtonHighlightTex,
                0.0f,
                0.0f,
                GameConfig.PauseScreen.BigPlayButton.SPRITE_WIDTH,
                java.lang.Float.NaN)
        button.setClickListener(listener, message)
        return button
    }

    fun makeQuitButton(listener: Widget.WidgetTriggerListener, message: Int): Button {
        val x = renderer.getRelativePos(
                GameConfig.PauseScreen.QuitBar.X_REL,
                GameConfig.PauseScreen.QuitBar.X_DELTA)
        val y = renderer.getRelativePos(
                GameConfig.PauseScreen.QuitBar.Y_REL,
                GameConfig.PauseScreen.QuitBar.Y_DELTA)
        val button = Button(
                renderer,
                x,
                y,
                GameConfig.PauseScreen.QuitBar.WIDTH,
                GameConfig.PauseScreen.QuitBar.HEIGHT)
        button.addNormalTex(
                quitBarTex, 0.0f, 0.0f, GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH,
                java.lang.Float.NaN)
        button.addHighlightTex(
                quitBarPressedTex,
                0.0f,
                0.0f,
                GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH,
                java.lang.Float.NaN)
        button.setClickListener(listener, message)
        return button
    }
}
