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

package com.google.android.apps.santatracker.games.gamebase;


import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.simpleengine.Renderer;
import com.google.android.apps.santatracker.games.simpleengine.game.GameObject;
import com.google.android.apps.santatracker.games.simpleengine.game.World;
import com.google.android.apps.santatracker.games.simpleengine.ui.Button;
import com.google.android.apps.santatracker.games.simpleengine.ui.Widget;

import java.util.Arrays;
import java.util.Random;

public class GameObjectFactory {

    protected Renderer mRenderer;
    protected World mWorld;
    protected Random mRandom = new Random();

    // Textures
    int mTexClock;
    int mTexPodium;
    int mPlayAgainTex;
    int mScoreLabelTex;
    int mSignInLabelTex;
    int mSignInNormalTex;
    int mSignInHighlightTex;
    int mSignInTextTex;
    int mScoreBarTex;
    int mScoreBarLabelTex;
    int mPauseIconTex;
    int mPauseIconPressedTex;
    int mSpeakerMuteIconTex;
    int mSpeakerOnIconTex;
    int mBigPlayButtonNormalTex;
    int mBigPlayButtonHighlightTex;
    int mQuitBarTex;
    int mQuitBarPressedTex;
    int mQuitBarLabelTex;
    int mInviteBarTex;
    int mInviteBarPressedTex;

    public GameObjectFactory(Renderer r, World w) {
        mRenderer = r;
        mWorld = w;
    }

    GameObject[] mTmpDigits = new GameObject[5];

    public void makeScorePopup(float x, float y, int score, DigitObjectFactory df) {
        int digits;
        if(score >= 0) {
            digits = (score >= 10000) ? 5
                    : (score >= 1000) ? 4
                    : (score >= 100) ? 3
                    : (score >= 10) ? 2
                    : 1;
        } else  {
            digits = (score <= -10000) ? 6
                    : (score <= -1000) ? 5
                    : (score <= -100) ? 4
                    : (score <= -10) ? 3
                    : 2;
        }

        Arrays.fill(mTmpDigits, null);
        df.makeDigitObjects(digits, GameConfig.TYPE_DECOR, x, y,
                GameConfig.ScorePopup.DIGIT_SIZE, GameConfig.ScorePopup.DIGIT_SPACING,
                mTmpDigits);
        df.setDigits(score, mTmpDigits, 0, digits);

        int i;
        for (i = 0; i < digits; i++) {
            GameObject o = mTmpDigits[i];
            o.velY = GameConfig.ScorePopup.POPUP_VEL_Y;
            o.timeToLive = GameConfig.ScorePopup.POPUP_EXPIRE;
        }
    }

