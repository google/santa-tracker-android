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

package com.google.android.apps.santatracker.data;

public class PresentCounter {

    private long mInitialPresents = 0L;
    private long mPresentsDelivery = 0L;
    private long mStartTime = 0L;
    private double mDuration = 0L;

    public void init(long initialPresents, long totalPresents,
            long startTime, long endTime) {
        this.mInitialPresents = initialPresents;

        this.mPresentsDelivery = totalPresents - initialPresents;
        this.mStartTime = startTime;
        this.mDuration = endTime - startTime;
    }

    public long getPresents(long time) {
        double progress = (double) (time - mStartTime) / mDuration;

        if (progress < 0.0) {
            // do not return negative presents if progress is incorrect
            return mInitialPresents;
        } else if (progress > 1.0) {
            return mInitialPresents + mPresentsDelivery;
        } else {
            return Math.round(mInitialPresents + (mPresentsDelivery * progress));
        }
    }
}
