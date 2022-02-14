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
import com.google.android.apps.santatracker.tracker.vo.StreamEntry

@Dao
interface StreamDao {

    @Query("SELECT COUNT(*) FROM stream_entries")
    fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(streamEntries: List<StreamEntry>)

    @Query("SELECT * FROM stream_entries WHERE timestamp = :timestamp")
    fun byTimestamp(timestamp: Long): StreamEntry?

    @Query("SELECT * FROM stream_entries ORDER BY timestamp")
    fun all(): List<StreamEntry>

    @Query("DELETE FROM stream_entries")
    fun deleteAll()
}