    protected void requestTextures() {
        mTexClock = mRenderer.requestImageTex(R.drawable.jetpack_clock, "jetpack_clock",
                Renderer.DIM_WIDTH, GameConfig.TimeDisplay.ICON_SIZE);
        mTexPodium = mRenderer.requestImageTex(R.drawable.jetpack_podium, "jetpack_podium",
                Renderer.DIM_WIDTH, GameConfig.Podium.WIDTH);

        mPlayAgainTex = mRenderer.requestTextTex(R.string.return_to_map, "return_to_map",
                GameConfig.Podium.ReplayButton.FONT_SIZE);
        mScoreLabelTex = mRenderer.requestTextTex(R.string.score, "score",
                GameConfig.Podium.ScoreLabel.FONT_SIZE);

        mSignInLabelTex = mRenderer.requestTextTex(R.string.common_signin_button_text,
                "jetpack_sign_in",
                GameConfig.SignInButton.FONT_SIZE);
        mSignInNormalTex = mRenderer.requestImageTex(R.drawable.jetpack_signin, "jetpack_siginin",
                Renderer.DIM_WIDTH, GameConfig.SignInButton.WIDTH);
        mSignInHighlightTex = mRenderer.requestImageTex(R.drawable.jetpack_signin_pressed,
                "jetpack_signin_pressed", Renderer.DIM_WIDTH,
                GameConfig.SignInButton.WIDTH);
        mSignInTextTex = mRenderer.requestTextTex(R.string.why_sign_in,
                "jetpack_why_sign_in", GameConfig.SignInText.FONT_SIZE,
                GameConfig.SignInText.ANCHOR, GameConfig.SignInText.COLOR);
        mScoreBarTex = mRenderer.requestImageTex(R.drawable.icon_ribbon_upsidedown_short, "games_scorebar",
                Renderer.DIM_WIDTH, GameConfig.ScoreBar.WIDTH);
        mScoreBarLabelTex = mRenderer.requestTextTex(R.string.score, "score_bar_label",
                GameConfig.ScoreBar.ScoreBarLabel.FONT_SIZE);
        mPauseIconTex = mRenderer.requestImageTex(R.drawable.common_btn_pause, "games_pause",
                Renderer.DIM_WIDTH, GameConfig.ScoreBar.PauseButton.SPRITE_WIDTH);
        mPauseIconPressedTex = mRenderer.requestImageTex(R.drawable.common_btn_pause,
                "games_pause_pressed", Renderer.DIM_WIDTH, GameConfig.ScoreBar.PauseButton.SPRITE_WIDTH);
        mSpeakerMuteIconTex = mRenderer.requestImageTex(R.drawable.common_btn_speaker_off,
                "speaker_mute", Renderer.DIM_WIDTH, GameConfig.Speaker.SPRITE_WIDTH);
        mSpeakerOnIconTex = mRenderer.requestImageTex(R.drawable.common_btn_speaker_on,
                "speaker_on", Renderer.DIM_WIDTH, GameConfig.Speaker.SPRITE_WIDTH);
        mBigPlayButtonNormalTex = mRenderer.requestImageTex(R.drawable.btn_play_yellow,
                "btn_play_yellow", Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.BigPlayButton.SPRITE_WIDTH);
        mBigPlayButtonHighlightTex = mRenderer.requestImageTex(R.drawable.btn_play_yellow,
                "btn_play_pressed", Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.BigPlayButton.SPRITE_WIDTH);
        mQuitBarTex = mRenderer.requestImageTex(R.drawable.purple_rectangle_button,
                "quit_button", Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH);
        mQuitBarLabelTex = mRenderer.requestTextTex(R.string.quit_bar_label, "quit_bar_label",
                GameConfig.PauseScreen.QuitBar.QuitBarLabel.FONT_SIZE);
        mQuitBarPressedTex = mRenderer.requestImageTex(R.drawable.purple_rectangle_button,
                "quit_button_pressed", Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH);
        mInviteBarTex = mRenderer.requestImageTex(R.drawable.games_share,
                "games_share", Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH);
        mInviteBarPressedTex = mRenderer.requestImageTex(R.drawable.games_share_pressed,
                "games_share_pressed", Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH);
    }

    public GameObject makeClockIcon() {
        float x = mRenderer.getRelativePos(GameConfig.TimeDisplay.POS_X_REL,
                GameConfig.TimeDisplay.POS_X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.TimeDisplay.POS_Y_REL,
                GameConfig.TimeDisplay.POS_Y_DELTA);
        return mWorld.newGameObjectWithImage(GameConfig.TYPE_DECOR, x, y, mTexClock,
                GameConfig.TimeDisplay.ICON_SIZE, GameConfig.TimeDisplay.ICON_SIZE);
    }

    public GameObject makeTvClockIcon() {
        float x = mRenderer.getRelativePos(GameConfig.TimeDisplay.POS_X_REL,
                GameConfig.TimeDisplay.POS_X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.TimeDisplay.POS_Y_REL_TV,
                GameConfig.TimeDisplay.POS_Y_DELTA_TV);
        return mWorld.newGameObjectWithImage(GameConfig.TYPE_DECOR, x, y, mTexClock,
                GameConfig.TimeDisplay.ICON_SIZE, GameConfig.TimeDisplay.ICON_SIZE);
    }

    public GameObject makePodium() {
        float x = mRenderer.getRelativePos(GameConfig.Podium.X_REL, GameConfig.Podium.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.Podium.Y_REL, GameConfig.Podium.Y_DELTA);
        return mWorld.newGameObjectWithImage(GameConfig.TYPE_DECOR, x, y, mTexPodium,
                GameConfig.Podium.WIDTH, Float.NaN);
    }

    public GameObject makeScoreBar() {
        float x = mRenderer.getRelativePos(GameConfig.ScoreBar.X_REL, GameConfig.ScoreBar.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.ScoreBar.Y_REL, GameConfig.ScoreBar.Y_DELTA);
        return mWorld.newGameObjectWithImage(GameConfig.TYPE_DECOR, x, y, mScoreBarTex,
                GameConfig.ScoreBar.WIDTH, Float.NaN);
    }

