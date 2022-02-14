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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.apps.santatracker.R;

/** View to display over the screen at game end. */
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
    }

    public void initialize(
            int score,
            @Nullable OnClickListener replayListener,
            @NonNull OnClickListener exitListener) {

        // Set score
        TextView scoreTextView = findViewById(R.id.scoreTextView);
        TextView scoreLabelTextView = findViewById(R.id.scoreLabel);

        // Only show positive scores
        if (score >= 0) {
            scoreTextView.setText(
                    getResources()
                            .getString(
                                    com.google.android.apps.playgames.R.string.end_of_game_score,
                                    score));
            scoreTextView.setVisibility(View.VISIBLE);
            scoreLabelTextView.setVisibility(View.VISIBLE);
        } else {
            scoreTextView.setVisibility(View.INVISIBLE);
            scoreLabelTextView.setVisibility(View.INVISIBLE);
        }

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
