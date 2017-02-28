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

package com.google.android.apps.santatracker.games.common;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

import java.util.HashMap;

public abstract class PlayGamesActivity extends GameActivity {

    // list of achievements we are pending to unlock or increment (waiting for sign in)
    // Key is the achievement ID, value is the number of steps to increment. 0 means
    // unlock rather than increment.
    private HashMap<String, Integer> mAchievementsToSend = new HashMap<String, Integer>();

    // score we are pending to send (waiting for sign in). Hash of leaderboard ID to score.
    private HashMap<String, Long> mScoresToSend = new HashMap<String, Long>();

    // id of the layout to load during  onCreate
    private int mLayoutId;
    protected Class<?> mBackClass;

    public PlayGamesActivity(int layoutId, Class<?> backClass) {
        super();
        mLayoutId = layoutId;
        mBackClass = backClass;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mLayoutId);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        launchStartupActivity();
        return true;
    }

    @Override
    public void onBackPressed() {
        launchStartupActivity();
    }

    protected void launchStartupActivity() {
        finish();
    }

    @Override
    public void onSignInSucceeded() {
        super.onSignInSucceeded();
        tryToSendGameData();
    }

    // Call from any thread
    public void postUnlockAchievement(final int achResId) {
        final String achievementId = getString(achResId);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mAchievementsToSend.containsKey(achievementId)) {
                    mAchievementsToSend.put(achievementId, 0);
                }
                tryToSendGameData();
            }
        });
    }

    // Call from any thread
    public void postIncrementAchievement(final int achResId, final int steps) {
        final String achievementId = getString(achResId);
        if (steps <= 0) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mAchievementsToSend.containsKey(achievementId)) {
                    mAchievementsToSend.put(achievementId, steps);
                } else {
                    mAchievementsToSend.put(achievementId,
                            mAchievementsToSend.get(achievementId) + steps);
                }
                tryToSendGameData();
            }
        });
    }

    // Call from any thread
    public void postSubmitScore(final int lbResId, final long score) {
        final String leaderboardId = getString(lbResId);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mScoresToSend.containsKey(leaderboardId)) {
                    long existingScore = mScoresToSend.get(leaderboardId);
                    if (existingScore >= score) {
                        return;
                    }
                }
                mScoresToSend.put(leaderboardId, score);
                tryToSendGameData();
            }
        });
    }

    // Call from UI thread only.
    private void tryToSendGameData() {
        if (isSignedIn() && getGamesApiClient().isConnected()) {
            GoogleApiClient apiClient = getGamesApiClient();
            for (String achId : mAchievementsToSend.keySet()) {
                int arg = mAchievementsToSend.get(achId);
                if (arg <= 0) {
                    Games.Achievements.unlock(apiClient, achId);
                } else {
                    Games.Achievements.increment(apiClient, achId, arg);
                }
            }
            mAchievementsToSend.clear();

            for (String lbId : mScoresToSend.keySet()) {
                Games.Leaderboards.submitScore(apiClient, lbId,
                        mScoresToSend.get(lbId));
            }
            mScoresToSend.clear();
        }
    }

    public void startSignIn() {
        if (!isSignedIn()) {
            beginUserInitiatedSignIn();
        }
    }
}
