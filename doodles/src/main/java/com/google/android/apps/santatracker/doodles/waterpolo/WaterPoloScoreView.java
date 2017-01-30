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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.apps.santatracker.doodles.R;
import com.google.android.apps.santatracker.doodles.shared.EventBus;
import com.google.android.apps.santatracker.doodles.shared.GameOverlayButton;
import com.google.android.apps.santatracker.doodles.shared.LaunchDecisionMaker;
import com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent;
import com.google.android.apps.santatracker.doodles.shared.ScoreView;


/**
 * ScoreView for WaterPolo game.
 */
public class WaterPoloScoreView extends ScoreView {

    private WaterPoloModel mModel;

    public WaterPoloScoreView(Context context, OnShareClickedListener shareClickedListener) {
        super(context, shareClickedListener);
    }

    public WaterPoloScoreView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WaterPoloScoreView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void goToMoreGames(Context context) {
        EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.menu_item_click);
        logger.logEvent(new PineappleLogEvent
                .Builder(PineappleLogEvent.DEFAULT_DOODLE_NAME, PineappleLogEvent.HOME_CLICKED)
                .withEventSubType(listener.gameType())
                .build());
        if(mModel != null) {
            int stars;
            if (mModel.score > WaterPoloModel.THREE_STAR_THRESHOLD) {
                stars = 3;
            } else if (mModel.score > WaterPoloModel.TWO_STAR_THRESHOLD) {
                stars = 2;
            } else {
                stars = 1;
            }

            Bundle bundle = new Bundle();
            bundle.putInt(LaunchDecisionMaker.EXTRA_PRESENT_DROP_SCORE, mModel.score);
            bundle.putInt(LaunchDecisionMaker.EXTRA_PRESENT_DROP_STARS, stars);
            LaunchDecisionMaker.finishActivityWithResult(context, Activity.RESULT_OK, bundle);
        } else {
            LaunchDecisionMaker.finishActivity(context);
        }
    }

    @Override
    protected void loadLayout(Context context) {
        super.loadLayout(context);

        // Disable replay for this game
        View replayButton = findViewById(R.id.replay_button);
        replayButton.setVisibility(View.GONE);

        // Disable sharing for this game
        GameOverlayButton shareButton = (GameOverlayButton) findViewById(R.id.share_button);
        shareButton.setVisibility(View.GONE);
    }

    public void setModel(WaterPoloModel model) {
       mModel = model;
    }
}
