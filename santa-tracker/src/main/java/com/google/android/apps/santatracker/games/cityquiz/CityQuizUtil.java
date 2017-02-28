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

import android.content.Context;
import android.util.Log;

import com.google.android.apps.santatracker.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Utility class to assist with loading city data into City Quiz Games.
 */
public class CityQuizUtil {

    private static final String TAG = CityQuizUtil.class.getSimpleName();

    /**
     * Retrieve a random list of cities.
     *
     * @param amt Max number of cities to retrieve.
     *
     * @return Random list of cities. If amt is more than the amount of cities available, all cities are returned.
     */
    public static List<City> getCities(Context context, int amt) {
        List<City> allCities = getCities(context);
        Collections.shuffle(allCities, new Random());
        if (amt > allCities.size()) {
            amt = allCities.size();
        }
        // Only return the cities that will be used in the game.
        List<City> cities = new ArrayList<>();
        cities.addAll(0, allCities.subList(0, amt));
        return cities;
    }

    private static List<City> getCities(Context context) {
        List<City> cities = new ArrayList<>();
        JSONArray jCities = getCitiesFromFile(context);
        for (int i = 0; jCities != null && i < jCities.length(); i++) {
            try {
                JSONObject jCity = jCities.getJSONObject(i);
                double lat = jCity.getDouble("lat");
                double lng = jCity.getDouble("lng");
                String cityResourceName = jCity.getString("name");
                int cityNameResourceId = context.getResources()
                        .getIdentifier(cityResourceName, "string", context.getPackageName());
                String cityName;
                // Check if city name string resource is found.
                if (cityNameResourceId != 0) {
                    // Use string resource for city name.
                    cityName = context.getResources().getString(cityNameResourceId);
                } else {
                    // Use default English city name.
                    cityName = jCity.getString("default_name");
                }
                String imageUrl = jCity.getString("image_name");
                String imageAuthor = jCity.getString("image_author");
                City city = new City(lat, lng, imageUrl, imageAuthor, cityName);
                cities.add(city);
            } catch(JSONException e) {
                Log.e(TAG, "Unable to get city from json, " + e);
            }
        }

        // Check if there are enough cities to set fake ones.
        if (cities.size() > 3) {
            // Set fake locations for each city.
            for (int i = 0; i < cities.size(); i++) {
                City city = cities.get(i);
                List<City> tempCities = new ArrayList<>(cities);
                // Sort tempCities in order of closest to the current city.
                Collections.sort(tempCities, new CityLocationComparator(city));

                // Get the closest three cities, excluding the current city.
                List<City> closestCities = tempCities.subList(1, 4);
                Collections.shuffle(closestCities);

                // Choose the first two of the three cities from the closestCities list.
                city.setIncorrectLoaciton(City.FIRST_FAKE_INDEX, closestCities.get(0).getCorrectLocation());
                city.setIncorrectLoaciton(City.SECOND_FAKE_INDEX, closestCities.get(1).getCorrectLocation());
            }
        }

        return cities;
    }

    private static JSONArray getCitiesFromFile(Context context) {
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.city_quiz_cities);
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }

            JSONArray jsonArray = new JSONArray(total.toString());
            return jsonArray;
        } catch(IOException e) {
            Log.e(TAG, "Unable to read city quiz json file, " + e);
        } catch(JSONException e) {
            Log.e(TAG, "Unable to parse city quiz json, " + e);
        }
        return null;
    }

}
