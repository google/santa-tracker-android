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
package com.google.android.apps.santatracker.doodles.tilt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.apps.santatracker.doodles.R;
import com.google.android.apps.santatracker.doodles.shared.AndroidUtils;
import com.google.android.apps.santatracker.doodles.shared.AnimatedSprite;
import com.google.android.apps.santatracker.doodles.shared.Camera;
import com.google.android.apps.santatracker.doodles.shared.CameraShake;
import com.google.android.apps.santatracker.doodles.shared.DoodleConfig;
import com.google.android.apps.santatracker.doodles.shared.EventBus;
import com.google.android.apps.santatracker.doodles.shared.EventBus.EventBusListener;
import com.google.android.apps.santatracker.doodles.shared.GameFragment;
import com.google.android.apps.santatracker.doodles.shared.GameType;
import com.google.android.apps.santatracker.doodles.shared.HistoryManager;
import com.google.android.apps.santatracker.doodles.shared.HistoryManager.HistoryListener;
import com.google.android.apps.santatracker.doodles.shared.PineappleLogger;
import com.google.android.apps.santatracker.doodles.shared.RectangularInstructionActor;
import com.google.android.apps.santatracker.doodles.shared.SpriteActor;
import com.google.android.apps.santatracker.doodles.shared.Sprites;
import com.google.android.apps.santatracker.doodles.shared.UIUtil;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.sound.SoundManager;
import com.google.android.apps.santatracker.doodles.tilt.SwimmingModel.SwimmingState;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.SWIMMING_GAME_TYPE;

/**
 * The main fragment for the swimming game.
 */
