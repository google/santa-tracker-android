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
package com.google.android.apps.santatracker.games

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.apps.santatracker.BuildConfig
import com.google.android.apps.santatracker.R
import com.google.android.apps.santatracker.util.ImmersiveModeHelper
import com.google.android.play.core.splitinstall.SplitInstallHelper
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import kotlinx.android.synthetic.main.activity_splash.progressbar
import kotlinx.android.synthetic.main.activity_splash.splash_image
import kotlinx.android.synthetic.main.activity_splash.splash_title

/**
 * Splash screen for games. The splash screen rotates at runtime to match the orientation of the
 * game that will be launched. This makes launching the splash screen jank-free from any
 * orientation.
 */
class SplashActivity : AppCompatActivity() {

    private val handler = Handler()

    private var activityClassName: String? = null

    private val splitInstallManager: SplitInstallManager by lazy(LazyThreadSafetyMode.NONE) {
        SplitInstallManagerFactory.create(this)
    }

    private val installStateListener: FeatureLoadStateListener by lazy(LazyThreadSafetyMode.NONE) {
        object : FeatureLoadStateListener() {
            override fun onRequiresConfirmation(intentSender: IntentSender) {
                startIntentSender(intentSender, null, 0, 0, 0)
            }

            override fun onPending() {
                progressbar.isVisible = true
                progressbar.isIndeterminate = true
            }

            override fun onDownloading(bytesDownloaded: Long, totalBytesToDownload: Long) {
                if (bytesDownloaded >= (totalBytesToDownload * MIN_PROGRESS_DETERMINATE_PERC)) {
                    progressbar.isIndeterminate = false
                    // Use KB to minimize any overflow issues
                    progressbar.progress = (bytesDownloaded / 1024).toInt()
                    progressbar.max = (totalBytesToDownload / 1024).toInt()
                }
            }

            override fun onInstalling() = Unit

            override fun onInstalled() {
                SplitInstallHelper.updateAppInfo(this@SplashActivity)
                tryLaunchOrInstallModule(showFakeDownload = false)
            }

            override fun onFailure() {
                onFeatureModuleLaunchFailure()
            }
        }
    }

    private lateinit var title: String

    private val intentToLaunch: Intent by lazy {
        intent.getParcelableExtra<Intent>(EXTRA_GAME_INTENT).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameIsLandscape = intent.getBooleanExtra(EXTRA_LANDSCAPE, false)
        activityClassName = intent.getStringExtra(EXTRA_CLASS_NAME)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape != gameIsLandscape) {
            requestedOrientation = if (gameIsLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            return
        }

        setContentView(R.layout.activity_splash)

        // Immersive mode (to hide nav).
        ImmersiveModeHelper.setImmersiveSticky(window)

        // Set Image
        val imageTransitionName = intent.getStringExtra(EXTRA_IMAGE_TRANSITION_NAME)
        splash_image.transitionName = imageTransitionName

        val splashImageUrl = intent.getStringExtra(EXTRA_SPLASH_IMAGE_URL)
        if (splashImageUrl.isNullOrEmpty()) {
            val splashImageId = intent.getIntExtra(EXTRA_SPLASH_IMAGE_ID, -1)
            if (splashImageId != -1) {
                Glide.with(splash_image.context).load(splashImageId).into(splash_image)
            }
        } else {
            Glide.with(splash_image.context).load(splashImageUrl).into(splash_image)
        }

        val splashScreenColorId = intent.getIntExtra(EXTRA_SPLASH_SCREEN_COLOR_ID, -1)
        if (splashScreenColorId != -1) {
            val mainView = findViewById<View>(R.id.splash_layout)
            mainView.setBackgroundColor(ContextCompat.getColor(this, splashScreenColorId))
        }

        progressbar.apply {
            progressTintList = ContextCompat.getColorStateList(
                    this@SplashActivity, R.color.SantaWhite)
            indeterminateTintList = ContextCompat.getColorStateList(
                    this@SplashActivity, R.color.SantaWhite)
        }

        // Set Title
        title = getString(intent.getIntExtra(EXTRA_SPLASH_TITLE_ID, -1))
        splash_title.text = title

        if (intent.hasExtra(EXTRA_DYNAMIC_FEATURE_NAME_ID)) {
            tryLaunchOrInstallModule(SHOW_FAKE_FEATURE_MODULE_DOWNLOAD)
        } else {
            // We're not launching a feature module so just launch it delayed
            launchTargetActivityDelayed()
        }
    }

