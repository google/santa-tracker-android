/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.cityquiz

import android.content.Context
import com.google.android.apps.santatracker.util.SantaLog
import org.json.JSONArray
import org.json.JSONException
import java.util.ArrayList

/** Utility class to assist with loading city data into City Quiz Games.  */
object CityQuizUtil {
    private const val TAG = "CityQuizUtil"

    /**
     * Retrieve a random list of cities.
     *
     * @param amountOfCities Max number of cities to retrieve.
     * @return Random list of cities. If amountOfCities is more than the amount of cities available, all cities
     * are returned.
     */
    fun getCities(context: Context, amountOfCities: Int): List<City> {
        val allCities = getCities(context)
        allCities.shuffle()
        // Only return the cities that will be used in the game.
        return allCities.subList(0, amountOfCities.coerceAtMost(allCities.size))
    }

    private fun getCities(context: Context): MutableList<City> {
        val cities = ArrayList<City>()
        val jCities = getCitiesFromFile(context)

        for (i in 0 until jCities.length()) {
            try {
                val jCity = jCities.getJSONObject(i)
                val lat = jCity.getDouble("lat")
                val lng = jCity.getDouble("lng")
                val cityResourceName = jCity.getString("name")
                val cityNameResourceId = context.resources
                        .getIdentifier(cityResourceName, "string", context.packageName)

                // Check if city name string resource is found.
                val cityName = if (cityNameResourceId != 0) {
                    // Use string resource for city name.
                    context.resources.getString(cityNameResourceId)
                } else {
                    // Use default English city name.
                    jCity.getString("default_name")
                }

                val imageUrl = jCity.getString("image_name")
                val imageAuthor = jCity.getString("image_author")
                val city = City(lat, lng, imageUrl, imageAuthor, cityName)
                cities.add(city)
            } catch (e: JSONException) {
                SantaLog.e(TAG, "Unable to get city from json, $e")
            }
        }

        // Check if there are enough cities to set fake ones.
        if (cities.size > 3) {
            // Set fake locations for each city.
            val tempCities = ArrayList(cities)

            for (city in cities) {
                // Sort tempCities in order of closest to the current city.
                tempCities.sortWith(CityLocationComparator(city))

                // Get the closest three cities, excluding the current city.
                val closestCities = tempCities.subList(1, 4)
                closestCities.shuffle()

                // Choose the first two of the three cities from the closestCities list.
                city.incorrectLocationOne = closestCities[0].correctLocation
                city.incorrectLocationTwo = closestCities[1].correctLocation
            }
        }

        return cities
    }

    private fun getCitiesFromFile(context: Context) = try {
        val inputStream = context.resources.openRawResource(R.raw.city_quiz_cities)
        inputStream.bufferedReader().use { r ->
            JSONArray(r.readText())
        }
    } catch (e: Exception) {
        SantaLog.e(TAG, "Unable to parse city quiz json, $e")
        JSONArray()
    }
}
