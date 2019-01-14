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

package com.google.android.apps.santatracker.launch;

import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowPresenter;
import com.bumptech.glide.Glide;
import com.google.android.apps.santatracker.R;

public class TvCardPresenter extends Presenter {

    public TvCardPresenter(SantaContext context) {}

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {

        final View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layout_village_card_tv, parent, false);

        view.setFocusable(true);
        view.setFocusableInTouchMode(false);

        return new TvLaunchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {

        final AbstractLaunch launch = (AbstractLaunch) item;
        final TvLaunchViewHolder holder = (TvLaunchViewHolder) viewHolder;
        holder.setLauncher(launch);
        holder.launcher.applyState();

        if (holder.launcher.getState() == AbstractLaunch.STATE_DISABLED) {
            // TODO: Blur or darken the image
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {}

    static class TvLaunchViewHolder extends RowPresenter.ViewHolder {

        public AbstractLaunch launcher;
        public ImageView backgroundImageView;
        public TextView nameView;
        public TextView verbView;
        public View lockedView;

        public TvLaunchViewHolder(View itemView) {
            super(itemView);

            backgroundImageView = itemView.findViewById(R.id.card_background_image);
            nameView = itemView.findViewById(R.id.card_name_text);
            verbView = itemView.findViewById(R.id.card_verb);
            lockedView = itemView.findViewById(R.id.card_disabled);
        }

        public void setLauncher(AbstractLaunch launcher) {
            this.launcher = launcher;

            // Loading all of these beautiful images at full res is laggy without using
            // Glide, however this makes it asynchronous.  We should consider either compromising
            // on image resolution or doing some sort of nifty placeholder.
            Glide.with(view.getContext())
                    .load(launcher.getCardDrawableRes())
                    .apply(centerCropTransform())
                    .into(backgroundImageView);

            nameView.setText(launcher.getTitle());
            verbView.setText(launcher.getVerb());

            view.setContentDescription(launcher.getTitle());

            if (launcher.getState() == AbstractLaunch.STATE_DISABLED
                    || launcher.getState() == AbstractLaunch.STATE_LOCKED
                    || launcher.getState() == AbstractLaunch.STATE_FINISHED) {
                lockedView.setVisibility(View.VISIBLE);
            } else {
                lockedView.setVisibility(View.GONE);
            }

            if (launcher.getState() != AbstractLaunch.STATE_HIDDEN) {
                launcher.attachToView(this.view);
            }
        }
    }
}
