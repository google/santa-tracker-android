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
import com.google.android.apps.santatracker.doodles.shared.AnimatedSprite;
import com.google.android.apps.santatracker.doodles.shared.AnimatedSprite.AnimatedSpriteListener;
import com.google.android.apps.santatracker.doodles.shared.EventBus;
import com.google.android.apps.santatracker.doodles.shared.MultiSpriteActor;
import com.google.android.apps.santatracker.doodles.shared.Sprites;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.physics.Polygon;
import java.util.HashMap;
import java.util.Map;

/**
 * The hand grab obstacle in the swimming game.
 */
public class HandGrabActor extends BoundingBoxSpriteActor {
  private static final String X_SPRITE = "x";
  private static final String LEMON_GRAB_SPRITE = "lemon grab";
  private static final String LEMON_GRAB_SPRITE_FLIPPED = "lemon grab flipped";

  private static final float COLLISION_DISTANCE_THRESHOLD = 100;
  private static final Vector2D[] VERTEX_OFFSETS = {
      Vector2D.get(0, 0),
      Vector2D.get(110f, 0),
      Vector2D.get(110f, 146.0f),
      Vector2D.get(0, 146.0f)
  };

  private static final Map<String, Vector2D> OFFSET_MAP;
  static {
    OFFSET_MAP = new HashMap<>();
    OFFSET_MAP.put(X_SPRITE, Vector2D.get());
    OFFSET_MAP.put(LEMON_GRAB_SPRITE, Vector2D.get(-100, -100));
    OFFSET_MAP.put(LEMON_GRAB_SPRITE_FLIPPED, Vector2D.get(100, -100));
  }

  public HandGrabActor(Polygon collisionBody, MultiSpriteActor spriteActor) {
    super(collisionBody, spriteActor,
        Vector2D.get(OFFSET_MAP.get(X_SPRITE)).scale(SwimmerActor.SWIMMER_SCALE), HAND_GRAB);
    zIndex = -1;
    scale = SwimmerActor.SWIMMER_SCALE;
  }

  @Override
  public void draw(Canvas canvas) {
    if (hidden) {
      return;
    }
    super.draw(canvas);
  }

  public void setSprite(String key) {
    if (key.equals(X_SPRITE)) {
      zIndex = -1;
    } else if (key.equals(LEMON_GRAB_SPRITE)) {
      zIndex = 3;
    }
    ((MultiSpriteActor) spriteActor).setSprite(key);

    if (isFlippedX() && key.equals(LEMON_GRAB_SPRITE)) {
      spriteOffset.set(OFFSET_MAP.get(LEMON_GRAB_SPRITE_FLIPPED)).scale(scale);
    } else {
      spriteOffset.set(OFFSET_MAP.get(key)).scale(scale);
    }
  }

  public boolean isFlippedX() {
    return ((MultiSpriteActor) spriteActor).sprites.get(LEMON_GRAB_SPRITE).isFlippedX();
  }

  @Override
  protected boolean resolveCollisionInternal(final SwimmerActor swimmer) {
    if (swimmer.isInvincible || swimmer.isUnderwater) {
      return false;
    }

    // Only collide if the two actor's minimum collision coordinates are close together. This is
    // so that the swimmer will only collide with the hand grab if it is right over the x.
    if (swimmer.collisionBody.min.distanceTo(collisionBody.min) < COLLISION_DISTANCE_THRESHOLD) {
      swimmer.collide(type);

      setSprite(LEMON_GRAB_SPRITE);
      spriteActor.sprite.addListener(new AnimatedSpriteListener() {
        @Override
        public void onFrame(int index) {
          if (index == 10) {
            swimmer.hidden = true;
            swimmer.spriteActor.sprite.setPaused(true);
            EventBus.getInstance().sendEvent(EventBus.VIBRATE);
          }

        }

        @Override
        public void onFinished() {
          hidden = true;
          swimmer.isDead = true;
        }
      });
    }
    return false;
  }

  public static HandGrabActor create(Vector2D position, Resources res) {
    Map<String, AnimatedSprite> spriteMap = new HashMap<>();
    boolean shouldFlip = position.x + VERTEX_OFFSETS[1].x / 2 < SwimmingModel.LEVEL_WIDTH / 2;

    AnimatedSprite lemonGrabSprite = AnimatedSprite.fromFrames(res, Sprites.penguin_swim_canegrab);
    lemonGrabSprite.setLoop(false);
    lemonGrabSprite.setFlippedX(shouldFlip);

    spriteMap.put(LEMON_GRAB_SPRITE, lemonGrabSprite);
    spriteMap.put(X_SPRITE, AnimatedSprite.fromFrames(res, Sprites.penguin_swim_candy));

    MultiSpriteActor spriteActor =
        new MultiSpriteActor(spriteMap, X_SPRITE, position, Vector2D.get(0, 0));

    Polygon boundingBox = getBoundingBox(position, VERTEX_OFFSETS, SwimmerActor.SWIMMER_SCALE);

    return new HandGrabActor(boundingBox, spriteActor);
  }
}
