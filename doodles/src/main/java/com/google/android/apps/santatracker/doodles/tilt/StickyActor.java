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

import android.graphics.Color;
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.physics.Polygon;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An actor which slows down the colliding actor upon entry.
 */
public class StickyActor extends CollisionActor {
  public static final String TYPE = "Sticky";
  // A "stickiness" factor, which is essentially friction, but doesn't take into account the tilt
  // of the device.
  private float stickiness = 2.0f;
  public boolean isEnabled = true;

  public StickyActor(Polygon collisionBody) {
    super(collisionBody);
    // cyan, yellow, semi-transparent green.
    collisionBody.setPaintColors(Color.CYAN, 0xffffff00, 0x6400ff00);
  }

  /**
   * Cause the other actor to be slowed down by this actor.
   */
  @Override
  public boolean resolveCollision(Actor other, float deltaMs) {
    if (!isEnabled || other.velocity.getLength() == 0 || !collisionBody.contains(other.position)) {
      // Don't bother resolving the collision if the other actor is either not moving, or if the
      // other actor is not colliding with this object.
      return false;
    }
    float deltaSeconds = deltaMs / 1000.0f;
    Vector2D acceleration = Vector2D.get(other.velocity).normalize();
    if (other.velocity.getLength() < 400) {
      acceleration.scale(0.01f * stickiness * Constants.GRAVITY);
    } else {
      acceleration.scale(stickiness * Constants.GRAVITY);
    }
    if (Math.signum(other.velocity.x - acceleration.x * deltaSeconds)
        != Math.signum(other.velocity.x)) {
      // Don't allow stickiness to accelerate the ball in the other direction.
      other.velocity.x = 0;
      other.velocity.y = 0;
    } else {
      other.velocity.x -= acceleration.x * deltaSeconds;
      other.velocity.y -= acceleration.y * deltaSeconds;
    }
    return false;
  }

  @Override
  public String getType() {
    return StickyActor.TYPE;
  }

  public static StickyActor fromJSON(JSONObject json) throws JSONException {
    return new StickyActor(Polygon.fromJSON(json.getJSONArray(POLYGON_KEY)));
  }
}
