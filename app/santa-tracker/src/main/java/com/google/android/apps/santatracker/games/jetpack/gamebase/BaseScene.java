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
import com.google.android.apps.santatracker.games.common.ImmersiveModeHelper;
import com.google.android.apps.santatracker.games.gumball.Utils;
import com.google.android.apps.santatracker.games.simpleengine.Renderer;
import com.google.android.apps.santatracker.games.simpleengine.Scene;
import com.google.android.apps.santatracker.games.simpleengine.SceneManager;
import com.google.android.apps.santatracker.games.simpleengine.SmoothValue;
import com.google.android.apps.santatracker.games.simpleengine.SoundManager;
import com.google.android.apps.santatracker.games.simpleengine.game.GameObject;
import com.google.android.apps.santatracker.games.simpleengine.game.World;
import com.google.android.apps.santatracker.games.simpleengine.ui.Button;
import com.google.android.apps.santatracker.games.simpleengine.ui.SimpleUI;
import com.google.android.apps.santatracker.games.simpleengine.ui.Widget;
import com.google.example.games.basegameutils.GameHelper;

import android.app.Activity;

import java.util.Random;

public abstract class BaseScene extends Scene implements Widget.WidgetTriggerListener {

    // digit object factory (to display score, etc)
    protected DigitObjectFactory mDigitFactory;
    protected GameObjectFactory mObjectFactory;

    protected World mWorld;
    protected Renderer mRenderer;
    protected Random mRandom = new Random();

    // score bar object
    protected GameObject mScoreBarObj;

    // score digit objects
    protected GameObject[] mScoreDigitObj = new GameObject[GameConfig.ScoreDisplay.DIGIT_COUNT];

    // timer digit objects
    protected GameObject mClockIconObj = null;
    protected GameObject[] mTimeDigitObj = new GameObject[GameConfig.TimeDisplay.DIGIT_COUNT];

    // player's current score
    protected int mScore = 0;
    protected SmoothValue mDisplayedScore = new SmoothValue(0.0f,
            GameConfig.ScoreDisplay.UPDATE_SPEED);

    // game ended?
    protected boolean mGameEnded = false;

    // our UI (buttons, etc)
    protected SimpleUI mUI = null;

    // widget trigger messages
    private static final int MSG_REPLAY = 1001;
    private static final int MSG_SIGN_IN = 1002;
    private static final int MSG_PAUSE = 1003;
    private static final int MSG_RESUME = 1004;
    private static final int MSG_QUIT = 1005;

    // sfx IDs
    protected int mGameOverSfx;

    // paused?
    protected boolean mPaused = false;

    // back key pressed?
    private boolean mBackKeyPending = false;

    // pause and resume buttons
    Button mPauseButton, mResumeButton;

    // pause curtain, that is, the full screen object we display as a translucent
    // screen over the whole display when the game is paused
    GameObject mPauseCurtain = null;

    // the big play button
    Button mBigPlayButton = null;

    // quit button
    Button mQuitButton = null;

    // game objects that compose the Sign In ui
    GameObject mSignInBarObj = null;
    Button mSignInButton = null;
    GameObject mSignInTextObj = null;

    // to be implemented by subclasses
    protected abstract String getBgmAssetFile();

    protected abstract float getDisplayedTime();

    protected abstract BaseScene makeNewScene();

    // are we signed in
    private boolean mSignedIn = false;

