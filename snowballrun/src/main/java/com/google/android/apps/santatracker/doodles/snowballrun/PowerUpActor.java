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
import com.google.android.apps.santatracker.doodles.shared.animation.AnimatedSprite;
import com.google.android.apps.santatracker.doodles.shared.animation.AnimatedSprite.AnimatedSpriteListener;

/** A power up that speeds up the player. */
public class PowerUpActor extends Actor {
    public static final float RADIUS_WORLD = 25f;
    private static final int Z_INDEX = 0;
    private boolean isPickedUp;

    private AnimatedSprite sprite;

    public PowerUpActor(float x, float y, Resources resources) {
        zIndex = Z_INDEX;
        position.x = x;
        position.y = y;
        isPickedUp = false;

        sprite = AnimatedSprite.fromFrames(resources, SnowballRunSprites.running_powerup);
        sprite.setFPS(18);
        sprite.setAnchor(sprite.frameWidth / 2, sprite.frameHeight / 2);
        sprite.setLoop(false);
        sprite.addListener(
                new AnimatedSpriteListener() {
                    @Override
                    public void onFinished() {
                        super.onFinished();
                        hidden = true;
                    }
                });
    }

    @Override
    public void update(float deltaMs) {
        super.update(deltaMs);
        if (isPickedUp) {
            sprite.update(deltaMs);
        }
    }

    public void pickUp() {
        isPickedUp = true;
    }

    public boolean isPickedUp() {
        return isPickedUp;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (hidden) {
            return;
        }
        float framesPercent = ((float) sprite.getFrameIndex()) / sprite.getNumFrames();
        sprite.setPosition(
                position.x, position.y + (3f * framesPercent * scale * sprite.frameHeight));
        sprite.setScale(scale, scale);
        sprite.draw(canvas);
    }
}
