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
package com.google.android.apps.santatracker.launch;

import com.google.android.apps.santatracker.R;

/**
 * RecyclerView adapter for list of Movies in the village.
 */

public class MoviesCardAdapter extends CardAdapter {
    public static final int VIDEO01_CARD = 0;
    public static final int VIDEO15_CARD = 1;
    public static final int VIDEO23_CARD = 2;

    public MoviesCardAdapter(SantaContext santaContext) {
        super(santaContext, new AbstractLaunch[3]);
    }

    @Override
    public void initializeLaunchers(SantaContext santaContext) {
        mAllLaunchers[VIDEO01_CARD] = new LaunchVideo(santaContext, this,
                R.drawable.android_game_cards_santas_back, 1);
        mAllLaunchers[VIDEO15_CARD] = new LaunchVideo(santaContext, this,
                R.drawable.android_game_cards_office_prank, 15);
        mAllLaunchers[VIDEO23_CARD] = new LaunchVideo(santaContext, this,
                R.drawable.android_game_cards_elf_car, 23);
    }

}
