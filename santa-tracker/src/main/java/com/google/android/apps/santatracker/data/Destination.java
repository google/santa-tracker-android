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

package com.google.android.apps.santatracker.data;

import com.google.android.gms.maps.model.LatLng;

/**
 * A destination that is on Santa's path.
 *
 */
public class Destination {

    // delimiter between city,region and country when constructing name
    private static final String NAME_DELIMITER = ", ";

    public int id;
    public String identifier;

    public String city;
    public String region;
    String country;

    public long arrival;
    public long departure;

    public LatLng position;

    public long presentsDelivered;
    public long presentsDeliveredAtDestination;

    // Details
    long timezone;
    long altitude;
    String photoString;
    String weatherString;
    String streetViewString;
    String gmmStreetViewString;

    // Parsed data
    public Weather weather = null;
    public Photo[] photos = null;
    public StreetView streetView = null;
    StreetView gmmStreetView = null;

    /**
     * Returns the concatenated name of this destination. City is required,
     * region and country are optional.
     */
    public String getPrintName() {
        StringBuilder s = new StringBuilder(city);

        if (region != null) {
            s.append(NAME_DELIMITER);
            s.append(region);
        }

        if (country != null) {
            s.append(NAME_DELIMITER);
            s.append(country);
        }

        return s.toString();
    }

    @Override
    public String toString() {
        return "Destination [id=" + id + ", identifier=" + identifier
                + ", city=" + city + ", region=" + region + ", country="
                + country + ", arrival=" + arrival + ", departure=" + departure
                + ", position=" + position + ", presentsDelivered="
                + presentsDelivered + ", presentsDeliveredAtDestination="
                + presentsDeliveredAtDestination + "]";
    }

    public static class Weather {

        public String url = null;
        public double tempC = Integer.MAX_VALUE;
        public double tempF = Integer.MAX_VALUE;
    }

    public static class Photo {
        public String url = null;
        public String attributionHTML = null;
    }

    public static class StreetView {

        public String id = null;
        public LatLng position = null;
        public double heading = 0.0;
    }
}