public class SwimmingFragment extends GameFragment
    implements SensorEventListener, EventBusListener {
  private static final String TAG = SwimmingFragment.class.getSimpleName();
  public static final String CURRENT_LEVEL_KEY = "CURRENT_LEVEL";
  private static final float ACCELEROMETER_SCALE = 1.5f / 9.8f; // Scale to range -1.5:1.5.
  private static final int END_VIEW_ON_DEATH_DELAY_MS = 1400;

  public static boolean editorMode = false;

  private SwimmingView swimmingView;
  private DiveView diveView;
  private TextView countdownView;
  private Button saveButton;
  private Button loadButton;
  private Button resetButton;
  private Button deleteButton;
  private ToggleButton collisionModeButton;
  private SwimmingModel model;

  private SwimmingLevelManager levelManager;
  private SensorManager sensorManager;
  private Sensor accelerometerSensor;
  private int displayRotation;

  private int playCount = 0;
  private long titleDurationMs = GameFragment.TITLE_DURATION_MS;
  private SwimmingModel tempLevel;
  private boolean mIsGameOver = false;

  public SwimmingFragment() {
    super();
  }

  public SwimmingFragment(Context context, DoodleConfig doodleConfig, PineappleLogger logger,
      boolean editorMode) {
    super(context, doodleConfig, logger);
    SwimmingFragment.editorMode = editorMode;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    if (context == null) {
      return null;
    }

    levelManager = new SwimmingLevelManager(context);

    wrapper = new FrameLayout(context);

    titleView = getTitleView(R.drawable.penguin_swim_loadingscreen, R.string.swimming);
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
    final FrameLayout.LayoutParams wrapperLP =
        new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    final SwimmingFragment that = this;
    scoreView = getScoreView();
    pauseView = getPauseView();

    int diveViewBottomMargin = (int) context.getResources().getDimension(
        R.dimen.dive_margin_bottom);
    int diveViewStartMargin = (int) context.getResources().getDimension(
        R.dimen.dive_margin_left);
    int diveViewSize = (int) context.getResources().getDimension(
        R.dimen.dive_image_size);

    FrameLayout.LayoutParams diveViewLP = new LayoutParams(diveViewSize, diveViewSize);
    diveViewLP.setMargins(diveViewStartMargin, 0, 0, diveViewBottomMargin);
    diveViewLP.gravity = Gravity.BOTTOM | Gravity.LEFT;

    if (VERSION.SDK_INT >= 17) {
      diveViewLP.setMarginStart(diveViewStartMargin);
    }
    diveView = new DiveView(context);

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

    LinearLayout gameView = new LinearLayout(context);
    gameView.setOrientation(LinearLayout.VERTICAL);

    // Add game view.
    swimmingView = new SwimmingView(context);
    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            7);
    gameView.addView(swimmingView, lp);

    if (editorMode) {
      LinearLayout buttonWrapper = new LinearLayout(context);
      buttonWrapper.setOrientation(LinearLayout.HORIZONTAL);
      lp =
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT,
              LinearLayout.LayoutParams.WRAP_CONTENT,
              1);
      gameView.addView(buttonWrapper, lp);

      resetButton =
          getButton(
              R.string.reset_level,
              new OnClickListener() {
                @Override
                public void onClick(View v) {
                  SwimmingModel level = levelManager.loadDefaultLevel();
                  initializeLevel(level, false);

                  getActivity().getSharedPreferences(
                      context.getString(R.string.swimming), Context.MODE_PRIVATE)
                      .edit()
                      .putString(CURRENT_LEVEL_KEY, null)
                      .commit();
                }
              });
      deleteButton =
          getButton(
              R.string.delete_levels,
              new OnClickListener() {
                @Override
                public void onClick(View v) {
                  DialogFragment dialogFragment = new DeleteLevelDialogFragment();
                  dialogFragment.show(getActivity().getFragmentManager(), "delete");
                }
              });
      loadButton =
          getButton(
              R.string.load_level,
              new OnClickListener() {
                @Override
                public void onClick(View v) {
                  DialogFragment dialogFragment = new LoadLevelDialogFragment(that);
                  dialogFragment.show(getActivity().getFragmentManager(), "load");
                }
              });
      saveButton =
          getButton(
              R.string.save_level,
              new OnClickListener() {
                @Override
                public void onClick(View v) {
                  DialogFragment dialogFragment = new SaveLevelDialogFragment(that);
                  dialogFragment.show(getActivity().getFragmentManager(), "save");
                }
              });
      collisionModeButton = new ToggleButton(context);
      collisionModeButton.setText(R.string.scenery_mode);
      collisionModeButton.setTextOff(context.getString(R.string.scenery_mode));
      collisionModeButton.setTextOn(context.getString(R.string.collision_mode));
      collisionModeButton.setOnCheckedChangeListener(
          new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
              model.collisionMode = isChecked;
            }
          });

      lp =
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.WRAP_CONTENT,
              LinearLayout.LayoutParams.MATCH_PARENT,
              1);
      buttonWrapper.addView(deleteButton, lp);
      buttonWrapper.addView(resetButton, lp);
      buttonWrapper.addView(loadButton, lp);
      buttonWrapper.addView(saveButton, lp);
      buttonWrapper.addView(collisionModeButton, lp);
    }

    sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
    accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    if (accelerometerSensor == null) {
      // TODO: The game won't be playable without this, so what should we do?
      Log.d(TAG, "Accelerometer sensor is null");
    }
    displayRotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
        .getDefaultDisplay().getRotation();

    wrapper.addView(gameView, 0, wrapperLP);
    wrapper.addView(countdownView, 1);
    wrapper.addView(diveView, 2, diveViewLP);
    wrapper.addView(scoreView, 3);
    wrapper.addView(pauseView, 4);
  }

  @Override
  protected void secondPassLoadOnBackgroundThread() {
    super.secondPassLoadOnBackgroundThread();

    tempLevel = new SwimmingModel();
    final SwimmingModel level = tempLevel;
    historyManager = new HistoryManager(
            context,
        new HistoryListener() {
          @Override
          public void onFinishedLoading() {
            addBestTimeLine(level);
          }

          @Override
          public void onFinishedSaving() {}
        });

    initializeLevel(tempLevel, true);
  }

  @Override
  protected void finalPassLoadOnUiThread() {
    soundManager = SoundManager.getInstance();
    loadSounds();

    onFinishedLoading();
    startHandlers();
    tempLevel = null;
  }

  @Override
  protected void replay() {
    super.replay();
    SwimmingModel level = new SwimmingModel();
    initializeLevel(level, false);
  }

  @Override
  protected void resume() {
    super.resume();
    if (uiRefreshHandler != null) {
      sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
      uiRefreshHandler.start(swimmingView);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (sensorManager != null) {
      sensorManager.unregisterListener(this);
    }
    // Clear the path list because it's static and otherwise it hangs around forever.
    if (SwimmingLevelChunk.pathList != null) {
      SwimmingLevelChunk.pathList.clear();
    }
  }

  @Override
  protected void onDestroyHelper() {
    if (swimmingView != null) {
      swimmingView.setModel(null);
    }
    model = null;
    tempLevel = null;
    levelManager = null;

    if (SwimmingLevelChunk.swimmingLevelChunks != null) {
      SwimmingLevelChunk.swimmingLevelChunks.clear();
    }
    if (SwimmingLevelChunk.pathList != null) {
      SwimmingLevelChunk.pathList.clear();
    }
  }

  @Override
  public void update(float deltaMs) {
    if (isPaused) {
      return;
    }
    model.update(deltaMs);
    diveView.update(deltaMs);

    // If we are in editor mode, hide the intro animation right away and skip the camera pan.
    // Otherwise, wait until it has finished playing before fading and then run the camera pan.
    if ((editorMode || model.timeElapsed >= titleDurationMs)
        && model.getState() == SwimmingState.INTRO) {
      if (editorMode) {
        Log.d(TAG, "Hiding intro animation right away.");

        // Run on UI thread since background threads can't touch the intro view.
        getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            titleView.setVisibility(View.GONE);
          }
        });
        model.setState(SwimmingState.WAITING);
      } else {
        Log.d(TAG, "Fading out and hiding intro animation.");

        // Run on UI thread since background threads can't touch the intro view.
        getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            hideTitle();
          }
        });
        model.setState(SwimmingState.WAITING);
      }
    }

  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event.values == null || model == null) {
      return;
    }
    int sensorType = event.sensor.getType();
    if (sensorType == Sensor.TYPE_ACCELEROMETER) {
      float[] adjustedEventValues =
          AndroidUtils.getAdjustedAccelerometerValues(displayRotation, event.values);
      float x = -adjustedEventValues[0] * ACCELEROMETER_SCALE;
      float y = -adjustedEventValues[1] * ACCELEROMETER_SCALE;
      // Accelerometer input is very noisy, so we filter it using a (simple) low-pass filter.
      float weight = 0.1f;
      model.tilt.x = model.tilt.x + weight * (x - model.tilt.x);
      model.tilt.y = model.tilt.y + weight * (y - model.tilt.y);
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {
  }

  @Override
  public void onEventReceived(int type, final Object data) {
    if (isDestroyed) {
      return;
    }
    switch(type) {
      case EventBus.SCORE_CHANGED:
        int distance = integerValue(data);
        boolean shouldAddStar = false;

        if (model.currentScoreThreshold < SwimmingModel.SCORE_THRESHOLDS.length
            && distance >= SwimmingModel.SCORE_THRESHOLDS[model.currentScoreThreshold]) {
          // Pop in star.
          shouldAddStar = true;
          model.currentScoreThreshold++;
        }
        updateScoreUi(distance, shouldAddStar);
        break;
      case EventBus.SWIMMING_DIVE:
        getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            diveView.startCooldown();
          }
        });
        break;

      case EventBus.GAME_STATE_CHANGED:
        SwimmingState state = (SwimmingState) data;
        mIsGameOver = false;
        if (state == SwimmingState.FINISHED) {
          mIsGameOver = true;
          calculateBestDistance();
          calculateStars();
          getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              pauseView.hidePauseButton();
            }
          });
          final Handler handler = new Handler(Looper.getMainLooper());
          handler.postDelayed(new Runnable() {
            @Override
            public void run() {
              if (model == null) {
                return;
              }
              int bestDistance = historyManager.getBestDistance(GameType.SWIMMING).intValue();
              String collidedObjectType = model.swimmer.getCollidedObjectType();
              scoreView.updateBestScore(
                  AndroidUtils.getText(
                      context.getResources(), R.string.swimming_score, bestDistance));
              scoreView.setShareDrawable(
                      getShareImageDrawable(collidedObjectType));

              diveView.hide();
              scoreView.animateToEndState();
            }
          }, END_VIEW_ON_DEATH_DELAY_MS);
        }

        // [ANALYTICS]
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getActivity());
        MeasurementManager.recordSwimmingEnd(analytics,
                model.getStarCount(),
                model.distanceMeters,
                model.swimmer.getCollidedObjectType());
        break;

      case EventBus.PLAY_SOUND:
        int resId = (int) data;
        if (soundManager != null) {
          soundManager.play(resId);
        }
        break;

      case EventBus.MUTE_SOUNDS:
        boolean shouldMute = (boolean) data;
        if (soundManager != null) {
          if (shouldMute) {
            soundManager.mute();
          } else {
            soundManager.unmute();
          }
        }
        break;

      case EventBus.GAME_LOADED:
        long loadTimeMs = (long) data;
        titleDurationMs = Math.max(0, titleDurationMs - loadTimeMs);
        Log.d(TAG, "Waiting " + titleDurationMs + "ms and then hiding title.");
        break;
    }

  }

  @Nullable
  private Drawable getShareImageDrawable(String collidedObjectType) {
    return ContextCompat.getDrawable(getActivity(), R.drawable.winner);
  }

  private void initializeLevel(final SwimmingModel newLevel, boolean shouldPlayIntro) {
    EventBus.getInstance().clearListeners();
    EventBus.getInstance().register(newLevel, EventBus.VIBRATE);
    EventBus.getInstance().register(newLevel, EventBus.SHAKE_SCREEN);
    EventBus.getInstance().register(newLevel, EventBus.GAME_STATE_CHANGED);

    // Initialize Activity-specific stuff.
    EventBus.getInstance().register(this);

    Point size = AndroidUtils.getScreenSize();
    newLevel.screenWidth = size.x;
    newLevel.screenHeight = size.y;
    newLevel.addActor(new Camera(newLevel.screenWidth, newLevel.screenHeight));
    newLevel.addActor(new CameraShake());
    // Center the level horizontally on the screen and scale to fit.
    newLevel.camera.moveImmediatelyTo(
        Vector2D.get(0, 0),
        Vector2D.get(SwimmingModel.LEVEL_WIDTH, 0));
    newLevel.playCount = playCount++;
    newLevel.locale = getResources().getConfiguration().locale;

    if (isDestroyed) {
      return;
    }
    SwimmerActor swimmer = SwimmerActor.create(Vector2D.get(0, 0), context.getResources(),
        this);
    if (swimmer == null) {
      return;
    }
    swimmer.moveTo(
        SwimmingModel.LEVEL_WIDTH / 2 - swimmer.collisionBody.getWidth() / 2,
        SwimmerActor.KICKOFF_IDLE_Y_OFFSET);
    newLevel.addActor(swimmer);


    DistanceMarkerActor startingBlock =
        new DistanceMarkerActor(0, ColoredRectangleActor.STARTING_BLOCK, 1000);
    newLevel.addActor(startingBlock);

    SpriteActor banner = new SpriteActor(
        AnimatedSprite.fromFrames(context.getResources(), Sprites.penguin_swim_banner),
        Vector2D.get(SwimmingModel.LEVEL_WIDTH / 2, startingBlock.position.y), Vector2D.get());
    banner.scale = 2;
    banner.zIndex = 2;
    banner.sprite.setAnchor(banner.sprite.frameWidth / 2, banner.sprite.frameHeight - 50);
    newLevel.addActor(banner);

    newLevel.addActor(new DistanceMarkerActor(30, ColoredRectangleActor.DISTANCE_30M));
    newLevel.addActor(new DistanceMarkerActor(50, ColoredRectangleActor.DISTANCE_50M));
    newLevel.addActor(new DistanceMarkerActor(100, ColoredRectangleActor.DISTANCE_100M));
    newLevel.addActor(new DistanceMarkerActor(
        SwimmingLevelChunk.LEVEL_LENGTH_IN_METERS, ColoredRectangleActor.DISTANCE_LEVEL_LENGTH));
    if (historyManager != null) {
      addBestTimeLine(newLevel);
    }

    if (isDestroyed) {
      return;
    }
    ObstacleManager obstacleManager = new ObstacleManager(swimmer, context);
    newLevel.addActor(obstacleManager);

    newLevel.camera.position.y = -newLevel.camera.toWorldScale(newLevel.screenHeight);
    newLevel.clampCameraPosition();  // Clamp camera so that it doesn't jump when we start swimming.

    AnimatedSprite swimmingTutorialSprite =
        AnimatedSprite.fromFrames(context.getResources(), Sprites.tutorial_swimming);
    swimmingTutorialSprite.setFPS(6);
    RectangularInstructionActor instructions = new RectangularInstructionActor(
        context.getResources(), swimmingTutorialSprite);
    instructions.hidden = true;
    instructions.scale = (newLevel.screenWidth * 0.6f) / instructions.rectangle.frameWidth;
    // Put instructions at top right, slightly below pause button.
    instructions.position.set(newLevel.screenWidth / 2 - instructions.getScaledWidth() / 2, 70);
    newLevel.addUiActor(instructions);

    newLevel.setCountdownView(countdownView);

    if (!shouldPlayIntro) {
      // Skip State.INTRO.
      newLevel.setState(SwimmingState.WAITING);
    }
    if (isDestroyed) {
      return;
    }
    model = newLevel;
    swimmingView.setModel(model);

    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        initializeLevelUiThread(newLevel);
      }
    });

    // Hint that this would be a good time to do some garbage collection since we should be able
    // to get rid of any stuff from the previous level at this time.
    System.gc();
  }

  // Initialize level parts that need to happen in UI thread.
  private void initializeLevelUiThread(final SwimmingModel newLevel) {
    if (isDestroyed) {
      return;
    }
    scoreView.updateCurrentScore(
        AndroidUtils.getText(context.getResources(), R.string.swimming_score, 0), false);
    pauseView.showPauseButton();
    diveView.show();
    newLevel.vibrator = (Vibrator) getActivity().getSystemService(Activity.VIBRATOR_SERVICE);
  }

  @Override
  protected void loadSounds() {
    super.loadSounds();
    soundManager.loadShortSound(context, R.raw.swimming_dive_down);
    soundManager.loadShortSound(context, R.raw.swimming_dive_up);
    soundManager.loadShortSound(context, R.raw.swimming_ice_splash_a);
    soundManager.loadShortSound(context, R.raw.swimming_duck_collide);
    soundManager.loadShortSound(context, R.raw.swimming_ice_collide);
    soundManager.loadShortSound(context, R.raw.swimming_grab);
  }

  private void addBestTimeLine(SwimmingModel level) {
    Double bestDistance = historyManager.getBestDistance(GameType.SWIMMING);
    if (bestDistance != null) {
      level.addActor(new DistanceMarkerActor(bestDistance.intValue(),
          ColoredRectangleActor.DISTANCE_PR));
    }
  }

  private void calculateStars() {
    Integer bestStarCount = historyManager.getBestStarCount(GameType.SWIMMING);
    int modelStarCount = model.getStarCount();
    if (bestStarCount == null || bestStarCount < modelStarCount) {
      historyManager.setBestStarCount(GameType.SWIMMING, modelStarCount);
    }
  }

  private void calculateBestDistance() {
    Double bestDistance = historyManager.getBestDistance(GameType.SWIMMING);
    if (bestDistance == null || bestDistance < model.distanceMeters) {
      historyManager.setBestDistance(GameType.SWIMMING, model.distanceMeters);
    }
  }

  private Button getButton(int textId, OnClickListener onClickListener) {
    Button button = new Button(context);
    button.setText(textId);
    button.setOnClickListener(onClickListener);
    return button;
  }

  private void updateScoreUi(final int score, final boolean shouldShowStars) {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        scoreView.updateCurrentScore(
            AndroidUtils.getText(context.getResources(), R.string.swimming_score, score),
            shouldShowStars);
        if (shouldShowStars) {
          scoreView.addStar();
        }
      }
    });

  }

  private int integerValue(Object object) {
    if (object instanceof Integer) {
      return (int) object;
    } else {
      throw new IllegalArgumentException("Unknown event data type");
    }
  }

  @Override
  public void onGamePause() {
    if (model != null && model.swimmer != null) {
      model.swimmer.controlsEnabled = false;
    }
    super.onGamePause();
  }

  @Override
  protected void onGameResume() {
    if (model != null && model.swimmer != null) {
      model.swimmer.controlsEnabled = true;
    }
    super.onGameResume();
  }

  @Override
  protected  void onGameReplay() {
    if (model != null && model.swimmer != null) {
      model.swimmer.controlsEnabled = true;
    }
    super.onGameReplay();
  }

  @Override
  protected boolean onTitleTapped() {
    if (model != null && model.getState() == SwimmingState.INTRO) {
      // If the user taps the screen while the intro animation is playing, end the intro
      // immediately and transition into the game.
      model.timeElapsed = GameFragment.TITLE_DURATION_MS;
      return true;
    }
    return false;
  }

  @Override
  protected String getGameType() {
    return SWIMMING_GAME_TYPE;
  }

  @Override
  protected float getScore() {
    return model.distanceMeters;
  }

  @Override
  protected int getShareImageId() {
    String collidedObjectType = model.swimmer.getCollidedObjectType();
    if (BoundingBoxSpriteActor.ICE_CUBE.equals(collidedObjectType)) {
      return 0;
    } else if (BoundingBoxSpriteActor.DUCK.equals(collidedObjectType)) {
      return 1;
    } else {  // Octopus hand grab.
      return 2;
    }
  }

  /**
   * A dialog fragment for loading a swimming level.
   */
  public static class LoadLevelDialogFragment extends DialogFragment {
    private SwimmingFragment swimmingFragment;
    public LoadLevelDialogFragment(SwimmingFragment swimmingFragment) {
      this.swimmingFragment = swimmingFragment;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      final String[] fileList =
          SwimmingLevelManager.levelsDir.exists()
              ? SwimmingLevelManager.levelsDir.list()
              : new String[0];

      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder
          .setTitle(R.string.load_level)
          .setItems(
              fileList,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  SwimmingModel level = swimmingFragment.levelManager.loadLevel(fileList[which]);
                  swimmingFragment.initializeLevel(level, false);

                  // Save the current level so we automatically come back to it later.
                  getActivity()
                      .getSharedPreferences(
                          swimmingFragment.context.getString(R.string.swimming),
                          Context.MODE_PRIVATE)
                      .edit()
                      .putString(CURRENT_LEVEL_KEY, fileList[which])
                      .commit();
                }
              });
      return builder.create();
    }
  }

  /**
   * A dialog fragment for saving a swimming level.
   */
  public static class SaveLevelDialogFragment extends DialogFragment {
    private SwimmingFragment swimmingFragment;
    public SaveLevelDialogFragment(SwimmingFragment swimmingFragment) {
      this.swimmingFragment = swimmingFragment;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      final EditText editText = new EditText(swimmingFragment.context);
      editText.setText(swimmingFragment.model.levelName);
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder
          .setTitle(R.string.save_level)
          .setView(editText)
          .setPositiveButton(
              R.string.save,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                  swimmingFragment.levelManager.saveLevel(
                      swimmingFragment.model, editText.getText().toString());
                }
              })
          .setNegativeButton(
              R.string.cancel,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                  // Do nothing.
                }
              });
      return builder.create();
    }
  }

  /**
   * A dialog fragment for deleting a swimming level.
   */
  public static class DeleteLevelDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      final List<Integer> selectedItems = new ArrayList<>();
      final String[] fileList =
          SwimmingLevelManager.levelsDir.exists()
              ? SwimmingLevelManager.levelsDir.list()
              : new String[0];

      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder
          .setTitle(R.string.delete_levels)
          .setMultiChoiceItems(
              fileList,
              null,
              new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                  if (isChecked) {
                    selectedItems.add(which);
                  } else if (selectedItems.contains(which)) {
                    selectedItems.remove(Integer.valueOf(which));
                  }
                }
              })
          .setPositiveButton(
              R.string.delete,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                  for (Integer selection : selectedItems) {
                    File file = new File(SwimmingLevelManager.levelsDir, fileList[selection]);
                    file.delete();
                  }
                }
              })
          .setNegativeButton(
              R.string.cancel,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                  // Do nothing.
                }
              });
      return builder.create();
    }
  }

  public boolean isGameOver() {
    return mIsGameOver;
  }
}
