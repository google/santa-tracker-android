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

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;

import com.google.android.apps.santatracker.R;
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
import com.google.android.apps.santatracker.util.ImmersiveModeHelper;

import java.util.Random;

public abstract class BaseScene extends Scene implements Widget.WidgetTriggerListener, GameEndedListener {

    // digit object factory (to display score, etc)
    protected DigitObjectFactory mDigitFactory;
    protected GameObjectFactory mObjectFactory;

    protected World mWorld;
    protected Renderer mRenderer;
    protected Random mRandom = new Random();

    // score bar object
    protected GameObject mScoreBarObj;

    // score bar label object
    protected GameObject mScoreBarLabelObj;

    // score digit objects
    protected GameObject[] mScoreDigitObj = new GameObject[GameConfig.ScoreDisplay.DIGIT_COUNT];

    // timer digit objects
    protected GameObject[] mTimeDigitObj = new GameObject[GameConfig.TimeDisplay.DIGIT_COUNT];

    // countdown objects
    public GameObject mCountdownDigitObj;

    // player's current score
    protected int mScore = 0;
    protected SmoothValue mDisplayedScore = new SmoothValue(0.0f,
            GameConfig.ScoreDisplay.UPDATE_SPEED);

    // game ended?
    protected boolean mGameEnded = false;

    protected boolean mIsInGameEndingTransition = false;

    // our UI (buttons, etc)
    protected SimpleUI mUI = null;

    // widget trigger messages
    private static final int MSG_RETURN_WITH_VALUE = 1001;
    private static final int MSG_SIGN_IN = 1002;
    private static final int MSG_PAUSE = 1003;
    private static final int MSG_RESUME = 1004;
    private static final int MSG_QUIT = 1005;
    private static final int MSG_SHARE = 1006;
    private static final int MSG_REPLAY = 1007;
    private static final int MSG_MUTE = 1008;
    private static final int MSG_UNMUTE = 1009;
    private static final int MSG_START = 1010;
    protected static final int MSG_GO_TO_END_GAME = 1011;

    // sfx IDs
    protected int mGameOverSfx;

    // paused?
    protected boolean mPaused = false;

    // in start countdown?
    protected boolean mInStartCountdown = false;
    protected float mStartCountdownTimeRemaining;

    // back key pressed?
    private boolean mBackKeyPending = false;

    // DPAD_CENTER key pressed?
    private boolean mConfirmKeyPending;
    private long mConfirmKeyEventTime;
    private final long CENTER_KEY_DELAY_MS = 500;

    // isRunning on Tv?
    boolean mIsTv;

    // pause button
    Button mPauseButton;

    // speaker on and mute buttons
    Button mSpeakerOnButton, mSpeakerMuteButton;

    // pause curtain, that is, the full screen object we display as a translucent
    // screen over the whole display when the game is paused
    GameObject mPauseCurtain = null;

    // the big play button
    Button mBigPlayButton = null;
    Button mBigStartGameButton = null;

    // quit button
    Button mQuitButton = null;
    GameObject mQuitBarLabel = null;

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

        // are we signed in?
        SceneActivity act = (SceneActivity) SceneManager.getInstance().getActivity();
        if (act != null) {
            mSignedIn = act.isSignedIn();
            UiModeManager manger = (UiModeManager)act.getSystemService(Context.UI_MODE_SERVICE);
            mIsTv = Configuration.UI_MODE_TYPE_TELEVISION == manger.getCurrentModeType();
        }

        mRenderer = SceneManager.getInstance().getRenderer();
        mWorld = new World(mRenderer);
        mDigitFactory = new DigitObjectFactory(mRenderer, mWorld);
        mDigitFactory.requestWhiteTextures(GameConfig.ScoreDisplay.DIGIT_SIZE);
        mObjectFactory = new GameObjectFactory(mRenderer, mWorld);
        mObjectFactory.requestTextures();

        mUI = new SimpleUI(mRenderer);

