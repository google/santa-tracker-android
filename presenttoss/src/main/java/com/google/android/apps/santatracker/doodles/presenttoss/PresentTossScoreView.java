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
package com.google.android.apps.santatracker.doodles.presenttoss;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import com.google.android.apps.santatracker.doodles.shared.AndroidUtils;
import com.google.android.apps.santatracker.doodles.shared.EventBus;
import com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogEvent;
import com.google.android.apps.santatracker.doodles.shared.views.GameOverlayButton;
import com.google.android.apps.santatracker.doodles.shared.views.ScoreView;

/** ScoreView for WaterPolo game. */
public class PresentTossScoreView extends ScoreView {

    // Key for value of final Present Drop score.
    public static final String EXTRA_PRESENT_DROP_SCORE = "presentDropScore";
    public static final String EXTRA_PRESENT_DROP_STARS = "presentDropStars";

    private PresentTossModel mModel;

    public PresentTossScoreView(Context context, OnShareClickedListener shareClickedListener) {
        super(context, shareClickedListener);
    }

    public PresentTossScoreView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PresentTossScoreView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void goToMoreGames(Context context) {
        EventBus.getInstance()
                .sendEvent(
                        EventBus.PLAY_SOUND,
                        com.google.android.apps.santatracker.doodles.R.raw.menu_item_click);
        logger.logEvent(
                new DoodleLogEvent.Builder(
                                DoodleLogEvent.DEFAULT_DOODLE_NAME, DoodleLogEvent.HOME_CLICKED)
                        .withEventSubType(listener.gameType())
                        .build());
        if (mModel != null) {
            int stars;
            if (mModel.score > PresentTossModel.THREE_STAR_THRESHOLD) {
                stars = 3;
            } else if (mModel.score > PresentTossModel.TWO_STAR_THRESHOLD) {
                stars = 2;
            } else {
                stars = 1;
            }

            Bundle bundle = new Bundle();
            bundle.putInt(EXTRA_PRESENT_DROP_SCORE, mModel.score);
            bundle.putInt(EXTRA_PRESENT_DROP_STARS, stars);
            AndroidUtils.finishActivityWithResult(context, Activity.RESULT_OK, bundle);
        } else {
            AndroidUtils.finishActivity(context);
        }
    }

    @Override
    protected void loadLayout(Context context) {
        super.loadLayout(context);

        // Disable replay for this game
        View replayButton =
                findViewById(com.google.android.apps.santatracker.doodles.R.id.replay_button);
        replayButton.setVisibility(View.GONE);

        // Disable sharing for this game
        GameOverlayButton shareButton =
                findViewById(com.google.android.apps.santatracker.doodles.R.id.share_button);
        shareButton.setVisibility(View.GONE);
    }

    public void setModel(PresentTossModel model) {
        mModel = model;
    }
}
