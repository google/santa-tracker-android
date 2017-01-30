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

package com.google.android.apps.santatracker.games.gamebase;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.simpleengine.Renderer;
import com.google.android.apps.santatracker.games.simpleengine.game.GameObject;
import com.google.android.apps.santatracker.games.simpleengine.game.World;

public class DigitObjectFactory {

    int mWhiteDigitTex[] = new int[10];
    int mNegativeTex[] = new int[1];
    Renderer mRenderer;
    World mWorld;

    public DigitObjectFactory(Renderer renderer, World world) {
        mRenderer = renderer;
        mWorld = world;
    }

    public void requestWhiteTextures(float maxDigitWidth) {
        int[] res = new int[]{
                R.drawable.games_digit_0, R.drawable.games_digit_1, R.drawable.games_digit_2,
                R.drawable.games_digit_3, R.drawable.games_digit_4, R.drawable.games_digit_5,
                R.drawable.games_digit_6, R.drawable.games_digit_7, R.drawable.games_digit_8,
                R.drawable.games_digit_9
        };
        for (int i = 0; i < 10; i++) {
            mWhiteDigitTex[i] = mRenderer.requestImageTex(res[i], "white_digit_" + i,
                    Renderer.DIM_WIDTH, maxDigitWidth);
        }
        mNegativeTex[0] = mRenderer.requestImageTex(R.drawable.games_digit_negative, "digit_negative",
                Renderer.DIM_WIDTH, maxDigitWidth);
    }

    public GameObject makeDigitObject(int type, float x, float y, float size) {
        return mWorld.newGameObjectWithImage(type, x, y, mWhiteDigitTex[0], size, size);
    }

    public void setDigit(GameObject digitObject, int digit) {
        digit = digit > 9 ? 9 : digit < 0 ? 0 : digit;
        digitObject.getSprite(0).texIndex = mWhiteDigitTex[digit];
    }

    public void makeDigitObjects(int count, int type, float x, float y, float size,
            float stride, GameObject[] result) {
        int i;
        for (i = 0; i < count; i++) {
            result[i] = makeDigitObject(type, x, y, size);
            x += stride;
        }
    }

    public void setDigits(int valueToShow, GameObject[] digitObjects) {
        setDigits(valueToShow, digitObjects, 0, digitObjects.length);
    }

    public void setDigits(int valueToShow, GameObject[] digitObjects, int start, int length) {
        if (valueToShow >= 0) {
            int i;
            for (i = start + length - 1; i >= start; --i) {
                setDigit(digitObjects[i], valueToShow % 10);
                valueToShow /= 10;
            }
        } else {
            valueToShow = -valueToShow;
            int i;
            for (i = start + length - 1; i >= start; --i) {
                if (i == start) {
                    digitObjects[i].getSprite(0).texIndex = mNegativeTex[0];
                } else {
                    setDigit(digitObjects[i], valueToShow % 10);
                    valueToShow /= 10;
                }
            }

        }
    }
}