    @Override
    public void onInstall() {
        mRenderer = SceneManager.getInstance().getRenderer();
        mWorld = new World(mRenderer);
        mDigitFactory = new DigitObjectFactory(mRenderer, mWorld);
        mDigitFactory.requestTextures(GameConfig.ScoreDisplay.DIGIT_SIZE);
        mObjectFactory = new GameObjectFactory(mRenderer, mWorld);
        mObjectFactory.requestTextures();

        mDigitFactory.makeDigitObjects(GameConfig.ScoreDisplay.DIGIT_COUNT, GameConfig.TYPE_DECOR,
                mRenderer.getRelativePos(GameConfig.ScoreDisplay.POS_X_REL,
                        GameConfig.ScoreDisplay.POS_X_DELTA),
                mRenderer.getRelativePos(GameConfig.ScoreDisplay.POS_Y_REL,
                        GameConfig.ScoreDisplay.POS_Y_DELTA),
                GameConfig.ScoreDisplay.DIGIT_SIZE,
                GameConfig.ScoreDisplay.DIGIT_SPACING, mScoreDigitObj);

        mClockIconObj = mObjectFactory.makeClockIcon();
        mScoreBarObj = mObjectFactory.makeScoreBar();

        mUI = new SimpleUI(mRenderer);

        mPauseButton = mObjectFactory.makePauseButton(this, MSG_PAUSE);
        mResumeButton = mObjectFactory.makeResumeButton(this, MSG_RESUME);
        mResumeButton.hide();

        mUI.add(mPauseButton);
        mUI.add(mResumeButton);

        mPauseCurtain = mObjectFactory.makePauseCurtain();
        mPauseCurtain.hide();

        mBigPlayButton = mObjectFactory.makeBigPlayButton(this, MSG_RESUME);
        mBigPlayButton.hide();
        mUI.add(mBigPlayButton);

        mQuitButton = mObjectFactory.makeQuitButton(this, MSG_QUIT);
        mQuitButton.hide();
        mUI.add(mQuitButton);

        float x = GameConfig.TimeDisplay.POS_X_DELTA + GameConfig.TimeDisplay.ICON_SIZE;
        mDigitFactory.makeDigitObjects(GameConfig.TimeDisplay.DIGIT_COUNT, GameConfig.TYPE_DECOR,
                mRenderer.getRelativePos(GameConfig.TimeDisplay.POS_X_REL, x),
                mRenderer.getRelativePos(GameConfig.TimeDisplay.POS_Y_REL,
                        GameConfig.TimeDisplay.POS_Y_DELTA),
                GameConfig.TimeDisplay.DIGIT_SIZE,
                GameConfig.TimeDisplay.DIGIT_SPACING, mTimeDigitObj);

        SoundManager soundManager = SceneManager.getInstance().getSoundManager();
        soundManager.requestBackgroundMusic(getBgmAssetFile());
        mGameOverSfx = soundManager.requestSfx(R.raw.jetpack_gameover);

        mRenderer.setClearColor(0xffffffff);

        // are we signed in?
        SceneActivity act = (SceneActivity) SceneManager.getInstance().getActivity();
        if (act != null && act.getGameHelper() != null) {
            mSignedIn = act.getGameHelper().isSignedIn();
        }

    }

    @Override
    public void onUninstall() {
    }

    @Override
    public void doStandbyFrame(float deltaT) {
    }

    @Override
    public void doFrame(float deltaT) {
        if (mPaused) {
            deltaT = 0.0f;
        }

        if (mBackKeyPending) {
            processBackKey();
        }

        // If Activity lost focus and we're playing the game, pause
        if (!SceneManager.getInstance().shouldBePlaying() && !mGameEnded && !mPaused) {
            pauseGame();
        }

        if (!mGameEnded) {
            updateScore(deltaT);
            updateTime(deltaT);
        } else {
            updateScore(deltaT);
            checkSignInWidgetsNeeded();
        }

        mWorld.doFrame(deltaT);
    }

    private void processBackKey() {
        mBackKeyPending = false;
        if (mGameEnded) {
            quitGame();
        } else if (mPaused) {
            unpauseGame();
        } else {
            pauseGame();
        }
    }

    private void updateScore(float deltaT) {
        if (mGameEnded) {
            mDisplayedScore.setValue(mScore);
        } else {
            mDisplayedScore.setTarget(mScore);
            mDisplayedScore.update(deltaT);
        }
        mDigitFactory.setDigits((int) Math.round(mDisplayedScore.getValue()), mScoreDigitObj);
        mScoreBarObj.bringToFront();
        bringObjectsToFront(mScoreDigitObj);
        mPauseButton.bringToFront();
        mResumeButton.bringToFront();
    }

    protected void endGame() {
        mGameEnded = true;

        // hide the score bar
        mScoreBarObj.hide();

        // show the podium object
        mObjectFactory.makePodium();

        // move score to final position
        float x = mRenderer.getRelativePos(GameConfig.Podium.ScoreDisplay.X_REL,
                GameConfig.Podium.ScoreDisplay.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.Podium.ScoreDisplay.Y_REL,
                GameConfig.Podium.ScoreDisplay.Y_DELTA);
        displaceObjectsTo(mScoreDigitObj, x, y);
        bringObjectsToFront(mScoreDigitObj);

        // hide time counter
        mClockIconObj.hide();
        hideObjects(mTimeDigitObj);

        // make the "your score is" label
        mObjectFactory.makeScoreLabel();

        // create the end of game UI and add the "play again" button to it
        mUI.add(mObjectFactory.makePlayAgainButton(this, MSG_REPLAY));
        mResumeButton.hide();
        mPauseButton.hide();

        Button quitButton = mObjectFactory.makePodiumQuitButton(this, MSG_QUIT);
        mUI.add(quitButton);
        quitButton.bringToFront();
        quitButton.show();

        // create the sign in bar and sign in button
        if (!mSignedIn) {
            mSignInBarObj = mObjectFactory.makeSignInBar();
            mSignInTextObj = mObjectFactory.makeSignInText();
            mUI.add(mSignInButton = mObjectFactory.makeSignInButton(this, MSG_SIGN_IN));
        }

        // disable the background music
        SceneManager.getInstance().getSoundManager().enableBgm(false);

        // play the game over sfx
        SceneManager.getInstance().getSoundManager().playSfx(mGameOverSfx);
    }

