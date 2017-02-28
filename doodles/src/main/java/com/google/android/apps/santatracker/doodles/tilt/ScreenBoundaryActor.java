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
import com.google.android.apps.santatracker.doodles.shared.physics.Polygon;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An actor which defines a screen boundary. Used for camera positioning.
 */
public class ScreenBoundaryActor extends CollisionActor {
  public static final String TYPE = "Screen boundary";

  private static int currentZIndex = 0;

  public ScreenBoundaryActor(Polygon collisionBody) {
    super(collisionBody);
    // orange, green, semi-transparent green.
    collisionBody.setPaintColors(0xffffa500, Color.BLACK, 0x6400ff00);

    // Give ScreenBoundaryActors increasing z-indices, as these determine sort order, and
    // the behavior of ScreenBoundaryActors is based on sort order.
    zIndex = currentZIndex++;
  }

  @Override
  public boolean resolveCollision(Actor other, float deltaMs) {
    return false;
  }

  @Override
  public String getType() {
    return ScreenBoundaryActor.TYPE;
  }

  public static ScreenBoundaryActor fromJSON(JSONObject json) throws JSONException {
    return new ScreenBoundaryActor(Polygon.fromJSON(json.getJSONArray(POLYGON_KEY)));
  }
}