    public GameObject makeScoreBarLabel() {
        float x = mRenderer.getRelativePos(GameConfig.ScoreBar.ScoreBarLabel.X_REL, GameConfig.ScoreBar.ScoreBarLabel.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.ScoreBar.ScoreBarLabel.Y_REL, GameConfig.ScoreBar.ScoreBarLabel.Y_DELTA);
        return mWorld.newGameObjectWithImage(GameConfig.TYPE_DECOR, x, y, mScoreBarLabelTex,
                GameConfig.ScoreBar.ScoreBarLabel.WIDTH, Float.NaN);
    }

    public GameObject makeQuitBarLabel() {
        float x = mRenderer.getRelativePos(GameConfig.PauseScreen.QuitBar.QuitBarLabel.X_REL, GameConfig.PauseScreen.QuitBar.QuitBarLabel.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.PauseScreen.QuitBar.QuitBarLabel.Y_REL, GameConfig.PauseScreen.QuitBar.QuitBarLabel.Y_DELTA);
        return mWorld.newGameObjectWithImage(GameConfig.TYPE_DECOR, x, y, mQuitBarLabelTex,
                GameConfig.PauseScreen.QuitBar.QuitBarLabel.WIDTH, Float.NaN);
    }

    public GameObject makeScoreLabel() {
        // create the "score" static label
        float x = mRenderer.getRelativePos(GameConfig.Podium.ScoreLabel.X_REL,
                GameConfig.Podium.ScoreLabel.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.Podium.ScoreLabel.Y_REL,
                GameConfig.Podium.ScoreLabel.Y_DELTA);
        GameObject obj = mWorld.newGameObjectWithImage(GameConfig.TYPE_DECOR, x, y, mScoreLabelTex,
                Float.NaN, Float.NaN);
        return obj;
    }

    public Button makeReturnToMapButton(Widget.WidgetTriggerListener listener, int message) {
        float x = mRenderer.getRelativePos(GameConfig.Podium.ReplayButton.X_REL,
                GameConfig.Podium.ReplayButton.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.Podium.ReplayButton.Y_REL,
                GameConfig.Podium.ReplayButton.Y_DELTA);
        Button returnToMapButton = new Button(mRenderer, x, y, GameConfig.Podium.ReplayButton.WIDTH,
                GameConfig.Podium.ReplayButton.HEIGHT);
        returnToMapButton.addFlatBackground(GameConfig.Podium.ReplayButton.NORMAL_COLOR,
                GameConfig.Podium.ReplayButton.HIGHLIGHT_COLOR);
        returnToMapButton.addTex(mPlayAgainTex);
        returnToMapButton.setClickListener(listener, message);
        return returnToMapButton;
    }

    public GameObject makeSignInBar() {
        float x = mRenderer.getRelativePos(GameConfig.SignInBar.X_REL,
                GameConfig.SignInBar.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.SignInBar.Y_REL,
                GameConfig.SignInBar.Y_DELTA);
        return mWorld
                .newGameObjectWithColor(GameConfig.TYPE_DECOR, x, y, GameConfig.SignInBar.COLOR,
                        GameConfig.SignInBar.WIDTH, GameConfig.SignInBar.HEIGHT);
    }

    public GameObject makeSignInText() {
        float x = mRenderer.getRelativePos(GameConfig.SignInText.X_REL,
                GameConfig.SignInText.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.SignInText.Y_REL,
                GameConfig.SignInText.Y_DELTA);
        return mWorld.newGameObjectWithImage(GameConfig.TYPE_DECOR, x, y,
                mSignInTextTex, Float.NaN, Float.NaN);
    }

    public Button makeSignInButton(Widget.WidgetTriggerListener listener, int message) {
        float x = mRenderer.getRelativePos(GameConfig.SignInButton.X_REL,
                GameConfig.SignInButton.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.SignInButton.Y_REL,
                GameConfig.SignInButton.Y_DELTA);
        Button signInButton = new Button(mRenderer, x, y, GameConfig.SignInButton.WIDTH,
                GameConfig.SignInButton.HEIGHT);
        signInButton.addNormalTex(mSignInNormalTex);
        signInButton.addHighlightTex(mSignInHighlightTex);
        signInButton.addTex(mSignInLabelTex, GameConfig.SignInButton.TEXT_DELTA_X, 0.0f,
                Float.NaN, Float.NaN);
        signInButton.setClickListener(listener, message);
        return signInButton;
    }

