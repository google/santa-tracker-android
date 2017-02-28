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
import android.graphics.Color;
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.physics.Polygon;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A BounceActor which can be toggled on an off with trigger polygons.
 */
public class ToggleableBounceActor extends BounceActor {
  public static final String TYPE = "Toggleable Bouncy";
  protected static final String ON_TRIGGER_KEY = "on trigger";
  protected static final String OFF_TRIGGER_KEY = "off trigger";

  private CollisionActor onTrigger;
  private CollisionActor offTrigger;
  public boolean enabled;

  public ToggleableBounceActor(Polygon collisionBody,
      CollisionActor onTrigger, CollisionActor offTrigger) {
    super(collisionBody);
    this.onTrigger = onTrigger;
    this.offTrigger = offTrigger;

    collisionBody.setPaintColors(Color.RED, Color.LTGRAY, 0x6400ff00);
    onTrigger.collisionBody.setPaintColors(Color.GREEN, Color.RED, 0x6400ff00);
    offTrigger.collisionBody.setPaintColors(Color.BLACK, Color.RED, 0x6400ff00);
  }

  @Override
  public void update(float deltaMs) {
    super.update(deltaMs);
    onTrigger.update(deltaMs);
    offTrigger.update(deltaMs);
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    onTrigger.draw(canvas);
    offTrigger.draw(canvas);
  }

  @Override
  public String getType() {
    return ToggleableBounceActor.TYPE;
  }

  @Override
  public boolean canHandleTouchAt(Vector2D worldCoords, float cameraScale) {
    return super.canHandleTouchAt(worldCoords, cameraScale)
        || onTrigger.canHandleTouchAt(worldCoords, cameraScale)
        || offTrigger.canHandleTouchAt(worldCoords, cameraScale);
  }

  @Override
  public void startTouchAt(Vector2D worldCoords, float cameraScale) {
    super.startTouchAt(worldCoords, cameraScale);
    onTrigger.startTouchAt(worldCoords, cameraScale);
    offTrigger.startTouchAt(worldCoords, cameraScale);
  }

  @Override
  public boolean handleMoveEvent(Vector2D delta) {
    return super.handleMoveEvent(delta)
        || onTrigger.handleMoveEvent(delta) || offTrigger.handleMoveEvent(delta);
  }

  @Override
  public boolean handleLongPress() {
    return super.handleLongPress() || onTrigger.handleLongPress() || offTrigger.handleLongPress();
  }

  @Override
  public boolean resolveCollision(Actor other, float deltaMs) {
    // Resolve trigger collisions.
    if (onTrigger.collisionBody.contains(other.position)) {
      enabled = true;
      collisionBody.setPaintColors(Color.RED, Color.WHITE, 0x6400ff00);
    } else if (offTrigger.collisionBody.contains(other.position)) {
      enabled = false;
      collisionBody.setPaintColors(Color.RED, Color.LTGRAY, 0x6400ff00);
    }

    // Handle the actual collision.
    if (enabled) {
      return super.resolveCollision(other, deltaMs);
    }
    return false;
  }

  @Override
  public JSONObject toJSON() throws JSONException {
    JSONObject json = super.toJSON();
    json.put(ON_TRIGGER_KEY, onTrigger.toJSON());
    json.put(OFF_TRIGGER_KEY, offTrigger.toJSON());
    return json;
  }

  public static ToggleableBounceActor fromJSON(JSONObject json) throws JSONException {
    return new ToggleableBounceActor(Polygon.fromJSON(json.getJSONArray(POLYGON_KEY)),
        CollisionActor.fromJSON(json.getJSONObject(ON_TRIGGER_KEY)),
        CollisionActor.fromJSON(json.getJSONObject(OFF_TRIGGER_KEY)));
  }
}