        if (isTv()) {
            mDigitFactory.makeDigitObjects(GameConfig.ScoreDisplay.DIGIT_COUNT, GameConfig.TYPE_DECOR,
                    mRenderer.getRelativePos(GameConfig.ScoreDisplay.POS_X_REL,
                            GameConfig.ScoreDisplay.POS_X_DELTA),
                    mRenderer.getRelativePos(GameConfig.ScoreDisplay.POS_Y_REL_TV,
                            GameConfig.ScoreDisplay.POS_Y_DELTA_TV),
                    GameConfig.ScoreDisplay.DIGIT_SIZE,
                    GameConfig.ScoreDisplay.DIGIT_SPACING, mScoreDigitObj);
            mBigPlayButton = mObjectFactory.makeBigPlayButton(this, MSG_RESUME);
            mBigPlayButton.hide();
            mUI.add(mBigPlayButton);

            float x = GameConfig.TimeDisplay.POS_X_DELTA + GameConfig.TimeDisplay.ICON_SIZE;
            mDigitFactory.makeDigitObjects(GameConfig.TimeDisplay.DIGIT_COUNT, GameConfig.TYPE_DECOR,
                    mRenderer.getRelativePos(GameConfig.TimeDisplay.POS_X_REL, x),
                    mRenderer.getRelativePos(GameConfig.TimeDisplay.POS_Y_REL_TV,
                            GameConfig.TimeDisplay.POS_Y_DELTA_TV),
                    GameConfig.TimeDisplay.DIGIT_SIZE,
                    GameConfig.TimeDisplay.DIGIT_SPACING, mTimeDigitObj);

            mPauseCurtain = mObjectFactory.makePauseCurtain();
            mPauseCurtain.hide();
        } else {
            mScoreBarObj = mObjectFactory.makeScoreBar();
            mScoreBarLabelObj = mObjectFactory.makeScoreBarLabel();
            mDigitFactory.makeDigitObjects(GameConfig.ScoreDisplay.DIGIT_COUNT, GameConfig.TYPE_DECOR,
                    mRenderer.getRelativePos(GameConfig.ScoreDisplay.POS_X_REL,
                            GameConfig.ScoreDisplay.POS_X_DELTA),
                    mRenderer.getRelativePos(GameConfig.ScoreDisplay.POS_Y_REL,
                            GameConfig.ScoreDisplay.POS_Y_DELTA),
                    GameConfig.ScoreDisplay.DIGIT_SIZE,
                    GameConfig.ScoreDisplay.DIGIT_SPACING, mScoreDigitObj);
            hideObjects(mScoreDigitObj);
            bringObjectsToFront(getScoreDigitVisibleObj());
            float x = GameConfig.TimeDisplay.POS_X_DELTA + GameConfig.TimeDisplay.ICON_SIZE;
            mDigitFactory.makeDigitObjects(GameConfig.TimeDisplay.DIGIT_COUNT, GameConfig.TYPE_DECOR,
                    mRenderer.getRelativePos(GameConfig.TimeDisplay.POS_X_REL, x),
                    mRenderer.getRelativePos(GameConfig.TimeDisplay.POS_Y_REL,
                            GameConfig.TimeDisplay.POS_Y_DELTA),
                    GameConfig.TimeDisplay.DIGIT_SIZE,
                    GameConfig.TimeDisplay.DIGIT_SPACING, mTimeDigitObj);
            mCountdownDigitObj = mDigitFactory.makeDigitObject(
                    GameConfig.TYPE_DECOR, 0.0f, 0.0f, GameConfig.Countdown.DIGIT_SIZE);
            mCountdownDigitObj.hide();
            mQuitButton = mObjectFactory.makeQuitButton(this, MSG_QUIT);
            mQuitBarLabel = mObjectFactory.makeQuitBarLabel();
            mQuitButton.hide();
            mQuitBarLabel.hide();
            mUI.add(mQuitButton);

            mPauseButton = mObjectFactory.makePauseButton(this, MSG_PAUSE);
            mUI.add(mPauseButton);

            mSpeakerMuteButton = mObjectFactory.makeSpeakerMuteButton(this, MSG_UNMUTE);
            mSpeakerOnButton = mObjectFactory.makeSpeakerOnButton(this, MSG_MUTE);
            mUI.add(mSpeakerMuteButton);
            mUI.add(mSpeakerOnButton);
            mPauseCurtain = mObjectFactory.makePauseCurtain();
            mPauseCurtain.hide();

            mBigPlayButton = mObjectFactory.makeBigPlayButton(this, MSG_RESUME);
            mBigPlayButton.hide();
            mUI.add(mBigPlayButton);
            mBigStartGameButton = mObjectFactory.makeBigPlayButton(this, MSG_START);
            mBigStartGameButton.hide();
            mUI.add(mBigStartGameButton);
        }

