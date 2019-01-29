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

package com.google.android.apps.santatracker.di

import android.app.Application
import androidx.room.Room
import com.google.android.apps.santatracker.BuildConfig
import com.google.android.apps.santatracker.config.Config
import com.google.android.apps.santatracker.tracker.api.RemoteSantaApi
import com.google.android.apps.santatracker.tracker.api.SantaApi
import com.google.android.apps.santatracker.tracker.db.DestinationDao
import com.google.android.apps.santatracker.tracker.db.MetadataDao
import com.google.android.apps.santatracker.tracker.db.SantaDatabase
import com.google.android.apps.santatracker.tracker.db.StreamDao
import com.google.android.apps.santatracker.tracker.time.Clock
import com.google.android.apps.santatracker.tracker.time.LocalOffsettableClock
import com.google.android.apps.santatracker.tracker.time.OffsettableClock
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import javax.inject.Singleton

/**
 * Application level module for Dagger
 */
@Module(includes = arrayOf(ViewModelModule::class))
class AppModule {

    @Singleton
    @Provides
    fun provideDb(application: Application): SantaDatabase {
        return Room.databaseBuilder(application, SantaDatabase::class.java,
                "SantaDatabase").build()
    }

    @Singleton
    @Provides
    fun provideDestinationDao(db: SantaDatabase): DestinationDao {
        return db.destination()
    }

    @Singleton
    @Provides
    fun provideStreamDao(db: SantaDatabase): StreamDao {
        return db.stream()
    }

    @Singleton
    @Provides
    fun provideMetadataDao(db: SantaDatabase): MetadataDao {
        return db.metadata()
    }

    @Singleton
    @Provides
    fun provideExecutor(): Executor {
        val deviceCpuCount = Runtime.getRuntime().availableProcessors()
        return Executors.newFixedThreadPool((deviceCpuCount + 1).coerceAtLeast(3))
    }

    @Singleton
    @Provides
    fun provideSantaApi(app: Application): SantaApi {
        return RemoteSantaApi(app)
    }

    @Singleton
    @Provides
    fun provideClock(application: Application, config: Config, executor: Executor): Clock {
        return if (BuildConfig.DEBUG) {
            LocalOffsettableClock(application, config, executor)
        } else {
            OffsettableClock(config, executor)
        }
    }

    @Provides
    fun provideScheduledExecutorService(): ScheduledExecutorService {
        return ScheduledThreadPoolExecutor(1)
    }

    @Provides
    fun provideFirebaseAnalytics(app: Application): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(app)
    }

    @Provides
    fun provideFirebaseAppIndexing(): FirebaseAppIndex {
        return FirebaseAppIndex.getInstance()
    }

    @Provides
    @Singleton
    fun provideConfig(): Config {
        return Config()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient() = OkHttpClient()

    @Provides
    @Singleton
    fun provideGson() = Gson()
}
