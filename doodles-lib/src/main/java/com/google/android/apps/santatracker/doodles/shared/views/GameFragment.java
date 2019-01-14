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

package com.google.android.apps.santatracker.doodles.shared.views;

import static com.google.android.apps.santatracker.doodles.shared.EventBus.GAME_LOADED;
import static com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogEvent.DEFAULT_DOODLE_NAME;
import static com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogEvent.LOADING_COMPLETE;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.google.android.apps.santatracker.doodles.BaseDoodleActivity;
import com.google.android.apps.santatracker.doodles.R;
import com.google.android.apps.santatracker.doodles.shared.AndroidUtils;
import com.google.android.apps.santatracker.doodles.shared.EventBus;
import com.google.android.apps.santatracker.doodles.shared.GameLoop;
import com.google.android.apps.santatracker.doodles.shared.HistoryManager;
import com.google.android.apps.santatracker.doodles.shared.LogicRefreshThread;
import com.google.android.apps.santatracker.doodles.shared.UIRefreshHandler;
import com.google.android.apps.santatracker.doodles.shared.UIUtil;
import com.google.android.apps.santatracker.doodles.shared.animation.AnimatedSprite;
import com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogEvent.Builder;
import com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogTimer;
import com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogger;
import com.google.android.apps.santatracker.doodles.shared.logging.DoodleNullLogger;
import com.google.android.apps.santatracker.doodles.shared.sound.SoundManager;
import com.google.android.apps.santatracker.invites.AppInvitesFragment;

