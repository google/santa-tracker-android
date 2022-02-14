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

package com.google.android.apps.santatracker.tracker.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.Date
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Fetch and cache data from Firebase Storage.

 * TODO(samstern): Improvements
 * - Firebase authentication to protect the JSON (if desired)
 * - Make all timeouts configurable
 * - Log errors in a way that we can track them from Firebase or Analytics
 */
class FirebaseStorageFetcher {

    private val context: Context
    private val prefs: SharedPreferences
    private val storage: FirebaseStorage
    private val executor: Executor

    constructor(context: Context) {
        this.context = context.applicationContext
        prefs = context.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE)
        storage = FirebaseStorage.getInstance()
        executor = EXECUTOR
    }

    @SuppressLint("VisibleForTests")
    internal constructor(
        context: Context,
        prefs: SharedPreferences,
        storage: FirebaseStorage,
        executor: Executor
    ) {
        this.context = context
        this.prefs = prefs
        this.storage = storage
        this.executor = executor
    }

    /**
     * Get the contents of a file from Firebase Storage, or the cache.
     *
     * @param path the path to the file in Firebase Storage.
     * @param maxAge the max age for an acceptable cached result.
     * @param unit time unit for max age.
     * @return a Task that will resolve to the String contents of the file.
     */
    operator fun get(path: String, maxAge: Long, unit: TimeUnit): Task<String> {
        val source = TaskCompletionSource<String>()

        executor.execute {
            val fileContents = getFileContents(path, maxAge, unit)
            if (fileContents == null) {
                source.setException(Exception("Could not get file contents. Path: $path"))
            } else {
                source.setResult(fileContents)
            }
        }
        return source.task
    }

    /**
     * Get the contents of a file, reading from cache or Firebase Storage as appropriate.
     */
    @WorkerThread
    private fun getFileContents(path: String, maxAge: Long, unit: TimeUnit): String? {
        SantaLog.d(TAG, "getFileContents:" + path)

        if (cachedCopyExists(path)) {
            SantaLog.d(TAG, "Cached copy exists")

            val lastCacheTime = getLastCacheTime(path)
            val now = Date()

            val cacheAgeMs = now.time - lastCacheTime.time
            val maxAgeMs = TimeUnit.MILLISECONDS.convert(maxAge, unit)

            val cacheExpired = cacheAgeMs > maxAgeMs

            // Cache is not expired, return the cached copy
            if (!cacheExpired) {
                SantaLog.d(TAG, "Cached not expired, age " + cacheAgeMs)
                return getCachedCopy(path)
            }

            // Check with the server to see if w newer copy exists
            SantaLog.d(TAG, "Cache is too old, checking server")
            var lastUpdateTime = getLastUpdatedTime(path)

            // If we can't get last updated time, we will consider the last update time to be
            // the epoch and continue the flow. This will result in going to the cache.
            if (lastUpdateTime == null) {
                SantaLog.d(TAG, "Failed to get last updated time")
                lastUpdateTime = Date(0)
            }

            // If the cache is at least as new as the server, we can return the cached
            // copy and update our cache time
            if (lastCacheTime.time >= lastUpdateTime.time) {
                SantaLog.d(TAG, "Got last update time, cached copy still good")
                setLastCacheTime(path, now)
                return getCachedCopy(path)
            }

            // Need to download the file
            SantaLog.d(TAG, "Cache needs to be refreshed, downloading")
            return downloadAndCache(path)
        } else {
            // Need to download the file
            SantaLog.d(TAG, "No cached copy, downloading.")
            return downloadAndCache(path)
        }
    }

    /**
     * Determine if a cached copy of a file exists.
     */
    @SuppressLint("VisibleForTests")
    fun cachedCopyExists(path: String): Boolean {
        return getCacheLocation(path).exists() && getLastCacheTime(path).time > 0
    }

    /**
     * Get the last time that a file was cached.
     */
    private fun getLastCacheTime(path: String): Date {
        val key = KEY_PREFIX_LAST_CACHE + path
        val lastCachedMs = prefs.getLong(key, 0)

        return Date(lastCachedMs)
    }

    /**
     * Set the last time that a file was cached.
     */
    private fun setLastCacheTime(path: String, date: Date) {
        val key = KEY_PREFIX_LAST_CACHE + path
        val lastCachedMs = date.time

        prefs.edit().putLong(key, lastCachedMs).apply()
    }

    /**
     * Get the last time that the file was updated in Firebase Storage.
     */
    @WorkerThread
    private fun getLastUpdatedTime(path: String): Date? {
        val reference = storage.getReference(path)
        val getMetadataTask = reference.metadata

        return try {
            val metadata = Tasks.await(getMetadataTask, 10, TimeUnit.SECONDS)
            val updatedTimeMs = metadata.updatedTimeMillis
            Date(updatedTimeMs)
        } catch (e: Exception) {
            SantaLog.w(TAG, "getLastUpdatedTime:" + path, e)
            null
        }
    }

    /**
     * Download a file from Firebase Storage and put the results in the cache.
     */
    @WorkerThread
    private fun downloadAndCache(path: String): String? {
        val serverCopy = getServerCopy(path) ?: return null

        setCachedCopy(path, serverCopy)
        return getCachedCopy(path)
    }

    /**
     * Get the contents of a file from Firebase Storage.
     */
    @WorkerThread
    private fun getServerCopy(path: String): String? {
        val reference = storage.getReference(path)
        val getBytesTask = reference.getBytes(MAX_DOWNLOAD_BYTES)

        return try {
            val bytes = Tasks.await(getBytesTask, 60, TimeUnit.SECONDS)
            String(bytes)
        } catch (e: Exception) {
            SantaLog.w(TAG, "downloadAndCache:getBytes:" + path, e)
            null
        }
    }

    /**
     * Get the contents of a file from the cache.
     */
    @WorkerThread
    @SuppressLint("VisibleForTests")
    fun getCachedCopy(path: String): String? {
        if (!cachedCopyExists(path)) {
            return null
        }
        val cache = getCacheLocation(path)
        val buffer = CharArray(1024)
        val builder = StringBuilder()
        try {
            val isr = InputStreamReader(FileInputStream(cache), "UTF-8")

            while (true) {
                val read = isr.read(buffer, 0, buffer.size)
                if (read < 0) {
                    break
                }
                builder.append(buffer, 0, read)
            }
            isr.close()
        } catch (e: IOException) {
            SantaLog.w(TAG, "getCachedCopy:" + path, e)
            return null
        }
        return builder.toString()
    }

    /**
     * Set the contents of a file in the cache, and update the cache timestamp.
     */
    @WorkerThread
    private fun setCachedCopy(path: String, contents: String) {
        val cache = getCacheLocation(path)

        try {
            // Make sure the file exists
            if (!cache.exists()) {
                cache.createNewFile()
            }

            // Write the file contents
            val fos = FileOutputStream(cache)
            fos.write(contents.toByteArray())

            // Set the last cached time
            setLastCacheTime(path, Date())
        } catch (e: IOException) {
            SantaLog.w(TAG, "setCachedCopy:setCachedCopy:" + path, e)
        }
    }

    /**
     * Get the location of a cached file based on its Firebase Storage path.
     */
    private fun getCacheLocation(path: String): File {
        val cacheDir = context.cacheDir
        val fileName = path.replace("/", "_")

        return File(cacheDir, fileName)
    }

    companion object {

        private const val TAG = "FSFetcher"

        private val EXECUTOR = ThreadPoolExecutor(2, 4,
                60, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

        private const val PREFERENCES_FILENAME = "FirebaseStoragePrefs"

        // Should not be more than 5MB for a single file or we messed up somehow
        private const val MAX_DOWNLOAD_BYTES = (5 * 1000 * 1000).toLong()

        private const val KEY_PREFIX_LAST_CACHE = "last_cached:"
    }
}
