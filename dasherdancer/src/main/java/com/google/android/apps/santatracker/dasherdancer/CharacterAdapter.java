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


import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import java.lang.*;

public class CharacterAdapter extends PagerAdapter {

	private static final String LOG_TAG = CharacterAdapter.class.getSimpleName();
	
	private static final int[] mBackgrounds = new int[]{
		R.color.bg_blue,R.color.bg_pink,R.color.bg_purple,
		R.color.bg_green,R.color.bg_red,R.color.bg_orange,
		R.color.bg_yellow
	};
	
	private SparseArray<ImageView> mViews;
	private Character[] mCharacters;
	
	public CharacterAdapter(Character[] characters) {
		mCharacters = characters;
		mViews = new SparseArray<ImageView>();
	}
	
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		if (position < mCharacters.length) {
			View view = mViews.get(position);
			if ((view == null) && (object != null)) {
				try {
					view = (ImageView)object;
				} catch (ClassCastException e) {
					Log.e(LOG_TAG, "Wrong type of object supplied to destroyItem.");
				}
			}
			
			if (view != null) {
				container.removeView(view);
			}
			
			mViews.remove(position);
		}
	}
	
	@Override
	public int getCount() {
		if(mCharacters != null) {
			return mCharacters.length;
		}
		return 0;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		ImageView view = null;
		
		if (position < mCharacters.length) {
			if (mViews.get(position) != null) {
				view = mViews.get(position);
			} else {
				view = new FrameAnimationView(container.getContext());
				view.setScaleType(ImageView.ScaleType.MATRIX);
				//Load the first idle frame.
				view.setBackgroundResource(mBackgrounds[position]);
				view.setTag(position);
				mViews.put(position, view);
			}
			
			LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			container.addView(view, lp);
		}
		
		return view;
	}
	
	@Override
	public boolean isViewFromObject(View arg0, Object arg1) {
		return arg0 == arg1;
	}

}
