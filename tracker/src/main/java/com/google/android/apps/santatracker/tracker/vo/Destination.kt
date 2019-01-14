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

package com.google.android.apps.santatracker.tracker.vo

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.apps.santatracker.tracker.parser.ApiDestination
import com.google.android.gms.maps.model.LatLng

@Keep
@Entity(tableName = "destinations")
data class Destination(

    @PrimaryKey
    val id: String,

    val arrival: Long,

    val departure: Long,

    val population: Long,

    val presentsDelivered: Long,

    val city: String,

    val region: String,

    @Embedded(prefix = "location_")
    val location: DestinationLocation,

    val timezone: Long?,

    val altitude: Double,

    @Embedded(prefix = "weather_")
    val weather: DestinationWeather?,

    @Embedded(prefix = "sv_")
    val streetView: DestinationStreetView?,

    @Embedded(prefix = "gsv_")
    val gmmStreetView: DestinationStreetView?,

    @Embedded(prefix = "photo_")
    val photo: DestinationPhoto?
) : TrackerCard {

    constructor(obj: ApiDestination) : this(
                obj.id,
                obj.arrival,
                obj.departure,
                obj.population,
                obj.presentsDelivered,
                obj.city,
                obj.region,
                obj.location,
                obj.details.timezone,
                obj.details.altitude,
                obj.details.weather,
                obj.details.streetView,
                obj.details.gmmStreetView,
                obj.details.firstPhoto()
        )

    val printName: String
        get() = if (region.isNotEmpty()) "$city, $region" else city

    override val value: Long
        get() = departure

    val latLng: LatLng
        get() = LatLng(location.lat, location.lng)
}
