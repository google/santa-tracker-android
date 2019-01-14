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

package com.google.android.apps.jetpack

import com.google.android.apps.playgames.simpleengine.Renderer
import com.google.android.apps.playgames.simpleengine.game.GameObject
import com.google.android.apps.playgames.simpleengine.game.World

class DigitObjectFactory(private var renderer: Renderer, private var world: World) {

    private var whiteDigitTex = IntArray(10)
    private var negativeTex = IntArray(1)

    fun requestWhiteTextures(maxDigitWidth: Float) {
        val res = intArrayOf(
                com.google.android.apps.playgames.R.drawable.games_digit_0,
                com.google.android.apps.playgames.R.drawable.games_digit_1,
                com.google.android.apps.playgames.R.drawable.games_digit_2,
                com.google.android.apps.playgames.R.drawable.games_digit_3,
                com.google.android.apps.playgames.R.drawable.games_digit_4,
                com.google.android.apps.playgames.R.drawable.games_digit_5,
                com.google.android.apps.playgames.R.drawable.games_digit_6,
                com.google.android.apps.playgames.R.drawable.games_digit_7,
                com.google.android.apps.playgames.R.drawable.games_digit_8,
                com.google.android.apps.playgames.R.drawable.games_digit_9
        )
        for (i in 0..9) {
            whiteDigitTex[i] = renderer.requestImageTex(
                    res[i], "white_digit_$i", Renderer.DIM_WIDTH, maxDigitWidth)
        }
        negativeTex[0] = renderer.requestImageTex(
                com.google.android.apps.playgames.R.drawable.games_digit_negative,
                "digit_negative",
                Renderer.DIM_WIDTH,
                maxDigitWidth)
    }

    fun makeDigitObject(type: Int, x: Float, y: Float, size: Float): GameObject {
        return world.newGameObjectWithImage(type, x, y, whiteDigitTex[0], size, size)
    }

    fun setDigit(digitObject: GameObject?, digit: Int) {
        var digit = digit
        digit = if (digit > 9) 9 else if (digit < 0) 0 else digit
        digitObject?.getSprite(0)?.texIndex = whiteDigitTex[digit]
    }

    fun makeDigitObjects(
        count: Int,
        type: Int,
        x: Float,
        y: Float,
        size: Float,
        stride: Float,
        result: Array<GameObject?>
    ) {
        var x = x
        var i = 0
        while (i < count) {
            result[i] = makeDigitObject(type, x, y, size)
            x += stride
            i++
        }
    }

    fun setDigits(
        valueToShow: Int,
        digitObjects: Array<GameObject?>,
        start: Int = 0,
        length: Int = digitObjects.size
    ) {
        var valueToShow = valueToShow
        if (valueToShow >= 0) {
            var i: Int = start + length - 1
            while (i >= start) {
                setDigit(digitObjects[i], valueToShow % 10)
                valueToShow /= 10
                --i
            }
        } else {
            valueToShow = -valueToShow
            var i: Int = start + length - 1
            while (i >= start) {
                if (i == start) {
                    digitObjects[i]?.getSprite(0)!!.texIndex = negativeTex[0]
                } else {
                    setDigit(digitObjects[i], valueToShow % 10)
                    valueToShow /= 10
                }
                --i
            }
        }
    }
}
