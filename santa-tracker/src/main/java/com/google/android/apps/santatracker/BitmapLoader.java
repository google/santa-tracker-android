/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

package com.google.android.apps.santatracker;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

public class BitmapLoader {

    // Size is a multiplier
    // Alpha is a value from 0 to 255
    public static Bitmap loadImage(Resources resources, int resId, float size,
            int alpha) {

        Bitmap temp = BitmapFactory.decodeResource(resources, resId);
        Bitmap rc = Bitmap.createScaledBitmap(temp,
                (int) (temp.getWidth() * size),
                (int) (temp.getHeight() * size), true);

        Paint paint = new Paint();
        paint.setAlpha(alpha);

        temp = rc.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(temp);
        canvas.drawBitmap(rc, 0, 0, paint);

        return temp;
    }

}
