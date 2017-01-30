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

import android.app.Activity;
import android.view.KeyEvent;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.gamebase.BaseScene;
import com.google.android.apps.santatracker.games.gamebase.SceneActivity;
import com.google.android.apps.santatracker.games.simpleengine.Logger;
import com.google.android.apps.santatracker.games.simpleengine.SceneManager;
import com.google.android.apps.santatracker.games.simpleengine.SmoothValue;
import com.google.android.apps.santatracker.games.simpleengine.SoundManager;
import com.google.android.apps.santatracker.games.simpleengine.game.GameObject;

import java.util.ArrayList;
import java.util.HashSet;

public final class JetpackScene extends BaseScene {
    private static final String TAG = "JetpackScene";

    // GameObject types:
    static final int TYPE_PLAYER = 0;
    static final int TYPE_GOOD_ITEM = 1;
    static final int TYPE_BAD_ITEM = 2;

    // player
    GameObject mPlayerObj;

    // background
    GameObject mBackground;

    // our object factory
    JetpackObjectFactory mFactory;

    // current difficulty level
    int mLevel = 1;

    // player is currently injured
    boolean mPlayerHit = false;

    // time remaining of player's injury
    float mPlayerHitTime = JetpackConfig.Time.HIT_TIME;

    // total items collected
    int mItemsCollected = 0;

    // item fall speed multipler (increases with level)
    float mFallMult = 3.0f;

    // score multipler (increases with level)
    float mScoreMult = 1.0f;

    SmoothValue mSpriteAngle = new SmoothValue(0.0f,
            JetpackConfig.Player.SpriteAngle.MAX_CHANGE_RATE,
            -JetpackConfig.Player.SpriteAngle.MAX_ANGLE,
            JetpackConfig.Player.SpriteAngle.MAX_ANGLE,
            JetpackConfig.Player.SpriteAngle.FILTER_SAMPLES);

    float mPlayerTargetX = 0.0f;
    float mPlayerTargetY = 0.0f;
    
    // working array
    ArrayList<GameObject> mTmpList = new ArrayList<GameObject>();

    // how long til we spawn the next item?
    float mSpawnCountdown = JetpackConfig.Items.SPAWN_INTERVAL;

    // cloud sprites
    GameObject[] mCloudObj = new GameObject[JetpackConfig.Clouds.COUNT];

    // time remaining
    float mTimeRemaining = JetpackConfig.Time.INITIAL;

    // sfx IDs
    int[] mItemSfxSuccess = null;

    // current combo
    private class Combo {

        int items = 0;
        float countdown = 0.0f;
        float centroidX, centroidY;
        float points;
        float timeRecovery;

        void reset() {
            items = 0;
            countdown = centroidX = centroidY = points = timeRecovery = 0.0f;
        }
    }

    private Combo mCombo = new Combo();

    // set of achievements we know we unlocked (to prevent repeated API calls)
    private HashSet<Integer> mUnlockedAchievements = new HashSet<Integer>();

    // achievement increments we are pending to send
    private int mAchPendingPresents = 0;
    private int mAchPendingCandy = 0;
    private float mAchPendingSeconds = 0;

    // countdown to next sending of incremental achievements
    private float mIncAchCountdown = JetpackConfig.Achievements.INC_ACH_SEND_INTERVAL;

    // what pointer Id is the one that's steering the elf
    private int mActivePointerId = -1;

    // accumulated play time
    private float mPlayTime = 0.0f;

    @Override
    protected String getBgmAssetFile() {
        return JetpackConfig.BGM_ASSET_FILE;
    }

    @Override
    protected float getDisplayedTime() {
        return mTimeRemaining;
    }

    @Override
    protected BaseScene makeNewScene() {
        return new JetpackScene();
    }

    @Override
    public void onInstall() {
        super.onInstall();
        mFactory = new JetpackObjectFactory(mRenderer, mWorld);
        mFactory.requestTextures();
        mBackground = mFactory.makeBackground();
        mBackground.sendToBack();
        // Make the game shorter when debugging
        Activity activity = SceneManager.getInstance().getActivity();
        if (activity != null && activity.getPackageName().contains("debug")) {
            mTimeRemaining = 10.0f;
        }

        mPlayerObj = mFactory.makePlayer();
        mPlayerTargetX = 0.0f;
        mPlayerTargetY = mRenderer.getBottom() + JetpackConfig.Player.VERT_MOVEMENT_MARGIN;
        SoundManager soundManager = SceneManager.getInstance().getSoundManager();
        mItemSfxSuccess = new int[3];
        mItemSfxSuccess[0] = soundManager.requestSfx(R.raw.jetpack_score1);
        mItemSfxSuccess[1] = soundManager.requestSfx(R.raw.jetpack_score2);
        mItemSfxSuccess[2] = soundManager.requestSfx(R.raw.jetpack_score3);
        SceneManager.getInstance().getVibrator();
        // start paused
        startGameScreen();
    }

