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

package com.google.android.apps.santatracker.games.simpleengine;

public class SmoothValue {

    private float mValue = 0.0f;
    private float mTarget = 0.0f;
    private float mChangeSpeed = 1.0f;
    private boolean mOnTarget = false;
    private float mMin = Float.NEGATIVE_INFINITY;
    private float mMax = Float.POSITIVE_INFINITY;
    private int mSamples = 1;

    public SmoothValue() {
    }

    public SmoothValue(float initialValue, float changeSpeed) {
        init(initialValue, changeSpeed, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 1);
    }

    public SmoothValue(float initialValue, float changeSpeed, float min, float max) {
        init(initialValue, changeSpeed, min, max, 1);
    }

    public SmoothValue(float initialValue, float changeSpeed, float min, float max, int samples) {
        init(initialValue, changeSpeed, min, max, samples);
    }

    private void init(float initialValue, float changeSpeed, float min, float max, int samples) {
        mValue = initialValue;
        mChangeSpeed = changeSpeed;
        mMin = min;
        mMax = max;
        mSamples = samples;
    }

    public void setTarget(float target) {
        mTarget = target;
        mOnTarget = false;
    }

    public void update(float deltaT) {
        float displac = deltaT * mChangeSpeed;
        float value;
        if (Math.abs(mValue - mTarget) <= displac) {
            value = mTarget;
            mOnTarget = true;
        } else if (mTarget > mValue) {
            value = mValue + displac;
        } else {
            value = mValue - displac;
        }

        if (mSamples > 0) {
            mValue = (mValue * mSamples + value) / (mSamples + 1);
        } else {
            mValue = value;
        }
        mValue = mValue < mMin ? mMin : mValue > mMax ? mMax : mValue;
    }

    public float getValue() {
        return mValue;
    }

    public void setValue(float value) {
        mValue = value;
    }

    public float getTarget() {
        return mTarget;
    }

    public boolean isOnTarget() {
        return mOnTarget;
    }

    public float getMin() {
        return mMin;
    }

    public float getMax() {
        return mMax;
    }
}
