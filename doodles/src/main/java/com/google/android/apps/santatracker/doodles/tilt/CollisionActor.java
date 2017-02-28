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

import android.graphics.Canvas;

import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.physics.Polygon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An actor which represents an object in the world which causes some reaction when it is collided
 * with.
 */
public class CollisionActor extends Actor implements Touchable {
  public static final String TYPE = "collision";
  protected static final String POLYGON_KEY = "polygon";

  // These measurements are in "world units", which are really just arbitrary units where one world
  // unit == one pixel at the default camera zoom level.
  public static final float DEFAULT_WIDTH = 200;
  public static final float DEFAULT_HEIGHT = 200;
  public Polygon collisionBody;

  protected int selectedIndex = -1;
  protected int midpointIndex = -1;

  public CollisionActor(Polygon collisionBody) {
    super(Vector2D.get(collisionBody.min.x, collisionBody.min.y), Vector2D.get());
    this.collisionBody = collisionBody;
    restitution = 0.7f;
    inverseMass = Actor.INFINITE_MASS; // Give collision actors infinite mass.
    zIndex = 10;
  }

  @Override
  public void update(float deltaMs) {
    positionBeforeFrame.set(position);
    float deltaX = velocity.x * deltaMs / 1000.0f;
    float deltaY = velocity.y * deltaMs / 1000.0f;
    collisionBody.move(deltaX, deltaY);
    position.set(collisionBody.min);
  }

  @Override
  public boolean canHandleTouchAt(Vector2D worldCoords, float cameraScale) {
    return collisionBody.getSelectedIndex(worldCoords, cameraScale) >= 0
        || collisionBody.getMidpointIndex(worldCoords, cameraScale) >= 0;
  }

  @Override
  public void startTouchAt(Vector2D worldCoords, float cameraScale) {
    selectedIndex = collisionBody.getSelectedIndex(worldCoords, cameraScale);
    midpointIndex = collisionBody.getMidpointIndex(worldCoords, cameraScale);
  }

  @Override
  public boolean handleMoveEvent(Vector2D delta) {
    if (selectedIndex >= 0) {
      Vector2D positionDelta = Vector2D.get(delta).scale(-1);
      collisionBody.moveVertex(selectedIndex, positionDelta);
      positionDelta.release();
      return true;
    }
    return false;
  }

  @Override
  public boolean handleLongPress() {
    if (selectedIndex >= 0) {
      if (canRemoveCollisionVertex()) {
        // If we can, just remove the vertex.
        collisionBody.removeVertexAt(selectedIndex);
        return true;
      }
    } else if (midpointIndex >= 0) {
      // Long press on a midpoint, add a vertex to the selected obstacle's polygon.
      collisionBody.addVertexAfter(midpointIndex);
      return true;
    }
    return false;
  }

  @Override
  public String getType() {
    return CollisionActor.TYPE;
  }

  @Override
  public JSONObject toJSON() throws JSONException {
    JSONObject json = new JSONObject();
    json.put(TYPE_KEY, getType());
    json.put(POLYGON_KEY, collisionBody.toJSON());
    return json;
  }

  /**
   * Resolve a collision with another physics actor.
   *
   * @param other The actor being collided with.
   * @param deltaMs The length of the collision frame.
   * @return true if resolving the collision moves the other actor, false otherwise.
   */
  public boolean resolveCollision(Actor other, float deltaMs) {
    return false;
  }

  public void draw(Canvas canvas) {
    collisionBody.draw(canvas);
  }

  public boolean canRemoveCollisionVertex() {
    return collisionBody.vertices.size() > 3;
  }

  public static CollisionActor fromJSON(JSONObject json) throws JSONException {
    return new CollisionActor(Polygon.fromJSON(json.getJSONArray(POLYGON_KEY)));
  }
}
