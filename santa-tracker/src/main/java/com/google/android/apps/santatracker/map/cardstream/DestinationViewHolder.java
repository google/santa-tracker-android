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

package com.google.android.apps.santatracker.map.cardstream;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.LocaleList;
import android.support.v4.content.res.ResourcesCompat;
import android.text.method.LinkMovementMethod;
import android.text.method.TransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.santatracker.R;

import java.util.Locale;

class DestinationViewHolder extends CardViewHolder {

    private static AllCaps sAllCaps;
    private static LinkMovementMethod sLinkMovementMethod;

    TextView region;
    TextView city;
    TextView copyright;
    TextView arrival;
    TextView weather;
    TextView weatherLabel;
    ImageView image;
    Button streetView;

    DestinationViewHolder(View itemView) {
        super(itemView);
        region = (TextView) itemView.findViewById(R.id.destination_region);
        city = (TextView) itemView.findViewById(R.id.destination_city);
        copyright = (TextView) itemView.findViewById(R.id.destination_copyright);
        arrival = (TextView) itemView.findViewById(R.id.destination_arrival);
        weather = (TextView) itemView.findViewById(R.id.destination_weather);
        weatherLabel = (TextView) itemView.findViewById(R.id.destination_weather_label);
        image = (ImageView) itemView.findViewById(R.id.destination_image);
        streetView = (Button) itemView.findViewById(R.id.destination_street_view);

        image.setColorFilter(ResourcesCompat.getColor(itemView.getResources(),
                R.color.overlayDestinationCardFilter, itemView.getContext().getTheme()),
                PorterDuff.Mode.MULTIPLY);

        ensureMethods(itemView.getContext());
        region.setTransformationMethod(sAllCaps);
        copyright.setMovementMethod(sLinkMovementMethod);
    }

    private void ensureMethods(Context context) {
        if (sAllCaps == null) {
            sAllCaps = new AllCaps(context);
        }
        if (sLinkMovementMethod == null) {
            sLinkMovementMethod = new LinkMovementMethod();
        }
    }

    @Override
    public void setTypefaces(Typeface label, Typeface body) {
        setTypeface(new TextView[]{copyright, arrival, weather}, body);
        setTypeface(new TextView[]{city}, label);
    }

    private static class AllCaps implements TransformationMethod {

        private final Locale mLocale;

        public AllCaps(Context context) {
            if (Build.VERSION.SDK_INT >= 24) {
                LocaleList locales = context.getResources().getConfiguration().getLocales();
                mLocale = locales.get(0);
            } else {
                //noinspection deprecation
                mLocale = context.getResources().getConfiguration().locale;
            }
        }

        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return source != null ? source.toString().toUpperCase(mLocale) : null;
        }

        @Override
        public void onFocusChanged(View view, CharSequence sourceText, boolean focused,
                int direction, Rect previouslyFocusedRect) {
            // Do nothing
        }

    }

}
