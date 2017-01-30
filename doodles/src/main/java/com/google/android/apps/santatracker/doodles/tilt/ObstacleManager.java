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
import com.google.android.apps.santatracker.doodles.shared.Actor;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A class for managing obstacles in the swimming game.
 */
public class ObstacleManager extends Actor {
  private static final String TAG = ObstacleManager.class.getSimpleName();
  private static final int NUM_INITIAL_CHUNKS = 4;
  private static final int RETAIN_THRESHOLD = 2000;

  private LinkedList<SwimmingLevelChunk> levelChunks;
  private SwimmerActor swimmer;

  public ObstacleManager(SwimmerActor swimmer, Context context) {
    levelChunks = new LinkedList<>();
    SwimmingLevelChunk.generateAllLevelChunks(-1000, context);
    for (int i = 1; i < NUM_INITIAL_CHUNKS; i++) {
      levelChunks.add(SwimmingLevelChunk.getNextChunk());
    }
    this.swimmer = swimmer;
    zIndex = 1;
  }

  @Override
  public void update(float deltaMs) {
    for (int i = 0; i < levelChunks.size(); i++) {
      levelChunks.get(i).update(deltaMs);
    }

    if (!levelChunks.isEmpty()) {
      SwimmingLevelChunk lastChunk = levelChunks.getLast();
      // If the swimmer is within 2000 units of the end of the last chunk, add a new one.
      if (swimmer.position.y - lastChunk.endY < RETAIN_THRESHOLD) {
        SwimmingLevelChunk nextChunk = SwimmingLevelChunk.getNextChunk();
        if (nextChunk != null) {
          levelChunks.add(nextChunk);
        }
      }

      if (levelChunks.getFirst().endY - swimmer.position.y > RETAIN_THRESHOLD) {
        levelChunks.remove(0);
      }
    }
  }

  public void resolveCollisions(SwimmerActor swimmer, float deltaMs) {
    for (int i = 0; i < levelChunks.size(); i++) {
      levelChunks.get(i).resolveCollisions(swimmer, deltaMs);
    }
  }

  public List<Actor> getActors() {
    List<Actor> actors = new ArrayList<>();
    for (int i = 0; i < levelChunks.size(); i++) {
      actors.addAll(levelChunks.get(i).obstacles);
    }
    return actors;
  }
}
