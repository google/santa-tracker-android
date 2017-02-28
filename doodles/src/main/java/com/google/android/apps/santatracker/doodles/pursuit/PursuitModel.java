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

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import com.google.android.apps.santatracker.doodles.R;
import com.google.android.apps.santatracker.doodles.pursuit.RunnerActor.RunnerState;
import com.google.android.apps.santatracker.doodles.pursuit.RunnerActor.RunnerType;
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.ActorHelper;
import com.google.android.apps.santatracker.doodles.shared.ActorTween;
import com.google.android.apps.santatracker.doodles.shared.ActorTween.Callback;
import com.google.android.apps.santatracker.doodles.shared.AndroidUtils;
import com.google.android.apps.santatracker.doodles.shared.AnimatedSprite;
import com.google.android.apps.santatracker.doodles.shared.Camera;
import com.google.android.apps.santatracker.doodles.shared.CameraShake;
import com.google.android.apps.santatracker.doodles.shared.EmptyTween;
import com.google.android.apps.santatracker.doodles.shared.EventBus;
import com.google.android.apps.santatracker.doodles.shared.FakeButtonActor;
import com.google.android.apps.santatracker.doodles.shared.GameFragment;
import com.google.android.apps.santatracker.doodles.shared.Interpolator;
import com.google.android.apps.santatracker.doodles.shared.RectangularInstructionActor;
import com.google.android.apps.santatracker.doodles.shared.Sprites;
import com.google.android.apps.santatracker.doodles.shared.Tween;
import com.google.android.apps.santatracker.doodles.shared.TweenManager;
import com.google.android.apps.santatracker.doodles.shared.UIUtil;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * All the game logic for the second version of the running game. Mostly consists of managing
 * a list of Actors.
 */
public class PursuitModel {
  private static final String TAG = PursuitModel.class.getSimpleName();

  /**
   * High-level phases of the game are controlled by a state machine which uses these states.
   */
  public enum State {
    TITLE,
    SETUP,
    READY,
    RUNNING,
    SUCCESS,
    FAIL,
  }

  /**
   * Will be notified when game enters a new state.
   */
  public interface StateChangedListener {
    void onStateChanged(State state);
  }

  /**
   * Will be notified when the score changes.
   */
  public interface ScoreListener {
    void newScore(float timeInSeconds);
  }

  /**
   * The other fruit the player is racing against.
   */
  public class OpponentActor extends RunnerActor {
    // Flag that indicates whether the opponent has crossed the finishing line.
    // Used for decelerating the opponent.
    public boolean isFinished;

    OpponentActor(Resources resources, RunnerType type, int lane) {
      super(resources, type, lane);
      isFinished = false;
    }
  }

  // Game-world uses a fixed coordinate system which, for simplicity, matches the resolution of the
  // art assets (i.e. 1 pixel in the art == 1 game-world unit). Measurements from art assets
  // directly translate to game-world units. Example: player1.png's racket is 57 pixels from the
  // bottom, so if the ball is 57 game-world units above the bottom of player's sprite, the ball is
  // at the same height as the racket). The view is responsible for scaling to fit the screen.

  // Visualization Constants
  public static final int HEIGHT = 960;
  public static final int WIDTH = 540;
  public static final int HALF_WIDTH = WIDTH / 2;
  public static final int HALF_HEIGHT = HEIGHT / 2;
  public static final int EXPECTED_PLAYER_WIDTH = (int) (WIDTH * 0.14f);

  // UI Constants
  private static final float BUTTON_SCALE = 0.8f;
  private static final float TUTORIAL_DURATION = 4f;
  private static final int COUNTDOWN_LABEL_COLOR = 0xffffbb39;

  // High level Android variables that lays the foundation for the game.
  private final Resources resources;
  private final TweenManager tweenManager = new TweenManager();
  private final Vibrator vibrator;
  private final EventBus eventBus;
  private final Locale locale;

  // Temporary Power-up generation variables that will be replaced by a more robust system.
  private float powerUpTimer;
  private int mapRow = 0;
  private PursuitLevel map;

  // Gameplay Constants.

  // Currently this game is divided into lanes.
  // Power ups are added at the center of each discrete lane.
  // As the game speed is increased, more lanes are added, increasing the gameplay area.
  public static final int NUM_LANES = 3;
  public static final int MIN_LANE = 0;
  public static final int MAX_LANE = NUM_LANES - 1;
  public static final int INITIAL_LANE = NUM_LANES / 2;
  public static final int LANE_SIZE = WIDTH / (NUM_LANES + 2);

  // All duration in seconds.
  private static final float LANE_SWITCH_DURATION = 0.10f;
  public static final float RUNNER_ENTER_DURATION = 1.72f;
  private static final float RUNNER_STANDING_DURATION = 0.5f;
  private static final float SETUP_DURATION =
      RUNNER_ENTER_DURATION + RUNNER_STANDING_DURATION + 0.6f;

  // Speed constants.
  public static final float BASE_SPEED = 340f;

  private static final float PLAYER_MINIMUM_SPEED = BASE_SPEED;
  private static final float PLAYER_MAXIMUM_SPEED = BASE_SPEED + 450f;
  private static final float PLAYER_DECELERATION = 300f;

  private static final float MANGO_SPEED = BASE_SPEED + 250f;
  private static final float GRAPE_SPEED = BASE_SPEED + 315f;
  private static final float APRICOT_SPEED = BASE_SPEED + 280f;

