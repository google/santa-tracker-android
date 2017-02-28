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

package com.google.android.apps.santatracker.rocketsleigh;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.santatracker.games.PlayGamesFragment;
import com.google.android.apps.santatracker.games.SignInListener;
import com.google.android.apps.santatracker.invites.AppInvitesFragment;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.text.NumberFormat;


public class EndGameActivity extends FragmentActivity implements SignInListener {

    private PlayGamesFragment mGamesFragment;
    private AppInvitesFragment mInviteFragment;

    private View mSignIn;
    private View mPlayAgain;

    // To handle UI events like KeyDown.
    private Handler mHandler = new Handler();

    private int[] mAchievements = new int[] {
            R.string.achievement_hidden_presents,
            R.string.achievement_rocket_junior_score_10000,
            R.string.achievement_rocket_intermediate_score_30000,
            R.string.achievement_rocket_pro_score_50000,
            R.string.achievement_safe_tapper,
            R.string.achievement_untouchable
    };

    private FirebaseAnalytics mMeasurement;

    private static final int REQUEST_ACHIEVEMENTS = 2001;
    private static final int REQUEST_LEADERBOARD = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rocket_end_game);

        TextView sv = (TextView)findViewById(R.id.score_text);
        final long score = getIntent().getLongExtra("score", 0);
        sv.setText(NumberFormat.getNumberInstance().format(score));

        mSignIn = findViewById(R.id.play_again_gplus);

        // App Invites
        mInviteFragment = AppInvitesFragment.getInstance(this);
        findViewById(R.id.invite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInviteFragment.sendGameInvite(
                        getString(R.string.rocket), "rocketsleigh", (int) score);
            }
        });

        // App Measurement
        mMeasurement = FirebaseAnalytics.getInstance(this);
        MeasurementManager.recordScreenView(mMeasurement,
                getString(R.string.analytics_screen_rocket_endgame));

        if (TvUtil.isTv(this)) {
            mSignIn.setVisibility(View.GONE);
            mPlayAgain = findViewById(R.id.play_again);
            findViewById(R.id.exit).setVisibility(View.GONE);
            findViewById(R.id.invite).setVisibility(View.GONE);
            findViewById(R.id.popup_view).setVisibility(View.GONE);
        } else {
            mGamesFragment = PlayGamesFragment.getInstance(this, this);

            if (mGamesFragment.isSignedIn()) {
                mSignIn.setVisibility(View.GONE);
                updateAchievementsAndLeaderboard();
            } else {
                mSignIn.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseResources();
    }

    private void releaseResources() {
        View scoreScene = findViewById(R.id.bg_finalscore);
        if (scoreScene != null) {
            ((ImageView) scoreScene).setImageDrawable(null);
        }
        View snow = findViewById(R.id.bg_img_finalscore_snowground_blue);
        if (snow != null) {
            ((ImageView) snow).setImageDrawable(null);
        }
    }

    public void onSignIn(View view) {
        mGamesFragment.beginUserInitiatedSignIn();
    }

    public void onPlayAgain(View view) {
        Intent intent = new Intent(this.getApplicationContext(), RocketSleighActivity.class);
        intent.putExtra("nomovie", true);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    public void onLeaderboards(View view) {
        if (mGamesFragment.isSignedIn()) {
            Intent intent = Games.Leaderboards.getLeaderboardIntent(
                    mGamesFragment.getGamesApiClient(), getString(R.string.leaderboard_rocket));
            startActivityForResult(intent, REQUEST_LEADERBOARD);
        }
    }

    public void onAchievements(View view) {
        if (mGamesFragment.isSignedIn()) {
            Intent intent = Games.Achievements.getAchievementsIntent(
                    mGamesFragment.getGamesApiClient());
            startActivityForResult(intent, REQUEST_ACHIEVEMENTS);
        }
    }

    public void onExit(View view) {
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:
                //fall through
            case KeyEvent.KEYCODE_DPAD_CENTER:
                performPlayClick();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSignInFailed() {
        mSignIn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSignInSucceeded() {
        mSignIn.setVisibility(View.GONE);
        updateAchievementsAndLeaderboard();

        MeasurementManager.recordLogin(mMeasurement);
    }

    private boolean mProcessingPlayClick = false;
    private void performPlayClick() {
        if (!mProcessingPlayClick) {
            mProcessingPlayClick = true;
            mPlayAgain.setPressed(true);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mProcessingPlayClick = false;
                    mPlayAgain.setPressed(false);
                    mPlayAgain.performClick();
                }
            }, 300);
        }
    }

    private void updateAchievementsAndLeaderboard() {
        if (mGamesFragment.isSignedIn()) {
            GoogleApiClient apiClient = mGamesFragment.getGamesApiClient();
            Bundle bundle = getIntent().getExtras();

            for (int id : mAchievements) {
                String achievementStr = getString(id);
                if (bundle.containsKey(achievementStr)) {
                    Games.Achievements.unlock(mGamesFragment.getGamesApiClient(), achievementStr);
                    MeasurementManager.recordAchievement(mMeasurement,
                            achievementStr,
                            getString(R.string.analytics_screen_rocket));
                }
            }

            Long score = bundle.getLong("score");
            Games.Leaderboards.submitScore(apiClient,
                    getString(R.string.leaderboard_rocket), score);
            MeasurementManager.recordGameScore(mMeasurement, score, null,
                    getString(R.string.analytics_screen_rocket));
        }
    }
}
