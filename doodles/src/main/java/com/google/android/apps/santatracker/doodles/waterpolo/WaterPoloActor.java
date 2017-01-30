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
package com.google.android.apps.santatracker.doodles.waterpolo;


import android.graphics.Canvas;
import android.util.Log;
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.ActorTween.Callback;
import com.google.android.apps.santatracker.doodles.shared.AnimatedSprite;
import com.google.android.apps.santatracker.doodles.shared.AnimatedSprite.AnimatedSpriteListener;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.physics.Util;
import com.google.android.apps.santatracker.doodles.tilt.ColoredRectangleActor;
import java.util.HashMap;

/**
 * Actor for both the player and the opponent in the water polo game.
 * The debug position marker is omitted.
 */
public class WaterPoloActor extends Actor {
  private static final String TAG = WaterPoloActor.class.getSimpleName();

  /**
   * Labels for all the different sprites which make up the actor.
   */
  public enum WaterPoloActorPart {
    BodyIdle,
    BodyIdleNoBall, // Used at end of game.
    BodyEntrance, // Used when character enters the game.
    BodyLeft,
    BodyRight,
    BodyBlock,
    BodyThrow,
    BodyPickUpBall, // Catch a new ball from the slide.
  }

  private static final int RELEASE_THROW_FRAME = 2;

  HashMap<WaterPoloActorPart, AnimatedSprite> sprites;
  WaterPoloActorPart body;
  AnimatedSprite currentSprite;

  float collisionWidthUnscaled;
  float collisionHeightUnscaled;
  ColoredRectangleActor collisionBox;
  Callback shotBlockCallback;

  public WaterPoloActor() {
    sprites = new HashMap<>();
    body = WaterPoloActorPart.BodyIdle;
  }

  @Override
  public void update(float deltaMs) {
    super.update(deltaMs);
    if (currentSprite == null) {
      return;
    }
    currentSprite.update(deltaMs);
    if (collisionBox != null) {
      collisionBox.dimens =
          Vector2D.get(collisionWidthUnscaled * scale, collisionHeightUnscaled * scale);

      // The collision box should be centered on the sprite.
      float centerX = position.x - (currentSprite.anchor.x) + (currentSprite.frameWidth * 0.5f);
      float centerY = position.y - (currentSprite.anchor.y) + (currentSprite.frameHeight * 0.5f);
      collisionBox.position.set(centerX - collisionBox.dimens.x * 0.5f,
          centerY - collisionBox.dimens.y * 0.5f);
    }
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    if (currentSprite == null) {
      Log.e(TAG, "Body part null: " + body);
      return;
    }
    currentSprite.setPosition(position.x, position.y);
    currentSprite.setScale(scale, scale);
    currentSprite.setHidden(hidden);
    currentSprite.draw(canvas);
  }

  // Add a sprite to the actor for the given part. You need to call this for a part before
  // trying to use that part. Offsets are measured from the actor's position
  public void addSprite(WaterPoloActorPart part, int xOffset, int yOffset, AnimatedSprite sprite) {
    // After picking up the ball, go straight to idle.
    if (part == WaterPoloActorPart.BodyPickUpBall) {
      sprite.addListener(new AnimatedSpriteListener() {
        @Override
        public void onLoop() {
          idle();
        }
      });
    }

    // This makes sure the sprite scales from self.position
    sprite.setAnchor(-xOffset, yOffset);

    if (part != body) {
      sprite.setHidden(true);
    }
    sprites.put(part, sprite);
  }

  // The collision box position, relative to self.position, is hardcoded in update, because the
  // opponents all have the same position.
  public void setCollisionBox(float width, float height) {
    collisionWidthUnscaled = width;
    collisionHeightUnscaled = height;
    collisionBox = new ColoredRectangleActor(
        Vector2D.get(0, 0),
        Vector2D.get(0, 0),
        ColoredRectangleActor.UNSPECIFIED);
  }

  // Returns YES iff (x, y) lies within the actor's collision box.
  public boolean canBlock(float x, float y) {
    if (hidden || body == WaterPoloActorPart.BodyEntrance || collisionBox == null) {
      return false;
    }
    Vector2D worldCoords = Vector2D.get(x, y);
    Vector2D lowerRight = Vector2D.get(collisionBox.position).add(collisionBox.dimens);
    return Util.pointIsWithinBounds(collisionBox.position, lowerRight, worldCoords);
  }

