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

package com.google.android.apps.santatracker.dasherdancer;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

public class NoSwipeViewPager extends ViewPager {

	private GestureDetector mGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;
	
	public NoSwipeViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		//Take all events, as we are going to animate the current view based on touch events.
		return true;
	}

	@Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean retVal = false;
        if(ev.getPointerCount() > 1) {
            retVal = mScaleGestureDetector.onTouchEvent(ev);
        }
        if(!retVal && ev.getPointerCount() == 1) {
            retVal = mGestureDetector.onTouchEvent(ev) || retVal;
        }
        return retVal;
	}
	
	public void setGestureDetectorListeners(Context context, 
			OnGestureListener listener, OnScaleGestureListener scaleListener) {
		mGestureDetector = new GestureDetector(context, listener);
		mScaleGestureDetector = new ScaleGestureDetector(context, scaleListener);
	}
}
