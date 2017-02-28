/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.apps.santatracker.games.cityquiz;

import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class City {

    public static final int FIRST_FAKE_INDEX = 1;
    public static final int SECOND_FAKE_INDEX = 2;

    // Padding around the bounds created by the city's location. This is used to ensure that the markers are visible,
    // otherwise they would be at the edges of the map's viewport.
    public static final int MAP_PADDING = 200;

    private LatLng location;
    private LatLng firstFakeLocation;
    private LatLng secondFakeLocation;
    private String imageName;
    private String imageAuthor;
    private String name;

    public City(double lat, double lng, String imageName, String imageAuthor, String name) {
        location = new LatLng(lat, lng);
        firstFakeLocation = new LatLng(lat, lng);
        secondFakeLocation = new LatLng(lat, lng);
        this.imageName = imageName;
        this.imageAuthor = imageAuthor;
        this.name = name;
    }

    public City(LatLng loc, LatLng fakeLoc1, LatLng fakeLoc2, String imageName, String imageAuthor, String name) {
        this(loc.latitude, loc.longitude, imageName, imageAuthor, name);
        firstFakeLocation = fakeLoc1;
        secondFakeLocation = fakeLoc2;
    }

    public LatLng getCorrectLocation() {
        return location;
    }

    @Nullable
    public LatLng getIncorrectLocation(int fake) {
        switch(fake) {
            case FIRST_FAKE_INDEX:
                return firstFakeLocation;
            case SECOND_FAKE_INDEX:
                return secondFakeLocation;
            default:
                return null;
        }
    }

    public void setIncorrectLoaciton(int fakeIndex, LatLng fakeLocation) {
        switch(fakeIndex) {
            case FIRST_FAKE_INDEX:
                firstFakeLocation = fakeLocation;
                break;
            case SECOND_FAKE_INDEX:
                secondFakeLocation = fakeLocation;
                break;
            default:
                // do nothing
        }
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    /**
     * Provide bounds so map ensure all markers representing the city locations are visible.
     *
     * @return Bounds given city's locations.
     */
    public LatLngBounds getBounds() {
        return new LatLngBounds.Builder()
                .include(location)
                .include(firstFakeLocation)
                .include(secondFakeLocation)
                .build();
    }

    public String getImageName() {
        return imageName;
    }

    public String getImageAuthor() {
        return imageAuthor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
