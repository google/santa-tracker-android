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

import android.content.res.Resources;
import android.graphics.Canvas;

import com.google.android.apps.santatracker.doodles.R;
import com.google.android.apps.santatracker.doodles.shared.AnimatedSprite;
import com.google.android.apps.santatracker.doodles.shared.AnimatedSprite.AnimatedSpriteListener;
import com.google.android.apps.santatracker.doodles.shared.CallbackProcess;
import com.google.android.apps.santatracker.doodles.shared.EventBus;
import com.google.android.apps.santatracker.doodles.shared.GameFragment;
import com.google.android.apps.santatracker.doodles.shared.MultiSpriteActor;
import com.google.android.apps.santatracker.doodles.shared.ProcessChain;
import com.google.android.apps.santatracker.doodles.shared.Sprites;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.WaitProcess;
import com.google.android.apps.santatracker.doodles.shared.physics.Polygon;
import com.google.android.apps.santatracker.doodles.shared.physics.Util;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The player-controlled swimmer in the swimming game.
 */
public class SwimmerActor extends BoundingBoxSpriteActor {
  private static final String TAG = SwimmerActor.class.getSimpleName();

  private static final float ACCELERATION_Y = -500;
  private static final float MIN_SPEED = 400;
  private static final float DEFAULT_MAX_SPEED = 800;
  private static final long SPEED_STEP_DURATION_MS = 10000;
  private static final float TILT_VELOCITY = 10000;

  public static final float SWIMMER_SCALE = 1.6f;
  public static final int DIVE_DURATION_MS = 1500;
  public static final int DIVE_COOLDOWN_MS = 5000;
  public static final String SWIMMER_ACTOR_TYPE = "swimmer";
  public static final String KICKOFF_IDLE_SPRITE = "kickoff_idle";
  public static final String KICKOFF_START_SPRITE = "kickoff_start";
  public static final String RINGS_SPRITE = "rings";
  public static final String SWIM_LOOP_SPRITE = "swimming";
  public static final String CAN_COLLIDE_SPRITE = "can_collide";
  public static final String FREEZE_SPRITE = "freeze";
  public static final String DIVE_DOWN_SPRITE = "dive";
  public static final String UNDER_LOOP_SPRITE = "under_loop";
  public static final String RISE_UP_SPRITE = "rise_up";
  public static final float KICKOFF_IDLE_Y_OFFSET = -240;

  private static final Vector2D[] VERTEX_OFFSETS = {
      Vector2D.get(0, 0), Vector2D.get(96, 0),
      Vector2D.get(96, 90), Vector2D.get(0, 90)
  };

  private static final Map<String, Vector2D> OFFSET_MAP;

  static {
    OFFSET_MAP = new HashMap<>();
    OFFSET_MAP.put(KICKOFF_IDLE_SPRITE, Vector2D.get(0, 0));
    OFFSET_MAP.put(KICKOFF_START_SPRITE, Vector2D.get(0, 0));
    OFFSET_MAP.put(RINGS_SPRITE, Vector2D.get(-60, -20)); // TODO
    OFFSET_MAP.put(SWIM_LOOP_SPRITE, Vector2D.get(0, 0));
    OFFSET_MAP.put(CAN_COLLIDE_SPRITE, Vector2D.get(0, 0));
    OFFSET_MAP.put(FREEZE_SPRITE, Vector2D.get(0, 0));
    OFFSET_MAP.put(DIVE_DOWN_SPRITE, Vector2D.get(0, 0));
    OFFSET_MAP.put(UNDER_LOOP_SPRITE, Vector2D.get(0, 0));
    OFFSET_MAP.put(RISE_UP_SPRITE, Vector2D.get(0, 0));
  }

  public boolean controlsEnabled = true;
  public boolean isInvincible = false;
  public boolean isUnderwater = false;
  public boolean isDead = false;

  private MultiSpriteActor multiSpriteActor;
  private AnimatedSprite canCollideSprite;
  private AnimatedSprite freezeSprite;
  private String collidedObjectType;

  private AnimatedSprite ringsSprite;
  private Vector2D ringsSpriteOffset;

  private float restartSpeed = MIN_SPEED;
  private float maxSpeed = DEFAULT_MAX_SPEED;
  private long currentSpeedStepTime = 0;

  private boolean diveEnabled = false;
  private float targetX;

  private List<ProcessChain> processChains = new ArrayList<>();

