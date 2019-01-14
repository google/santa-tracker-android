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
package com.google.android.apps.santatracker.presentquest;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.apps.santatracker.presentquest.repository.PQRepository;
import com.google.android.apps.santatracker.presentquest.util.Config;
import com.google.android.apps.santatracker.presentquest.util.Distance;
import com.google.android.apps.santatracker.presentquest.util.PreferencesUtil;
import com.google.android.apps.santatracker.presentquest.vo.Place;
import com.google.android.apps.santatracker.presentquest.vo.Present;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

public class PlacesIntentService extends IntentService {

    private static final String TAG = "PQ(PlacesService)";
    private static final String ACTION_SEARCH_NEARBY = "ACTION_SEARCH_NEARBY";

    private static final String EXTRA_LAT_LNG = "extra_lat_lng";
    private static final String EXTRA_RADIUS = "extra_radius";
    private static final String EXTRA_PLACE_RESULT = "extra_place_result";
    private static final int MAX_QUERIES_IN_PROGRESS = 1;

    private AtomicInteger mQueriesInProgress = new AtomicInteger(0);

    private String mAppSignature;

    // Shared Prefs
    private PreferencesUtil mPreferences;

    // Firebase Config
    private Config mConfig;

    // Repository
    PQRepository repository;

    public PlacesIntentService() {
        super(TAG);
    }

    @NonNull
    public static IntentFilter getNearbySearchIntentFilter() {
        return new IntentFilter(ACTION_SEARCH_NEARBY);
    }

    public static void startNearbySearch(Context context, LatLng center, int radius) {
        SantaLog.d(TAG, "startNearbySearch: radius=" + radius);
        Intent intent = new Intent(context, PlacesIntentService.class);
        intent.setAction(ACTION_SEARCH_NEARBY);
        intent.putExtra(EXTRA_LAT_LNG, center);
        intent.putExtra(EXTRA_RADIUS, radius);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // TODO inject this
        repository = PQRepository.getInstance(this);
        if (intent != null) {
            final String action = intent.getAction();
            switch (action) {
                case ACTION_SEARCH_NEARBY:
                    // Don't allow more than X queries at once.
                    if (mQueriesInProgress.get() >= MAX_QUERIES_IN_PROGRESS) {
                        SantaLog.d(TAG, "Dropping excess query");
                        return;
                    }

                    // Mark query started
                    mQueriesInProgress.incrementAndGet();

                    if (mPreferences == null) {
                        mPreferences = new PreferencesUtil(this);
                    }
                    if (mConfig == null) {
                        mConfig = new Config();
                    }

                    // Perform query
                    final LatLng center = intent.getParcelableExtra(EXTRA_LAT_LNG);
                    final int radius = intent.getIntExtra(EXTRA_RADIUS, 0);
                    getPlaceAndBroadcast(center, radius);

                    // Mark query finished
                    mQueriesInProgress.decrementAndGet();
                    break;
                default:
                    SantaLog.w(TAG, "Unknown action: " + action);
            }
        }
    }

    @WorkerThread
    private void getPlaceAndBroadcast(LatLng center, int radius) {

        long now = System.currentTimeMillis();
        boolean useCache = now - mPreferences.getLastPlacesApiRequest() < mConfig.CACHE_REFRESH_MS;

        // Try and retrieve place from DB cache if CACHE_REFRESH_MS has not elapsed
        // since last API request.
        Place place = null;
        if (useCache) {
            place = getCachedPlace(center, radius);
        }

        // If CACHE_REFRESH_MS has elapsed, or no nearby places in cache, fetch from API and
        // cache the results, then return one of them. Guaranteed to have results since we
        // back-fill with random locations if too few returned from Places API.
        if (place == null) {
            // Set the last API request time with a +/- 30 sec jitter.
            int jitter = ((new Random()).nextInt(60) - 30) * 1000;
            mPreferences.setLastPlacesApiRequest(now + jitter);
            SantaLog.d(
                    TAG,
                    "getPlaceAndBroadcast: " + (useCache ? "cache miss" : "cache refresh elapsed"));
            place = fetchPlacesAndGetCached(center, radius);
        } else {
            SantaLog.d(TAG, "getPlaceAndBroadcast: cache hit");
        }

        // If the place is STILL null, just bail
        if (place == null) {
            SantaLog.w(TAG, "getPlaceAndBroadcast: total cache failure");
            return;
        }

        // Log some stats about the place picked
        int distance = Distance.between(center, place.getLatLng());
        SantaLog.d(TAG, "getPlaceAndBroadcast: distance=" + distance + ", used=" + place.used);

        // Create result intent and broadcast the result.
        Intent intent = new Intent();
        intent.setAction(ACTION_SEARCH_NEARBY);
        intent.putExtra(EXTRA_PLACE_RESULT, place.getLatLng());

        boolean received = LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        SantaLog.d(TAG, "getPlaceAndBroadcast: received=" + received);
        if (received) {
            // Increments usage counter.
            repository.usePlace(place);
        }
    }

