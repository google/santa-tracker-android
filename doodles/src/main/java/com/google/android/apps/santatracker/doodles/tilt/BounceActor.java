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

import com.google.android.apps.santatracker.doodles.R;
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.EventBus;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.physics.Polygon;
import com.google.android.apps.santatracker.doodles.shared.physics.Polygon.LineSegment;
import com.google.android.apps.santatracker.doodles.shared.physics.Util;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An actor which causes a colliding object to bounce off of it.
 */
public class BounceActor extends CollisionActor {
  public static final String TYPE = "Bouncy";

  private static final float VIBRATE_VELOCITY_THRESHOLD = 150;

  public BounceActor(Polygon collisionBody) {
    super(collisionBody);
  }

  /**
   * Cause the other actor to bounce off of this actor.
   * Implementation based on: http://goo.gl/2gcLVd
   */
  @Override
  public boolean resolveCollision(Actor other, float deltaMs) {
    // Short-circuit this call if a bounding box check is not passed.
    if (!collisionBody.isInverted() &&
        !Util.pointIsWithinBounds(collisionBody.min, collisionBody.max, other.position)) {
      // For a non-inverted polygon, we will collide with the body by moving inside of it. If the
      // other actor is not inside of the bounds of the collision body, it cannot cause a collision.
      return false;
    }

    float deltaSeconds = deltaMs / 1000.0f;
    Vector2D relativeVelocity = Vector2D.get(other.velocity.x - velocity.x,
        other.velocity.y - velocity.y);

    // Find which line segment was crossed in order to intersect with the collision actor.
    LineSegment intersectingSegment = collisionBody.getIntersectingLineSegment(
        other.position, other.positionBeforeFrame);
    if (intersectingSegment == null) {
      relativeVelocity.release();
      return false;
    }
    Vector2D normal = intersectingSegment.getDirection().toNormal();

    // Calculate relative velocity in terms of the normal direction.
    float velocityAlongNormal = relativeVelocity.dot(normal);

    // Don't collide if velocities are separating.
    if (velocityAlongNormal > 0) {
      relativeVelocity.release();
      normal.release();
      return false;
    } else if (velocityAlongNormal < -VIBRATE_VELOCITY_THRESHOLD) {
      EventBus.getInstance().sendEvent(EventBus.VIBRATE);
      EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.golf_hit_wall);
    }

    // Correct the position by moving the other actor along the normal of the colliding line
    // segment until it passes back over the colliding segment. This works for the general case.
    // Add the normal vector so that collisions with tiny velocities aren't affected by floating
    // point precision.
    Vector2D correctedPosition = Vector2D.get(other.position).add(normal).subtract(
        normal.x * velocityAlongNormal * deltaSeconds,
        normal.y * velocityAlongNormal * deltaSeconds);

    // This code checks to see if the corrected position causes the actor's path to intersect
    // another line segment of the polygon. This could happen if the actor is colliding with a
    // corner of this collision object. If this is the case, then we should just do the safe thing
    // and undo the other actor's position update before applying the impulse.
    intersectingSegment = collisionBody.getIntersectingLineSegment(
        other.positionBeforeFrame, correctedPosition);
    boolean movedBall = false;
    if (intersectingSegment != null) {
      // It is possible, if moving through the corner of an obstacle, that this correction causes
      // the colliding actor to fall through this actor anyway. Correct for that case by moving
      // the colliding actor back to its original position.
      other.position.set(other.positionBeforeFrame);
    } else {
      other.position.set(correctedPosition);
      movedBall = true;
    }

    // Calculate restitution.
    float e = Math.min(other.restitution, restitution);

    // Calculate impulse.
    float j = -(1 + e) * velocityAlongNormal;
    j /= other.inverseMass + inverseMass;

    // Apply impulse.
    Vector2D impulse = normal.scale(j);
    other.velocity.add(impulse.x * other.inverseMass, impulse.y * other.inverseMass);
    velocity.subtract(impulse.x * inverseMass, impulse.y * inverseMass);

    relativeVelocity.release();
    correctedPosition.release();
    normal.release();
    return movedBall;
  }

  @Override
  public String getType() {
    return BounceActor.TYPE;
  }

  public static BounceActor fromJSON(JSONObject json) throws JSONException {
    return new BounceActor(Polygon.fromJSON(json.getJSONArray(POLYGON_KEY)));
  }
}
