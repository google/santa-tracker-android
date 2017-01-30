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

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import com.google.android.apps.santatracker.R;

class UpdateViewHolder extends CardViewHolder {

    TextView content;

    UpdateViewHolder(View itemView) {
        super(itemView);
        content = (TextView) itemView.findViewById(R.id.update_text);
    }

    @Override
    public void setTypefaces(Typeface label, Typeface body) {
        setTypeface(new TextView[]{content}, body);
    }

}
