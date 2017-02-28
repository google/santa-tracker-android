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
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.google.android.apps.santatracker.doodles.R;

/**
 * A view for the stars on the end screen. This is just a wrapper class so that we can contain
 * star layout behavior in a single layout file instead of having to specify each one individually.
 */
public class StarView extends FrameLayout {

  public StarView(Context context) {
    this(context, null);
  }

  public StarView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StarView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    inflate(context, R.layout.star_view, this);
  }
}
