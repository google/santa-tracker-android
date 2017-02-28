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
package com.google.android.apps.santatracker.games;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.util.FontHelper;

/**
 * View to display over the screen at game end.
 */
public class EndOfGameView extends FrameLayout {

    public EndOfGameView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Inflate custom layout
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.layout_end_game, this, true);

        // Stop here if we're in the editor
        if (isInEditMode()) {
            return;
        }

        // Change font on Game Over view
        TextView gameOverTextView = (TextView) findViewById(R.id.gameOverTextView);
        FontHelper.makeLobster(gameOverTextView);
    }

    public void initialize(int score,
                           @Nullable View.OnClickListener replayListener,
                           @NonNull View.OnClickListener exitListener) {

        // Set score
        TextView scoreTextView = (TextView) findViewById(R.id.scoreTextView);
        scoreTextView.setText(getResources().getString(R.string.end_of_game_score, score));

        // Set replay listener (or hide replay button)
        if (replayListener != null) {
            findViewById(R.id.replayButton).setOnClickListener(replayListener);
        } else {
            findViewById(R.id.replayButton).setVisibility(View.GONE);
        }

        // Set end game listener
        findViewById(R.id.exitButton).setOnClickListener(exitListener);
    }

}