  public SwimmerActor(Polygon collisionBody, MultiSpriteActor spriteActor) {
    super(collisionBody, spriteActor,
        Vector2D.get(OFFSET_MAP.get(KICKOFF_IDLE_SPRITE)).scale(SWIMMER_SCALE), SWIMMER_ACTOR_TYPE);

    multiSpriteActor = spriteActor;
    canCollideSprite = multiSpriteActor.sprites.get(CAN_COLLIDE_SPRITE);
    canCollideSprite.setLoop(false);
    freezeSprite = multiSpriteActor.sprites.get(FREEZE_SPRITE);
    freezeSprite.setLoop(false);
    multiSpriteActor.sprites.get(DIVE_DOWN_SPRITE).addListener(new AnimatedSpriteListener() {
      @Override
      public void onLoop() {
        zIndex = -3;
        setSprite(UNDER_LOOP_SPRITE);
      }
    });
    multiSpriteActor.sprites.get(RISE_UP_SPRITE).addListener(new AnimatedSpriteListener() {
      @Override
      public void onLoop() {
        zIndex = 0;
        setSprite(SWIM_LOOP_SPRITE);
        EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.swimming_dive_up);
      }
    });
    multiSpriteActor.sprites.get(SWIM_LOOP_SPRITE).addListener(new AnimatedSpriteListener() {
      @Override
      public void onFrame(int index) {
        if (index == 5 || index == 13) {
          EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.swimming_ice_splash_a);
        }
      }
    });

    ringsSprite = multiSpriteActor.sprites.get(RINGS_SPRITE);
    ringsSprite.setPaused(true);
    ringsSprite.setHidden(true);
    ringsSprite.setScale(SWIMMER_SCALE, SWIMMER_SCALE);
    ringsSprite.addListener(new AnimatedSpriteListener() {
      @Override
      public void onLoop() {
        ringsSprite.setHidden(true);
        ringsSprite.setPaused(true);
      }
    });
    ringsSpriteOffset = Vector2D.get(OFFSET_MAP.get(RINGS_SPRITE)).scale(SWIMMER_SCALE);

    multiSpriteActor.sprites.get(KICKOFF_START_SPRITE).addListener(new AnimatedSpriteListener() {
      @Override
      public void onLoop() {
        diveEnabled = true;
        diveDown();
      }

      @Override
      public void onFrame(int index) {
        if (index == 3) {
          velocity.set(0, -restartSpeed);
        }
      }
    });

    zIndex = 0;
    alpha = 1.0f;
    scale = SWIMMER_SCALE;
    targetX = position.x;
  }

  @Override
  public void update(float deltaMs) {
    super.update(deltaMs);

    ProcessChain.updateChains(processChains, deltaMs);

    // Update x position based on tilt.
    float frameVelocityX = TILT_VELOCITY * deltaMs / 1000;
    float positionDeltaX = targetX - position.x;
    if (Math.abs(positionDeltaX) < frameVelocityX) {
      // We will overshoot if we apply the frame velocity. Just go straight to the target position.
      moveTo(targetX, position.y);
    } else {
      moveTo(position.x + Math.signum(positionDeltaX) * frameVelocityX, position.y);
    }

    // Update acceleration and frame rate if necessary.
    if (velocity.getLength() > 1) {
      velocity.y = Math.max(-maxSpeed, velocity.y + ACCELERATION_Y * deltaMs / 1000);
      if (velocity.y == -maxSpeed) {
        multiSpriteActor.sprites.get(SWIM_LOOP_SPRITE).setFPS(24);
      } else {
        multiSpriteActor.sprites.get(SWIM_LOOP_SPRITE).setFPS(48);
      }
      currentSpeedStepTime += deltaMs;
      if (currentSpeedStepTime > SPEED_STEP_DURATION_MS) {
        maxSpeed += 500;
        currentSpeedStepTime = 0;
      }
    }

    ringsSprite.update(deltaMs);
    ringsSprite.setPosition(spriteActor.position.x + ringsSpriteOffset.x,
        spriteActor.position.y + ringsSpriteOffset.y);
  }

  @Override
  public void draw(Canvas canvas) {
    if (hidden) {
      return;
    }
    spriteActor.draw(canvas, spriteOffset.x, spriteOffset.y,
        spriteActor.sprite.frameWidth * scale, spriteActor.sprite.frameHeight * scale);
    ringsSprite.draw(canvas);
    collisionBody.draw(canvas);
  }

  @Override
  public JSONObject toJSON() {
    return null;
  }

  public void setSprite(String key) {
    multiSpriteActor.setSprite(key);
    spriteOffset.set(OFFSET_MAP.get(key)).scale(SWIMMER_SCALE);
  }

  public void moveTo(float x, float y) {
    collisionBody.moveTo(x, y);
    position.set(x, y);
    spriteActor.position.set(x, y);
    targetX = x;
  }

  public void updateTargetPositionFromTilt(Vector2D tilt, float levelWidth) {
    if (controlsEnabled) {
      // Decrease the amount of tilt necessary to move the swimmer.
      float tiltPercentage = (float) (tilt.x / (Math.PI / 2));
      tiltPercentage *= 2.5f;

      int levelPadding = 60;
      targetX = Util.clamp(
          (levelWidth / 2) - (collisionBody.getWidth() / 2) + (tiltPercentage * levelWidth / 2),
          0 - collisionBody.getWidth() + levelPadding,
          levelWidth - (2 * collisionBody.getWidth()) - levelPadding);
    }
  }

  public void startSwimming() {
    setSprite(KICKOFF_START_SPRITE);
  }

  public void collide(String objectType) {
    restartSpeed = Math.max(MIN_SPEED, Math.abs(velocity.y / 4));
    maxSpeed = restartSpeed;
    currentSpeedStepTime = 0;
    moveTo(positionBeforeFrame.x, positionBeforeFrame.y);
    velocity.set(0, 0);
    controlsEnabled = false;

    if (objectType.equals(DUCK) || objectType.equals(ICE_CUBE)) {
      EventBus.getInstance().sendEvent(EventBus.SHAKE_SCREEN);
      EventBus.getInstance().sendEvent(EventBus.VIBRATE);

      if (objectType.equals(DUCK)) {
        setSprite(SwimmerActor.CAN_COLLIDE_SPRITE);
        EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.swimming_duck_collide);
      } else {  // Ice cube.
        setSprite(SwimmerActor.FREEZE_SPRITE);
        EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.swimming_ice_collide);
      }
    } else {  // Octopus.
      // Just play the sound for the octopus. It vibrates later (when it actually grabs the lemon).
      EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.swimming_grab);
    }
    collidedObjectType = objectType;
    isDead = true;
  }

  public void endGameWithoutCollision() {
    controlsEnabled = false;
    collidedObjectType = HAND_GRAB;
    isDead = true;
  }

  public String getCollidedObjectType() {
    return collidedObjectType;
  }

  public void diveDown() {
    if (controlsEnabled && diveEnabled) {
      EventBus.getInstance().sendEvent(EventBus.SWIMMING_DIVE);
      EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.swimming_dive_down);
      ringsSprite.setHidden(false);
      ringsSprite.setPaused(false);
      isUnderwater = true;
      setSprite(DIVE_DOWN_SPRITE);
      diveEnabled = false;

      ProcessChain waitThenRiseUp = new WaitProcess(DIVE_DURATION_MS).then(new CallbackProcess() {
        @Override
        public void updateLogic(float deltaMs) {
          isUnderwater = false;
          setSprite(RISE_UP_SPRITE);
        }
      }).then(new WaitProcess(DIVE_COOLDOWN_MS)).then(new CallbackProcess() {
        @Override
        public void updateLogic(float deltaMs) {
          diveEnabled = true;
        }
      });
      processChains.add(waitThenRiseUp);
    }
  }

  public static final SwimmerActor create(Vector2D position, Resources res,
      final GameFragment gameFragment) {
    if (gameFragment.isDestroyed) {
      return null;
    }
    Map<String, AnimatedSprite> spriteMap = new HashMap<>();
    spriteMap.put(KICKOFF_IDLE_SPRITE,
        AnimatedSprite.fromFrames(res, Sprites.penguin_swim_idle));
    if (gameFragment.isDestroyed) {
      return null;
    }
    spriteMap.put(KICKOFF_START_SPRITE,
        AnimatedSprite.fromFrames(res, Sprites.penguin_swim_start));
    if (gameFragment.isDestroyed) {
      return null;
    }
    spriteMap.put(RINGS_SPRITE,
        AnimatedSprite.fromFrames(res, Sprites.swimming_rings));
    if (gameFragment.isDestroyed) {
      return null;
    }
    spriteMap.put(SWIM_LOOP_SPRITE,
        AnimatedSprite.fromFrames(res, Sprites.penguin_swim_swimming));
    if (gameFragment.isDestroyed) {
      return null;
    }
    spriteMap.put(CAN_COLLIDE_SPRITE,
        AnimatedSprite.fromFrames(res, Sprites.penguin_swim_dazed));
    if (gameFragment.isDestroyed) {
      return null;
    }
    spriteMap.put(FREEZE_SPRITE,
        AnimatedSprite.fromFrames(res, Sprites.penguin_swim_frozen));
    if (gameFragment.isDestroyed) {
      return null;
    }
    spriteMap.put(DIVE_DOWN_SPRITE,
        AnimatedSprite.fromFrames(res, Sprites.penguin_swim_descending));
    if (gameFragment.isDestroyed) {
      return null;
    }
    spriteMap.put(UNDER_LOOP_SPRITE,
        AnimatedSprite.fromFrames(res, Sprites.penguin_swim_swimmingunderwater));
    if (gameFragment.isDestroyed) {
      return null;
    }
    spriteMap.put(RISE_UP_SPRITE,
        AnimatedSprite.fromFrames(res, Sprites.penguin_swim_ascending));
    if (gameFragment.isDestroyed) {
      return null;
    }
    MultiSpriteActor spriteActor =
        new MultiSpriteActor(spriteMap, KICKOFF_IDLE_SPRITE, position, Vector2D.get(0, 0));
    if (gameFragment.isDestroyed) {
      return null;
    }

    return new SwimmerActor(getBoundingBox(position, VERTEX_OFFSETS, SWIMMER_SCALE), spriteActor);
  }
}