/** Base class for Pineapple game fragments. */
public abstract class GameFragment extends Fragment
        implements GameLoop, ScoreView.OnShareClickedListener {

    // Minimum length of the title screen.
    public static final long TITLE_DURATION_MS = 1000;

    // Require 128MB and if we don't have it, downsample the resources.
    private static final int AVAILABLE_MEMORY_REQUIRED = (2 << 27);

    // Note this is a context, not an activity.  Use getActivity() for an activity.
    public Context context;
    public final DoodleLogger logger;
    public boolean isDestroyed = false;
    protected FrameLayout wrapper;
    protected View titleView;
    protected ScoreView scoreView;
    protected PauseView pauseView;
    protected LogicRefreshThread logicRefreshThread;
    protected UIRefreshHandler uiRefreshHandler;
    protected SoundManager soundManager;
    protected HistoryManager historyManager;
    protected boolean isFinishedLoading = false;
    protected boolean isPaused = false;
    protected PauseView.GamePausedListener gamePausedListener =
            new PauseView.GamePausedListener() {
                @Override
                public void onPause() {
                    onGamePause();
                }

                @Override
                public void onResume() {
                    onGameResume();
                }

                @Override
                public void onReplay() {
                    onGameReplay();
                }

                @Override
                public String gameType() {
                    return getGameType();
                }

                @Override
                public float score() {
                    return getScore();
                }
            };
    protected ScoreView.LevelFinishedListener levelFinishedListener =
            new ScoreView.LevelFinishedListener() {
                @Override
                public void onReplay() {
                    onGameReplay();
                }

                @Override
                public String gameType() {
                    return getGameType();
                }

                @Override
                public float score() {
                    return getScore();
                }

                @Override
                public int shareImageId() {
                    return getShareImageId();
                }
            };
    protected boolean resumeAfterLoading;
    private ImageView titleImageView;
    private AsyncTask<Void, Void, Void> asyncLoadGameTask;

    public GameFragment() {
        this.logger = new DoodleNullLogger();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // TODO: Remove context from the field. Workaround not to change the existing code
        this.context = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.context = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (context == null) {
            return;
        }
        resume();
        if ((pauseView == null || pauseView.isPauseButtonEnabled)
                && (scoreView == null || scoreView.getVisibility() != View.VISIBLE)) {
            // If we aren't paused or finished, keep the screen on.
            AndroidUtils.forceScreenToStayOn(getActivity());
        }
        if (soundManager != null) {
            soundManager.loadMutePreference(context);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (pauseView != null) {
            pauseView.pause();
        }
        resumeAfterLoading = false;
        if (uiRefreshHandler != null) {
            logicRefreshThread.stopHandler();
            uiRefreshHandler.stop();
            historyManager.save();
        }
        if (soundManager != null) {
            soundManager.pauseAll();
            soundManager.storeMutePreference(context);
        }
        AndroidUtils.allowScreenToTurnOff(getActivity());
    }

    // To be overrided by games if they want to do more cleanup on destroy.
    protected void onDestroyHelper() {}

    @Override
    public void onDestroyView() {
        isDestroyed = true;
        if (asyncLoadGameTask != null) {
            asyncLoadGameTask.cancel(true);
        }
        EventBus.getInstance().clearListeners();
        AnimatedSprite.clearCache();
        if (soundManager != null) {
            soundManager.releaseAll();
        }
        onDestroyHelper();
        super.onDestroyView();
    }

    protected void resume() {
        if (uiRefreshHandler == null) {
            resumeAfterLoading = true;
        } else {
            logicRefreshThread.startHandler(this);
            if (titleView == null || titleView.getVisibility() != View.VISIBLE) {
                playMusic();
            }
        }
    }

    /**
     * Loads the game. Do not override this function. Instead use the three helper functions:
     * firstPassLoadOnUiThread, secondPassLoadOnBackgroundThread, finalPassLoadOnUiThread.
     */
    public final void loadGame() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager =
                (ActivityManager)
                        getActivity().getSystemService(android.content.Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        if (mi.availMem < AVAILABLE_MEMORY_REQUIRED || AnimatedSprite.lastUsedSampleSize > 2) {
            // Low available memory, go ahead and load things with a larger sample size.
            AnimatedSprite.lastUsedSampleSize = 2;
        }

        firstPassLoadOnUiThread();
        secondPassLoadOnBackgroundThread();
        finalPassLoadOnUiThread();

        asyncLoadGameTask =
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        if (getActivity() != null) {
                            secondPassLoadOnBackgroundThread();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        if (getActivity() != null) {
                            finalPassLoadOnUiThread();
                        }
                    }
                };
        asyncLoadGameTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    protected void firstPassLoadOnUiThread() {}

    protected void secondPassLoadOnBackgroundThread() {
        EventBus.getInstance().clearListeners();
    }

    protected void finalPassLoadOnUiThread() {}

    protected void onFinishedLoading() {
        DoodleLogTimer logTimer = DoodleLogTimer.getInstance();
        long latencyMs = logTimer.timeElapsedMs();
        logger.logEvent(
                new Builder(DEFAULT_DOODLE_NAME, LOADING_COMPLETE)
                        .withEventSubType(getGameType())
                        .withLatencyMs(latencyMs)
                        .build());
        EventBus.getInstance().sendEvent(GAME_LOADED, latencyMs);
        logTimer.reset();
        if (pauseView != null) {
            pauseView.onFinishedLoading();
        }
        if (scoreView != null) {
            scoreView.setVisibility(View.VISIBLE);
        }

        isFinishedLoading = true;
    }

    public boolean isFinishedLoading() {
        return isFinishedLoading;
    }

    protected void startHandlers() {
        logicRefreshThread = new LogicRefreshThread();
        logicRefreshThread.start();
        uiRefreshHandler = new UIRefreshHandler();

        // It's annoying when the GC kicks in during gameplay and makes the game stutter. Hint
        // that now would be a good time to free up some space before the game starts.
        System.gc();
        if (resumeAfterLoading) {
            resume();
        }
    }

    protected void playMusic() {
        soundManager.play(R.raw.fruit_doodle_music);
    }

    protected void hideTitle() {
        if (titleView != null && titleView.getVisibility() == View.VISIBLE) {
            UIUtil.fadeOutAndHide(
                    titleView,
                    1,
                    500,
                    new Runnable() {
                        @Override
                        public void run() {
                            if (titleImageView != null) {
                                titleImageView.setImageDrawable(null);
                                titleImageView = null;
                            }
                        }
                    });
        }

        playMusic();
    }

    public boolean isGamePaused() {
        return isPaused;
    }

    public void onGamePause() {
        isPaused = true;
    }

    protected void onGameResume() {
        isPaused = false;
    }

    protected void onGameReplay() {
        replay();
        isPaused = false;
        DoodleLogTimer.getInstance().reset();
    }

    protected abstract float getScore();

    protected abstract int getShareImageId();

    protected boolean onTitleTapped() {
        return false;
    }

    protected void replay() {
        getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                scoreView.resetToStartState();
                            }
                        });
    }

    protected void loadSounds() {
        soundManager.loadLongSound(context, R.raw.fruit_doodle_music, true);
        soundManager.loadShortSound(context, R.raw.menu_item_click);
        soundManager.loadShortSound(context, R.raw.ui_positive_sound);
    }

    protected ScoreView getScoreView() {
        ScoreView scoreView = new ScoreView(context, this);
        scoreView.setLogger(logger);
        scoreView.setListener(levelFinishedListener);
        return scoreView;
    }

    protected PauseView getPauseView() {
        PauseView pauseView = new PauseView(context);
        pauseView.setLogger(logger);
        pauseView.setListener(gamePausedListener);
        return pauseView;
    }

    @Override
    public void onShareClicked() {
        Activity activity = getActivity();
        if (activity instanceof BaseDoodleActivity) {
            AppInvitesFragment invites = ((BaseDoodleActivity) activity).getAppInvitesFragment();
            if (invites != null) {
                invites.sendGenericInvite();
            }
        }
    }

    protected abstract String getGameType();

    public void onBackPressed() {
        pauseView.pause();
    }

    public abstract boolean isGameOver();
}
