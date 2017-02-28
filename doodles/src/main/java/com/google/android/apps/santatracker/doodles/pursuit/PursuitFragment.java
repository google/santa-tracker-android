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
package com.google.android.apps.santatracker.doodles.pursuit;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.apps.santatracker.doodles.R;
import com.google.android.apps.santatracker.doodles.pursuit.PursuitModel.ScoreListener;
import com.google.android.apps.santatracker.doodles.pursuit.PursuitModel.State;
import com.google.android.apps.santatracker.doodles.pursuit.PursuitModel.StateChangedListener;
import com.google.android.apps.santatracker.doodles.shared.AndroidUtils;
import com.google.android.apps.santatracker.doodles.shared.DoodleConfig;
import com.google.android.apps.santatracker.doodles.shared.EventBus;
import com.google.android.apps.santatracker.doodles.shared.EventBus.EventBusListener;
import com.google.android.apps.santatracker.doodles.shared.GameFragment;
import com.google.android.apps.santatracker.doodles.shared.GameType;
import com.google.android.apps.santatracker.doodles.shared.HistoryManager;
import com.google.android.apps.santatracker.doodles.shared.PineappleLogger;
import com.google.android.apps.santatracker.doodles.shared.UIUtil;
import com.google.android.apps.santatracker.doodles.shared.sound.SoundManager;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.text.NumberFormat;
import java.util.Locale;

import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.RUNNING_GAME_TYPE;

/**
 * Fragment for the second version of the running game.
 * Manages input & threads, delegates to PursuitModel & PursuitView for the rest.
 */
public class PursuitFragment extends GameFragment implements EventBusListener {
  private static final String TAG = PursuitFragment.class.getSimpleName();

  private static final long GAME_OVER_DELAY_MILLISECONDS = 1000;
  private static final long GAME_OVER_SHORT_DELAY_MILLISECONDS = 150;

  private PursuitView gameView;
  private TextView countdownView;
  private PursuitModel model;
  private boolean mIsGameOver = false;

  public PursuitFragment() {
    super();
  }

  public PursuitFragment(Context context, DoodleConfig doodleConfig, PineappleLogger logger) {
    super(context, doodleConfig, logger);
  }

