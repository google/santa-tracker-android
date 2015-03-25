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

package com.google.android.apps.santatracker.games.jetpack;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.jetpack.gamebase.BaseScene;
import com.google.android.apps.santatracker.games.jetpack.gamebase.SceneActivity;
import com.google.android.apps.santatracker.games.simpleengine.Logger;
import com.google.android.apps.santatracker.games.simpleengine.SceneManager;
import com.google.android.apps.santatracker.games.simpleengine.SmoothValue;
import com.google.android.apps.santatracker.games.simpleengine.SoundManager;
import com.google.android.apps.santatracker.games.simpleengine.game.GameObject;

import java.util.ArrayList;
import java.util.HashSet;

public final class JetpackScene extends BaseScene {

    // GameObject types:
    static final int TYPE_PLAYER = 0;
    static final int TYPE_ITEM = 1;

    // player
    GameObject mPlayerObj;

    // our object factory
    JetpackObjectFactory mFactory;

    // current difficulty level
    int mLevel = 1;

    // total items collected
    int mItemsCollected = 0;

    // item fall speed multipler (increases with level)
    float mFallMult = 1.0f;

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
    int[] mItemSfx = null;

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

        mPlayerObj = mFactory.makePlayer();

        SoundManager soundManager = SceneManager.getInstance().getSoundManager();
        mItemSfx = new int[3];
        mItemSfx[0] = soundManager.requestSfx(R.raw.jetpack_score1);
        mItemSfx[1] = soundManager.requestSfx(R.raw.jetpack_score2);
        mItemSfx[2] = soundManager.requestSfx(R.raw.jetpack_score3);

        // start paused
        pauseGame();
    }

    @Override
    public void onUninstall() {
        super.onUninstall();
    }

    @Override
    public void doStandbyFrame(float deltaT) {
        super.doStandbyFrame(deltaT);
        mRenderer.setClearColor(JetpackConfig.SKY_COLOR);
    }

    @Override
    public void doFrame(float deltaT) {
        if (mPaused) {
            deltaT = 0;
        }

        if (!mGameEnded) {
            mPlayTime += deltaT;
            updatePlayer(deltaT);
            detectCollectedPresents();
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

        if (!mGameEnded && (mSpawnCountdown -= deltaT) < 0.0f) {
            mSpawnCountdown = JetpackConfig.Items.SPAWN_INTERVAL;
            mFactory.makeRandomItem(mFallMult);
        }

        mRenderer.setClearColor(JetpackConfig.SKY_COLOR);
        super.doFrame(deltaT);
    }

    protected void endGame() {
        super.endGame();

        // hide the player
        mPlayerObj.hide();

        // delete all remaining items
        killAllPresents();

        // force send all incremental achievements
        sendIncrementalAchievements(true);

        // submit our score
        submitScore(JetpackConfig.LEADERBOARD, mScore);
    }

    private void updateTimeRemaining(float deltaT) {
        mTimeRemaining -= deltaT;
        if (mTimeRemaining < 0.0f) {
            endGame();
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
        mPlayerObj.displaceTowards(mPlayerTargetX, mPlayerTargetY, deltaT *
                JetpackConfig.Player.MAX_SPEED);
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
        return o.type == TYPE_ITEM &&
                o.ivar[JetpackConfig.Items.IVAR_TYPE] == JetpackObjectFactory.ITEM_CANDY;
    }

    private boolean isPresent(GameObject o) {
        return o.type == TYPE_ITEM &&
                o.ivar[JetpackConfig.Items.IVAR_TYPE] == JetpackObjectFactory.ITEM_PRESENT;
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
            if (o.type == TYPE_ITEM && o.y < JetpackConfig.Items.DELETE_Y) {
                o.dead = true;
            }
        }
    }

    private void killAllPresents() {
        int i;
        for (i = 0; i < mWorld.gameObjects.size(); i++) {
            GameObject o = mWorld.gameObjects.get(i);
            if (o.type == TYPE_ITEM) {
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
        int baseValue = item.ivar[JetpackConfig.Items.IVAR_BASE_VALUE];
        int value = roundScore((int) (baseValue * mScoreMult));

        if (isCandy(item)) {
            mAchPendingCandy++;
        } else if (isPresent(item)) {
            mAchPendingPresents++;
        }

        mObjectFactory.makeScorePopup(item.x, item.y, value, mDigitFactory);
        float thisX = item.x;
        float thisY = item.y;
        item.dead = true;
        mItemsCollected++;

        float timeRecovery = JetpackConfig.Time.RECOVERED_BY_ITEM -
                mLevel * JetpackConfig.Time.RECOVERED_DECREASE_PER_LEVEL;
        if (timeRecovery < JetpackConfig.Time.RECOVERED_MIN) {
            timeRecovery = JetpackConfig.Time.RECOVERED_MIN;
        }

        // rewards: score and time
        addScore(value);
        addTime(timeRecovery);

        // play sfx
        SceneManager.getInstance().getSoundManager().playSfx(
                mItemSfx[mRandom.nextInt(mItemSfx.length)]);

        // increment combo
        mCombo.centroidX = (mCombo.centroidX * mCombo.items + thisX) / (mCombo.items + 1);
        mCombo.centroidY = (mCombo.centroidY * mCombo.items + thisY) / (mCombo.items + 1);
        mCombo.items++;
        mCombo.countdown = JetpackConfig.Items.COMBO_INTERVAL;
        mCombo.points += value;
        mCombo.timeRecovery += timeRecovery;
    }

    private void detectCollectedPresents() {
        mWorld.detectCollisions(mPlayerObj, mTmpList, true);
        int i;
        for (i = 0; i < mTmpList.size(); i++) {
            GameObject o = mTmpList.get(i);
            if (o.type == TYPE_ITEM) {
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

        // if this finger is the owner of the steering, steer!
        if (mActivePointerId == pointerId) {
            mPlayerTargetX += deltaX * JetpackConfig.Input.TOUCH_SENSIVITY;
            mPlayerTargetY += deltaY * JetpackConfig.Input.TOUCH_SENSIVITY;

            // don't let the player wander off screen
            limitPlayerMovement();
        }
    }

    private void limitPlayerMovement() {
        float minX = mRenderer.getLeft() + JetpackConfig.Player.HORIZ_MOVEMENT_MARGIN;
        float maxX = mRenderer.getRight() - JetpackConfig.Player.HORIZ_MOVEMENT_MARGIN;
        float minY = mRenderer.getBottom() + JetpackConfig.Player.VERT_MOVEMENT_MARGIN;
        float maxY = mRenderer.getTop() - JetpackConfig.Player.VERT_MOVEMENT_MARGIN;
        mPlayerTargetX = mPlayerTargetX < minX ? minX :
                mPlayerTargetX > maxX ? maxX : mPlayerTargetX;
        mPlayerTargetY = mPlayerTargetY < minY ? minY :
                mPlayerTargetY > maxY ? maxY : mPlayerTargetY;
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
