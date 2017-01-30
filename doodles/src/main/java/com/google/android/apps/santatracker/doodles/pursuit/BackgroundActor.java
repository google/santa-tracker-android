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
package com.google.android.apps.santatracker.doodles.pursuit;

import android.content.res.Resources;
import android.graphics.Canvas;
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.AnimatedSprite;
import com.google.android.apps.santatracker.doodles.shared.Camera;
import com.google.android.apps.santatracker.doodles.shared.Sprites;

/**
 * A background actor that follows the camera.
 */
public class BackgroundActor extends Actor {

  private AnimatedSprite backgroundSprite;

  private AnimatedSprite treeSprite;
  private AnimatedSprite umbrellaSprite;

  private Camera camera;

  public BackgroundActor(Resources resources, Camera camera) {
    this.camera = camera;

    backgroundSprite =
        AnimatedSprite.fromFrames(resources, Sprites.snowballrunner_background);
    treeSprite =
        AnimatedSprite.fromFrames(resources, Sprites.snowball_runner_trees1);
    umbrellaSprite =
        AnimatedSprite.fromFrames(resources, Sprites.snowball_runner_trees2);

    backgroundSprite.setAnchor(backgroundSprite.frameWidth / 2, 0);
  }

  @Override
  public void draw(Canvas canvas) {
    backgroundSprite.setScale(scale, scale);
    float h = backgroundSprite.frameHeight * scale * (960f / 980f);

    // Always draws the background at 0, 0, where the camera is.
    // Draws the background sprite three times so the background is not cut off on larger
    // devices where the device dimension exceeds the background sprite's size.
    backgroundSprite.setPosition(position.x, camera.position.y + -camera.position.y % h);
    backgroundSprite.draw(canvas);

    backgroundSprite.setPosition(position.x, camera.position.y + h + (-camera.position.y % h));
    backgroundSprite.draw(canvas);

    backgroundSprite.setPosition(position.x, camera.position.y - h + (-camera.position.y % h));
    backgroundSprite.draw(canvas);
  }

  public void drawTop(Canvas canvas) {
    float h = backgroundSprite.frameHeight * scale;

    // Makes repeating trees and umbrellas with integer division.
    float rightTreeY = h * 0.5f + ((h * 2) * ((int) ((camera.position.y + h) / (h * 2))));
    float rightUmbrellaY = h * 1.5f + ((h * 2) * ((int) ((camera.position.y) / (h * 2))));

    float leftTreeY = (h * 2) * ((int) ((camera.position.y + (h * 1.5f)) / (h * 2)));
    float leftUmbrellaY =
        h + ((h * 2) * ((int) ((camera.position.y + (h * 0.5f)) / (h * 2))));

    treeSprite.setScale(scale, scale);
    treeSprite.setPosition(PursuitModel.HALF_WIDTH - (scale * treeSprite.frameWidth) * 0.35f,
        rightTreeY + (h * 0.08f) - ((camera.position.y - rightTreeY) / h) * h * 0.12f);
    treeSprite.draw(canvas);

    treeSprite.setScale(-scale, scale);
    treeSprite.setPosition(-(scale * treeSprite.frameWidth) * 0.3f,
        leftTreeY + (h * 0.08f) - ((camera.position.y - leftTreeY) / h) * h * 0.12f);
    treeSprite.draw(canvas);

    umbrellaSprite.setScale(scale, scale);
    umbrellaSprite.setPosition(
        PursuitModel.HALF_WIDTH - (scale * umbrellaSprite.frameWidth) * 0.3f,
        rightUmbrellaY - ((camera.position.y - rightUmbrellaY) / h) * h * 0.07f);
    umbrellaSprite.draw(canvas);

    umbrellaSprite.setScale(-scale, scale);
    umbrellaSprite.setPosition(-(scale * umbrellaSprite.frameWidth) * 0.5f,
        leftUmbrellaY - ((camera.position.y - leftUmbrellaY) / h) * h * 0.07f);
    umbrellaSprite.draw(canvas);
  }
}