    private fun tryLaunchOrInstallModule(showFakeDownload: Boolean) {
        if (getDynamicFeatureModuleName() in splitInstallManager.installedModules) {
            if (showFakeDownload) {
                // We're set to show our fake download progress. This handy for testing the
                // UI without having to upload to Play
                showFakeFeatureModuleDownload()
            } else {
                // The feature module is already installed, so just launch it with a delay
                launchTargetActivityDelayed()
            }
        } else {
            val mgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (mgr.activeNetworkInfo?.isConnected == true) {
                // We have a active network...
                if (mgr.isActiveNetworkMetered) {
                    // ...but it is metered. Confirm with the user first
                    showMeteredNetworkConfirmDialog(::startModuleInstall)
                } else {
                    // ...otherwise just download the module
                    startModuleInstall()
                }
            } else {
                // We have no network connection. Show a failure and finish
                onFeatureModuleLaunchFailure()
            }
        }
    }

    private fun startModuleInstall() {
        progressbar.isVisible = true
        progressbar.isIndeterminate = true

        // The split isn't installed so lets start the split install request
        installStateListener.register(splitInstallManager)
        val request = SplitInstallRequest.newBuilder()
                .addModule(getDynamicFeatureModuleName())
                .build()
        splitInstallManager.startInstall(request)
    }

