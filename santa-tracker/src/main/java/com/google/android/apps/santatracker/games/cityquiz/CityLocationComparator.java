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

import com.google.maps.android.SphericalUtil;

import java.util.Comparator;

/**
 * Compare the distance between two Cities and the City of this comparator.
 */
public class CityLocationComparator implements Comparator<City> {

    private City city;

    public CityLocationComparator(City city) {
        this.city = city;
    }

    @Override
    public int compare(City c1, City c2) {
        double dist1 = SphericalUtil.computeDistanceBetween(
                city.getCorrectLocation(), c1.getCorrectLocation());
        double dist2 = SphericalUtil.computeDistanceBetween(
                city.getCorrectLocation(), c2.getCorrectLocation());

        if (dist1 > dist2) {
            return 1;
        } else if(dist1 < dist2) {
            return -1;
        } else {
            return 0;
        }
    }

}