  private static final float WATERMELON_MINIMUM_SPEED = PLAYER_MINIMUM_SPEED + 100f;
  private static final float WATERMELON_MAXIMUM_SPEED = PLAYER_MAXIMUM_SPEED + 25f;
  private static final float WATERMELON_SPEED_INCREASE_PER_SECOND = 2.5f;
  private static final float WATERMELON_SPEED_DECREASE_PER_SECOND = 6f;

  private static final float OPPONENT_SLOW_DOWN_DURATION = 0.24f;
  private static final float OPPONENT_SLOW_DOWN_AMOUNT = 450f;

  private static final float RUNNER_FINISH_DURATION = 5f;
  private static final float WATERMELON_FINISH_DURATION = 4f;

  // Size constants.
  private static final float STRAWBERRY_RADIUS = 15f;
  private static final float MANGO_RADIUS = 28f;
  private static final float GRAPE_RADIUS = 12f;
  private static final float APRICOT_RADIUS = 22f;

  // Misc. constants.
  private static final float POWER_UP_SPEED_BOOST = 320f;
  private static final float TOTAL_DISTANCE = 16000f;
  private static final int VIBRATION_MS = 60;

  // Positional constants.
  private static final float PLAYER_INITIAL_POSITION_Y = HEIGHT * 0.25f;
  private static final float OPPONENT_INITIAL_POSITION_Y = HEIGHT * 0.4f;
  private static final float RIBBON_INITIALIZATION_POSITION_Y
      = PLAYER_INITIAL_POSITION_Y + TOTAL_DISTANCE;

  // Camera constants.
  private static final float CAMERA_LOOKAHEAD_INCREASE_PER_SECOND = 0.8f;
  private static final float CAMERA_LOOKAHEAD_DECREASE_PER_SECOND = 2.5f;
  private static final float CAMERA_MAXIMUM_LOOKAHEAD = -(HEIGHT * 0.25f);

  // Gameplay variables.
  private float baseScale;
  private State state;
  private float laneSwitchTimer = 0f;
  private float cameraLookAhead; // The camera trails behind the player when player is going fast.
  private boolean watermelonHasEntered = false;
  private boolean playerHasStarted = false;

  // Player performance variables.
  private float time;
  private int finishingPlace; // First place = 1.

  // Listeners.
  private StateChangedListener stateListener;
  private ScoreListener scoreListener;

  // Public so view can render the actors.
  public final List<Actor> actors = Collections.synchronizedList(new ArrayList<Actor>());
  public final List<Actor> ui = Collections.synchronizedList(new ArrayList<Actor>());

  private final List<PowerUpActor> powerUps = new ArrayList<PowerUpActor>();
  private final List<OpponentActor> opponents = new ArrayList<OpponentActor>();

  public final CameraShake cameraShake; // Public so view can read the amount of shake.
  public final Camera camera;

  // Actors.
  public PlayerActor player;
  public OpponentActor mango;
  public OpponentActor grape;
  public OpponentActor apricot;

  public WatermelonActor watermelon;
  public RibbonActor ribbon;

  // UI Buttons.
  public FakeButtonActor leftButton;
  public FakeButtonActor rightButton;
  private RectangularInstructionActor instructions;
  private TextView countdownView;

  // Background
  public BackgroundActor backgroundActor;

  // public so that the GameFragment can update the duration.
  public long titleDurationMs = GameFragment.TITLE_DURATION_MS;

  public PursuitModel(Resources resources, Context context) {
    this.resources = resources;

    eventBus = EventBus.getInstance();

    vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    locale = resources.getConfiguration().locale;

    cameraShake = new CameraShake();
    camera = new Camera(WIDTH, HEIGHT);
    camera.position.x = -HALF_WIDTH;

    map = new PursuitLevel(R.raw.running, resources);

    initActors();
    showTitle();
  }

  private void initActors() {
    // Initialize background
    backgroundActor = new BackgroundActor(resources, camera);
    backgroundActor.scale = 1.15f;

    player = new PlayerActor(resources, INITIAL_LANE);
    player.setRadius(STRAWBERRY_RADIUS);
    ribbon = new RibbonActor(0, RIBBON_INITIALIZATION_POSITION_Y, resources);
    watermelon = new WatermelonActor(resources);

    // Scale actors to fit screen
    baseScale = (1f * EXPECTED_PLAYER_WIDTH) / player.currentSprite.frameWidth;

    player.scale = baseScale * 1.07f;
    watermelon.scale = baseScale * 1.6f;
    ribbon.scale = baseScale * 1.03f;

    // Add the essential actors to screen
    synchronized (actors) {
      actors.add(player);
      actors.add(ribbon);
      actors.add(watermelon);
    }

    // Add the opponents
    mango = addOpponent(RunnerType.MANGO, 0, OPPONENT_INITIAL_POSITION_Y, MANGO_RADIUS);
    grape = addOpponent(RunnerType.GRAPE, 1, OPPONENT_INITIAL_POSITION_Y, GRAPE_RADIUS);
    apricot = addOpponent(RunnerType.APRICOT, 2, OPPONENT_INITIAL_POSITION_Y, APRICOT_RADIUS);

    AnimatedSprite runningTutorialSprite =
        AnimatedSprite.fromFrames(resources, Sprites.tutorial_running);
    runningTutorialSprite.setFPS(7);

    // Initialize and add the UI actors
    instructions = new RectangularInstructionActor(resources, runningTutorialSprite);
    instructions.hidden = true;

    instructions.scale = 0.54f;
    instructions.position.set(HALF_WIDTH - instructions.getScaledWidth() / 2,
        HEIGHT * 0.65f - instructions.getScaledHeight() / 2);

    leftButton = new FakeButtonActor(
        AnimatedSprite.fromFrames(resources, Sprites.running_button));
    leftButton.rotation = -(float) Math.PI / 2;
    leftButton.sprite.setFPS(12);
    leftButton.scale = BUTTON_SCALE;
    leftButton.position.x = WIDTH * 0.22f - (leftButton.sprite.frameWidth * BUTTON_SCALE / 2);
    leftButton.position.y = HEIGHT - 20;
    leftButton.alpha = 0;

    rightButton = new FakeButtonActor(
        AnimatedSprite.fromFrames(resources, Sprites.running_button));
    rightButton.rotation = (float) Math.PI / 2;
    rightButton.sprite.setFPS(12);
    rightButton.scale = BUTTON_SCALE;
    rightButton.position.x = WIDTH * 0.78f + (rightButton.sprite.frameWidth * BUTTON_SCALE / 2);
    rightButton.position.y = HEIGHT - (rightButton.sprite.frameHeight * BUTTON_SCALE) - 20;
    rightButton.alpha = 0;

    ui.add(instructions);
    ui.add(leftButton);
    ui.add(rightButton);
  }

