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

package com.google.android.apps.santatracker.dasherdancer

import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable

class InsetDrawableCompat : InsetDrawable {

    private var inDrawable: Drawable? = null
    var left: Int = 0
        private set
    var top: Int = 0
        private set
    var right: Int = 0
        private set
    var bottom: Int = 0
        private set

    override fun getDrawable(): Drawable? {
        return inDrawable
    }

    constructor(
        drawable: Drawable,
        insetLeft: Int,
        insetTop: Int,
        insetRight: Int,
        insetBottom: Int
    ) : super(drawable, insetLeft, insetTop, insetRight, insetBottom) {
        inDrawable = drawable
        left = insetLeft
        top = insetTop
        right = insetRight
        bottom = insetBottom
    }
}
