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
    int mPauseIconTex;
    int mResumeIconTex;
    int mBigPlayButtonNormalTex;
    int mBigPlayButtonHighlightTex;
    int mQuitBarTex;

    public GameObjectFactory(Renderer r, World w) {
        mRenderer = r;
        mWorld = w;
    }

    GameObject[] mTmpDigits = new GameObject[5];

    public void makeScorePopup(float x, float y, int score, DigitObjectFactory df) {
        int digits = (score >= 10000) ? 5 : (score >= 1000) ? 4 : (score >= 100) ? 3 : 2;

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

        mPlayAgainTex = mRenderer.requestTextTex(R.string.play_again, "play_again",
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
        mScoreBarTex = mRenderer.requestImageTex(R.drawable.games_scorebar, "games_scorebar",
                Renderer.DIM_WIDTH, GameConfig.ScoreBar.WIDTH);
        mResumeIconTex = mRenderer.requestImageTex(R.drawable.games_play, "games_play",
                Renderer.DIM_WIDTH, GameConfig.ScoreBar.PauseButton.SPRITE_WIDTH);
        mPauseIconTex = mRenderer.requestImageTex(R.drawable.games_pause, "games_pause",
                Renderer.DIM_WIDTH, GameConfig.ScoreBar.PauseButton.SPRITE_WIDTH);
        mBigPlayButtonNormalTex = mRenderer.requestImageTex(R.drawable.games_bigplay,
                "games_bigplay", Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.BigPlayButton.SPRITE_WIDTH);
        mBigPlayButtonHighlightTex = mRenderer.requestImageTex(R.drawable.games_bigplay_pressed,
                "games_bigplay_pressed", Renderer.DIM_WIDTH,
                GameConfig.PauseScreen.BigPlayButton.SPRITE_WIDTH);
        mQuitBarTex = mRenderer.requestImageTex(R.drawable.games_cancelbar,
                "games_cancelbar", Renderer.DIM_WIDTH,
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

    public GameObject makeScoreLabel() {
        // create the "score" static label
        float x = mRenderer.getRelativePos(GameConfig.Podium.ScoreLabel.X_REL,
                GameConfig.Podium.ScoreLabel.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.Podium.ScoreLabel.Y_REL,
                GameConfig.Podium.ScoreLabel.Y_DELTA);
        return mWorld.newGameObjectWithImage(GameConfig.TYPE_DECOR, x, y, mScoreLabelTex,
                Float.NaN, Float.NaN);
    }

    public Button makePlayAgainButton(Widget.WidgetTriggerListener listener, int message) {
        float x = mRenderer.getRelativePos(GameConfig.Podium.ReplayButton.X_REL,
                GameConfig.Podium.ReplayButton.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.Podium.ReplayButton.Y_REL,
                GameConfig.Podium.ReplayButton.Y_DELTA);
        Button replayButton = new Button(mRenderer, x, y, GameConfig.Podium.ReplayButton.WIDTH,
                GameConfig.Podium.ReplayButton.HEIGHT);
        replayButton.addFlatBackground(GameConfig.Podium.ReplayButton.NORMAL_COLOR,
                GameConfig.Podium.ReplayButton.HIGHLIGHT_COLOR);
        replayButton.addTex(mPlayAgainTex);
        replayButton.setClickListener(listener, message);
        return replayButton;
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

    private Button makePauseOrResumeButton(boolean isPause, Widget.WidgetTriggerListener listener,
            int message) {
        float x = mRenderer.getRelativePos(GameConfig.ScoreBar.PauseButton.X_REL,
                GameConfig.ScoreBar.PauseButton.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.ScoreBar.PauseButton.Y_REL,
                GameConfig.ScoreBar.PauseButton.Y_DELTA);
        Button button = new Button(mRenderer, x, y, GameConfig.ScoreBar.PauseButton.WIDTH,
                GameConfig.ScoreBar.PauseButton.HEIGHT);
        button.addTex(isPause ? mPauseIconTex : mResumeIconTex, 0.0f, 0.0f,
                GameConfig.ScoreBar.PauseButton.SPRITE_WIDTH,
                GameConfig.ScoreBar.PauseButton.SPRITE_HEIGHT);
        button.setClickListener(listener, message);
        return button;
    }

    public Button makePauseButton(Widget.WidgetTriggerListener listener, int message) {
        return makePauseOrResumeButton(true, listener, message);
    }

    public Button makeResumeButton(Widget.WidgetTriggerListener listener, int message) {
        return makePauseOrResumeButton(false, listener, message);
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
        button.addTex(mQuitBarTex, 0.0f, 0.0f,
                GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH, Float.NaN);
        button.setClickListener(listener, message);
        return button;
    }

    // As above, but anchored at the top of screen (hence -y)
    public Button makePodiumQuitButton(Widget.WidgetTriggerListener listener, int message) {
        float x = mRenderer.getRelativePos(GameConfig.PauseScreen.QuitBar.X_REL,
                GameConfig.PauseScreen.QuitBar.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.PauseScreen.QuitBar.Y_REL,
                GameConfig.PauseScreen.QuitBar.Y_DELTA);
        Button button = new Button(mRenderer, x, -y, GameConfig.PauseScreen.QuitBar.WIDTH,
                GameConfig.PauseScreen.QuitBar.HEIGHT);
        button.addTex(mQuitBarTex, 0.0f, 0.0f,
                GameConfig.PauseScreen.QuitBar.SPRITE_WIDTH, Float.NaN);
        button.setClickListener(listener, message);
        return button;
    }
}