  // Resets the gameplay variables for a replay or a first play.
  private void resetGame() {
    Log.i(TAG, "Reset game.");

    camera.position.y = 0;

    laneSwitchTimer = 0;

    player.setRunnerState(RunnerState.CROUCH);
    player.setSweat(false);
    player.setCelebrate(false);

    mango.setRunnerState(RunnerState.CROUCH);
    mango.isFinished = false;
    grape.setRunnerState(RunnerState.CROUCH);
    grape.isFinished = false;
    apricot.setRunnerState(RunnerState.CROUCH);
    apricot.isFinished = false;

    tweenManager.removeAll();

    watermelonHasEntered = false;
    playerHasStarted = false;
    cameraLookAhead = 0;

    watermelon.position.y = -HEIGHT * 0.5f;
    watermelon.velocity.y = 0;

    player.position.x = 0;
    player.position.y = PLAYER_INITIAL_POSITION_Y;
    player.velocity.y = 0;

    ribbon.position.y = RIBBON_INITIALIZATION_POSITION_Y;

    synchronized (actors) {
      for (PowerUpActor powerUp : powerUps) {
        actors.remove(powerUp);
      }
    }
    powerUps.clear();

    mango.position.y = OPPONENT_INITIAL_POSITION_Y;
    grape.position.y = OPPONENT_INITIAL_POSITION_Y;
    apricot.position.y = OPPONENT_INITIAL_POSITION_Y;

    mango.velocity.y = 0;
    grape.velocity.y = 0;
    apricot.velocity.y = 0;

    leftButton.alpha = 1;
    rightButton.alpha = 1;

    powerUpTimer = 0;
    mapRow = 0;

    time = 0;
    finishingPlace = 0;
    updateScore();

    eventBus.sendEvent(EventBus.PAUSE_SOUND, R.raw.running_foot_loop_fast);
  }

  private void beginGame() {
    mango.setRunnerState(RunnerState.RUNNING);
    grape.setRunnerState(RunnerState.RUNNING);
    apricot.setRunnerState(RunnerState.RUNNING);

    player.velocity.y = 0;
    watermelon.velocity.y = PLAYER_MAXIMUM_SPEED;

    tweenManager.add(new Tween(0.6f) {
      @Override
      protected void updateValues(float percentDone) {
        mango.velocity.y = percentDone * MANGO_SPEED;
        grape.velocity.y = percentDone * GRAPE_SPEED;
        apricot.velocity.y = percentDone * APRICOT_SPEED;
      }
    });

    eventBus.sendEvent(EventBus.PLAY_SOUND, R.raw.running_foot_loop_fast);
    eventBus.sendEvent(EventBus.PLAY_SOUND, R.raw.running_foot_power_up);
  }

  public void update(float deltaMs) {
    synchronized (this) {
      float timeInSeconds = deltaMs / 1000f;

      camera.update(deltaMs);
      cameraShake.update(deltaMs);

      if (state == State.RUNNING) {
        if (!(player.getRunnerState() == RunnerState.RUNNING_LEFT
            || player.getRunnerState() == RunnerState.RUNNING_RIGHT)) {
          player.setRunnerState(RunnerState.RUNNING);
        }

        if (player.getRunnerState() == RunnerState.RUNNING) {
          updatePlayerSpeed(timeInSeconds);
        }
      }

      if (state != State.TITLE) {
        for (int i = 0; i < actors.size(); i++) {
          actors.get(i).update(deltaMs);
        }

        for (int i = 0; i < ui.size(); i++) {
          ui.get(i).update(deltaMs);
        }
      }

      tweenManager.update(deltaMs);
      synchronized (actors) {
        Collections.sort(actors);
      }

      laneSwitchTimer = Math.max(0, laneSwitchTimer - timeInSeconds);

      if (state == State.RUNNING || (state == State.SUCCESS && finishingPlace == 1)) {
        updateRunningCamera(timeInSeconds);
      }

      if (state == State.RUNNING) {
        // Reads the next row of power ups from PursuitLevel and adds them,
        // Then resets the timer.
        powerUpTimer -= timeInSeconds;
        if (powerUpTimer <= 0) {
          powerUpTimer = 0.18f - 0.06f * (Math.max(0, player.velocity.y) / PLAYER_MAXIMUM_SPEED);
          addPowerUpRow();
          mapRow++;
        }
        checkPowerUps();

        updateWatermelon(timeInSeconds);

        // If player is getting close to the watermelon, make the sprite sweat.
        if (player.position.y - player.getRadius()
            < watermelon.position.y + WatermelonActor.VERTICAL_RADIUS_WORLD * 2.7f) {
          player.setSweat(true);
        } else {
          player.setSweat(false);
        }

        // Check success and fail states based on player's location
        if (watermelonCollidesWithRunner(player)) {
          gameFail();
        } else if (player.position.y > ribbon.position.y) {
          gameSuccess();
        }

        // Update the score
        time += timeInSeconds;
        updateScore();
      }

      checkOpponentsPlayerCollision();
      checkOpponentsFinished();
      checkOpponentsWatermelonCollision();
    }
  }

