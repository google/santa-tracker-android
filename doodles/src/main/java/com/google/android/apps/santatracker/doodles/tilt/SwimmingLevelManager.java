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

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A level manager for the Pineapple 2016 swimming game.
 */
public class SwimmingLevelManager extends LevelManager<SwimmingModel> {

  public static File levelsDir = null;

  private static final int DEFAULT_OBSTACLE_Y = -1000;

  public SwimmingLevelManager(Context context) {
    super(context);
  }

  @Override
  public SwimmingModel loadDefaultLevel() {
    SwimmingModel model = getEmptyModel();

    List<String> types = new ArrayList<>(BoundingBoxSpriteActor.TYPE_TO_RESOURCE_MAP.keySet());
    for (int i = 0; i < types.size(); i++) {
      int x = i * SwimmingModel.LEVEL_WIDTH / types.size();
      int y = DEFAULT_OBSTACLE_Y;
      BoundingBoxSpriteActor obstacle =
          BoundingBoxSpriteActor.create(Vector2D.get(x, y), types.get(i), context.getResources());
      model.addActor(obstacle);
    }
    return model;
  }

  @Override
  protected File getLevelsDir() {
    if (levelsDir == null) {
      levelsDir = new File(Environment.getExternalStorageDirectory(), "swimming_levels");
    }
    return levelsDir;
  }

  @Override
  Actor loadActorFromJSON(JSONObject json) throws JSONException {
    String type = json.getString(Actor.TYPE_KEY);
    Actor actor = null;
    if (BoundingBoxSpriteActor.TYPE_TO_RESOURCE_MAP.containsKey(type)) {
      actor = BoundingBoxSpriteActor.fromJSON(json, context);
    } else {
      Log.w(TAG, "Unable to create object of type: " + type);
    }
    return actor;
  }

  @Override
  protected SwimmingModel getEmptyModel() {
    return new SwimmingModel();
  }
}