    @Override
    public void onUninstall() {
        super.onUninstall();
    }

    @Override
    public void doStandbyFrame(float deltaT) {
        super.doStandbyFrame(deltaT);
    }

    @Override
    public boolean isGameEnded() {
        return mGameEnded;
    }

    @Override
    public void doFrame(float deltaT) {
        if(!mPaused) {
            if (!mGameEnded) {
                mPlayTime += deltaT;
                updatePlayer(deltaT);
                if (!mPlayerHit) {
                    detectCollectedItems();
                }
                updatePlayerHit(deltaT);
                updateTimeRemaining(deltaT);
                updateCombo(deltaT);
                checkLevelUp();
                mAchPendingSeconds += deltaT;
            }
            updateClouds();
            updateCandy(deltaT);
            killMissedPresents();

            mIncAchCountdown -= deltaT;
            sendIncrementalAchievements(false);

            if (!mIsInGameEndingTransition && (mSpawnCountdown -= deltaT) < 0.0f) {
                mSpawnCountdown = JetpackConfig.Items.SPAWN_INTERVAL;
                mFactory.makeRandomItem(mFallMult, SceneManager.getInstance()
                        .getLargePresentMode(), mDisplayedScore.getValue());
            }
            super.doFrame(deltaT);
        }
        if(mInStartCountdown) {
            updatePlayer(deltaT);
            float newCount = mStartCountdownTimeRemaining - (deltaT);
            if(newCount <= 0) {
                mInStartCountdown = false;
                unpauseGame();
            } else if((int) newCount < (int) mStartCountdownTimeRemaining) {
                mDigitFactory.setDigit(mCountdownDigitObj, Math.min((int) newCount + 1, 3));
                mCountdownDigitObj.bringToFront();
            }
            mStartCountdownTimeRemaining = newCount;
        }
        if(mGameEnded) {
            goToEndGame();
        }
    }

    protected void endGame() {
        mIsInGameEndingTransition = true;

        // force send all incremental achievements
        sendIncrementalAchievements(true);

        // submit our score
        submitScore(JetpackConfig.LEADERBOARD, mScore);

        // Start end game activity
        onWidgetTriggered(MSG_GO_TO_END_GAME);
    }

    private void updateTimeRemaining(float deltaT) {
        mTimeRemaining -= deltaT;
        if (mTimeRemaining < 0.0f && !mIsInGameEndingTransition) {
            endGame();
        }
    }

    private void updatePlayerHit(float deltaT) {
        mPlayerHitTime -= deltaT;
        if(mPlayerHitTime < 0.0f && mPlayerHit) {
            mFactory.recoverPlayerHit(mPlayerObj);
            mPlayerHit = false;
        }
    }

    private float sineWave(float period, float amplitude, float t) {
        return (float) Math.sin(2 * Math.PI * t / period) * amplitude;
    }

    private void updatePlayer(float deltaT) {
        mSpriteAngle.setTarget(
                (mPlayerObj.x - mPlayerTargetX) * JetpackConfig.Player.SpriteAngle.ANGLE_CONST);
        mSpriteAngle.update(deltaT);
        mPlayerObj.getSprite(0).rotation = mSpriteAngle.getValue();
        mPlayerObj.getSprite(1).rotation = mSpriteAngle.getValue();
        mPlayerObj.getSprite(1).width = JetpackConfig.Player.Fire.WIDTH *
                (1.0f + sineWave(JetpackConfig.Player.Fire.ANIM_PERIOD,
                        JetpackConfig.Player.Fire.ANIM_AMPLITUDE, mPlayTime));
        mPlayerObj.getSprite(1).height = Float.NaN; // proportional to width

        if (isTv()) {
            // On TV, player moves based on its speed.
            mPlayerObj.displaceBy(mPlayerObj.velX * deltaT, mPlayerObj.velY * deltaT);
        } else {
            mPlayerObj.displaceTowards(mPlayerTargetX, mPlayerTargetY, deltaT *
                    JetpackConfig.Player.MAX_SPEED);
        }
    }

