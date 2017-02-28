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
package com.google.android.apps.santatracker.doodles.shared;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.google.android.apps.santatracker.doodles.R;

/**
 * A button in the game overlay screens (pause and end screens).
 */
public class GameOverlayButton extends RelativeLayout {

  public GameOverlayButton(Context context) {
    this(context, null);
  }

  public GameOverlayButton(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public GameOverlayButton(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    inflate(context, R.layout.game_overlay_button, this);
    TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.GameOverlayButton, 0, 0);

    int imageRes =
        ta.getResourceId(R.styleable.GameOverlayButton_imageSrc, R.drawable.common_btn_pause);
    ImageView icon = (ImageView) findViewById(R.id.game_overlay_button_image);
    icon.setImageResource(imageRes);

    String text = ta.getString(R.styleable.GameOverlayButton_text);
    TextView description = (TextView) findViewById(R.id.game_overlay_button_description);
    if (text == null || text.isEmpty()) {
      description.setVisibility(GONE);
    } else {
      description.setText(text);
    }
  }
}