  public void idle() {
    setBodyUnchecked(WaterPoloActorPart.BodyIdle);
  }

  public void idleNoBall() {
    setBodyUnchecked(WaterPoloActorPart.BodyIdleNoBall);
  }

  public void pickUpBall() {
    setBodyUnchecked(WaterPoloActorPart.BodyPickUpBall);
  }

  public void swimLeft() {
    if (body == WaterPoloActorPart.BodyBlock) {
      AnimatedSprite sprite = sprites.get(WaterPoloActorPart.BodyBlock);
      sprite.clearListeners();
      sprite.addListener(new AnimatedSpriteListener() {
        @Override
        public void onLoop() {
          setBodyUnchecked(WaterPoloActorPart.BodyLeft);
        }

        @Override
        public void onFrame(int index) {
          if (index == 3) {
            if (shotBlockCallback != null) {
              shotBlockCallback.call();
              shotBlockCallback = null;
            }
          }
        }
      });
    } else {
      setBodyUnchecked(WaterPoloActorPart.BodyLeft);
    }
  }

  public void swimRight() {
    if (body == WaterPoloActorPart.BodyBlock) {
      AnimatedSprite sprite = sprites.get(WaterPoloActorPart.BodyBlock);
      sprite.clearListeners();
      sprite.addListener(new AnimatedSpriteListener() {
        @Override
        public void onLoop() {
          setBodyUnchecked(WaterPoloActorPart.BodyRight);
        }

        @Override
        public void onFrame(int index) {
          if (index == 3) {
            if (shotBlockCallback != null) {
              shotBlockCallback.call();
              shotBlockCallback = null;
            }
          }
        }
      });
    } else {
      setBodyUnchecked(WaterPoloActorPart.BodyRight);
    }
  }

  // The callback gets called when the actor is at the top of the blocking jump (so the game
  // can deflect the ball at that instant).
  public void blockShot(final Callback callback) {
    AnimatedSprite sprite = sprites.get(WaterPoloActorPart.BodyBlock);
    sprite.clearListeners();
    final WaterPoloActorPart previousBody = body;
    shotBlockCallback = callback;
    sprite.addListener(new AnimatedSpriteListener() {
      @Override
      public void onLoop() {
        setBodyUnchecked(previousBody);
      }

      @Override
      public void onFrame(int index) {
        if (index == 3) {
          if (shotBlockCallback != null) {
            shotBlockCallback.call();
            shotBlockCallback = null;
          }
        }
      }
    });
    setBodyUnchecked(WaterPoloActorPart.BodyBlock);
  }

  // releaseCallback gets called when the actor releases the ball (so the game can swap in the real
  // ball). endCallback will be called when the actor is done throwing and starts picking up another
  // ball (so the game can start the grapeOnSlide animation at the correct time).
  public void throwBall(final Callback releaseCallback, final Callback endCallback) {
    AnimatedSprite sprite = sprites.get(WaterPoloActorPart.BodyThrow);
    sprite.clearListeners();
    sprite.addListener(new AnimatedSpriteListener() {
      @Override
      public void onLoop() {
        pickUpBall();
        endCallback.call();
      }

      @Override
      public void onFrame(int index) {
        if (index == RELEASE_THROW_FRAME) {
          releaseCallback.call();
        }
      }
    });
    setBodyUnchecked(WaterPoloActorPart.BodyThrow);
  }

  // Callback is called at the end of the animation, when actor is ready to play.
  public void enter(final Callback callback) {
    AnimatedSprite sprite = sprites.get(WaterPoloActorPart.BodyEntrance);
    sprite.clearListeners();
    sprite.addListener(new AnimatedSpriteListener() {
      @Override
      public void onLoop() {
        callback.call();
      }
    });
    setBodyUnchecked(WaterPoloActorPart.BodyEntrance);
  }

  private void setBodyUnchecked(WaterPoloActorPart part) {
    AnimatedSprite newSprite = sprites.get(part);
    if (newSprite == null) {
      Log.e(TAG, "Error: sprite " + part + " not loaded.");
      assert(false);
    }
    newSprite.setFrameIndex(0);
    body = part;
    update(0);

    currentSprite = newSprite;
  }

}