        SoundManager soundManager = SceneManager.getInstance().getSoundManager();
        SceneManager.getInstance().loadMute();
        if(soundManager != null) {
            soundManager.requestBackgroundMusic(getBgmAssetFile());
            mGameOverSfx = soundManager.requestSfx(R.raw.jetpack_gameover);
            if(soundManager.getMute()) {
                muteSpeaker();
            } else {
                unmuteSpeaker();
            }
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

        if (mConfirmKeyPending) {
            // TODO: move a focus based on KeyEvent
            processCenterKey();
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
        if (mGameEnded || (mPaused && mIsTv)) {
            quitGame();
        } else if (mPaused) {
            unpauseGame();
        } else {
            pauseGame();
        }
    }

    private void processCenterKey() {
        final long currTime = System.currentTimeMillis();
        if (currTime - mConfirmKeyEventTime < CENTER_KEY_DELAY_MS) {
            mBigPlayButton.setPressed(true);
        } else {
            mConfirmKeyPending = false;
            mBigPlayButton.setPressed(false);
            if (mPaused) {
                unpauseGame();
            } else if (mGameEnded) {
                //re-start new game
                SceneManager.getInstance().requestNewScene(makeNewScene());
            }
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
        bringObjectsToFront(getScoreDigitVisibleObj());
    }

    protected GameObject[] getScoreDigitVisibleObj() {
        int numDigits = Math.max(String.valueOf((int)mDisplayedScore.getValue()+1).length(),
                GameConfig.ScoreBar.MIN_DIGITS_VISIBLE);
        GameObject[] mScoreDigitVisibleObj = new GameObject[numDigits];
        for(int i = mScoreDigitObj.length - numDigits; i < mScoreDigitObj.length; i++) {
            mScoreDigitVisibleObj[i - (mScoreDigitObj.length - numDigits)] = mScoreDigitObj[i];
        }
        return mScoreDigitVisibleObj;
    }

    protected void endGame() {
        mGameEnded = true;
        // show the podium object
        mObjectFactory.makePodium();

        // move score to final position
        float x = mRenderer.getRelativePos(GameConfig.Podium.ScoreDisplay.X_REL,
                GameConfig.Podium.ScoreDisplay.X_DELTA);
        float y = mRenderer.getRelativePos(GameConfig.Podium.ScoreDisplay.Y_REL,
                GameConfig.Podium.ScoreDisplay.Y_DELTA);
        displaceObjectsTo(mScoreDigitObj, x, y);
        bringObjectsToFront(getScoreDigitVisibleObj());

        // hide time counter
        hideObjects(mTimeDigitObj);

        // make the "your score is" label
        mObjectFactory.makeScoreLabel();

        // create the end of game UI and add the "play again" button to it
        mUI.add(mObjectFactory.makeReturnToMapButton(this, MSG_RETURN_WITH_VALUE));

        if (isTv()) {
            //TODO: tv specific ui layout

        } else {
            // hide the score bar
            mScoreBarObj.hide();
            mScoreBarLabelObj.hide();
            mPauseButton.hide();
            mSpeakerMuteButton.hide();
            mSpeakerOnButton.hide();
            // TODO: real message

            // create the sign in bar and sign in button
            if (!mSignedIn) {
                mSignInBarObj = mObjectFactory.makeSignInBar();
                mSignInTextObj = mObjectFactory.makeSignInText();
                mUI.add(mSignInButton = mObjectFactory.makeSignInButton(this, MSG_SIGN_IN));
            }
        }
        
        // disable the background music
        SceneManager.getInstance().getSoundManager().enableBgm(false);

        // play the game over sfx
        SceneManager.getInstance().getSoundManager().playSfx(mGameOverSfx);
    }

    protected boolean isTv() {
        return mIsTv;
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
            objs[i].show();
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

    protected void startGameScreen() {
        mPaused = true;
        SceneManager.getInstance().getSoundManager().stopSound();
        if (isTv()) {
            mPauseCurtain.show();
            mPauseCurtain.bringToFront();

            mBigPlayButton.show();
            mBigPlayButton.bringToFront();
        } else {
            mPauseButton.hide();

            mPauseCurtain.show();
            mPauseCurtain.bringToFront();

            mQuitButton.hide();
            mQuitBarLabel.hide();
            mSpeakerOnButton.bringToFront();
            mSpeakerMuteButton.bringToFront();
            mSpeakerMuteButton.setEnabled(true);
            mSpeakerOnButton.setEnabled(true);
            mBigStartGameButton.show();
            mBigStartGameButton.bringToFront();
        }

        if (Utils.hasKitKat() && SceneManager.getInstance().getActivity() != null) {
            SceneManager.getInstance().getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ImmersiveModeHelper.setImmersiveStickyWithActionBar(
                            SceneManager.getInstance().getActivity().getWindow());
                }
            });
        }
    }

