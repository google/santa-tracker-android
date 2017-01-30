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

import android.content.res.Resources;
import android.graphics.Canvas;

import com.google.android.apps.santatracker.doodles.R;
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.AnimatedSprite;
import com.google.android.apps.santatracker.doodles.shared.AnimatedSprite.AnimatedSpriteListener;
import com.google.android.apps.santatracker.doodles.shared.Sprites;

/**
 * A fruit that runs down the screen.
 */
public class RunnerActor extends Actor {
  /**
   * Currently there are four different kinds of fruits.
   */

  public enum RunnerType {
    STRAWBERRY(Sprites.snowballrun_running_normal, R.drawable.snowballrun_running_starting_runner,
        Sprites.snowballrun_running_sidestep, Sprites.snowballrun_running_appearing, R.drawable.snowballrun_standing_elf,
        Sprites.snowballrun_elf_squish, R.drawable.snowballrun_elf_squished_05),

    APRICOT(Sprites.snowballrun_running_snowman_opponent, R.drawable.snowballrun_running_starting_snowman,
        Sprites.empty_frame, Sprites.empty_frame, R.drawable.snowballrun_standing_snowman,
        Sprites.running_apricot_squish, R.drawable.snowballrun_snowman_squished_09),

    GRAPE(Sprites.snowballrun_running_elf_opponent, R.drawable.snowballrun_running_starting_elfopponent,
        Sprites.empty_frame, Sprites.empty_frame, R.drawable.snowballrun_standing_elfopponent,
        Sprites.running_elfopponent_squish, R.drawable.snowballrun_elfopponent_squished_09),

    MANGO(Sprites.snowballrun_running_reindeer_opponent, R.drawable.snowballrun_running_starting_reindeer,
        Sprites.empty_frame, Sprites.empty_frame, R.drawable.snowballrun_standing_reindeer,
        Sprites.snowballrun_reindeer_squish, R.drawable.snowballrun_reindeer_squished_05);

    public int[] runRes, crouchRes, runLeftRes, enteringRes, standRes, dyingRes, deadRes;

    RunnerType(int[] runRes, int crouchRes,
               int[] runLeftRes, int[] enteringRes, int standRes,
               int[] dyingRes, int deadRes) {

      this.runRes = runRes;
      this.crouchRes = new int[] { crouchRes };
      this.runLeftRes = runLeftRes;
      this.enteringRes = enteringRes;
      this.standRes = new int[] { standRes };
      this.dyingRes = dyingRes;
      this.deadRes = new int[] { deadRes };
    }
  }

  // TODO: Because the running left animation is no longer used for the opponents'
  // entrance, consider moving RUNNING_LEFT and RUNNING_RIGHT to PlayerActor.
  enum RunnerState {
    RUNNING,
    CROUCH,
    ENTERING,
    RUNNING_LEFT,
    RUNNING_RIGHT,
    STANDING,
    DYING,
    DEAD,
  }

  private static final long EYE_BLINK_DELAY_MILLISECONDS = 1300;
  private static final int RUNNING_Z_INDEX = 10;
  private static final int DEAD_Z_INDEX = 3;

  protected int lane;

  protected RunnerType type;

  protected RunnerState state;

  protected float radius;

  protected AnimatedSprite currentSprite;

  protected AnimatedSprite runningSprite;
  protected AnimatedSprite crouchSprite;
  protected AnimatedSprite enteringSprite;
  protected AnimatedSprite runningLeftSprite;
  protected AnimatedSprite runningRightSprite;
  protected AnimatedSprite standingSprite;
  protected AnimatedSprite deadSprite;
  protected AnimatedSprite dyingSprite;

