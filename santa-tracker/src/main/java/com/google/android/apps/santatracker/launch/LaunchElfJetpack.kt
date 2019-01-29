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

package com.google.android.apps.santatracker.launch

import android.view.View
import com.google.android.apps.santatracker.R
import com.google.android.apps.santatracker.games.SplashActivity

class LaunchElfJetpack(
    context: SantaContext,
    adapter: LauncherDataChangedCallback
) : AbstractFeatureModuleLaunch(context, adapter, R.string.jetpack, R.drawable.android_game_cards_elf_jetpack) {

    override val featureModuleNameId: Int = R.string.feature_jetpack

    override fun onClick(v: View) {
        when (state) {
            AbstractLaunch.STATE_READY, AbstractLaunch.STATE_FINISHED -> {
                val intent = SplashActivity.getIntent(
                        mContext.activity,
                        cardDrawableRes,
                        R.string.jetpack,
                        featureModuleNameId,
                        R.color.elf_jetpack_splash_screen_background,
                        true /* landscape */,
                        title,
                        imageView,
                        mContext.applicationContext.packageName,
                        "com.google.android.apps.jetpack.JetpackActivity")
                mContext.launchActivity(intent)
            }
            AbstractLaunch.STATE_DISABLED, AbstractLaunch.STATE_LOCKED -> {
                notify(mContext.applicationContext, R.string.generic_game_disabled, title)
            }
            else -> {
                notify(mContext.applicationContext, R.string.generic_game_disabled, title)
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        when (state) {
            AbstractLaunch.STATE_READY, AbstractLaunch.STATE_FINISHED -> {
                notify(mContext.applicationContext, title)
            }
            AbstractLaunch.STATE_DISABLED, AbstractLaunch.STATE_LOCKED -> {
                notify(mContext.applicationContext, R.string.generic_game_disabled, title)
            }
            else -> {
                notify(mContext.applicationContext, R.string.generic_game_disabled, title)
            }
        }
        return true
    }
}
