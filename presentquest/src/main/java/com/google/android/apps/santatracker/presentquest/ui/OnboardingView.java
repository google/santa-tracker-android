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
package com.google.android.apps.santatracker.presentquest.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.santatracker.presentquest.R;
import com.google.android.apps.santatracker.util.FontHelper;

/**
 * View that contains the onboarding slides.
 */
public class OnboardingView extends FrameLayout {

    public interface OnFinishListener {
        void onFinish();
    }

    private static final int NUM_STEPS = 4;

    private static final int[] IMAGES = new int[] {
            R.drawable.onboard_step_1,
            R.drawable.onboard_step_2,
            R.drawable.onboard_step_3,
            R.drawable.onboard_step_4,
    };

    private static final int[] INSTRUCTIONS = new int[] {
            R.string.onboarding_msg_1,
            R.string.onboarding_msg_2,
            R.string.onboarding_msg_3,
            0,
    };

    private static final int[] BUTTON_TEXT = new int[] {
            R.string.next,
            R.string.next,
            R.string.next,
            R.string.play,
    };

    private ImageView mImageView;
    private TextView mTextView;
    private TextView mFinalTextView;
    private Button mAdvanceButton;
    private View[] mIndicators = new View[4];

    private OnFinishListener mListener;
    private int mStepIndex = 0;

    public OnboardingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.layout_onboarding, this, true);

        mImageView = (ImageView) findViewById(R.id.onboarding_image);
        mTextView = (TextView) findViewById(R.id.onboarding_text);
        mFinalTextView = (TextView) findViewById(R.id.onboarding_text_final);
        mAdvanceButton = (Button) findViewById(R.id.onboarding_button);

        FontHelper.makeLobster(mFinalTextView);

        mIndicators[0] = findViewById(R.id.indicator_1);
        mIndicators[1] = findViewById(R.id.indicator_2);
        mIndicators[2] = findViewById(R.id.indicator_3);
        mIndicators[3] = findViewById(R.id.indicator_4);

        mAdvanceButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mStepIndex++;

                if (mStepIndex < NUM_STEPS) {
                    setStep(mStepIndex);
                } else {
                    setVisibility(View.GONE);
                    if (mListener != null) {
                        mListener.onFinish();
                    }
                }
            }
        });

        setStep(0);
    }

    private void setStep(int step) {
        mStepIndex = step;

        // Images and text
        mImageView.setImageResource(IMAGES[mStepIndex]);
        mAdvanceButton.setText(BUTTON_TEXT[mStepIndex]);
        if (INSTRUCTIONS[mStepIndex] != 0) {
            mTextView.setText(INSTRUCTIONS[mStepIndex]);
        }

        // Indicators
        for (int i = 0; i < NUM_STEPS; i++) {
            if (i == mStepIndex) {
                mIndicators[i].setAlpha(1.0f);
            } else {
                mIndicators[i].setAlpha(0.5f);
            }
        }

        // Last step has a special message
        if (mStepIndex == NUM_STEPS - 1) {
            mTextView.setVisibility(View.INVISIBLE);
            mFinalTextView.setVisibility(View.VISIBLE);
        } else {
            mTextView.setVisibility(View.VISIBLE);
            mFinalTextView.setVisibility(View.INVISIBLE);
        }
    }

    public void setOnFinishListener(OnFinishListener listener) {
        mListener = listener;
    }
}
