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

/**
 * The snowball that's always behind the player and the other running fruits. If the player or the
 * running fruits gets too close to the snowball, they get squished.
 */
public class RunSnowballActor extends SnowballBaseActor {

    public static final float VERTICAL_RADIUS_WORLD = 90f;
    public static final float HORIZONTAL_RADIUS_WORLD = 60f;

    private static final int Z_INDEX = 5;

    public RunSnowballActor(Resources resources) {
        super(resources);
        zIndex = Z_INDEX;
    }

    @Override
    public void update(float deltaMs) {
        super.update(deltaMs);

        int fps = (int) (10 * velocity.y / PursuitModel.BASE_SPEED);
        bodySprite.setFPS(fps);
    }
}
