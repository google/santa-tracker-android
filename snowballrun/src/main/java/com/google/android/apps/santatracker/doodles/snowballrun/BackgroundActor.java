/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.apps.santatracker.doodles.snowballrun;

import android.content.res.Resources;
import android.graphics.Canvas;
import com.google.android.apps.santatracker.doodles.shared.actor.Actor;
import com.google.android.apps.santatracker.doodles.shared.actor.Camera;
import com.google.android.apps.santatracker.doodles.shared.animation.AnimatedSprite;

/** A background actor that follows the camera. */
public class BackgroundActor extends Actor {

    private AnimatedSprite backgroundSprite;

    private AnimatedSprite treeSpriteOne;
    private AnimatedSprite treeSpriteTwo;

    private Camera camera;

    public BackgroundActor(Resources resources, Camera camera) {
        this.camera = camera;

        backgroundSprite =
                AnimatedSprite.fromFrames(resources, SnowballRunSprites.snowballrunner_background);
        treeSpriteOne =
                AnimatedSprite.fromFrames(resources, SnowballRunSprites.snowball_runner_trees1);
        treeSpriteTwo =
                AnimatedSprite.fromFrames(resources, SnowballRunSprites.snowball_runner_trees2);

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
        float rightTreeOneY = h * 0.5f + ((h * 2) * ((int) ((camera.position.y + h) / (h * 2))));
        float rightTreeTwoY = h * 1.5f + ((h * 2) * ((int) ((camera.position.y) / (h * 2))));

        float leftTreeOneY = (h * 2) * ((int) ((camera.position.y + (h * 1.5f)) / (h * 2)));
        float leftTreeTwoY = h + ((h * 2) * ((int) ((camera.position.y + (h * 0.5f)) / (h * 2))));

        treeSpriteOne.setScale(scale, scale);
        treeSpriteOne.setPosition(
                PursuitModel.HALF_WIDTH - (scale * treeSpriteOne.frameWidth) * 0.35f,
                rightTreeOneY
                        + (h * 0.08f)
                        - ((camera.position.y - rightTreeOneY) / h) * h * 0.12f);
        treeSpriteOne.draw(canvas);

        treeSpriteOne.setScale(-scale, scale);
        treeSpriteOne.setPosition(
                -(scale * treeSpriteOne.frameWidth) * 0.3f,
                leftTreeOneY + (h * 0.08f) - ((camera.position.y - leftTreeOneY) / h) * h * 0.12f);
        treeSpriteOne.draw(canvas);

        treeSpriteTwo.setScale(scale, scale);
        treeSpriteTwo.setPosition(
                PursuitModel.HALF_WIDTH - (scale * treeSpriteTwo.frameWidth) * 0.3f,
                rightTreeTwoY - ((camera.position.y - rightTreeTwoY) / h) * h * 0.07f);
        treeSpriteTwo.draw(canvas);

        treeSpriteTwo.setScale(-scale, scale);
        treeSpriteTwo.setPosition(
                -(scale * treeSpriteTwo.frameWidth) * 0.5f,
                leftTreeTwoY - ((camera.position.y - leftTreeTwoY) / h) * h * 0.07f);
        treeSpriteTwo.draw(canvas);
    }
}
