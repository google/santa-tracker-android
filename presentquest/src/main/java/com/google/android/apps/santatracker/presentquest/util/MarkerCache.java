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
package com.google.android.apps.santatracker.presentquest.util;

import android.content.Context;

import com.google.android.apps.santatracker.presentquest.R;
import com.google.android.apps.santatracker.presentquest.model.Present;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Map;

public class MarkerCache {

    private Context mContext;
    private Map<Integer, BitmapDescriptor> mDescriptors = new HashMap<>();

    public MarkerCache(Context context) {
        mContext = context;
    }

    public MarkerOptions getElfMarker() {
        int id = R.drawable.elf_marker;
        return new MarkerOptions()
                .zIndex(100)
                .icon(getDescriptorForResource(id));
    }

    public MarkerOptions getWorkshopMarker(boolean close) {
        int id = close ? R.drawable.workshop : R.drawable.pin_workshop;
        int zindex = close ? 20 : 2;
        return new MarkerOptions()
                .zIndex(zindex)
                .icon(getDescriptorForResource(id));
    }

    public MarkerOptions getPresentMarker(Present present, boolean isNear) {
        if (present.isLarge) {
            return getLgPresentMarker(isNear);
        } else {
            return getSmPresentMarker(isNear);
        }
    }

    public MarkerOptions getSmPresentMarker(boolean near) {
        int id = near ? R.drawable.presents_sm : R.drawable.pin_presents_sm;
        int zindex = near ? 30 : 3;
        return new MarkerOptions()
                .zIndex(zindex)
                .icon(getDescriptorForResource(id));
    }

    public MarkerOptions getLgPresentMarker(boolean near) {
        int id = near ? R.drawable.presents_lg : R.drawable.pin_presents_lg;
        int zindex = near ? 30 : 3;
        return new MarkerOptions()
                .zIndex(zindex)
                .icon(getDescriptorForResource(id));
    }

    private BitmapDescriptor getDescriptorForResource(int id) {
        if (mDescriptors.get(id) != null) {
            return mDescriptors.get(id);
        }

        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromResource(id);
        mDescriptors.put(id, descriptor);

        return descriptor;
    }

    public static void updateMarker(Marker marker, MarkerOptions options) {
        marker.setPosition(options.getPosition());
        marker.setIcon(options.getIcon());
        marker.setZIndex(options.getZIndex());
        marker.setTitle(options.getTitle());
    }

}