  // If the state is running, update the camera according to the player's position and speed.
  // If the player is sufficiently fast, the camera trails behind the player a little.
  private void updateRunningCamera(float timeInSeconds) {
    float targetCameraLookahead =
        CAMERA_MAXIMUM_LOOKAHEAD * Math.max(0, player.velocity.y - PLAYER_MINIMUM_SPEED)
            / (PLAYER_MAXIMUM_SPEED - PLAYER_MINIMUM_SPEED);
    float deltaCameraLookahead = (targetCameraLookahead - cameraLookAhead);
    if (deltaCameraLookahead > 0) {
      deltaCameraLookahead *= CAMERA_LOOKAHEAD_DECREASE_PER_SECOND * timeInSeconds;
    } else {
      deltaCameraLookahead *= CAMERA_LOOKAHEAD_INCREASE_PER_SECOND * timeInSeconds;
    }

    cameraLookAhead += deltaCameraLookahead;
    camera.position.y = -PLAYER_INITIAL_POSITION_Y + cameraLookAhead + player.position.y;
  }

  // Decelerates the player.
  private void updatePlayerSpeed(float timeInSeconds) {
    if (!playerHasStarted) {
      player.velocity.y += timeInSeconds * PLAYER_MINIMUM_SPEED * 1.7f;
      if (player.velocity.y > PLAYER_MINIMUM_SPEED) {
        playerHasStarted = true;
      }
    } else {
      player.velocity.y = player.velocity.y - PLAYER_DECELERATION * timeInSeconds;
      // Give speed lower and upper bound
      player.velocity.y = Math.max(player.velocity.y, PLAYER_MINIMUM_SPEED);
      player.velocity.y = Math.min(player.velocity.y, PLAYER_MAXIMUM_SPEED);
    }
  }

  // Changes the watermelon's speed to roughly match the player's.
  private void updateWatermelon(float timeInSeconds) {
    float targetSpeed = BASE_SPEED + ((player.velocity.y - BASE_SPEED) * 1.08f);
    // Give speed lower and upper bound
    targetSpeed = Math.max(targetSpeed, WATERMELON_MINIMUM_SPEED);
    targetSpeed = Math.min(targetSpeed, WATERMELON_MAXIMUM_SPEED);

    float targetSpeedDelta = targetSpeed - watermelon.velocity.y;

    float watermelonPlayerDistance =
        (player.position.y - player.getRadius())
            - (watermelon.position.y + WatermelonActor.VERTICAL_RADIUS_WORLD);

    if (targetSpeedDelta < 0 || !watermelonHasEntered) {
      // The watermelon decrease speed is inversely proportional to the distance between the
      // watermelon and the player. The closer the player the larger the decrease.
      float decreaseFactor = 1 + ((1 / Math.max(1, watermelonPlayerDistance / 40)) * 7f);
      float decreaseSpeed = WATERMELON_SPEED_DECREASE_PER_SECOND * decreaseFactor;
      watermelon.velocity.y += targetSpeedDelta * Math.min(1, decreaseSpeed * timeInSeconds);
    } else if (targetSpeedDelta > 0) {
      // The watermelon increase speed is proportional to the distance between the watermelon and
      // the player. The further the player the larger the increase.
      // The watermelon decrease speed is also affected by the amount of time played. The longer
      // the time the greater the increase.
      float increaseFactor = Math.max(0.6f, 1 + ((watermelonPlayerDistance - 140) / 240));
      float timeFactor = 0.3f + (0.7f * Math.min(1, time / 20f));
      float increaseSpeed = WATERMELON_SPEED_INCREASE_PER_SECOND * increaseFactor * timeFactor;
      watermelon.velocity.y += targetSpeedDelta * Math.min(1, increaseSpeed * timeInSeconds);
    }

    if (watermelon.position.y > camera.position.y) {
      watermelonHasEntered = true;
    }

    if (watermelonHasEntered) {
      watermelon.position.y = Math.max(watermelon.position.y, camera.position.y);
      watermelon.position.y = Math.min(watermelon.position.y, camera.position.y
          - CAMERA_MAXIMUM_LOOKAHEAD * 1.13f);
    }
  }

  private void updateScore() {
    scoreListener.newScore(time);
  }

  // Check power ups for collision with the player.
  private void checkPowerUps() {
    for (int i = powerUps.size() - 1; i >= 0; i--) {
      PowerUpActor powerUp = powerUps.get(i);

      // Check if PowerUp collides with the Player.
      double powerUpDistance = ActorHelper.distanceBetween(player, powerUp);
      if (powerUpDistance < (PowerUpActor.RADIUS_WORLD + player.getRadius())
          && !powerUp.isPickedUp()) {
        powerUpPlayer(powerUp);
      }
    }
  }

