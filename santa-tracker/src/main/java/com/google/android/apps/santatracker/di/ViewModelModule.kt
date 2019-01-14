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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.apps.santatracker.tracker.viewmodel.TrackerViewModel
import com.google.android.apps.santatracker.viewmodel.VillageViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(TrackerViewModel::class)
    abstract fun bindTrackerViewModel(trackerViewModel: TrackerViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(VillageViewModel::class)
    abstract fun bindVillageViewModel(villageViewModel: VillageViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: TrackerViewModelFactory): ViewModelProvider.Factory
}
