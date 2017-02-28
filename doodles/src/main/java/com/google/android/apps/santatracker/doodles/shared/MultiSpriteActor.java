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
package com.google.android.apps.santatracker.doodles.shared;

import android.content.res.Resources;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * An actor which has multiple sprites which it can switch between.
 */
public class MultiSpriteActor extends SpriteActor {
  private static final String TAG = MultiSpriteActor.class.getSimpleName();
  public Map<String, AnimatedSprite> sprites;

  public MultiSpriteActor(Map<String, AnimatedSprite> sprites, String selectedSpriteKey,
      Vector2D position, Vector2D velocity) {
    super(sprites.get(selectedSpriteKey), position, velocity);
    this.sprites = sprites;
  }

  public void setSprite(String key) {
    if (sprites.containsKey(key)) {
      sprite = sprites.get(key);
    } else {
      Log.w(TAG, "Couldn't set sprite, unrecognized key: " + key);
    }
  }

  /**
   * A class which makes it easier to re-construct MultiSpriteActors.
   */
  public static class Data {
    public String key;
    public int[] idList;
    public int numFrames;
    public Data(String key, int[] idList) {
      this.key = key;
      this.idList = idList;
      this.numFrames = idList.length;
    }
    public AnimatedSprite getSprite(Resources resources) {
      if (idList != null) {
        return AnimatedSprite.fromFrames(resources, idList);
      }
      return null;
    }
  }

  public static MultiSpriteActor create(
      Data[] data, String selectedSprite, Vector2D position, Resources resources) {
    Map<String, AnimatedSprite> sprites = new HashMap<>();
    for (int i = 0; i < data.length; i++) {
      sprites.put(data[i].key, data[i].getSprite(resources));
    }
    return new MultiSpriteActor(sprites, selectedSprite, position, Vector2D.get());
  }
}