    protected void displaceObjectsTo(GameObject[] objs, float x, float y) {
        float deltaX = x - objs[0].x;
        float deltaY = y - objs[0].y;
        int i;
        for (i = 0; i < objs.length; i++) {
            objs[i].displaceBy(deltaX, deltaY);
        }
    }

    protected void bringObjectsToFront(GameObject[] objs) {
        int i;
        for (i = 0; i < objs.length; i++) {
            objs[i].bringToFront();
        }
    }

    protected void hideObjects(GameObject[] objs) {
        int i;
        for (i = 0; i < objs.length; i++) {
            objs[i].hide();
        }
    }

    private void updateTime(float deltaT) {
        int seconds = (int) Math.ceil(getDisplayedTime());
        seconds = seconds < 0 ? 0 : seconds > 99 ? 99 : seconds;
        mDigitFactory.setDigits(seconds, mTimeDigitObj);
        bringObjectsToFront(mTimeDigitObj);
        mClockIconObj.bringToFront();
    }

    @Override
    public void onScreenResized(int width, int height) {

    }

    @Override
    public void onPointerDown(int pointerId, float x, float y) {
        super.onPointerDown(pointerId, x, y);
        if (mUI != null) {
            mUI.onPointerDown(pointerId, x, y);
        }
    }

    @Override
    public void onPointerUp(int pointerId, float x, float y) {
        super.onPointerUp(pointerId, x, y);
        if (mUI != null) {
            mUI.onPointerUp(pointerId, x, y);
        }
    }

    protected void pauseGame() {
        if (!mPaused) {
            mPaused = true;
            SceneManager.getInstance().getSoundManager().enableBgm(false);
            mPauseButton.hide();
            mResumeButton.show();
            mPauseCurtain.show();
            mPauseCurtain.bringToFront();
            mBigPlayButton.show();
            mBigPlayButton.bringToFront();
            mQuitButton.show();
            mQuitButton.bringToFront();
            if (Utils.hasKitKat() && SceneManager.getInstance().getActivity() !=null) {
                SceneManager.getInstance().getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImmersiveModeHelper.setImmersiveStickyWithActionBar(
                                SceneManager.getInstance().getActivity().getWindow());
                    }
                });
            }
        }
    }

    protected void unpauseGame() {
        if (mPaused) {
            mPaused = false;
            SceneManager.getInstance().getSoundManager().enableBgm(true);
            mResumeButton.hide();
            mPauseButton.show();
            mPauseCurtain.hide();
            mQuitButton.hide();
            mBigPlayButton.hide();
            if (Utils.hasKitKat() && SceneManager.getInstance().getActivity() !=null) {
                SceneManager.getInstance().getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImmersiveModeHelper.setImmersiveSticky(
                                SceneManager.getInstance().getActivity().getWindow());
                    }
                });
            }
        }
    }

    private int roundScore(int score) {
        score = (score / 50) * 50;
        return score <= 0 ? 50 : score;
    }

    @Override
    public void onPointerMove(int pointerId, float x, float y, float deltaX, float deltaY) {
        if (mUI != null) {
            mUI.onPointerMove(pointerId, x, y, deltaX, deltaY);
        }
    }

    @Override
    public void onWidgetTriggered(int message) {
        SceneActivity act;
        GameHelper helper;

        switch (message) {
            case MSG_REPLAY:
                SceneManager.getInstance().requestNewScene(makeNewScene());
                break;
            case MSG_SIGN_IN:
                act = (SceneActivity) SceneManager.getInstance().getActivity();
                if (act != null && null != (helper = act.getGameHelper())) {
                    // start sign in flow
                    helper.beginUserInitiatedSignIn();
                }
                break;
            case MSG_PAUSE:
                pauseGame();
                break;
            case MSG_RESUME:
                unpauseGame();
                break;
            case MSG_QUIT:
                quitGame();
                break;
        }
    }

    private void quitGame() {
        Activity act = SceneManager.getInstance().getActivity();
        if (act != null && act instanceof SceneActivity) {
            ((SceneActivity) act).postQuitGame();
        }
    }

    private void checkSignInWidgetsNeeded() {
        if (mSignedIn) {
            if (mSignInBarObj != null) {
                mSignInBarObj.hide();
            }
            if (mSignInTextObj != null) {
                mSignInTextObj.hide();
            }
            if (mSignInButton != null) {
                mSignInButton.hide();
            }
        }
    }

    // Caution: Called from the UI thread!
    public void setSignedIn(boolean signedIn) {
        mSignedIn = signedIn;
    }

    // Caution: Called from the UI thread!
    public boolean onBackKeyPressed() {
        // raise a flag and process later (on the game thread)
        mBackKeyPending = true;
        return true;
    }
}
