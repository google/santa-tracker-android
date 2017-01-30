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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.os.Handler;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.EndOfGameView;
import com.google.android.apps.santatracker.games.common.PlayGamesActivity;
import com.google.android.apps.santatracker.games.simpleengine.Scene;
import com.google.android.apps.santatracker.games.simpleengine.SceneManager;
import com.google.android.apps.santatracker.invites.AppInvitesFragment;

import static com.google.android.apps.santatracker.games.jetpack.JetpackActivity.JETPACK_SCORE;

public abstract class SceneActivity extends PlayGamesActivity {

    private AppInvitesFragment mInvitesFragment;
    private boolean mIsEnding = false;
    private GameEndedListener mGameEndedListener;

    protected abstract Scene getGameScene();

    public SceneActivity(int layoutId, Class<?> backClass) {
        super(layoutId, backClass);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            SceneManager.getInstance().enableDebugLog(getResources().getBoolean(
                    R.bool.debug_logs_enabled));
            SceneManager.getInstance().requestNewScene(getGameScene());
        }

        mInvitesFragment = AppInvitesFragment.getInstance(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        SceneManager.getInstance().onFocusChanged(hasFocus);
    }

    @Override
    public void onPause() {
        super.onPause();
        SceneManager.getInstance().onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        SceneManager.getInstance().onResume(this);
        if(SceneManager.getInstance().getCurrentScene() != null &&
                SceneManager.getInstance().getCurrentScene().isGameEnded()) {
            postGoToEndGame();
        }
    }

    @Override
    public void onSignInFailed() {
        super.onSignInFailed();

        // communicate to the BaseScene that we are no longer signed in
        Scene s = SceneManager.getInstance().getCurrentScene();
        if (s instanceof BaseScene) {
            ((BaseScene) s).setSignedIn(false);
        }
    }

    @Override
    public void onSignInSucceeded() {
        super.onSignInSucceeded();

        // communicate to the BaseScene that we are no longer signed in
        Scene s = SceneManager.getInstance().getCurrentScene();
        if (s instanceof BaseScene) {
            ((BaseScene) s).setSignedIn(true);
        }
    }

    @Override
    protected void launchStartupActivity() {
        SceneManager.getInstance().saveMute();
        finish();
    }

    public void postQuitGame() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                launchStartupActivity();
            }
        });
    }

    public void postReturnWithScore(final int score) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                returnWithScore(score);
            }
        });
    }


    public void returnWithScore(int score) {
        Intent intent = this.getIntent();
        intent.putExtra(JETPACK_SCORE, score);
        this.setResult(RESULT_OK, intent);
        finish();
    }

    public void setGameEndedListener(GameEndedListener gameEndedListener) {
        mGameEndedListener = gameEndedListener;
    }

    public void postDelayedGoToEndGame(final int delay) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mGameEndedListener != null) {
                            int score = mGameEndedListener.getScore();
                            mGameEndedListener.onGameEnded();
                            goToEndGame(score);
                        }
                    }
                }, delay);
            }
        });
    }

    public void postGoToEndGame() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mGameEndedListener != null) {
                    int score = mGameEndedListener.getScore();
                    mGameEndedListener.onGameEnded();
                    goToEndGame(score);
                }
            }
        });
    }

    private void goToEndGame(final int score) {
        // Prevent multiple calls
        if (mIsEnding) {
            return;
        }
        mIsEnding = true;
        // Show the end-game view
        EndOfGameView gameView = (EndOfGameView) findViewById(R.id.view_end_game);
        gameView.initialize(score, null /* no replay */, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnWithScore(score);
            }
        });
        gameView.setVisibility(View.VISIBLE);
    }

    public void share() {
        mInvitesFragment.sendGenericInvite();
    }
}
