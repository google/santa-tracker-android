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
package com.google.android.apps.santatracker.util;

import android.graphics.Typeface;
import android.os.Build;
import android.widget.TextView;

/**
 * Helper to apply the "Santa" font to text
 */
public class FontHelper {

    private static Typeface sTypeface;

    public static void makeLobster(TextView textView) {
        makeLobster(textView, true);
    }

    public static void makeLobster(TextView textView, boolean italic) {
        if (sTypeface == null) {
            sTypeface = Typeface.createFromAsset(textView.getContext().getAssets(),
                    "Lobster-Regular.otf");
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            textView.setTypeface(sTypeface);
        } else if (italic) {
            textView.setTypeface(sTypeface, Typeface.ITALIC);
        } else {
            textView.setTypeface(sTypeface, Typeface.NORMAL);
        }
    }

}
