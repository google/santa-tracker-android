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

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.CallbackProcess;
import com.google.android.apps.santatracker.doodles.shared.Camera;
import com.google.android.apps.santatracker.doodles.shared.CameraShake;
import com.google.android.apps.santatracker.doodles.shared.EventBus;
import com.google.android.apps.santatracker.doodles.shared.EventBus.EventBusListener;
import com.google.android.apps.santatracker.doodles.shared.Process;
import com.google.android.apps.santatracker.doodles.shared.ProcessChain;
import com.google.android.apps.santatracker.doodles.shared.RectangularInstructionActor;
import com.google.android.apps.santatracker.doodles.shared.UIUtil;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.WaitProcess;
import com.google.android.apps.santatracker.doodles.shared.physics.Util;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The model for the swimming game.
 */
public class SwimmingModel implements TiltModel, EventBusListener {
  private static final String TAG = SwimmingModel.class.getSimpleName();

  public static final int LEVEL_WIDTH = 1280;
  public static final int[] SCORE_THRESHOLDS = { 30, 50, 100 };

  private static final float SHAKE_FREQUENCY = 33;
  private static final float SHAKE_MAGNITUDE = 40;
  private static final float SHAKE_FALLOFF = 0.9f;
  private static final long VIBRATION_DURATION_MS = 50;
  private static final int WORLD_TO_METER_RATIO = 700;
  private static final long WAITING_STATE_DELAY_MS = 1000;
  private static final long COUNTDOWN_DELAY_MS = 4000;
  private static final float COUNTDOWN_BUMP_SCALE = 1.5f;

  public final List<String> collisionObjectTypes;

  public String levelName;
  public boolean collisionMode = true;

  public List<Actor> actors;
  public List<Actor> uiActors; // Will be drawn above actors.
  public Camera camera;
  public CameraShake cameraShake;
  public SwimmerActor swimmer;
  public RectangularInstructionActor instructions;
  public TextView countdownView;
  public ObstacleManager obstacleManager;
  public Vibrator vibrator;
  public Locale locale;
  public int distanceMeters;
  public int currentScoreThreshold = 0;

  public int screenWidth;
  public int screenHeight;
  public Vector2D tilt;

  // Measures the time elapsed in the current state. This value is reset to 0 upon entering a new
  // state.
  public long timeElapsed = 0;
  private float countdownTimeMs = 3000;
  private List<ProcessChain> processChains = new ArrayList<>();
  public int playCount;

  /**
   * States for the swimming game.
   */
  public enum SwimmingState {
    INTRO,
    WAITING,
    SWIMMING,
    FINISHED,
  }
  private SwimmingState state;

  public SwimmingModel() {
    tilt = Vector2D.get(0, 0);
    actors = new ArrayList<>();
    uiActors = new ArrayList<>();
    state = SwimmingState.INTRO;

    collisionObjectTypes = new ArrayList<>();
    collisionObjectTypes.addAll(BoundingBoxSpriteActor.TYPE_TO_RESOURCE_MAP.keySet());
  }

  @Override
  public void onEventReceived(int type, Object data) {
    switch(type) {
      case EventBus.VIBRATE:
        if (vibrator != null) {
          vibrator.vibrate(VIBRATION_DURATION_MS);
        }
        break;

      case EventBus.SHAKE_SCREEN:
        cameraShake.shake(SHAKE_FREQUENCY, SHAKE_MAGNITUDE, SHAKE_FALLOFF);
        break;

      case EventBus.GAME_STATE_CHANGED:
        SwimmingState state = (SwimmingState) data;
        if (state == SwimmingState.WAITING) {
          long countdownDelayMs = WAITING_STATE_DELAY_MS;
          if (playCount == 0) {
            // Wait for the crossfade to finish then show instructions.
            processChains.add(new WaitProcess(WAITING_STATE_DELAY_MS).then(new CallbackProcess() {
              @Override
              public void updateLogic(float deltaMs) {
                if (getState() == SwimmingState.WAITING) {
                  instructions.show();
                }
              }
            }).then(new WaitProcess(COUNTDOWN_DELAY_MS).then(new CallbackProcess() {
              @Override
              public void updateLogic(float deltaMs) {
                instructions.hide();
              }
            })));
            // If we're showing the instructions, wait until the instructions is hidden before
            // starting the countdown.
            countdownDelayMs += COUNTDOWN_DELAY_MS + 300;
          }
          // Start countdown.
          processChains.add(new WaitProcess(countdownDelayMs).then(new Process() {
            @Override
            public void updateLogic(float deltaMs) {
              final float oldCountdownTimeMs = countdownTimeMs;
              float newCountdownTimeMs = countdownTimeMs - deltaMs;
              if ((long) newCountdownTimeMs / 1000 != (long) oldCountdownTimeMs / 1000) {
                countdownView.post(new Runnable() {
                  @Override
                  public void run() {
                    countdownView.setVisibility(View.VISIBLE);
                    // Use the old integer value so that the countdown goes 3, 2, 1 and not 2, 1, 0.
                    String countdownValue =
                        NumberFormat.getInstance(locale).format((long) oldCountdownTimeMs / 1000);
                    setTextAndBump(countdownView, countdownValue);
                  }
                });
              }
              countdownTimeMs = newCountdownTimeMs;
            }

            @Override
            public boolean isFinished() {
              return countdownTimeMs <= 0;
            }
          }).then(new CallbackProcess() {
            @Override
            public void updateLogic(float deltaMs) {
              countdownView.post(new Runnable() {
                @Override
                public void run() {
                  countdownView.setVisibility(View.INVISIBLE);
                }
              });
              setState(SwimmingState.SWIMMING);
            }
          }));
        } else if (state == SwimmingState.SWIMMING) {
          swimmer.startSwimming();
        }
    }
  }

