/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FrameAnimationView extends ImageView {

	private Bitmap[] mFrames;
	private int[] mFrameIndices;
	private int mFrameIndex;
	private final Paint mPaint = new Paint();
	
	public FrameAnimationView(Context context) {
		super(context);
		init();
	}
	
	public FrameAnimationView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public FrameAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		mPaint.setAntiAlias(true);
	}
	
	/**
	 * Will attempt to recycle the old frames before setting the new frames.
	 * @param frames
	 * @param frameIndices
	 */
	public void setFrames(Bitmap[] frames, int[] frameIndices) {
		if(mFrames != null) {
			mFrames = null;
		}
		mFrames = frames;
		mFrameIndices = frameIndices;
	}
	
	public int getFrameIndex() {
		return mFrameIndex; 
	}
	
	public void setFrameIndex(int frameIndex) {
		mFrameIndex = frameIndex;
        if(mFrames != null && mFrameIndex >= 0 && mFrameIndex < mFrameIndices.length
                && mFrames[mFrameIndices[mFrameIndex]] != null && !mFrames[mFrameIndices[mFrameIndex]].isRecycled()) {
            invalidate();
        }
	}
	
	public void onDraw(Canvas c) {
		if(mFrames != null && mFrameIndex >= 0 && mFrameIndex < mFrameIndices.length) {
			setImageBitmap(mFrames[mFrameIndices[mFrameIndex]]);
		}
        if(getDrawable() == null) {
            super.onDraw(c);
            return;
        }
        if(((BitmapDrawable)getDrawable()).getBitmap() == null
                || ((BitmapDrawable)getDrawable()).getBitmap().isRecycled()) {
            return;
        }
		super.onDraw(c);
	}
}