  // Checks if any of the opponents collide with the player.
  // If there is a collision, set the player's position to the runner's.
  private void checkOpponentsPlayerCollision() {
    // If the player is currently moving just ignore any collision.
    if (laneSwitchTimer > 0) {
      return;
    }

    for (OpponentActor opponent : opponents) {
      if (opponent.getLane() == player.getLane()) {
        float diffY = player.position.y - opponent.position.y;
        float combinedRadius = opponent.getRadius() + player.getRadius();

        if (0 <= diffY && diffY < combinedRadius) {
          // Player is in front of opponent
          opponent.position.y =
              player.position.y - combinedRadius;
        } else if (-combinedRadius < diffY && diffY < 0) {
          // Player is behind the opponent
          player.position.y =
              opponent.position.y - combinedRadius;
        }
      }
    }
  }

  // Checks if any of the opponents are touching the watermelon. If so, squish the runner.
  private void checkOpponentsWatermelonCollision() {
    for (OpponentActor opponent : opponents) {
      if (opponent.getRunnerState() == RunnerState.RUNNING
          && watermelonCollidesWithRunner(opponent)) {
        eventBus.sendEvent(EventBus.PLAY_SOUND, R.raw.running_foot_power_squish);
        opponent.setRunnerState(RunnerState.DYING);
        opponent.velocity.y = 0;
      }
    }
  }

  private void checkOpponentsFinished() {
    for (final OpponentActor opponent : opponents) {
      if (opponent.position.y > ribbon.position.y) {
        // The opponent crosses the finish line, break the ribbon and decelerate the opponent.

        if (!opponent.isFinished) {
          opponent.isFinished = true;

          final float initialSpeed = opponent.velocity.y;
          tweenManager.add(new Tween(RUNNER_FINISH_DURATION) {
            @Override
            protected void updateValues(float percentDone) {
              if (opponent.state == RunnerState.RUNNING) {
                opponent.velocity.y = (1 - percentDone) * initialSpeed;
                if (percentDone > 0.95f) {
                  opponent.setRunnerState(RunnerState.STANDING);
                }
              } else {
                opponent.velocity.y = 0;
              }
            }
          });
        }
      }
    }
  }

  // TODO: Replace with an ellipse to circle collision model.
  public boolean watermelonCollidesWithRunner(RunnerActor actor) {
    if (actor.position.y - actor.getRadius()
        < watermelon.position.y + WatermelonActor.VERTICAL_RADIUS_WORLD
        && actor.getLane() != INITIAL_LANE) {
      return true;
    }
    return actor.position.y - actor.getRadius()
        < watermelon.position.y + WatermelonActor.VERTICAL_RADIUS_WORLD * 1.2f
        && actor.getLane() == INITIAL_LANE;
  }

  private void gameSetup() {
    Log.i(TAG, "Game Mode: Setup.");
    setState(State.SETUP);

    player.position.y = PLAYER_INITIAL_POSITION_Y;
    mango.position.y = OPPONENT_INITIAL_POSITION_Y;
    grape.position.y = OPPONENT_INITIAL_POSITION_Y;
    apricot.position.y = OPPONENT_INITIAL_POSITION_Y;

    mango.setRunnerState(RunnerState.STANDING);
    grape.setRunnerState(RunnerState.STANDING);
    apricot.setRunnerState(RunnerState.STANDING);

    final Tween mangoCrouchDelay = new EmptyTween(0.6f) {
      @Override
      protected void onFinish() {
        mango.setRunnerState(RunnerState.CROUCH);
      }
    };
    tweenManager.add(mangoCrouchDelay);

    final Tween grapeCrouchDelay = new EmptyTween(0.9f) {
      @Override
      protected void onFinish() {
        grape.setRunnerState(RunnerState.CROUCH);
      }
    };
    tweenManager.add(grapeCrouchDelay);

    final Tween apricotCrouchDelay = new EmptyTween(1.2f) {
      @Override
      protected void onFinish() {
        apricot.setRunnerState(RunnerState.CROUCH);
      }
    };
    tweenManager.add(apricotCrouchDelay);

    runnerEnter(player);

    watermelon.position.y = HEIGHT * -0.5f;

    final Tween setupDuration = new EmptyTween(SETUP_DURATION) {
      @Override
      protected void onFinish() {
        if (state == State.SETUP) {
          gameFirstPlay();
        }
      }
    };
    tweenManager.add(setupDuration);
  }

  private void gameFirstPlay() {
    Log.i(TAG, "Game Mode: Tutorial.");
    instructions.show();
    tweenManager.add(new EmptyTween(TUTORIAL_DURATION) {
      @Override
      protected void onFinish() {
        instructions.hide();
      }
    });
    tweenManager.add(new EmptyTween(TUTORIAL_DURATION + 0.3f) {
      @Override
      protected void onFinish() {
        startCountdownAnimation();
      }
    });
    tweenManager.add(new EmptyTween(TUTORIAL_DURATION + 0.3f + 3f) {
      @Override
      protected void onFinish() {
        gameStart();
        countdownView.post(new Runnable() {
          @Override
          public void run() {
            countdownView.setVisibility(View.INVISIBLE);
          }
        });
      }
    });
    player.setLane(INITIAL_LANE);
    showButtons();
    setState(State.READY);
  }

  public void gameReplay() {
    Log.i(TAG, "Game Restart.");
    resetGame();
    instructions.hide(); // In case the player hits the replay button before the game begins.
    tweenManager.add(new EmptyTween(0.2f) {
      @Override
      protected void onFinish() {
        startCountdownAnimation();
      }
    });
    tweenManager.add(new EmptyTween(0.2f + 3f) {
      @Override
      protected void onFinish() {
        gameStart();
        countdownView.post(new Runnable() {
          @Override
          public void run() {
            countdownView.setVisibility(View.INVISIBLE);
          }
        });
      }
    });
    player.setLane(INITIAL_LANE);
    setState(State.READY);
  }

