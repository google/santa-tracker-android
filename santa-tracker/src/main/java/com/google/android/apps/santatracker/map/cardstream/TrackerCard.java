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

import com.google.android.apps.santatracker.data.Destination;
import com.google.android.gms.maps.model.LatLng;

public abstract class TrackerCard {

    static final int TYPE_DASHBOARD = 1;
    static final int TYPE_FACTOID = 2; // Did you know...
    static final int TYPE_DESTINATION = 3;
    static final int TYPE_PHOTO = 4;
    static final int TYPE_MOVIE = 5;
    static final int TYPE_STATUS = 6;

    private static long sMaxId;

    public final long id;
    public final long timestamp;

    private TrackerCard(long timestamp) {
        this.id = ++sMaxId;
        this.timestamp = timestamp;
    }

    public abstract int getType();

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TrackerCard)) {
            return false;
        }
        TrackerCard card = (TrackerCard) o;
        return this.timestamp == card.timestamp && this.getType() == card.getType();
    }

    public static class Dashboard extends TrackerCard {

        private static Dashboard sInstance = new Dashboard(Long.MAX_VALUE);

        String nextDestination;

        /**
         * This card has a timestamp of {@link Long#MAX_VALUE}, which makes sure that the dashboard
         * is always at the top of the card list.
         */
        public static Dashboard getInstance() {
            return sInstance;
        }

        private Dashboard(long timestamp) {
            super(timestamp);
        }

        @Override
        public int getType() {
            return TYPE_DASHBOARD;
        }
    }

    public static class FactoidCard extends TrackerCard {
        String factoid;

        FactoidCard(long timestamp, String didYouKnow) {
            super(timestamp);
            this.factoid = didYouKnow;
        }

        @Override
        public int getType() {
            return TYPE_FACTOID;
        }
    }

    static class DestinationCard extends TrackerCard {

        final boolean fromUser;
        public final String city;
        final String region;
        public final String url;
        final Destination.StreetView streetView;
        final String attributionHtml;
        final boolean hasWeather;
        final double tempC;
        final double tempF;
        public LatLng position;

        DestinationCard(long timestamp, LatLng position, boolean fromUser, String city,
                        String region, String url, String attributionHtml,
                        Destination.StreetView streetView, boolean hasWeather,
                        double tempC, double tempF) {
            super(timestamp);
            this.fromUser = fromUser;
            this.city = city;
            this.region = region;
            this.url = url;
            this.attributionHtml = attributionHtml;
            this.position = position;
            this.streetView = streetView;
            this.hasWeather = hasWeather;
            this.tempC = tempC;
            this.tempF = tempF;
        }

        @Override
        public int getType() {
            return TYPE_DESTINATION;
        }
    }

    public static class PhotoCard extends TrackerCard {

        String imageUrl;
        public String caption;

        PhotoCard(long timestamp, String image, String caption) {
            super(timestamp);
            this.imageUrl = image;
            this.caption = caption;
        }

        @Override
        public int getType() {
            return TYPE_PHOTO;
        }
    }

    public static class MovieCard extends TrackerCard {

        String youtubeId;

        MovieCard(long timestamp, String video) {
            super(timestamp);
            this.youtubeId = video;
        }

        @Override
        public int getType() {
            return TYPE_MOVIE;
        }
    }

    public static class StatusCard extends TrackerCard {

        public String status;

        StatusCard(long timestamp, String santaStatus) {
            super(timestamp);
            this.status = santaStatus;
        }

        @Override
        public int getType() {
            return TYPE_STATUS;
        }
    }

}
