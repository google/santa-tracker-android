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

import com.google.android.apps.santatracker.AppIndexingUpdateService
import com.google.android.apps.santatracker.launch.StartupActivity
import com.google.android.apps.santatracker.launch.TvStartupActivity
import com.google.android.apps.santatracker.messaging.SantaMessagingService
import com.google.android.apps.santatracker.tracker.ui.TrackerActivity
import com.google.android.apps.santatracker.web.WebSceneActivityLandscape
import com.google.android.apps.santatracker.web.WebSceneActivityPortrait
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Dagger module for defining the injector for Android components
 * (Activity, Service, BroadcastReceiver and ContentProvider)
 */
@Module
abstract class AndroidModule {
    @ContributesAndroidInjector(modules = arrayOf(FragmentBuildersModule::class))
    internal abstract fun contributeTrackerActivity(): TrackerActivity

    @ContributesAndroidInjector
    internal abstract fun contributeStartupActivity(): StartupActivity

    @ContributesAndroidInjector
    internal abstract fun contributeTvStartupActivity(): TvStartupActivity

    @ContributesAndroidInjector
    internal abstract fun contributeWebSceneActivityPortrait(): WebSceneActivityPortrait

    @ContributesAndroidInjector
    internal abstract fun contributeWebSceneLandscapeActivity(): WebSceneActivityLandscape

    @ContributesAndroidInjector
    internal abstract fun contributeSantaMessagingService(): SantaMessagingService

    @ContributesAndroidInjector
    internal abstract fun contributeAppIndexingUpdateService(): AppIndexingUpdateService
}