    /**
     * Compares cached places by their distance from the requesting center, weighing their distance
     * by the number of times the place has been used.
     */
    private static class PlaceComparator implements Comparator<Place> {

        LatLng center;
        int radius;
        double usedPlaceRadiusWeight;

        PlaceComparator(LatLng center, int radius, double usedPlaceRadiusWeight) {
            this.center = center;
            this.radius = radius;
            this.usedPlaceRadiusWeight = usedPlaceRadiusWeight;
        }

        private int weightedDistanceTo(Place place) {
            return Distance.between(center, place.getLatLng())
                    + (int) (place.used * radius * usedPlaceRadiusWeight);
        }

        @Override
        public int compare(Place a, Place b) {
            return weightedDistanceTo(a) - weightedDistanceTo(b);
        }
    }

    // TODO move to repository
    @Nullable
    private Place getCachedPlace(LatLng center, int radius) {

        // Build a set of present locations we can use to check that we
        // don't choose a place that already exists as a present.
        Set<LatLng> presents = new HashSet<>();
        for (Present present : repository.getAllPresents()) {
            presents.add(present.getLatLng());
        }

        // Sort all places by distance, and filter down to the top X in correct proximity.
        // We'll then choose one of these randomly as the result.
        List<Place> allPlaces = repository.getAllPlaces();
        Collections.sort(
                allPlaces, new PlaceComparator(center, radius, mConfig.USED_PLACE_RADIUS_WEIGHT));
        List<Place> potentialPlaces = new ArrayList<>();
        for (Place place : allPlaces) {
            int distance = Distance.between(center, place.getLatLng());
            boolean closeEnough = distance <= radius;
            boolean farEnough = distance > mConfig.REACHABLE_RADIUS_METERS;
            if (closeEnough && farEnough && !presents.contains(place.getLatLng())) {
                potentialPlaces.add(place);
                if (potentialPlaces.size() >= mConfig.MAX_CACHE_RANDOM_SAMPLE_SIZE) {
                    break;
                }
            }
        }

        // Choose a random place from the possible results, or null for a cache miss.
        if (!potentialPlaces.isEmpty()) {
            return potentialPlaces.get(new Random().nextInt(potentialPlaces.size()));
        } else {
            return null;
        }
    }

    /**
     * Fetches places from the Places API, back-filling with random locations if too few returned,
     * and caches them all for future use.
     *
     * @param center
     * @param radius
     * @return
     */
    private Place fetchPlacesAndGetCached(LatLng center, int radius) {
        // Before we start, mark if this is the first run
        boolean firstRun = repository.getPlaceCount() == 0; // Place.count(Place.class)

        // Make places API request using double the radius, to have cached items while travelling.
        ArrayList<LatLng> places = fetchPlacesFromAPI(center, radius * 2);

        // TODO need to figure this one out
        // could make the id a combination of lat long...
        // these could each be executed in separate statements ... which would be terrible

        // TODO This should probably happen automatically, along with the places being loaded in
        // the repository
        repository.cachePlaces(places, mConfig, center, radius);

        // If it's the first run, try to return a particularly well-suited place
        if (firstRun) {
            Place firstRunPlace = getCachedFirstPlace(center);
            if (firstRunPlace != null) {
                return firstRunPlace;
            }
        }

        // Now that the cache is populated, use the logic to get a cached place and return it
        return getCachedPlace(center, radius);
    }

    // TODO move to repository
    /** Get a place from the cache that's particularly suited for the first drop. */
    @Nullable
    private Place getCachedFirstPlace(LatLng center) {
        // Try to find one in the cache
        List<Place> places = repository.getAllPlaces();
        for (Place place : places) {
            if (isValidFirstPlace(center, place.getLatLng())) {
                SantaLog.d(TAG, "getCachedFirstPlace: cache hit");
                return place;
            }
        }

        // If that didn't work, try to randomly generate one, we will search within
        // 1.5x the radius we want so that we have a better chance of a random hit.
        int maxSearchRadius =
                (int) (1.5 * mConfig.FIRST_PLACE_RADIUS_WEIGHT * mConfig.REACHABLE_RADIUS_METERS);
        int maxRandomTries = 100;
        for (int i = 0; i < maxRandomTries; i++) {
            LatLng latLng = PQRepository.randomLatLng(center, maxSearchRadius);
            if (isValidFirstPlace(center, latLng)) {
                SantaLog.d(TAG, "getCachedFirstPlace: got random, attempt " + i);
                SantaLog.d(
                        TAG,
                        "getCachedFirstPlace: distance is " + Distance.between(center, latLng));

                // Save place and return
                Place place = new Place(latLng);
                repository.savePlace(place);

                return place;
            }
        }

        // We got really, really unlucky
        SantaLog.d(TAG, "getCachedFirstPlace: no hits");
        return null;
    }