    private fun onFeatureModuleLaunchFailure() {
        Toast.makeText(
                applicationContext,
                getString(R.string.error_fetch_module, title),
                Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun launchTargetActivityDelayed() {
        handler.postDelayed(DELAY_MILLIS) {
            if (!isFinishing) {
                launchTargetActivity()
            }
        }
    }

    private fun launchTargetActivity() {
        val intent = intentToLaunch
        if (activityClassName != null) {
            if (classLoader.loadClass(activityClassName) != null) {
                createPackageContext(packageName, 0).startActivity(intent)
                finish()
            } else if (!isFinishing) {
                onFeatureModuleLaunchFailure()
            }
        } else {
            if (packageManager.resolveActivity(intent, 0) != null) {
                // The activity exists, so lets launch it
                createPackageContext(packageName, 0).startActivity(intent)
                finish()
            } else if (!isFinishing) {
                onFeatureModuleLaunchFailure()
            }
        }
    }

    private fun getDynamicFeatureModuleName(): String {
        val dynamicFeatureId = intent.getIntExtra(
                EXTRA_DYNAMIC_FEATURE_NAME_ID,
                DYNAMIC_FEATURE_NAME_DEFAULT_ID
        )
        return resources.getString(dynamicFeatureId)
    }

    private fun showFakeFeatureModuleDownload() {
        val fakeDownloadSize = 350 * 1024L
        installStateListener.onPending()

        val runnable = object : Runnable {
            var progress = 0L

            override fun run() {
                if (!isFinishing) {
                    if (progress <= fakeDownloadSize) {
                        installStateListener.onDownloading(progress, fakeDownloadSize)
                        handler.postDelayed(this, 200)
                    } else {
                        installStateListener.onInstalling()
                        installStateListener.onInstalled()
                    }
                    // Increment progress for next time
                    progress += (fakeDownloadSize / 20)
                }
            }
        }
        // Trigger the runnable by letting us pend for a second
        handler.postDelayed(runnable, 1000)
    }

    private fun showMeteredNetworkConfirmDialog(onAccept: () -> Unit) {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.download_dialog_title, title))
                .setMessage(R.string.download_dialog_message)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    onAccept()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }.show()
    }

    override fun onStop() {
        super.onStop()
        installStateListener.unregister(splitInstallManager)
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        const val DELAY_MILLIS = 1000L
        val SHOW_FAKE_FEATURE_MODULE_DOWNLOAD = BuildConfig.DEBUG

        // Wait until we've download 5% before show a determinate progress
        const val MIN_PROGRESS_DETERMINATE_PERC = 0.05f

        private const val EXTRA_DYNAMIC_FEATURE_NAME_ID = "extra_dynamic_feature_name_id"
        private const val EXTRA_SPLASH_IMAGE_ID = "extra_splash_image_id"
        private const val EXTRA_SPLASH_IMAGE_URL = "extra_splash_image_url"
        private const val EXTRA_SPLASH_TITLE_ID = "extra_splash_title_id"
        private const val EXTRA_GAME_INTENT = "extra_game_intent"
        private const val EXTRA_LANDSCAPE = "extra_landscape"
        private const val EXTRA_IMAGE_TRANSITION_NAME = "extra_image_transition_name"
        private const val EXTRA_SPLASH_SCREEN_COLOR_ID = "extra_splash_screen_color"
        private const val EXTRA_CLASS_NAME = "extra_class_name"

        private const val DYNAMIC_FEATURE_NAME_DEFAULT_ID = -1

        /**
         * Get an intent to launch a splash screen.
         *
         * @param activity launching activity.
         * @param splashImageId resource ID for splash image.
         * @param splashTitleId resource ID for splash title.
         * @param dynamicFeatureNameId resource ID for dynamic feature.
         * @param splashScreenColorId resource ID for the background color for the splash screen
         * @param isLandscape `true` if the game target is landscape only.
         * @param packageName Package name of the game to launch.
         * @param className Class name of the game to launch.
         */
        @JvmStatic
        @JvmOverloads
        fun getIntent(
            activity: Activity,
            @DrawableRes splashImageId: Int,
            @StringRes splashTitleId: Int,
            @StringRes dynamicFeatureNameId: Int,
            @ColorRes splashScreenColorId: Int,
            isLandscape: Boolean,
            transitionName: String?,
            sharedImageView: ImageView?,
            packageName: String,
            className: String,
            splashImageUrl: String? = null
        ): Intent {
            val intent = Intent()
            intent.setClassName(packageName, className)
            return getIntent(
                    activity,
                    splashImageId,
                    splashTitleId,
                    dynamicFeatureNameId,
                    splashScreenColorId,
                    isLandscape,
                    transitionName,
                    sharedImageView,
                    intent,
                    splashImageUrl,
                    className
            )
        }

        @JvmStatic
        fun getIntent(
            activity: Activity,
            @DrawableRes splashImageId: Int,
            @StringRes splashTitleId: Int,
            @ColorRes splashScreenColorId: Int,
            isLandscape: Boolean,
            transitionName: String?,
            sharedImageView: ImageView?,
            launchIntent: Intent,
            splashImageUrl: String? = null
        ): Intent {
            return getIntent(
                    activity,
                    splashImageId,
                    splashTitleId,
                    DYNAMIC_FEATURE_NAME_DEFAULT_ID,
                    splashScreenColorId,
                    isLandscape,
                    transitionName,
                    sharedImageView,
                    launchIntent,
                    splashImageUrl
            )
        }

        /**
         * Get an intent to launch a splash screen.
         *
         * @param activity launching Activity.
         * @param splashImageId resource ID for splash image.
         * @param splashTitleId resource ID for splash title.
         * @param dynamicFeatureNameId resource ID for dynamic feature.
         * @param splashScreenColorId resource ID for the background color for the splash screen
         * @param isLandscape `true` if the game target is landscape only.
         * @param launchIntent Intent to launch.
         */
        private fun getIntent(
            activity: Activity,
            @DrawableRes splashImageId: Int,
            @StringRes splashTitleId: Int,
            @StringRes dynamicFeatureNameId: Int,
            @ColorRes splashScreenColorId: Int,
            isLandscape: Boolean,
            transitionName: String?,
            sharedImageView: ImageView?,
            launchIntent: Intent,
            splashImageUrl: String?,
            className: String? = null
        ): Intent {
            return Intent(activity, SplashActivity::class.java).apply {
                putExtra(EXTRA_SPLASH_IMAGE_ID, splashImageId)
                putExtra(EXTRA_SPLASH_IMAGE_URL, splashImageUrl)
                putExtra(EXTRA_SPLASH_TITLE_ID, splashTitleId)
                if (dynamicFeatureNameId != DYNAMIC_FEATURE_NAME_DEFAULT_ID) {
                    putExtra(EXTRA_DYNAMIC_FEATURE_NAME_ID, dynamicFeatureNameId)
                }
                putExtra(EXTRA_LANDSCAPE, isLandscape)
                putExtra(EXTRA_GAME_INTENT, launchIntent)
                putExtra(EXTRA_SPLASH_SCREEN_COLOR_ID, splashScreenColorId)
                putExtra(EXTRA_IMAGE_TRANSITION_NAME,
                        sharedImageView?.transitionName ?: transitionName)
                if (className != null) {
                    putExtra(EXTRA_CLASS_NAME, className)
                }
            }
        }
    }
}
