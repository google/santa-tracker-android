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

package com.google.android.apps.santatracker.rocketsleigh;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Size;
import android.widget.ImageView;

import java.util.TreeMap;

/**
 * Created by rpetit on 11/13/14.
 */
public class BitmapLoader {
    private class BitmapLoaderTask extends AsyncTask<Integer, Void, Bitmap> {
        private boolean mForSize;
        private ImageView mView;

        @Override
        protected Bitmap doInBackground(Integer... params) {
            Bitmap bmp = null;
            if (params != null) {
                for (Integer id : params) {
                    if (mForSize) {
                        Size size = null;
                        synchronized (mBitmapSizes) {
                            size = mBitmapSizes.get(id);
                        }
                        if (size == null) {
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inJustDecodeBounds = true;
                            bmp = BitmapFactory.decodeResource(mResources, id, opts);
//                            size = new Size(bmp.getWidth(), bmp.getHeight());
                            synchronized (mBitmapSizes) {
                                mBitmapSizes.put(id, size);
                            }
                            bmp.recycle();
                        }
                        bmp = null;
                    } else {
                        synchronized (mBitmaps) {
                            bmp = mBitmaps.get(id);
                        }
                        if (bmp == null) {
                            bmp = BitmapFactory.decodeResource(mResources, id);
                            synchronized (mBitmaps) {
                                mBitmaps.put(id, bmp);
                            }
//                            Size size = new Size(bmp.getWidth(), bmp.getHeight());
//                            mBitmapSizes.put(id, size);
                        }
                    }
                    if (isCancelled()) {

                    }
                }
            }

            // We only load the bitmap in a view if it's a single bitmap request and not just for size.
            if (mForSize || (params.length > 1)) {
                bmp = null;
            }

            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap bmp) {
            if ((mView != null) && (bmp != null)) {
                // Should check to see if the view is visible yet.  If it is,
                // we may not want to show it.
                mView.setImageBitmap(bmp);
            }
        }
    }

    private TreeMap<Integer, Bitmap> mBitmaps;
    private TreeMap<Integer, Size> mBitmapSizes;
    private Resources mResources;

    private static BitmapLoader gLoader;

    public BitmapLoader getInstance(Context context) {
        if (gLoader == null) {
            gLoader = new BitmapLoader(context);
        }
        return gLoader;
    }

    private BitmapLoader(Context context) {
        mResources = context.getResources();
        mBitmaps = new TreeMap<Integer, Bitmap>();
        mBitmapSizes = new TreeMap<Integer, Size>();
    }

    public void loadBitmapForView(int resId, ImageView view) {
    }

    public void preloadBitmap(int resId) {
    }

    public void preloadBitmaps(int[] ids) {
    }

    public void preloadBitmapsForSize(int[] ids) {
    }

    public Size getBitmapSize(int resId) {
        return null;
    }
}
