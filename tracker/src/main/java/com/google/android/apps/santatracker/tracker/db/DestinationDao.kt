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

package com.google.android.apps.santatracker.tracker.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.apps.santatracker.tracker.vo.DestinationTimestamp

@Dao
interface DestinationDao {

    @Query("SELECT COUNT(*) FROM destinations")
    fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(destinations: List<Destination>)

    @Query("SELECT * FROM destinations WHERE id = :id")
    fun byId(id: String): Destination?

    @Query("SELECT * FROM destinations ORDER BY arrival")
    fun all(): List<Destination>

    @Query("SELECT arrival, departure FROM destinations ORDER BY arrival LIMIT 1")
    fun first(): DestinationTimestamp

    @Query("SELECT arrival, departure FROM destinations ORDER BY arrival DESC LIMIT 1")
    fun last(): DestinationTimestamp

    @Query("DELETE FROM destinations")
    fun deleteAll()
}