    private void updateClouds() {
        int i;
        for (i = 0; i < mCloudObj.length; i++) {
            GameObject o = mCloudObj[i];
            if (o == null) {
                o = mFactory.makeCloud();
                mCloudObj[i] = o;
                setupNewCloud(o);
            } else if (o.y < JetpackConfig.Clouds.DELETE_Y) {
                setupNewCloud(o);
            }
        }
    }

    private void updateCombo(float deltaT) {
        if (mCombo.items > 0 && (mCombo.countdown -= deltaT) <= 0.0f) {
            endCombo();
        }
    }

    private boolean isCandy(GameObject o) {
        return o.type == TYPE_GOOD_ITEM &&
                o.ivar[JetpackConfig.Items.IVAR_TYPE] == JetpackObjectFactory.ITEM_CANDY;
    }

    private boolean isPresent(GameObject o) {
        return o.type == TYPE_GOOD_ITEM &&
                o.ivar[JetpackConfig.Items.IVAR_TYPE] == JetpackObjectFactory.ITEM_PRESENT;
    }

    private boolean isCoal(GameObject o) {
        return o.type == TYPE_BAD_ITEM &&
                o.ivar[JetpackConfig.Items.IVAR_TYPE] == JetpackObjectFactory.ITEM_COAL;
    }

    private void updateCandy(float deltaT) {
        int i;
        for (i = 0; i < mWorld.gameObjects.size(); i++) {
            GameObject o = mWorld.gameObjects.get(i);
            if (isCandy(o)) {
                o.getSprite(0).rotation += deltaT * JetpackConfig.Items.CANDY_ROTATE_SPEED;
            }
        }
    }

    private void setupNewCloud(GameObject o) {
        o.displaceTo(mRenderer.getLeft() + mRandom.nextFloat() * (mRenderer.getRight() -
                mRenderer.getLeft()), JetpackConfig.Clouds.SPAWN_Y);
        o.velY = -(JetpackConfig.Clouds.SPEED_MIN + mRandom.nextFloat() *
                (JetpackConfig.Clouds.SPEED_MAX - JetpackConfig.Clouds.SPEED_MIN));
    }

    private void killMissedPresents() {
        int i;
        for (i = 0; i < mWorld.gameObjects.size(); i++) {
            GameObject o = mWorld.gameObjects.get(i);
            if ((o.type == TYPE_GOOD_ITEM || o.type == TYPE_BAD_ITEM) && o.y < JetpackConfig.Items.DELETE_Y) {
                o.dead = true;
            }
        }
    }

    private void killAllItems() {
        int i;
        for (i = 0; i < mWorld.gameObjects.size(); i++) {
            GameObject o = mWorld.gameObjects.get(i);
            if (o.type == TYPE_GOOD_ITEM || o.type == TYPE_BAD_ITEM) {
                o.dead = true;
            }
        }
    }

    private int roundScore(int score) {
        score = (score / 50) * 50;
        return score < 50 ? 50 : score;
    }

    private void addScore(float score) {
        mScore += score;
        unlockScoreBasedAchievements();
    }

    private void addTime(float time) {
        mTimeRemaining += time;
        if (mTimeRemaining > JetpackConfig.Time.MAX) {
            mTimeRemaining = JetpackConfig.Time.MAX;
        }
    }

    private void pickUpItem(GameObject item) {
        int value = item.ivar[JetpackConfig.Items.IVAR_BASE_VALUE];
        if (isCandy(item)) {
            mAchPendingCandy++;
            mObjectFactory.makeScorePopup(item.x, item.y, value, mDigitFactory);
            SceneManager.getInstance().getSoundManager().playSfx(
                    mItemSfxSuccess[mRandom.nextInt(mItemSfxSuccess.length)]);
        } else if (isPresent(item)) {
            mAchPendingPresents++;
            mObjectFactory.makeScorePopup(item.x, item.y, value, mDigitFactory);
            SceneManager.getInstance().getSoundManager().playSfx(
                    mItemSfxSuccess[mRandom.nextInt(mItemSfxSuccess.length)]);
        } else if (isCoal(item)) {
            mFactory.makePlayerHit(mPlayerObj);
            mPlayerHit = true;
            mPlayerHitTime = JetpackConfig.Time.HIT_TIME;
            mObjectFactory.makeScorePopup(item.x, item.y, value, mDigitFactory);
            SceneManager.getInstance().getSoundManager().playSfx(
                    mItemSfxSuccess[mRandom.nextInt(mItemSfxSuccess.length)]);
            SceneManager.getInstance().getVibrator().vibrate(JetpackConfig.Time.VIBRATE_SMALL);
        }
        item.dead = true;
        mItemsCollected++;

        addScore(value);

        // play sfx

    }

