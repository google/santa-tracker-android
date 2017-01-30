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

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class Config {

    /** Millisecond interval of location updates **/
    public final int LOCATION_REQUEST_INTERVAL_MS;

    /** Millisecond interval of fastest location updates **/
    public final int LOCATION_REQUEST_INTERVAL_FASTEST_MS;

    /** Minimum nearby presents required before we stop dropping presents **/
    public final int MIN_NEARBY_PRESENTS;

    /** Radius in meters for classifying various things as "nearby" **/
    public final int NEARBY_RADIUS_METERS;

    /** Radius in meters for various things as within reach **/
    public final int REACHABLE_RADIUS_METERS;

    /** Max number of presents that can exist on the map in total **/
    public final int MAX_PRESENTS;

    /** Minimum number of nearby places cached - if too few, we back-fill with random (for network performance) **/
    public final int MIN_CACHED_PLACES;

    /** Maximum number of nearby places cached - if too many, we cull the oldest (for disk performance) **/
    public final int MAX_CACHED_PLACES;

    /** Millisecond interval between hitting Places API requests, instead of using cached places **/
    public final long CACHE_REFRESH_MS;

    /** Weight of NEARBY_PRESENTS_RADIUS_METERS we use in ranking cached places by distance, by
     *  their number of uses **/
    public final double USED_PLACE_RADIUS_WEIGHT;

    /** Weight of REACHABLE_RADIUS_METERS we use for positioning the very first present **/
    public final double FIRST_PLACE_RADIUS_WEIGHT;

    /** Number of closest cached places we'll randomly choose from when returning a place from the cache **/
    public final int MAX_CACHE_RANDOM_SAMPLE_SIZE;

    /** Radius (in meters) within which we consider a workshop somewhat nearby **/
    public final int NEARBY_WORKSHOP_RADIUS;

    public Config() {
        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        LOCATION_REQUEST_INTERVAL_MS =         (int) config.getLong("PqLocationRequestIntervalMs");
        LOCATION_REQUEST_INTERVAL_FASTEST_MS = (int) config.getLong("PqLocationRequestIntervalFastestMs");
        MIN_NEARBY_PRESENTS =                  (int) config.getLong("PqMinNearbyPresents");
        NEARBY_RADIUS_METERS =                 (int) config.getLong("PqNearbyRadiusMeters");
        REACHABLE_RADIUS_METERS =              (int) config.getLong("PqReachableRadiusMeters");
        MAX_PRESENTS =                         (int) config.getLong("PqMaxPresents");
        MIN_CACHED_PLACES =                    (int) config.getLong("PqMinCachedPlaces");
        MAX_CACHED_PLACES =                    (int) config.getLong("PqMaxCachedPlaces");
        CACHE_REFRESH_MS =                           config.getLong("PqCacheRefreshMs");
        USED_PLACE_RADIUS_WEIGHT =                   config.getDouble("PqUsedPlaceRadiusWeight");
        FIRST_PLACE_RADIUS_WEIGHT =                  config.getDouble("PqFirstPlaceRadiusWeight");
        MAX_CACHE_RANDOM_SAMPLE_SIZE =         (int) config.getLong("PqMaxCacheRandomSampleSize");
        NEARBY_WORKSHOP_RADIUS =               (int) config.getLong("PqNearbyWorkshopRadius");
    }

}
