/*
 * Copyright (C) 2017 Google Inc. All Rights Reserved.
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

package com.google.android.apps.santatracker.presentquest.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.google.android.apps.santatracker.presentquest.db.PQDatabase;
import com.google.android.apps.santatracker.presentquest.util.Config;
import com.google.android.apps.santatracker.presentquest.vo.Place;
import com.google.android.apps.santatracker.presentquest.vo.Present;
import com.google.android.apps.santatracker.presentquest.vo.User;
import com.google.android.apps.santatracker.presentquest.vo.Workshop;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class PQRepository {
    private static final String LOG_TAG = PQRepository.class.getSimpleName();
    // For Singleton instantiation
    private static final Object LOCK = new Object();
    private static PQRepository repository;
    // private final Executor diskIO;
    private final PQDatabase database;

    public PQRepository(Context context) {
        // diskIO = Executors.newSingleThreadExecutor(); //TODO need to inject this
        database = PQDatabase.getInstance(context);
    }

    public static PQRepository getInstance(Context context) {
        SantaLog.d(LOG_TAG, "Getting the database");
        if (repository == null) {
            synchronized (LOCK) {
                repository = new PQRepository(context);
                SantaLog.d(LOG_TAG, "Made new database");
            }
        }
        return repository;
    }

    // Workshop methods

    public void saveWorkshopWithTimestamp(final Workshop workshop) {
        workshop.updated = System.currentTimeMillis();
        saveWorkshop(workshop);
    }

    public List<Workshop> getAllWorkshops() {
        return database.workshopDao().getAll();
    }

    public LiveData<List<Workshop>> getAllWorkshopsNewToOld() {
        return database.workshopDao().getAllNewToOld();
    }

    public Workshop getWorkshopById(long workshopId) {
        return database.workshopDao().findById(workshopId);
    }

    public Workshop getFirstWorkshop() {
        return database.workshopDao().getFirst();
    }

    public void saveWorkshop(Workshop workshop) {
        int rows = database.workshopDao().updateWorkshop(workshop);
        if (rows <= 0) database.workshopDao().insertWorkshop(workshop);
    }

    public void deleteWorkshop(Workshop workshop) {
        database.workshopDao().delete(workshop);
    }

    // Place methods

    public List<Place> getAllPlaces() {
        return database.placeDao().getAll();
    }

    public Place getPlaceByLatLong(LatLng placeToFind) {
        return database.placeDao().getByLatLong(placeToFind.latitude, placeToFind.longitude);
    }

    public void savePlace(Place place) {
        int rows = database.placeDao().updatePlace(place);
        if (rows <= 0) database.placeDao().insertPlace(place);
    }

    public void usePlace(final Place p) {
        if (p.id != 0) {
            p.used += 1;
            savePlace(p);
        }
    }

    public int getPlaceCount() {
        return database.placeDao().count();
    }

    // Build set of locations that Places API returned and are already cached, which
    // we can check against before caching a new location from Places API.
    public void cachePlaces(ArrayList<LatLng> places, Config config, LatLng center, int radius) {
        int numFetched = places.size();
        SantaLog.d(LOG_TAG, "fetchPlaces: API returned " + numFetched + " place(s)");

        Set<LatLng> cached = new HashSet<>();
        if (places.size() > 0) {
            database.beginTransaction();
            try {
                for (LatLng placeToFind : places) {
                    Place place = repository.getPlaceByLatLong(placeToFind);
                    // TODO might be null?
                    if (place != null) cached.add(place.getLatLng());
                }
            } finally {
                database.endTransaction();
            }
        }

        SantaLog.d(LOG_TAG, "fetchPlaces: " + cached.size() + " place(s) are already cached");

        // Back-fill with random locations to ensure up to MIN_CACHED_PLACES places.
        // We reduce radius to half for these, to decrease the likelihood of
        // adding an inaccessible location.
        int fill = config.MIN_CACHED_PLACES - numFetched;
        if (fill > 0) {
            SantaLog.d(LOG_TAG, "fetchPlaces: back-filling with " + fill + " random places");
            for (int i = 0; i < fill; i++) {
                LatLng randomLatLng = randomLatLng(center, radius / 2);
                places.add(randomLatLng);
            }
        }

        // Save results to cache.
        SantaLog.d(LOG_TAG, "fetchPlaces: caching " + places.size());
        for (LatLng latLng : places) {
            Place place = new Place(latLng);
            // Check that the place isn't already in the cache, which is very likely since
            // if the rate limit elapses and the user hasn't moved, duplicates will be returned.
            if (!cached.contains(place.getLatLng())) {
                // place.save();
                repository.savePlace(place);
            } else {
                SantaLog.d(LOG_TAG, "Location already cached, discarding: " + latLng);
            }
        }

        cullPlaceCacheIfTooLarge(config);
    }

    private void cullPlaceCacheIfTooLarge(Config config) {
        // Cull the cache if too large.
        int numberToCull = Math.max((int) repository.getPlaceCount() - config.MAX_CACHED_PLACES, 0);
        SantaLog.d(LOG_TAG, "fetchPlaces: culling " + numberToCull + " cached places");
        if (numberToCull > 0) {
            String[] emptyArgs = {};
            int i = 0;
            // Get the list of oldest cached places we want to cull, and use its highest ID
            // as the arg to delete.
            // eg: SELECT FROM places ORDER BY id LIMIT 20;

            // TODO assumes ID always increasing, should probably be by timestamp instead...
            database.placeDao().deleteOldestById(numberToCull);
        }
    }

    // Present methods
    public void savePresent(final Present present) {
        // TODO not sure why but present doesn't update the updated field

        int rows = database.presentDao().updatePresent(present);
        if (rows <= 0) database.presentDao().insertPresent(present);
    }

    public List<Present> getAllPresents() {
        return database.presentDao().getAll();
    }

    public void deletePresent(Present present) {
        database.presentDao().delete(present);
    }

    public void collectPresents(final int numberCollected, final User user) {
        // Can't hold more than max capacity
        int maxCapacity = user.getMaxPresentsCollected();
        int newCollected = Math.min(maxCapacity, user.presentsCollected + numberCollected);

        user.presentsCollected = newCollected;

        database.userDao().updateUser(user);
    }

    public Present getLastPresent() {
        return database.presentDao().getLast();
    }

    // User methods

    public LiveData<User> getUser() {
        // TODO need to deal with user creation
        return database.userDao().loadUser();
    }

    // TODO not sure if this should be a insert or update
    public void saveUser(User user) {
        if (database.userDao().updateUser(user) == 0) {
            database.userDao().insertUser(user);
        }
    }

    public void returnPresentsAndEmpty(final int numberReturned, final User user) {
        // Can't return more than you've collected
        int maxReturn = Math.min(numberReturned, user.presentsCollected);

        // Increment presents returned, empty bag
        user.presentsReturned = user.presentsReturned + maxReturn;
        user.presentsCollected = 0;

        database.userDao().updateUser(user);
    }

    // Combined methods

    // FOR DEBUG ONLY -- Moves user down a level
    public void downlevel(final User user) {
        int currentLevel = user.getLevel();
        int requiredPreviousLevel = user.PRESENTS_REQUIRED[currentLevel - 1];

        int newPresentsReturned = (int) (requiredPreviousLevel / 2);
        user.presentsReturned = newPresentsReturned;
        user.presentsCollected = 0;

        database.userDao().updateUser(user);
    }

    public void resetAll(final User user) {

        database.beginTransaction();
        try {
            user.presentsCollected = 0;
            user.presentsReturned = 0;

            database.userDao().updateUser(user);
            database.workshopDao().deleteAll();
            database.presentDao().deleteAll();
            database.placeDao().deleteAll();

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public static LatLng randomLatLng(LatLng center, int radius) {
        // Based on
        // http://gis.stackexchange.com/questions/25877/how-to-generate-random-locations-nearby-my-location
        Random random = new Random();
        double radiusInDegrees = radius / 111000f;
        double u = random.nextDouble();
        double v = random.nextDouble();
        double w = radiusInDegrees * Math.sqrt(u);
        double t = 2 * Math.PI * v;
        double x = w * Math.cos(t);
        double y = w * Math.sin(t);
        double new_x = x / Math.cos(center.latitude);
        return new LatLng(y + center.latitude, new_x + center.longitude);
    }

    public User getUserNormal() {
        return database.userDao().getUserNormal();
    }
}