    private void detectCollectedItems() {
        mWorld.detectCollisions(mPlayerObj, mTmpList, true);
        int i;
        for (i = 0; i < mTmpList.size(); i++) {
            GameObject o = mTmpList.get(i);
            if (o.type == TYPE_GOOD_ITEM || o.type == TYPE_BAD_ITEM) {
                pickUpItem(o);
            }
        }
    }

    private void endCombo() {
        if (mCombo.items > 1) {
            mFactory.makeComboPopup(mCombo.items, mCombo.centroidX, mCombo.centroidY);

            // give bonus
            addScore(mCombo.points * mCombo.items);
            addTime(mCombo.timeRecovery * mCombo.items);

            // unlock combo-based achievements
            unlockComboBasedAchievements(mCombo.items);
        }
        mCombo.reset();
    }

    @Override
    public void onPointerDown(int pointerId, float x, float y) {
        super.onPointerDown(pointerId, x, y);
        if (mActivePointerId < 0) {
            mActivePointerId = pointerId;
        }
    }

    @Override
    public void onPointerUp(int pointerId, float x, float y) {
        super.onPointerUp(pointerId, x, y);
        if (mActivePointerId == pointerId) {
            mActivePointerId = -1;
        }
    }

    @Override
    public void onPointerMove(int pointerId, float x, float y, float deltaX, float deltaY) {
        super.onPointerMove(pointerId, x, y, deltaX, deltaY);

        // if paused, do nothing.
        if (mPaused) {
            return;
        }

        // if no finger owns the steering of the elf, adopt this one.
        if (mActivePointerId < 0) {
            mActivePointerId = pointerId;
        }
    }