  private void gameStart() {
    Log.i(TAG, "Game Start.");
    beginGame();
    setState(State.RUNNING);
  }

  private void gameSuccess() {
    Log.i(TAG, "You're winner!");
    // Make the watermelon match the player's speed so the player won't get squished.

    hideButtons();

    int runnersBehindPlayer = 0;
    for (RunnerActor opponent : opponents) {
      if (player.position.y >= opponent.position.y) {
        runnersBehindPlayer++;
      }
    }
    finishingPlace = 4 - runnersBehindPlayer;

    if (finishingPlace == 1) {
      // If the player gets first place, stop the watermelon and decelerate the player.

      // Play the strawberry's celebration animation.
      player.setCelebrate(true);
      final float currentSpeed = player.velocity.y;
      final float targetSpeed = BASE_SPEED * 0.75f;

      tweenManager.add(new Tween(RUNNER_FINISH_DURATION) {
        @Override
        protected void updateValues(float percentDone) {
          float diffSpeed = targetSpeed - currentSpeed;
          player.velocity.y = currentSpeed + (diffSpeed * percentDone);
        }
      });

      tweenManager.add(new Tween(WATERMELON_FINISH_DURATION) {
        @Override
        protected void updateValues(float percentDone) {
          watermelon.velocity.y = currentSpeed * (1 - percentDone);
        }
      });
    } else {
      // If the player does not get first place, stop both the watermelon and the player.

      // If the player finishes fourth, make the strawberry cry.
      if (finishingPlace == 4) {
        player.setSweat(true);
      }

      final float currentSpeed = player.velocity.y;

      tweenManager.add(new Tween(WATERMELON_FINISH_DURATION * 0.5f) {
        @Override
        protected void updateValues(float percentDone) {
          watermelon.velocity.y = currentSpeed * (1 - percentDone);
        }
      });

      tweenManager.add(new Tween(RUNNER_FINISH_DURATION) {
        @Override
        protected void updateValues(float percentDone) {
          if (player.getRunnerState() == RunnerState.RUNNING) {
            player.velocity.y = currentSpeed * (1 - percentDone);
            if (percentDone > 0.95f) {
              player.setRunnerState(RunnerState.STANDING);
              player.velocity.y = 0;
            }
          }
        }
      });
    }

    eventBus.sendEvent(EventBus.PLAY_SOUND, R.raw.bmx_cheering);
    eventBus.sendEvent(EventBus.PAUSE_SOUND, R.raw.running_foot_loop_fast);

    setState(State.SUCCESS);
  }

  private void gameFail() {
    // Change the player's appearance.
    player.setRunnerState(RunnerState.DYING);

    // Stops the player.
    player.velocity.y = 0;

    hideButtons();

    // Zoom the camera on the player.
    zoomCameraTo(-HALF_WIDTH, player.position.y - HEIGHT * 0.75f, camera.scale, 1.5f);

    eventBus.sendEvent(EventBus.PLAY_SOUND, R.raw.bmx_cheering);
    eventBus.sendEvent(EventBus.PAUSE_SOUND, R.raw.running_foot_loop_fast);

    vibrator.vibrate(VIBRATION_MS);

    setState(State.FAIL);
  }

  private void showButtons() {
    tweenManager.add(new ActorTween(leftButton).withAlpha(1).withDuration(0.5f));
    tweenManager.add(new ActorTween(rightButton).withAlpha(1).withDuration(0.5f));
  }

  private void hideButtons() {
    tweenManager.add(new ActorTween(leftButton).withAlpha(0).withDuration(0.5f));
    tweenManager.add(new ActorTween(rightButton).withAlpha(0).withDuration(0.5f));
  }

  private void startCountdownAnimation() {
    countdownView.post(new Runnable() {
      @Override
      public void run() {
        countdownView.setVisibility(View.VISIBLE);
      }
    });
    final NumberFormat numberFormatter = NumberFormat.getInstance(locale);
    countdownBump(numberFormatter.format(3));
    tweenManager.add(new EmptyTween(1) {
      @Override
      protected void onFinish() {
        countdownBump(numberFormatter.format(2));
      }
    });
    tweenManager.add(new EmptyTween(2) {
      @Override
      protected void onFinish() {
        countdownBump(numberFormatter.format(1));
      }
    });
  }

