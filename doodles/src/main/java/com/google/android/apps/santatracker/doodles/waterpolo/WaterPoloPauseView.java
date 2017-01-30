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
package com.google.android.apps.santatracker.doodles.waterpolo;

import android.content.Context;
import android.view.View;

import com.google.android.apps.santatracker.doodles.R;
import com.google.android.apps.santatracker.doodles.shared.PauseView;

/**
 *  Special subclass of {@link PauseView} for Waterpolo.
 */
public class WaterPoloPauseView extends PauseView {

    public WaterPoloPauseView(Context context) {
        super(context);
    }

    @Override
    protected void loadLayout(Context context) {
        super.loadLayout(context);

        // Hide replay button
        findViewById(R.id.replay_button).setVisibility(View.GONE);
    }
}
