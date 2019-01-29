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

package com.google.android.apps.santatracker.launch.adapters;

import static com.bumptech.glide.request.RequestOptions.centerCropTransform;
import static com.bumptech.glide.request.RequestOptions.centerInsideTransform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.launch.AbstractLaunch;
import java.security.MessageDigest;

class ItemViewHolder extends RecyclerView.ViewHolder {

    AbstractLaunch launcher;
    ImageView backgroundImageView;
    View lockedView;
    private ImageView lockedLayerImageView;
    private TextView nameView;

    ItemViewHolder(View itemView) {
        super(itemView);

        backgroundImageView = itemView.findViewById(R.id.card_background_image);
        nameView = itemView.findViewById(R.id.card_name_text);
        lockedView = itemView.findViewById(R.id.card_disabled);
        lockedLayerImageView = itemView.findViewById(R.id.locked_layer);
    }

    void setLauncher(AbstractLaunch launcher, int lockedLayerColor, boolean isLocked) {
        this.launcher = launcher;

        Context context = itemView.getContext();
        // Loading all of these beautiful images at full res is laggy without using
        // Glide, however this makes it asynchronous.  We should consider either compromising
        // on image resolution or doing some sort of nifty placeholder.

        final String imageUrl = launcher.getCardImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // If we have a image url for the card, load it
            Glide.with(context)
                    .load(imageUrl)
                    .apply(centerInsideTransform())
                    .into(backgroundImageView);
        } else {
            // Otherwise we'll use the card drawable res
            Glide.with(context)
                    .load(launcher.getCardDrawableRes())
                    .apply(centerInsideTransform())
                    .into(backgroundImageView);
        }

        if (isLocked) {
            lockedView.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(R.drawable.locked_layer)
                    .apply(
                            centerCropTransform()
                                    .transform(new ColorTransformation(lockedLayerColor)))
                    .into(lockedLayerImageView);
        }

        nameView.setText(launcher.getTitle());
        itemView.setContentDescription(launcher.getTitle());

        launcher.attachToView(this.itemView);
    }

    static class ColorTransformation extends BitmapTransformation {
        private static final String ID = "ColorTransformation.v1";

        private int mColor;

        ColorTransformation(int color) {
            super();
            mColor = color;
        }

        @Override
        protected Bitmap transform(
                BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
            Bitmap.Config config =
                    toTransform.getConfig() != null
                            ? toTransform.getConfig()
                            : Bitmap.Config.ARGB_8888;
            Bitmap bitmap = pool.get(toTransform.getWidth(), toTransform.getHeight(), config);

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setColorFilter(new PorterDuffColorFilter(mColor, PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(toTransform, 0, 0, paint);
            return bitmap;
        }

        @Override
        public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
            messageDigest.update((ID + mColor).getBytes());
        }
    }
}
