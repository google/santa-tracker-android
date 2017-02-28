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

package com.google.android.apps.santatracker.dasherdancer;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;


public class InsetDrawableCompat extends InsetDrawable {

    private Drawable mDrawable;
    private int mLeft, mTop, mRight, mBottom;

    public Drawable getDrawable(){
        return mDrawable;
    }

    public int getLeft() {
        return mLeft;
    }

    public int getTop() {
        return mTop;
    }

    public int getRight() {
        return mRight;
    }

    public int getBottom() {
        return mBottom;
    }

    public InsetDrawableCompat(Drawable drawable, int inset) {
        super(drawable, inset);
        mDrawable = drawable;
        mLeft = inset;
        mTop = inset;
        mRight = inset;
        mBottom = inset;
    }

    public InsetDrawableCompat(Drawable drawable, int insetLeft, int insetTop, int insetRight, int insetBottom) {
        super(drawable, insetLeft, insetTop, insetRight, insetBottom);
        mDrawable = drawable;
        mLeft = insetLeft;
        mTop = insetTop;
        mRight = insetRight;
        mBottom = insetBottom;
    }

}