    private boolean isValidFirstPlace(LatLng center, LatLng placeLatLng) {
        int distance = Distance.between(center, placeLatLng);
        int minDistance = mConfig.REACHABLE_RADIUS_METERS;
        int maxDistance =
                (int) (mConfig.REACHABLE_RADIUS_METERS * mConfig.FIRST_PLACE_RADIUS_WEIGHT);
        return distance > minDistance && distance < maxDistance;
    }

    private ArrayList<LatLng> fetchPlacesFromAPI(LatLng center, int radius) {
        ArrayList<LatLng> places = new ArrayList<>();
        radius = Math.min(radius, 50000); // Max accepted radius is 50km.
        // For privacy, round latlng to 2 decimal places
        double roundFactor = 10000;
        double lat = Math.round(center.latitude * roundFactor) / roundFactor;
        double lng = Math.round(center.longitude * roundFactor) / roundFactor;

        try {
            InputStream is = null;
            URL url =
                    new URL(
                            getString(R.string.places_api_url)
                                    + "?location="
                                    + lat
                                    + ","
                                    + lng
                                    + "&radius="
                                    + radius);
            SantaLog.d(TAG, "fetchPlacesFromAPI for " + center + ": " + url);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            // Pass package name and signature as part of request
            String packageName = getPackageName();
            String signature = getAppSignature();

            conn.setRequestProperty("X-App-Package", packageName);
            conn.setRequestProperty("X-App-Signature", signature);

            conn.connect();
            int response = conn.getResponseCode();
            if (response != 200) {
                SantaLog.e(TAG, "Places API HTTP error: " + response + " / " + url);
            } else {
                BufferedReader reader;
                StringBuilder builder = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                for (String line; (line = reader.readLine()) != null; ) {
                    builder.append(line);
                }
                JSONArray resultsJson =
                        (new JSONObject(builder.toString())).getJSONArray("results");
                for (int i = 0; i < resultsJson.length(); i++) {
                    JSONObject latLngJson =
                            ((JSONObject) resultsJson.get(i))
                                    .getJSONObject("geometry")
                                    .getJSONObject("location");
                    places.add(
                            new LatLng(latLngJson.getDouble("lat"), latLngJson.getDouble("lng")));
                }
            }
        } catch (Exception e) {
            SantaLog.e(TAG, "Exception parsing places API: " + e.toString());
        }

        return places;
    }

    @Nullable
    private String getAppSignature() {
        // Cache this so we don't need to calculate the signature on every request
        if (mAppSignature != null) {
            return mAppSignature;
        }

        try {
            // Get signatures for the package
            Signature[] sigs =
                    getPackageManager()
                            .getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES)
                            .signatures;

            // There should only be one signature, anything else is suspicious
            if (sigs == null || sigs.length > 1 || sigs.length == 0) {
                SantaLog.w(TAG, "Either 0 or >1 signatures, returning null");
                return null;
            }

            byte[] certBytes = sigs[0].toByteArray();

            InputStream input = new ByteArrayInputStream(certBytes);
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(input);

            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert.getEncoded());

            // Build a hex string of the SHA1 Digest
            StringBuilder hexString = new StringBuilder();
            for (byte aPublicKey : publicKey) {
                // Convert each byte to hex
                String appendString = Integer.toHexString(0xFF & aPublicKey);
                if (appendString.length() == 1) {
                    hexString.append("0");
                }

                // Convert to upper case and add ":" separators so it matches keytool output
                appendString = appendString.toUpperCase() + ":";

                hexString.append(appendString);
            }

            // Convert to string, chop off trailing colon
            String signature = hexString.toString();
            if (signature.endsWith(":")) {
                signature = signature.substring(0, signature.length() - 1);
            }

            // Set and return
            mAppSignature = signature;
            return mAppSignature;
        } catch (Exception e) {
            SantaLog.e(TAG, "getSignature", e);
        }

        return null;
    }

    /** BroadcastReceiver to get result of nearby search. */
    public abstract static class NearbyResultReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            LatLng place = intent.getParcelableExtra(EXTRA_PLACE_RESULT);
            onResult(place);
        }

        /**
         * Called when a new result is returned.
         *
         * @param place resulting {@link LatLng}.
         */
        public abstract void onResult(LatLng place);
    }
}