    private Button makeSpeakerOnOrMuteButton(boolean isMute, Widget.WidgetTriggerListener listener,
                                            int message) {
        float x = mRenderer.getRelativePos(GameConfig.Speaker.X_REL,
                GameConfig.Speaker.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.Speaker.Y_REL,
                GameConfig.Speaker.Y_DELTA);
        Button button = new Button(mRenderer, x, y, GameConfig.Speaker.WIDTH,
                GameConfig.Speaker.HEIGHT);
        button.addNormalTex(isMute ? mSpeakerMuteIconTex : mSpeakerOnIconTex, 0.0f, 0.0f,
                GameConfig.Speaker.SPRITE_WIDTH,
                GameConfig.Speaker.SPRITE_HEIGHT);
        button.setClickListener(listener, message);
        return button;
    }

    public Button makePauseButton(Widget.WidgetTriggerListener listener, int message) {
        float x = mRenderer.getRelativePos(GameConfig.ScoreBar.PauseButton.X_REL,
                GameConfig.ScoreBar.PauseButton.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.ScoreBar.PauseButton.Y_REL,
                GameConfig.ScoreBar.PauseButton.Y_DELTA);
        Button button = new Button(mRenderer, x, y, GameConfig.ScoreBar.PauseButton.WIDTH,
                GameConfig.ScoreBar.PauseButton.HEIGHT);
        button.addNormalTex(mPauseIconTex, 0.0f, 0.0f,
                GameConfig.ScoreBar.PauseButton.SPRITE_WIDTH,
                GameConfig.ScoreBar.PauseButton.SPRITE_HEIGHT);
        button.addHighlightTex(mPauseIconPressedTex, 0.0f, 0.0f,
                GameConfig.ScoreBar.PauseButton.SPRITE_WIDTH,
                GameConfig.ScoreBar.PauseButton.SPRITE_HEIGHT);
        button.setClickListener(listener, message);
        return button;
    }

    public Button makeSpeakerOnButton(Widget.WidgetTriggerListener listener, int message) {
        return makeSpeakerOnOrMuteButton(false, listener, message);
    }

    public Button makeSpeakerMuteButton(Widget.WidgetTriggerListener listener, int message) {
        return makeSpeakerOnOrMuteButton(true, listener, message);
    }

    public GameObject makePauseCurtain() {
        GameObject o = mWorld.newGameObject(GameConfig.TYPE_DECOR, 0.0f, 0.0f);
        Renderer.Sprite sp = o.getSprite(o.addSprite());
        sp.width = mRenderer.getWidth() + 0.1f; // safety margin
        sp.height = mRenderer.getHeight() + 0.1f; // safety margin
        sp.color = GameConfig.PauseScreen.CURTAIN_COLOR;
        sp.tintFactor = 0.0f;
        return o;
    }

    public Button makeBigPlayButton(Widget.WidgetTriggerListener listener, int message) {
        float x = mRenderer.getRelativePos(GameConfig.PauseScreen.BigPlayButton.X_REL,
                GameConfig.PauseScreen.BigPlayButton.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.PauseScreen.BigPlayButton.Y_REL,
                GameConfig.PauseScreen.BigPlayButton.Y_DELTA);
        Button button = new Button(mRenderer, x, y, GameConfig.PauseScreen.BigPlayButton.WIDTH,
                GameConfig.PauseScreen.BigPlayButton.HEIGHT);
        button.addNormalTex(mBigPlayButtonNormalTex, 0.0f, 0.0f,
                GameConfig.PauseScreen.BigPlayButton.SPRITE_WIDTH, Float.NaN);
        button.addHighlightTex(mBigPlayButtonHighlightTex, 0.0f, 0.0f,
                GameConfig.PauseScreen.BigPlayButton.SPRITE_WIDTH, Float.NaN);
        button.setClickListener(listener, message);
        return button;
    }

    public Button makeQuitButton(Widget.WidgetTriggerListener listener, int message) {
        float x = mRenderer.getRelativePos(GameConfig.PauseScreen.QuitBar.X_REL,
                GameConfig.PauseScreen.QuitBar.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.PauseScreen.QuitBar.Y_REL,
                GameConfig.PauseScreen.QuitBar.Y_DELTA);
        Button button = new Button(mRenderer, x, y, GameConfig.PauseScreen.QuitBar.WIDTH,
                GameConfig.PauseScreen.QuitBar.HEIGHT);
        button.addNormalTex(mQuitBarTex, 0.0f, 0.0f,
                GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH, Float.NaN);
        button.addHighlightTex(mQuitBarPressedTex, 0.0f, 0.0f,
                GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH, Float.NaN);
        button.setClickListener(listener, message);
        return button;
    }
}