  @Override
  public void update(float deltaMs) {
    if (!isPaused) {
      model.update(deltaMs);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    if (context == null) {
      return null;
    }

    historyManager = new HistoryManager(context, new HistoryManager.HistoryListener() {
      @Override
      public void onFinishedLoading() {
      }
      @Override
      public void onFinishedSaving() {
      }
    });

    wrapper = new FrameLayout(context);

    titleView = getTitleView(R.drawable.snowballrunner_loadingscreen, R.string.running);
    wrapper.addView(titleView);
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        loadGame();
      }
    });
    return wrapper;
  }

  @Override
  protected void firstPassLoadOnUiThread() {
    wrapper.setOnTouchListener(new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
          return onTouchEvent(event);
        }
    });

    countdownView = new TextView(context);
    countdownView.setGravity(Gravity.CENTER);
    countdownView.setTextColor(context.getResources().getColor(R.color.ui_text_yellow));
    countdownView.setTypeface(Typeface.DEFAULT_BOLD);
    countdownView.setText("0");
    countdownView.setVisibility(View.INVISIBLE);
    Locale locale = context.getResources().getConfiguration().locale;
    countdownView.setText(NumberFormat.getInstance(locale).format(3));
    Point screenDimens = AndroidUtils.getScreenSize();
    UIUtil.fitToBounds(countdownView, screenDimens.x / 10, screenDimens.y / 10);

    final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    gameView = new PursuitView(context);
    wrapper.addView(gameView, 0, lp);

    wrapper.addView(countdownView, 1, lp);
    scoreView = getScoreView();
    wrapper.addView(scoreView, 2, lp);
    pauseView = getPauseView();
    pauseView.hidePauseButton();
    wrapper.addView(pauseView, 3, lp);
  }

  @Override
  protected void secondPassLoadOnBackgroundThread() {
    super.secondPassLoadOnBackgroundThread();
    EventBus.getInstance().register(this);

    model = new PursuitModel(context.getResources(), getActivity().getApplicationContext());
    gameView.setModel(model);
    model.setStateListener(new StateChangedListener() {
      @Override
      public void onStateChanged(State state) {
        if (state == State.SETUP) {
          getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              hideTitle();
              pauseView.showPauseButton();
            }
          });
        } else if (state == State.SUCCESS) {
          getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              if (model == null) {
                return;
              }
              mIsGameOver = true;
              float timeInSeconds = model.getScore();
              int starCount = 4 - model.getFinishingPlace();

              long delay = GAME_OVER_SHORT_DELAY_MILLISECONDS;
              if (model.getFinishingPlace() == 1) {
                delay = GAME_OVER_DELAY_MILLISECONDS;
              }

              Double bestTimeInSeconds = historyManager.getBestScore(GameType.PURSUIT);
              Integer bestStarCount = historyManager.getBestStarCount(GameType.PURSUIT);

              if (bestTimeInSeconds == null || timeInSeconds < bestTimeInSeconds) {
                bestTimeInSeconds = (double) timeInSeconds;
                historyManager.setBestScore(GameType.PURSUIT, bestTimeInSeconds);
                historyManager.save();
              }

              if (bestStarCount == null || starCount > bestStarCount) {
                bestStarCount = starCount;
                historyManager.setBestStarCount(GameType.PURSUIT, bestStarCount);
                historyManager.save();
              }

              pauseView.hidePauseButton();

              final int finalStarCount = starCount;
              final double finalBestTimeInSeconds = bestTimeInSeconds;

              scoreView.postDelayed(new Runnable() {
                @Override
                public void run() {
                  if (finalStarCount >= 1) {
                    scoreView.addStar();
                  }
                  if (finalStarCount >= 2) {
                    scoreView.addStar();
                  }
                  if (finalStarCount >= 3) {
                    scoreView.addStar();
                  }
                  scoreView.updateBestScore(AndroidUtils.getText(context.getResources(),
                      R.string.pursuit_score, finalBestTimeInSeconds));
                  scoreView.setShareDrawable(getShareImageDrawable(finalStarCount));
                  updateShareText();

                  scoreView.animateToEndState();
                }
              }, delay);
            }
          });

          // [ANALYTICS]
          FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getActivity());
          MeasurementManager.recordRunningEnd(analytics,
                  4 - model.getFinishingPlace(),
                  (int) model.getScore());
        } else if (state == State.FAIL) {
          getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              pauseView.hidePauseButton();
              mIsGameOver = true;

              scoreView.postDelayed(new Runnable() {
                @Override
                public void run() {
                  scoreView.setHeaderText(null);

                  updateShareText();
                  scoreView.setShareDrawable(getShareImageDrawable(0));
                  scoreView.animateToEndState();
                }
              }, GAME_OVER_DELAY_MILLISECONDS);
            }});

          // [ANALYTICS]
          FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getActivity());
          MeasurementManager.recordRunningEnd(analytics, 0, 0);
        } else if (state == State.READY) {
          getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mIsGameOver = false;
              pauseView.showPauseButton();
            }
          });
        }
      }
    });

    model.setScoreListener(new ScoreListener() {
      @Override
      public void newScore(final float timeInSeconds) {
        getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            scoreView.updateCurrentScore(
                AndroidUtils.getText(context.getResources(),
                    R.string.pursuit_score, timeInSeconds), false);
          }
        });
      }
    });
    model.setCountdownView(countdownView);
  }
  @Override
  protected void finalPassLoadOnUiThread() {
    soundManager = SoundManager.getInstance();
    loadSounds();

    onFinishedLoading();
    startHandlers();
  }

  @Override
  protected void loadSounds() {
    super.loadSounds();
    soundManager.loadShortSound(context, R.raw.bmx_cheering);
    soundManager.loadShortSound(context, R.raw.jumping_jump);
    soundManager.loadShortSound(context, R.raw.present_throw_character_appear);
    soundManager.loadShortSound(context, R.raw.running_foot_loop_fast, true, 2f);
    soundManager.loadShortSound(context, R.raw.running_foot_power_up);
    soundManager.loadShortSound(context, R.raw.running_foot_power_up_fast, false, 0.4f);
    soundManager.loadShortSound(context, R.raw.running_foot_power_squish);
    soundManager.loadShortSound(context, R.raw.present_throw_block);
  }

  @Override
  protected void replay() {
    super.replay();
    model.gameReplay();
  }

  private Drawable getShareImageDrawable(int starCount) {
    return ContextCompat.getDrawable(getActivity(), R.drawable.winner);
  }

  @Override
  protected void onDestroyHelper() {
    if (gameView != null) {
      gameView.setModel(null);
    }
    model = null;
  }

  @Override
  public void onEventReceived(int type, Object data) {
    if (isDestroyed) {
      return;
    }
    if (type == EventBus.PLAY_SOUND && soundManager != null) {
      int resId = (int) data;
      soundManager.play(resId);
    } else if (type == EventBus.PAUSE_SOUND && soundManager != null) {
      int resId = (int) data;
      soundManager.pause(resId);
    } else if (type == EventBus.MUTE_SOUNDS && soundManager != null) {
      boolean shouldMute = (boolean) data;
      if (shouldMute) {
        soundManager.mute();
      } else {
        soundManager.unmute();
      }
    } else if (type == EventBus.GAME_LOADED) {
      mIsGameOver = false;
      long loadTimeMs = (long) data;
      model.titleDurationMs = Math.max(0, model.titleDurationMs - loadTimeMs);
      Log.d(TAG, "Waiting " + model.titleDurationMs + "ms and then hiding title.");
    }
  }

  public boolean onTouchEvent(MotionEvent event) {
    if (model != null) {
      model.touch(event);
      return true;
    }
    return false;
  }

  @Override
  protected void resume() {
    super.resume();
    if (uiRefreshHandler != null) {
      uiRefreshHandler.start(gameView);
    }
  }

  private void updateShareText() {
  }

  @Override
  protected String getGameType() {
    return RUNNING_GAME_TYPE;
  }

  @Override
  protected float getScore() {
    if (model == null) {
      return 0;
    }
    return model.getScore();
  }

  @Override
  protected int getShareImageId() {
    return scoreView.getStarCount();
  }

  public boolean isGameOver() {
    return mIsGameOver;
  }
}
