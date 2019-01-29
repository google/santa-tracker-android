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
package com.google.android.apps.santatracker.launch;

import android.view.View;

/**
 * AbstractLaunch that Launches nothing that is used for representing a header. Creating this as one
 * of the implementation of {@link AbstractLaunch} because this item could be in the middle of the
 * list when the Santa's flying state is changed to flying (Track Santa should come to the first of
 * the list)
 */
public class LaunchHeader extends AbstractLaunch {

    public LaunchHeader(SantaContext context, LauncherDataChangedCallback adapter) {
        super(context, adapter);
    }

    @Override
    public void onClick(View view) {
        // No op
    }

    @Override
    public boolean onLongClick(View view) {
        return true;
    }
}