    protected void pauseGame() {
        if (!mPaused && !mGameEnded) {
            mPaused = true;
            SceneManager.getInstance().getSoundManager().stopSound();

            if (isTv()) {
                mPauseCurtain.show();
                mPauseCurtain.bringToFront();

                mBigPlayButton.show();
                mBigPlayButton.bringToFront();
            } else {
                mPauseButton.hide();

                mPauseCurtain.show();
                mPauseCurtain.bringToFront();

                mBigPlayButton.show();
                mBigPlayButton.bringToFront();

                mQuitButton.show();
                mQuitBarLabel.show();
                mQuitButton.bringToFront();
                mQuitBarLabel.bringToFront();

                mSpeakerMuteButton.setEnabled(false);
                mSpeakerOnButton.setEnabled(false);
            }

            if (Utils.hasKitKat() && SceneManager.getInstance().getActivity() != null) {
                SceneManager.getInstance().getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Activity activity = SceneManager.getInstance().getActivity();
                        if (activity != null) {
                            ImmersiveModeHelper.setImmersiveStickyWithActionBar(
                                    activity.getWindow());
                        }
                    }
                });
            }
        }
    }

    protected void muteSpeaker() {
        if(!mGameEnded) {
            SceneManager.getInstance().getSoundManager().mute();
            mSpeakerOnButton.hide();
            mSpeakerMuteButton.show();
            mSpeakerMuteButton.bringToFront();
        }
    }

    protected void unmuteSpeaker() {
        if(!mGameEnded) {
            SceneManager.getInstance().getSoundManager().unmute();
            mSpeakerMuteButton.hide();
            mSpeakerOnButton.show();
            mSpeakerOnButton.bringToFront();
        }
    }

    protected void startCountdown() {
        mInStartCountdown = true;
        mStartCountdownTimeRemaining = GameConfig.Countdown.TIME;
        mBigStartGameButton.hide();
        mDigitFactory.setDigit(mCountdownDigitObj, GameConfig.Countdown.TIME);
        mCountdownDigitObj.show();
        mCountdownDigitObj.bringToFront();
    }

    protected void unpauseGame() {
        if (!mPaused) {
            return;
        }
        mPaused = false;
        SceneManager.getInstance().getSoundManager().enableBgm(true);
        SceneManager.getInstance().getSoundManager().resumeSound();
        if (isTv()) {
            mPauseCurtain.hide();
            mBigPlayButton.hide();
        } else {
            mPauseButton.show();
            mPauseCurtain.hide();
            mQuitButton.hide();
            mQuitBarLabel.hide();
            mBigPlayButton.hide();
            mCountdownDigitObj.hide();
            mSpeakerMuteButton.setEnabled(true);
            mSpeakerOnButton.setEnabled(true);
        }

        if (Utils.hasKitKat() && SceneManager.getInstance().getActivity() != null) {
            SceneManager.getInstance().getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ImmersiveModeHelper.setImmersiveSticky(
                            SceneManager.getInstance().getActivity().getWindow());
                }
            });
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

        switch (message) {
            case MSG_RETURN_WITH_VALUE:
                returnWithScore();
                break;
            case MSG_REPLAY:
                SceneManager.getInstance().requestNewScene(makeNewScene());
                break;
            case MSG_SIGN_IN:
                act = (SceneActivity) SceneManager.getInstance().getActivity();
                if (act != null) {
                    // start sign in flow
                    act.beginUserInitiatedSignIn();
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
            case MSG_SHARE:
                share();
                break;
            case MSG_MUTE:
                muteSpeaker();
                break;
            case MSG_UNMUTE:
                unmuteSpeaker();
                break;
            case MSG_START:
                startCountdown();
                break;
            case MSG_GO_TO_END_GAME:
                goToEndGameWithDelay(GameConfig.EndGame.DELAY);
                break;
        }
    }

    private void quitGame() {
        Activity act = SceneManager.getInstance().getActivity();
        if (act != null && act instanceof SceneActivity) {
            ((SceneActivity) act).postQuitGame();
        }
    }

    private void returnWithScore() {
        Activity act = SceneManager.getInstance().getActivity();
        if (act != null && act instanceof SceneActivity) {
            ((SceneActivity) act).postReturnWithScore(mScore);
        }
    }

    protected void goToEndGameWithDelay(int delay) {
        Activity act = SceneManager.getInstance().getActivity();
        ((SceneActivity)act).setGameEndedListener(this);
        if (act != null && act instanceof SceneActivity) {
            ((SceneActivity) act).postDelayedGoToEndGame(delay);
        }
    }

    protected void goToEndGame() {
        Activity act = SceneManager.getInstance().getActivity();
        if (act != null && act instanceof SceneActivity) {
            ((SceneActivity)act).setGameEndedListener(this);
            ((SceneActivity) act).postGoToEndGame();
        }
    }

    @Override
    public void onGameEnded() {
        mGameEnded = true;
    }

    @Override
    public int getScore() {
        return mScore;
    }

    private void share() {
        Activity act = SceneManager.getInstance().getActivity();
        if (act != null && act instanceof SceneActivity) {
            ((SceneActivity) act).share();
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

    // Caution: Called from the UI thread!
    public boolean onConfirmKeyPressed() {
        // raise a flag and process later (on the game thread)
        if (mConfirmKeyPending) {
            return true;
        }

        mConfirmKeyPending = true;
        mConfirmKeyEventTime = System.currentTimeMillis();
        return true;
    }

}
