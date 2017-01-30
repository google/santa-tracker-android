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
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.WatermelonBaseActor;
import com.google.android.apps.santatracker.doodles.shared.physics.Polygon;
import com.google.android.apps.santatracker.doodles.shared.physics.Util;
import java.util.ArrayList;
import java.util.List;

/**
 * An actor for a watermelon which rolls down the screen.
 */
public class WatermelonActor extends BounceActor {
  private static final float SCALE = 2.0f;
  private static final float LEG_WIDTH = 50;

  public long ageMs;
  public long delayMs;
  private WatermelonBaseActor melonActor;

  private Vector2D spriteOffset = Vector2D.get(-LEG_WIDTH, 0);

  public WatermelonActor(WatermelonBaseActor actor, Polygon collisionBody) {
    super(collisionBody);
    this.melonActor = actor;
  }

  public void setVelocity(Vector2D velocity) {
    this.velocity.set(velocity);
    melonActor.velocity.set(velocity);
  }

  public void setPosition(Vector2D position) {
    float collisionBodyOffsetX = position.x - this.position.x;
    float collisionBodyOffsetY = position.y - this.position.y;
    collisionBody.move(collisionBodyOffsetX, collisionBodyOffsetY);

    this.position.set(position).add(spriteOffset);
    melonActor.position.set(position)
        .add(spriteOffset.scale(0.5f))
        .add(melonActor.bodySprite.frameWidth, melonActor.bodySprite.frameHeight);
  }

  @Override
  public void update(float deltaMs) {
    super.update(deltaMs);
    melonActor.update(deltaMs);
    ageMs += deltaMs;
  }

  @Override
  public void draw(Canvas canvas) {
    melonActor.draw(canvas);
    super.draw(canvas);
  }

  @Override
  public boolean resolveCollision(Actor other, float deltaMs) {
    // Just do bounding box checking since the watermelon's hitbox is a rectangle.
    if (Util.pointIsWithinBounds(collisionBody.min, collisionBody.max, other.position)) {
      return false;
    }
    return false;
  }

  public static WatermelonActor create(Vector2D position, Resources resources) {
    WatermelonBaseActor actor = new WatermelonBaseActor(resources);
    Polygon collisionBody = getCollisionPolygon(position,
        actor.bodySprite.frameWidth * SCALE, 3 * actor.bodySprite.frameHeight / 4 * SCALE);
    actor.scale = SCALE;

    WatermelonActor watermelon = new WatermelonActor(actor, collisionBody);
    watermelon.setPosition(position);
    return watermelon;
  }

  private static Polygon getCollisionPolygon(Vector2D spritePosition, float width, float height) {
    List<Vector2D> vertices = new ArrayList<>();
    // top left
    vertices.add(Vector2D.get(spritePosition.x + LEG_WIDTH, spritePosition.y));
    // top right
    vertices.add(Vector2D.get(spritePosition.x + width, spritePosition.y));
    // bottom right
    vertices.add(
        Vector2D.get(spritePosition.x + width, spritePosition.y + height));
    // bottom left
    vertices.add(
        Vector2D.get(spritePosition.x + LEG_WIDTH, spritePosition.y + height));
    return new Polygon(vertices);
  }
}