  private void countdownBump(final String text) {
    countdownView.post(new Runnable() {
      @Override
      public void run() {
        float countdownStartScale = countdownView.getScaleX() * 1.25f;
        float countdownEndScale = countdownView.getScaleX();

        countdownView.setVisibility(View.VISIBLE);
        countdownView.setText(text);

        if (!"Nexus 9".equals(Build.MODEL)) {
          ValueAnimator scaleAnimation = UIUtil.animator(200,
              new AccelerateDecelerateInterpolator(),
              new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                  float scaleValue = (float) valueAnimator.getAnimatedValue("scale");
                  countdownView.setScaleX(scaleValue);
                  countdownView.setScaleY(scaleValue);
                }
              },
              UIUtil.floatValue("scale", countdownStartScale, countdownEndScale)
          );
          scaleAnimation.start();
        }
      }
    });
  }

  // Make a row of power ups using the current row from PursuitLevel.
  private void addPowerUpRow() {
    char[] row = map.getRowArray(mapRow);
    float rowPositionY = player.position.y + HEIGHT;
    for (int i = 0; i < row.length; i++) {
      if (row[i] == '1') {
        if (ribbon.position.y > rowPositionY) {
          addPowerUp(getLanePositionX(i), rowPositionY);
        }
      }
    }
  }

  private PowerUpActor addPowerUp(float x, float y) {
    PowerUpActor powerUp =
        new PowerUpActor(x, y, resources);
    powerUp.scale = baseScale;
    synchronized (actors) {
      actors.add(powerUp);
    }
    powerUps.add(powerUp);
    return powerUp;
  }

  // Creates and adds a OpponentActor to the list of all actors.
  private OpponentActor addOpponent(RunnerType type, int lane, float y, float radius) {
    OpponentActor opponent = makeOpponent(type, lane, y, radius);
    synchronized (actors) {
      actors.add(opponent);
    }
    opponents.add(opponent);
    return opponent;
  }

  // Creates a OpponentActor.
  private OpponentActor makeOpponent(RunnerType type, int lane, float y, float radius) {
    OpponentActor opponent = new OpponentActor(resources, type, lane);
    opponent.position.x = getLanePositionX(lane);
    opponent.position.y = y;
    opponent.scale = baseScale;
    opponent.setRadius(radius);
    return opponent;
  }

  // Wait for one second for the title to display and then starts the game.
  private void showTitle() {
    setState(State.TITLE);
    tweenManager.add(new EmptyTween(titleDurationMs / 1000.0f) {
      @Override
      protected void onFinish() {
        if (state == State.TITLE) {
          gameSetup();
        }
      }
    });
  }

  // Handles all incoming motion events. Currently just calls down() and up().
  public void touch(MotionEvent event) {
    final int action = event.getActionMasked();

    int index = event.getActionIndex();
    Point screenSize = AndroidUtils.getScreenSize();
    float touchX = (event.getX(index) / screenSize.x) * PursuitModel.WIDTH;
    float touchY = (event.getY(index) / screenSize.y) * PursuitModel.HEIGHT;

    Vector2D worldPos = camera.getWorldCoords(touchX, touchY);

    if (action == MotionEvent.ACTION_DOWN
        || action == MotionEvent.ACTION_POINTER_DOWN) {
      down(worldPos.x, worldPos.y);
    }
  }

  // Called every time the user touches down on the screen.
  // Currently does not support multi-touch.
  private void down(float touchX, float touchY) {
    Log.i(TAG, "Touch down at: " + touchX + ", " + touchY);

    if (state == State.RUNNING || state == State.READY) {
      int newLane = getLaneNumberFromPositionX(touchX);
      if (newLane > player.getLane() && player.getLane() < MAX_LANE) {
        rightButton.press();
        movePlayer(player.getLane() + 1);
      } else if (newLane < player.getLane() && player.getLane() > MIN_LANE) {
        leftButton.press();
        movePlayer(player.getLane() - 1);
      }
    }
  }

  private void movePlayer(final int newLane) {
    // The player cannot move if it is already moving.
    if (laneSwitchTimer > 0) {
      return;
    }

    // If an opponent and the player are expected to collide after the lane change, the player does
    // not move.
    // If the player is slightly in front of an opponent after the lane change, the player moves and
    // the opponent moves back slightly.
    for (RunnerActor opponent : opponents) {
      if (opponent.getLane() == newLane) {
        float targetOpponentY = opponent.position.y + opponent.velocity.y * LANE_SWITCH_DURATION;
        float targetPlayerY =
            player.position.y + (player.velocity.y * LANE_SWITCH_DURATION * 0.8f);
        float yDistance = targetOpponentY - targetPlayerY;
        float combinedRadius = opponent.getRadius() + player.getRadius();

        if (0.2f * combinedRadius < yDistance && yDistance < combinedRadius) {
          Log.i(TAG, "Cannot switch to lane " + newLane);
          eventBus.sendEvent(EventBus.PLAY_SOUND, R.raw.present_throw_block);
          bumpPlayer(opponent);
          return;
        } else if (-1.2f * combinedRadius < yDistance && yDistance <= 0.2f * combinedRadius) {
          Log.i(TAG, "Switching to lane " + newLane + " with slowdown");
          slowDownOpponent(opponent);
        }
      }
    }

    // Make the tween
    if (state == State.RUNNING) {
      tweenManager.add(new ActorTween(player)
          .toX(getLanePositionX(newLane))
          .withDuration(LANE_SWITCH_DURATION)
          .withInterpolator(Interpolator.LINEAR));
      laneSwitchTimer = LANE_SWITCH_DURATION;
    } else {
      // If the player is not currently running. Play the running left and right animation when
      // switching lanes.
      if (player.lane > newLane) {
        player.setRunnerState(RunnerState.RUNNING_LEFT);
      } else {
        player.setRunnerState(RunnerState.RUNNING_RIGHT);
      }
      tweenManager.add(new ActorTween(player)
          .toX(getLanePositionX(newLane))
          .withDuration(LANE_SWITCH_DURATION * 2f)
          .withInterpolator(Interpolator.LINEAR)
          .whenFinished(new Callback() {
            @Override
            public void call() {
              if (player.getRunnerState() == RunnerState.RUNNING_LEFT
                  || player.getRunnerState() == RunnerState.RUNNING_RIGHT) {
                player.setRunnerState(RunnerState.CROUCH);
              } else if (player.getRunnerState() == RunnerState.CROUCH) {
                player.setRunnerState(RunnerState.RUNNING);
              }
            }
          }));
      laneSwitchTimer = LANE_SWITCH_DURATION * 2f;
    }

    player.setLane(newLane);
    eventBus.sendEvent(EventBus.PLAY_SOUND, R.raw.jumping_jump);
  }

  // Temporarily reduces the speed of an opponent so the player can go in front of it when changing
  // lanes.
  private void slowDownOpponent(final RunnerActor opponent) {

    final float initialSpeed = opponent.velocity.y;
    float targetOpponentY = opponent.position.y + opponent.velocity.y * LANE_SWITCH_DURATION;
    float targetPlayerY =
        player.position.y + (player.velocity.y * LANE_SWITCH_DURATION * 0.8f);
    float diffY = targetOpponentY - targetPlayerY;
    float combinedRadius = opponent.getRadius() + player.getRadius();

    // The further the opponent is head of the opponent, the greater the decrease in speed.
    final float slowDownAmount =
        OPPONENT_SLOW_DOWN_AMOUNT
            + (OPPONENT_SLOW_DOWN_AMOUNT * Math.max(-0.2f, (diffY / combinedRadius)));

    tweenManager.add(new Tween(OPPONENT_SLOW_DOWN_DURATION) {
      @Override
      protected void updateValues(float percentDone) {
        if (opponent.getRunnerState() == RunnerState.RUNNING) {
          float percentageSlowdown = 1 - percentDone;
          float speed =
              initialSpeed - (slowDownAmount * percentageSlowdown);
          opponent.velocity.y = speed;
        } else {
          opponent.velocity.y = 0;
        }
      }
    });
  }

  private void bumpPlayer(RunnerActor opponent) {
    float targetX = player.position.x + (opponent.position.x - player.position.x) * 0.25f;
    float endX = player.position.x;

    final ActorTween outTween = new ActorTween(player)
        .toX(endX)
        .withDuration(LANE_SWITCH_DURATION * 0.5f)
        .withInterpolator(Interpolator.EASE_IN_AND_OUT);

    final ActorTween inTween = new ActorTween(player)
        .toX(targetX)
        .withDuration(LANE_SWITCH_DURATION * 0.5f)
        .withInterpolator(Interpolator.EASE_IN_AND_OUT)
        .whenFinished(new Callback() {
          @Override
          public void call() {
            tweenManager.add(outTween);
          }
        });
    laneSwitchTimer = LANE_SWITCH_DURATION;
    tweenManager.add(inTween);
  }

  private ActorTween runnerEnter(final RunnerActor runner) {
    // Enter from the left.
    runner.setRunnerState(RunnerState.ENTERING);
    eventBus.sendEvent(EventBus.PLAY_SOUND, R.raw.present_throw_character_appear);

    // Stop and stand for 0.5 seconds.
    final Tween standingDelay = new EmptyTween(RUNNER_STANDING_DURATION) {
      @Override
      protected void onFinish() {
        runner.setRunnerState(RunnerState.CROUCH);
      }
    };

    ActorTween tween = new ActorTween(runner)
        .withDuration(RUNNER_ENTER_DURATION)
        .withInterpolator(Interpolator.LINEAR)
        .whenFinished(new Callback() {
          @Override
          public void call() {
            // Crouch down after standing still.
            runner.setRunnerState(RunnerState.STANDING);
            eventBus.sendEvent(EventBus.PAUSE_SOUND, R.raw.present_throw_character_appear);
            tweenManager.add(standingDelay);
          }
        });

    tweenManager.add(tween);
    return tween;
  }

  // Returns the world x of a lane.
  private int getLanePositionX(int lane) {
    return LANE_SIZE * (lane - INITIAL_LANE);
  }

  // Return the lane number corresponding to an area of the screen.
  // Warning: This function may return lane numbers that don't exist. Example: By clicking on the
  // area that is immediately left of lane 0, -1 is returned.
  private int getLaneNumberFromPositionX(float x) {
    int lane = INITIAL_LANE + Math.round(x / LANE_SIZE);
    Log.i(TAG, "Lane: " + lane);
    return lane;
  }

  // Called when the player picks up a power up.
  private void powerUpPlayer(PowerUpActor powerUpActor) {
    powerUpActor.pickUp();
    player.velocity.y += POWER_UP_SPEED_BOOST;
    eventBus.sendEvent(EventBus.PLAY_SOUND, R.raw.running_foot_power_up_fast);
  }

  public void setState(State newState) {
    Log.i(TAG, "State changed to " + newState);
    state = newState;
    if (stateListener != null) {
      stateListener.onStateChanged(newState);
    }
  }

  private void zoomCameraTo(float x, float y, float scale, float secondsDuration) {
    final float x1 = camera.position.x;
    final float y1 = camera.position.y;
    final float camScale1 = camera.scale;
    // Limit (x, y) so that the edge of the camera doesn't go past the normal edges (i.e. the edges
    // at scale==1).
    final float x2 = x;
    final float y2 = y;
    final float camScale2 = scale;

    tweenManager.add(new Tween(secondsDuration) {
      @Override
      protected void updateValues(float percentDone) {
        Interpolator interp = Interpolator.EASE_IN_AND_OUT;

        camera.position.x = interp.getValue(percentDone, x1, x2);
        camera.position.y = interp.getValue(percentDone, y1, y2);

        // Tween the height, then use that to calculate scale, because tweening scale
        // directly leads to a wobbly pan (scale isn't linear).
        float height1 = HEIGHT / camScale1;
        float height2 = HEIGHT / camScale2;
        camera.scale = HEIGHT / interp.getValue(percentDone, height1, height2);
      }
    });
  }

  public void setStateListener(StateChangedListener stateListener) {
    this.stateListener = stateListener;
  }

  public void setScoreListener(ScoreListener listener) {
    this.scoreListener = listener;
    updateScore();
  }

  public void setCountdownView(TextView countdownView) {
    this.countdownView = countdownView;
  }

  public float getScore() {
    return time; // Milliseconds
  }

  public int getFinishingPlace() {
    return finishingPlace;
  }
}
