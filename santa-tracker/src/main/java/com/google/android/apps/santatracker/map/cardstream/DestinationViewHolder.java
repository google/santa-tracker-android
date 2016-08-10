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

package com.google.android.apps.santatracker.map.cardstream;

import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.santatracker.R;

public class DestinationViewHolder extends CardViewHolder {

    public TextView region;
    public TextView city;
    public TextView copyright;
    public TextView arrival;
    public TextView arrivalLabel;
    public TextView weather;
    public TextView weatherLabel;
    public ImageView image;
    public Button streetView;

    public DestinationViewHolder(View itemView) {
        super(itemView);
        region = (TextView) itemView.findViewById(R.id.destination_region);
        city = (TextView) itemView.findViewById(R.id.destination_city);
        copyright = (TextView) itemView.findViewById(R.id.destination_copyright);
        arrival = (TextView) itemView.findViewById(R.id.destination_arrival);
        arrivalLabel = (TextView) itemView.findViewById(R.id.destination_arrival_label);
        weather = (TextView) itemView.findViewById(R.id.destination_weather);
        weatherLabel = (TextView) itemView.findViewById(R.id.destination_weather_label);
        image = (ImageView) itemView.findViewById(R.id.destination_image);
        streetView = (Button) itemView.findViewById(R.id.destination_street_view);

        image.setColorFilter(itemView.getResources().getColor(R.color.overlayDestinationCardFilter),
                PorterDuff.Mode.MULTIPLY);

        copyright.setMovementMethod(new LinkMovementMethod());
    }

    @Override
    public void setTypefaces(Typeface label, Typeface body) {
        setTypeface(new TextView[]{copyright, arrival, weather}, body);
    }

}
