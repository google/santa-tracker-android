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

package com.google.android.apps.santatracker.games;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.google.android.apps.santatracker.common.R;

/** Animated loading screen with progress indicator. */
public class LoadingSceneView extends LinearLayout {

    public LoadingSceneView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Inflate custom layout
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.layout_loading_screen, this, true);

        this.setGravity(Gravity.CENTER_VERTICAL);
        this.setBackgroundColor(getResources().getColor(R.color.loading_web_background_blue));
        this.setOrientation(LinearLayout.VERTICAL);

        final ProgressBar progressBar = findViewById(R.id.progressbar);
        if (Build.VERSION.SDK_INT >= 24) {
            progressBar.setIndeterminateDrawable(
                    getResources().getDrawable(R.drawable.avd_loading_bar, context.getTheme()));
        } else {
            progressBar.setIndeterminateTintList(
                    ColorStateList.valueOf(getResources().getColor(R.color.SantaWhite)));
        }
    }
}