    @Override
    public void onKeyDown(int keyCode, int repeatCount) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
                onConfirmKeyPressed();
                break;
            case KeyEvent.KEYCODE_BUTTON_B:
                onBackKeyPressed();
                break;
        }

        if (!mPaused) {
            float absVelocity = JetpackConfig.Player.WIDTH
                    * (1.5f + (float) repeatCount * JetpackConfig.Input.KEY_SENSIVITY);
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    mPlayerTargetY = Integer.MAX_VALUE;
                    mPlayerObj.velY = absVelocity;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    mPlayerTargetY = Integer.MIN_VALUE;
                    mPlayerObj.velY = -absVelocity;
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mPlayerTargetX = Integer.MIN_VALUE;
                    mPlayerObj.velX = -absVelocity;
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mPlayerTargetX = Integer.MAX_VALUE;
                    mPlayerObj.velX = absVelocity;
                    break;
            }
            // don't let the player wander off screen
            limitPlayerMovement();
        }
    }

    @Override
    public void onKeyUp(int keyCode) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                // if it's going up, stop it.
                mPlayerTargetY = mPlayerObj.y;
                if (mPlayerObj.velY > 0) {
                    mPlayerObj.velY = 0;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // if it's going down, stop it.
                mPlayerTargetY = mPlayerObj.y;
                if (mPlayerObj.velY < 0) {
                    mPlayerObj.velY = 0;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                // if it's going left, stop it.
                mPlayerTargetX = mPlayerObj.x;
                if (mPlayerObj.velX < 0) {
                    mPlayerObj.velX = 0;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // if it's going right, stop it.
                mPlayerTargetX = mPlayerObj.x;
                if (mPlayerObj.velX > 0) {
                    mPlayerObj.velX = 0;
                }
                break;
        }

        // don't let the player wander off screen
        limitPlayerMovement();
    }

    @Override
    public void onSensorChanged(float x, float y, int accuracy) {
        mPlayerTargetX += JetpackConfig.Input.Sensor.transformX(x);
        // don't let the player wander off screen
        limitPlayerMovement();
    }

    private void limitPlayerMovement() {
        float minX = mRenderer.getLeft() + JetpackConfig.Player.HORIZ_MOVEMENT_MARGIN;
        float maxX = mRenderer.getRight() - JetpackConfig.Player.HORIZ_MOVEMENT_MARGIN;
        float minY = mRenderer.getBottom() + JetpackConfig.Player.VERT_MOVEMENT_MARGIN;
        float maxY = mRenderer.getTop() - JetpackConfig.Player.VERT_MOVEMENT_MARGIN;

        if (isTv()) {
            mPlayerObj.velX = mPlayerObj.x + mPlayerObj.velX < minX ? minX - mPlayerObj.x :
                    mPlayerObj.x + mPlayerObj.velX > maxX ? maxX - mPlayerObj.x : mPlayerObj.velX;

            mPlayerObj.velY = mPlayerObj.y + mPlayerObj.velY < minY ? minY - mPlayerObj.y :
                    mPlayerObj.y + mPlayerObj.velY > maxY ? maxY - mPlayerObj.y : mPlayerObj.velY;
        } else {
            mPlayerTargetX = mPlayerTargetX < minX ? minX :
                    mPlayerTargetX > maxX ? maxX : mPlayerTargetX;
            mPlayerTargetY = mPlayerTargetY < minY ? minY :
                    mPlayerTargetY > maxY ? maxY : mPlayerTargetY;
        }
    }


    private void checkLevelUp() {
        int dueLevel = mItemsCollected / JetpackConfig.Progression.ITEMS_PER_LEVEL;
        while (mLevel < dueLevel) {
            mLevel++;
            Logger.d("Level up! Now at level " + mLevel);
            mFallMult *= JetpackConfig.Items.FALL_SPEED_LEVEL_MULT;
            mScoreMult *= JetpackConfig.Progression.SCORE_LEVEL_MULT;
        }
    }

    private void unlockScoreBasedAchievements() {
        int i;
        for (i = 0; i < JetpackConfig.Achievements.SCORE_ACHS.length; i++) {
            if (mScore >= JetpackConfig.Achievements.SCORE_FOR_ACH[i]) {
                unlockAchievement(JetpackConfig.Achievements.SCORE_ACHS[i]);
            }
        }
    }

    private void unlockComboBasedAchievements(int comboSize) {
        int i;
        for (i = 0; i < JetpackConfig.Achievements.COMBO_ACHS.length; i++) {
            // COMBO_ACHS[n] is the achievement to unlock for a combo of size n + 2
            if (comboSize >= i + 2) {
                unlockAchievement(JetpackConfig.Achievements.COMBO_ACHS[i]);
            }
        }
    }

    private void sendIncrementalAchievements(boolean force) {
        if (!force && mIncAchCountdown > 0.0f) {
            // it's not time to send yet
            return;
        }
        if (SceneManager.getInstance().getActivity() == null) {
            // no Activity (maybe we're in the background), so can't send yet
            return;
        }

        if (mAchPendingCandy > 0) {
            incrementAchievements(JetpackConfig.Achievements.TOTAL_CANDY_ACHS, mAchPendingCandy);
            mAchPendingCandy = 0;
        }
        if (mAchPendingPresents > 0) {
            incrementAchievements(JetpackConfig.Achievements.TOTAL_PRESENTS_ACHS,
                    mAchPendingPresents);
            mAchPendingPresents = 0;
        }
        if (mAchPendingSeconds >= 1.0f) {
            int seconds = (int) Math.floor(mAchPendingSeconds);
            incrementAchievements(JetpackConfig.Achievements.TOTAL_TIME_ACHS, seconds);
            mAchPendingSeconds -= seconds;
        }

        // submit score as well, since we're at it.
        submitScore(JetpackConfig.LEADERBOARD, mScore);

        // reset countdown
        mIncAchCountdown = JetpackConfig.Achievements.INC_ACH_SEND_INTERVAL;
    }

    private void unlockAchievement(int resId) {
        SceneActivity act = (SceneActivity) SceneManager.getInstance().getActivity();
        if (!mUnlockedAchievements.contains(resId) && act != null) {
            act.postUnlockAchievement(resId);
            mUnlockedAchievements.add(resId);
        }
    }

    private void incrementAchievements(int[] resId, int steps) {
        for (int i : resId) {
            incrementAchievement(i, steps);
        }
    }

    private void incrementAchievement(int resId, int steps) {
        SceneActivity act = (SceneActivity) SceneManager.getInstance().getActivity();
        if (steps > 0 && act != null) {
            act.postIncrementAchievement(resId, steps);
        }
    }

    private void submitScore(int resId, int score) {
        SceneActivity act = (SceneActivity) SceneManager.getInstance().getActivity();
        if (act != null) {
            act.postSubmitScore(resId, score);
        }
    }
}