  public void setState(SwimmingState state) {
    if (this.state != state) {
      this.state = state;
      timeElapsed = 0;
      EventBus.getInstance().sendEvent(EventBus.GAME_STATE_CHANGED, state);
    }
  }

  public SwimmingState getState() {
    return state;
  }

  public void update(float deltaMs) {
    synchronized (this) {
      timeElapsed += deltaMs;

      ProcessChain.updateChains(processChains, deltaMs);

      for (int i = 0; i < actors.size(); i++) {
        actors.get(i).update(deltaMs);
      }

      for (int i = 0; i < uiActors.size(); i++) {
        uiActors.get(i).update(deltaMs);
      }

      if (state == SwimmingState.SWIMMING || state == SwimmingState.WAITING) {
        swimmer.updateTargetPositionFromTilt(tilt, LEVEL_WIDTH);

        int newDistance = getMetersFromWorldY(swimmer.position.y);
        if (newDistance != distanceMeters) {
          if (newDistance >= SwimmingLevelChunk.LEVEL_LENGTH_IN_METERS) {
            swimmer.endGameWithoutCollision();
          }
          distanceMeters = Math.min(SwimmingLevelChunk.LEVEL_LENGTH_IN_METERS, newDistance);
          EventBus.getInstance().sendEvent(EventBus.SCORE_CHANGED, distanceMeters);
        }
        if (swimmer.isDead) {
          setState(SwimmingState.FINISHED);
        }

        resolveCollisions(deltaMs);

        clampCameraPosition();
      }
    }
  }

  public void clampCameraPosition() {
    if (!SwimmingFragment.editorMode) {
      float swimmerHeight = swimmer.collisionBody.getHeight();
      float minCameraOffset = camera.toWorldScale(screenHeight) - 3.5f * swimmerHeight;
      float maxCameraOffset = camera.toWorldScale(screenHeight) - 4.0f * swimmerHeight;
      camera.position.set(camera.position.x, Util.clamp(camera.position.y,
          swimmer.position.y - minCameraOffset, swimmer.position.y - maxCameraOffset));
    }
  }

  public void resolveCollisions(float deltaMs) {
    obstacleManager.resolveCollisions(swimmer, deltaMs);
  }

  public void drawActors(Canvas canvas) {
    List<Actor> actorsToDraw = new ArrayList<>(actors);
    actorsToDraw.addAll(obstacleManager.getActors());
    Collections.sort(actorsToDraw);
    for (int i = 0; i < actorsToDraw.size(); i++) {
      actorsToDraw.get(i).draw(canvas);
    }
  }

  public void drawUiActors(Canvas canvas) {
    for (int i = 0; i < uiActors.size(); i++) {
      uiActors.get(i).draw(canvas);
    }
  }

  public int getStarCount() {
    return currentScoreThreshold;
  }

  public void onTouchDown() {
    if (getState() == SwimmingState.SWIMMING) {
      swimmer.diveDown();
    }
  }

  public void createActor(Vector2D position, String objectType, Resources resources) {
    if (BoundingBoxSpriteActor.TYPE_TO_RESOURCE_MAP.containsKey(objectType)) {
      actors.add(BoundingBoxSpriteActor.create(position, objectType, resources));
    }
  }

  public void sortActors() {
    Collections.sort(actors);
  }

  @Override
  public List<Actor> getActors() {
    return actors;
  }

  @Override
  public void addActor(Actor actor) {
    if (actor instanceof SwimmerActor) {
      this.swimmer = (SwimmerActor) actor;
    } else if (actor instanceof Camera) {
      this.camera = (Camera) actor;
    } else if (actor instanceof CameraShake) {
      this.cameraShake = (CameraShake) actor;
    } else if (actor instanceof ObstacleManager) {
      this.obstacleManager = (ObstacleManager) actor;
    }
    actors.add(actor);
    sortActors();
  }

  public void addUiActor(Actor actor) {
    if (actor instanceof RectangularInstructionActor) {
      this.instructions = (RectangularInstructionActor) actor;
    }
    uiActors.add(actor);
  }

  public void setCountdownView(TextView countdownView) {
    this.countdownView = countdownView;
  }

  @Override
  public void setLevelName(String levelName) {
    this.levelName = levelName;
  }

  public static int getMetersFromWorldY(float distance) {
    return Math.max(0, (int) (-distance / WORLD_TO_METER_RATIO));
  }

  public static int getWorldYFromMeters(int meters) {
    return -meters * WORLD_TO_METER_RATIO;
  }

  private void setTextAndBump(final TextView textView, String text) {
    float endScale = textView.getScaleX();
    float startScale = COUNTDOWN_BUMP_SCALE * textView.getScaleX();
    textView.setText(text);
    if (!"Nexus 9".equals(Build.MODEL)) {
      ValueAnimator scaleAnimation = UIUtil.animator(200,
          new AccelerateDecelerateInterpolator(),
          new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
              float scaleValue = (float) valueAnimator.getAnimatedValue("scale");
              textView.setScaleX(scaleValue);
              textView.setScaleY(scaleValue);
            }
          },
          UIUtil.floatValue("scale", startScale, endScale)
      );
      scaleAnimation.start();
    }
  }
}