  RunnerActor(Resources resources, RunnerType type, int lane) {
    this.lane = lane;
    this.type = type;

    runningSprite = AnimatedSprite.fromFrames(resources, type.runRes);
    crouchSprite = AnimatedSprite.fromFrames(resources, type.crouchRes);
    enteringSprite = AnimatedSprite.fromFrames(resources, type.enteringRes);
    runningLeftSprite = AnimatedSprite.fromFrames(resources, type.runLeftRes);
    runningRightSprite = AnimatedSprite.fromFrames(resources, type.runLeftRes);
    standingSprite = AnimatedSprite.fromFrames(resources, type.standRes);
    deadSprite = AnimatedSprite.fromFrames(resources, type.deadRes);
    dyingSprite = AnimatedSprite.fromFrames(resources, type.dyingRes);

    enteringSprite.setLoop(false);
    enteringSprite.setFPS((int) ((enteringSprite.getNumFrames() + 1) / PursuitModel.RUNNER_ENTER_DURATION));

    setSpriteAnchorUpright(runningSprite);
    setSpriteAnchorWithYOffset(crouchSprite, 0);
    setSpriteAnchorUpright(enteringSprite);
    setSpriteAnchorUpright(runningLeftSprite);
    setSpriteAnchorUpright(runningRightSprite);
    setSpriteAnchorUpright(standingSprite);
    setSpriteAnchorCenter(deadSprite);
    switch (type) {
      case STRAWBERRY:
        setSpriteAnchorWithYOffset(dyingSprite, 0);
        break;
      case MANGO:
        setSpriteAnchorWithYOffset(dyingSprite, 0);
        break;
      case GRAPE:
        setSpriteAnchorWithYOffset(dyingSprite, 0);
        break;
      case APRICOT:
        setSpriteAnchorWithYOffset(dyingSprite, 0);
        break;
    }

    currentSprite = runningSprite;
    state = RunnerState.RUNNING;
    zIndex = RUNNING_Z_INDEX;
  }

  @Override
  public void update(float deltaMs) {
    super.update(deltaMs);

    if (state == RunnerState.RUNNING) {
      currentSprite.setFPS(3 + (int) (5 * velocity.y / PursuitModel.BASE_SPEED));
    }

    currentSprite.update(deltaMs);
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    if (currentSprite == null) {
      return;
    }

    float runnerScale = scale * 1.50f;
    currentSprite.setScale(runnerScale, runnerScale);
    currentSprite.setPosition(position.x, position.y);

    if (currentSprite == runningRightSprite) {
      currentSprite.setScale(-runnerScale, runnerScale);
    }

    currentSprite.draw(canvas);
  }

  public void setLane(int lane) {
    this.lane = lane;
  }

  public int getLane() {
    return lane;
  }

  public void setRadius(float radius) {
    this.radius = radius;
  }

  public float getRadius() {
    return radius;
  }

  public void setRunnerState(RunnerState state) {
    if (this.state == state) {
      return;
    }

    this.state = state;

    switch (state) {
      case RUNNING:
        currentSprite = runningSprite;
        break;
      case CROUCH:
        currentSprite = crouchSprite;
        break;
      case ENTERING:
        currentSprite = enteringSprite;
        break;
      case RUNNING_LEFT:
        currentSprite = runningLeftSprite;
        break;
      case RUNNING_RIGHT:
        currentSprite = runningRightSprite;
        break;
      case STANDING:
        currentSprite = standingSprite;
        break;
      case DYING:
        currentSprite = dyingSprite;
        dyingSprite.setLoop(false);
        dyingSprite.setFrameIndex(0);
        dyingSprite.clearListeners();

        int frame = 3;
        switch(type) {
          case APRICOT:
            frame = 2;
            break;
          case GRAPE:
            frame = 2;
            break;
        }
        final int finalFrame = frame;

        dyingSprite.addListener(new AnimatedSpriteListener() {
          @Override
          public void onFrame(int index) {
            super.onFrame(index);
            if (index == finalFrame) {
              setRunnerState(RunnerState.DEAD);
            }
          }
        });
        break;
      case DEAD:
        currentSprite = deadSprite;
        break;
    }

    if (state == RunnerState.DEAD) {
      zIndex = DEAD_Z_INDEX;
    } else {
      zIndex = RUNNING_Z_INDEX;
    }
  }

  public RunnerState getRunnerState() {
    return state;
  }

  // yOffset is in percentage not game units, or pixels
  // yOffset of 0.5f centers the sprite vertically
  // yOffset of 0 draws the sprite starting from its y position

  protected static void setSpriteAnchorCenter(AnimatedSprite sprite) {
    setSpriteAnchorWithYOffset(sprite, sprite.frameHeight * 0.5f);
  }

  protected static void setSpriteAnchorUpright(AnimatedSprite sprite) {
    setSpriteAnchorWithYOffset(sprite, 0);
  }

  protected static void setSpriteAnchorWithYOffset(AnimatedSprite sprite, float yOffset) {
    sprite.setAnchor(sprite.frameWidth * 0.5f, sprite.frameHeight - yOffset);
  }

}

