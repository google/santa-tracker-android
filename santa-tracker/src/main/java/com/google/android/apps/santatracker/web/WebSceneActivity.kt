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
package com.google.android.apps.santatracker.web

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.WindowManager
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.os.postDelayed
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.apps.santatracker.BuildConfig
import com.google.android.apps.santatracker.R
import com.google.android.apps.santatracker.common.CheckableImageButton
import com.google.android.apps.santatracker.customviews.WebPauseView
import com.google.android.apps.santatracker.data.SantaPreferences
import com.google.android.apps.santatracker.games.EndOfGameView
import com.google.android.apps.santatracker.games.LoadingSceneView
import com.google.android.apps.santatracker.games.ScoreTitleBar
import com.google.android.apps.santatracker.tracker.api.LocaleMapper
import com.google.android.apps.santatracker.tracker.di.Injectable
import com.google.android.apps.santatracker.tracker.util.Utils
import com.google.android.apps.santatracker.util.MeasurementManager
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import org.json.JSONObject
import java.util.concurrent.Executor
import javax.inject.Inject

@SuppressLint("Registered")
@RequiresApi(Build.VERSION_CODES.M)
abstract class WebSceneActivity : AppCompatActivity(), Injectable {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var endofgameScreen: EndOfGameView
    private lateinit var loadingScreen: LoadingSceneView
    private lateinit var pauseScreen: WebPauseView
    private lateinit var scoreBar: ScoreTitleBar
    private lateinit var pauseButton: ImageButton

    private lateinit var overlayMuteButton: CheckableImageButton
    private lateinit var scorebarMuteButton: CheckableImageButton

    private lateinit var webView: WebView
    private var webPort: WebMessagePort? = null

    enum class SceneState {
        NOT_LOADED, LOADED, RESUMED, PAUSED, GAME_OVER
    }

    private var sceneState = SceneState.NOT_LOADED
    private var sceneCanPause = false

    private lateinit var santaPreferences: SantaPreferences
    @Inject lateinit var executor: Executor

    private var pendingShareObject: JSONObject? = null

    private val handler = Handler()

    companion object {
        private const val TAG = "WebSceneActivity"
        const val ARGUMENT_URL = "ARGUMENT_URL"

        const val SEND_MSG_RESUME = "resumeGame"
        const val SEND_MSG_PAUSE = "pauseGame"
        const val SEND_MSG_RESTART = "restartGame"
        const val SEND_MESSAGE_MUTE = "mute"
        const val SEND_MESSAGE_UNMUTE = "unmute"

        const val RCV_MSG_TYPE_ON_GAME_OVER = "ongameover"
        const val RCV_MSG_TYPE_ON_READY = "onready"
        const val RCV_MSG_TYPE_ON_FAILURE = "onfailure"
        const val RCV_MSG_TYPE_ON_SCORE = "onscore"
        const val MSG_TYPE_ON_SHARE = "onshare"

        // Remove these once all of the scenes are migrated
        const val RCV_MSG_TYPE_ON_GOTO_VILLAGE = "ongotovillage"
        const val RCV_MSG_TYPE_ON_START = "onstart"

        const val PERMISSION_REQUEST_CODE = 5

        /** List of domains (schema + domain) against which the web games may access */
        private val ALLOWED_DOMAINS = listOf(
                "https://santa-staging.firebaseapp.com",
                "https://fonts.googleapis.com",
                "https://fonts.gstatic.com",
                "https://maps.gstatic.com",
                "https://next-santa-api.appspot.com",
                "https://santa-api.appspot.com",
                "https://santa-staging.appspot.com",
                "https://santatracker.google.com",
                "https://www.google-analytics.com",
                "https://www.gstatic.com",
                "https://www.googleapis.com"
        )

        const val CHANNEL_INIT_STRING = "santaandroid"

        fun intent(context: Context, isLandscape: Boolean, url: String): Intent {
            val intent: Intent = if (isLandscape) {
                Intent(context, WebSceneActivityLandscape::class.java)
            } else {
                Intent(context, WebSceneActivityPortrait::class.java)
            }
            intent.putExtra(ARGUMENT_URL, url)
            return intent
        }
    }

    // Listener for buttons on the pause screen.
    private val pauseListener: WebPauseView.Listener = object : WebPauseView.Listener {
        override fun onGoToVillageClick() {
            // Go to village
            finish()
        }

        override fun onPlayAgainClick() {
            restartGame()
        }

        override fun onResumeClick() {
            // Notify the web scene to resume the game.
            resumeGame()
        }
    }

    private val webClient: WebViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            initChannel()
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)

            // Ignore all requests that are not for HTML files
            if (!request?.url.toString().contains("html", ignoreCase = true)) {
                return
            }
            // App Measurement
            recordErrorEvent("HttpError: " + errorResponse?.statusCode.toString())

            SantaLog.e(TAG, "HttpError: " + errorResponse?.statusCode)
            finishWithError()
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)

            recordErrorEvent(error?.errorCode.toString())

            SantaLog.e(TAG, "Web error: " + error?.description)
            finishWithError()
        }

        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?):
                WebResourceResponse? {
            val url = request?.url ?: return super.shouldInterceptRequest(view, request)
            val domain = url.scheme + "://" + url.host
            return if (!isDomainWhitelisted(domain)) {
                WebResourceResponse("text/plain", "UTF-8",
                        403, "Resource not whitelisted", null, null)
            } else {
                super.shouldInterceptRequest(view, request)
            }
        }
    }

    private fun isDomainWhitelisted(domain: String): Boolean {
        return ALLOWED_DOMAINS.any { domain == it }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_webscene)

        santaPreferences = SantaPreferences(this)

        var url = extractUrl(intent)
        if (url.isNullOrBlank()) {
            recordErrorEvent("Missing URL")
            finishWithError()
        }

        // Urls could potentially be long, use only the name and parameters for screen view events
        val screenName = url.substringAfterLast('/')
        // Initialize measurement
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        MeasurementManager.recordScreenView(firebaseAnalytics,
                getString(R.string.analytics_screen_webscene, screenName))

        /*
        Replace the language parameter in the URL ($lang) with the system locale.
        Note that this is skipped if "$lang" is not present in the url. For example - in production
        we rely on the "Accept-Language" request header that is set by the system on the WebView and
        don't need to supply a language parameter in the URL.
         */
        val language =
                LocaleMapper().toServerLanguage(
                        Utils.extractLocale(LocaleListCompat.getAdjustedDefault().toLanguageTags()))
        url = replaceLanguage(url, language)

        loadingScreen = findViewById(R.id.view_loading_game)

        endofgameScreen = findViewById(R.id.view_endgame)

        pauseScreen = findViewById(R.id.view_pause)
        pauseScreen.clickListener = pauseListener

        pauseButton = findViewById(R.id.pause_button)
        pauseButton.setOnClickListener {
            pauseGame()
        }

        scorebarMuteButton = findViewById(R.id.scorebar_mute_button)
        scorebarMuteButton.isChecked = !santaPreferences.isMuted
        scorebarMuteButton.setOnClickListener {
            onMuteButtonClick()
        }

        overlayMuteButton = findViewById(R.id.overlay_mute_button)
        overlayMuteButton.isChecked = !santaPreferences.isMuted
        overlayMuteButton.setOnClickListener {
            onMuteButtonClick()
        }

        val root: ConstraintLayout = findViewById(R.id.scene_root)
        scoreBar = ScoreTitleBar(root)

        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true

        // Only enable web view debugging on debug builds
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        webView.webViewClient = webClient

        webView.loadUrl(url)

        // We'll give the scene 20 seconds to start running, if not we clear the cache and
        // reload
        handler.postDelayed(20 * 1000) {
            if (sceneState == SceneState.NOT_LOADED && !isFinishing) {
                webView.stopLoading()
                webView.clearCache(true)
                webView.loadUrl(url)
            }
        }

        updateUiForState()
    }

    private fun initChannel() {
        val channel = webView.createWebMessageChannel()
        if (channel.isEmpty()) {
            SantaLog.e(TAG, "Could not establish channel.")
            recordErrorEvent("No Channel")
            finishWithError()
        }

        webPort = channel[0]
        webPort?.setWebMessageCallback(object : WebMessagePort.WebMessageCallback() {
            override fun onMessage(port: WebMessagePort, message: WebMessage) {
                SantaLog.d(TAG, message.data)
                handleMessage(message.data)
            }
        })

        // Send empty message to establish connection
        webView.postWebMessage(WebMessage(CHANNEL_INIT_STRING, arrayOf(channel[1])), Uri.EMPTY)
    }

    private fun handleMessage(data: String?) {
        SantaLog.d(TAG, "handleMessage: $data")

        if (data == null) {
            return
        }
        val json = JSONObject(data)
        val type = json.getString("type")

        when (type) {
            RCV_MSG_TYPE_ON_GAME_OVER -> onWebGameOver(json)
            RCV_MSG_TYPE_ON_READY -> onWebLoaded(json)
            RCV_MSG_TYPE_ON_FAILURE -> onWebError()
            RCV_MSG_TYPE_ON_SCORE -> onWebScore(json)
            MSG_TYPE_ON_SHARE -> onWebShare(json)

            // TODO remove these states when all of the scenes are updated
            RCV_MSG_TYPE_ON_START -> onWebStart()
            RCV_MSG_TYPE_ON_GOTO_VILLAGE -> onWebVillage()
        }

        if (type != RCV_MSG_TYPE_ON_FAILURE) {
            recordEvent(type)
        }
    }

    private fun onWebGameOver(json: JSONObject?) {
        if (json == null || !json.has("data")) {
            return
        }
        val jsonData = json.getJSONObject("data")

        var score = -1
        if (jsonData.has("score")) {
            score = jsonData.getInt("score")
        }

        sceneState = SceneState.GAME_OVER
        updateUiForState()

        showGameOver(score)
    }

    private fun onWebScore(json: JSONObject?) {
        if (json == null || !json.has("data")) {
            return
        }

        val jsonData = json.getJSONObject("data")
        scoreBar.setUi(
                jsonData.optInt("score", -1),
                jsonData.optInt("time", -1),
                jsonData.optInt("level", -1),
                jsonData.optInt("maxLevel", -1)
        )
    }

    /**
     * TODO: remove this when the state is removed
     */
    private fun onWebVillage() {
        finish()
    }

    /**
     * TODO: remove this when the state is removed
     */
    private fun onWebStart() {
        resumeGame()
    }

    private fun onWebLoaded(json: JSONObject?) {
        sceneState = SceneState.LOADED
        sceneCanPause = false

        // Extract the initial state from the 'data' property. All properties are optional.
        if (json != null && json.has("data")) {
            val jsonData = json.getJSONObject("data")
            // Set the score, time and pause screen if the value has been specified in JSON

            scoreBar.setUi(
                    jsonData.optInt("score", -1),
                    jsonData.optInt("time", -1),
                    jsonData.optInt("level", -1),
                    jsonData.optInt("maxLevel", -1)
            )

            sceneCanPause = jsonData.optBoolean("hasPauseScreen")

            // Scenes that do not have a pause screen are always running.
            // Legacy scenes set this property by sending the 'onresume' message.
        }

        dispatchSoundState()

        updateUiForState()

        // Tell the game to resume to start
        resumeGame()
    }

    private fun onWebShare(json: JSONObject?) {
        if (json != null) share(json)
    }

    private fun requestExternalStoragePermission() {
        ActivityCompat.requestPermissions(
                this,
                arrayOf(WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val externalWriteIndex = permissions.indexOf(WRITE_EXTERNAL_STORAGE)
            if (externalWriteIndex >= 0 && pendingShareObject != null) {
                if (grantResults[externalWriteIndex] == PackageManager.PERMISSION_GRANTED) {
                    share(pendingShareObject!!)
                } else {
                    toast(R.string.elf_maker_write_fail, Toast.LENGTH_SHORT)
                }
            }
        }
    }

    private fun toast(stringRes: Int, length: Int) {
        Toast.makeText(this, stringRes, length).show()
    }

    private fun share(json: JSONObject) {
        val data = json.optJSONObject("data")
        val imageBase64 = data?.optString("image")

        if (imageBase64.isNullOrEmpty()) {
            return
        }

        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            pendingShareObject = json
            requestExternalStoragePermission()
            return
        }

        executor.execute {
            try {
                val bitmap = Base64.decode(imageBase64, Base64.DEFAULT).let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }

                if (bitmap != null) {
                    val url = MediaStore.Images.Media.insertImage(
                            contentResolver,
                            bitmap,
                            "",
                            ""
                    )
                    runOnUiThread {
                        if (url != null) {
                            Snackbar.make(webView, R.string.elf_maker_write_success,
                                    Snackbar.LENGTH_LONG)
                                    .show()
                        } else {
                            toast(R.string.elf_maker_write_fail, Toast.LENGTH_SHORT)
                        }
                    }
                }
            } catch (t: Throwable) {
                SantaLog.e(TAG, "Could not share image from web scene", t)
            }
        }
    }

    private fun onWebError() {
        // App Measurement
        MeasurementManager.recordCustomEvent(firebaseAnalytics,
                getString(com.google.android.apps.santatracker.common.R.string.analytics_event_category_webscene),
                getString(com.google.android.apps.santatracker.common.R.string.analytics_event_webscene_error),
                "weberror")

        finishWithError()
    }

    private fun updateUiForState() {
        when (sceneState) {
            SceneState.PAUSED -> {
                pauseButton.isGone = true
                showPauseScreen(true)
            }
            SceneState.RESUMED -> {
                showPauseScreen(false)
                pauseButton.isVisible = sceneCanPause
                endofgameScreen.isGone = true
            }
            SceneState.LOADED -> {
                loadingScreen.isGone = true
            }
            SceneState.NOT_LOADED -> {
                loadingScreen.isVisible = true

                endofgameScreen.isGone = true
                pauseScreen.isGone = true
                pauseButton.isGone = true
            }
            SceneState.GAME_OVER -> {
                pauseScreen.isGone = true
            }
        }
    }

    private fun recordEvent(event: String) {
        // App Measurement
        MeasurementManager.recordCustomEvent(firebaseAnalytics,
                getString(com.google.android.apps.santatracker.common.R.string.analytics_event_category_webscene),
                getString(com.google.android.apps.santatracker.common.R.string.analytics_event_webscene_onaction),
                event)
    }

    private fun recordErrorEvent(error: String) {
        // App Measurement
        MeasurementManager.recordCustomEvent(firebaseAnalytics,
                getString(com.google.android.apps.santatracker.common.R.string.analytics_event_category_webscene),
                getString(com.google.android.apps.santatracker.common.R.string.analytics_event_webscene_error),
                error)
    }

    private fun finishWithError() {
        Toast.makeText(this@WebSceneActivity, R.string.web_scene_error,
                Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun replaceLanguage(url: String, language: String) = if (language == "en") {
        url.replace("/intl/\$lang_ALL", "")
    } else {
        url.replace("\$lang", language)
    }

    private fun extractUrl(intent: Intent) = intent.getStringExtra(ARGUMENT_URL)

    override fun onBackPressed() {
        if (sceneCanPause && sceneState == SceneState.RESUMED) {
            // If Game is running and can pause, do so
            pauseGame()
        } else {
            // Otherwise just exit
            super.onBackPressed()
        }
    }

    private fun resumeGame() {
        postMessage(SEND_MSG_RESUME)

        sceneState = SceneState.RESUMED
        updateUiForState()
    }

    private fun pauseGame() {
        postMessage(SEND_MSG_PAUSE)

        sceneState = SceneState.PAUSED
        updateUiForState()
    }

    private fun restartGame() {
        postMessage(SEND_MSG_RESTART)

        sceneState = SceneState.RESUMED
        updateUiForState()
    }

    private fun postMessage(message: String) {
        webPort?.postMessage(WebMessage(message))
    }

    fun dispatchSoundState() {
        if (santaPreferences.isMuted) {
            postMessage(SEND_MESSAGE_MUTE)
        } else {
            postMessage(SEND_MESSAGE_UNMUTE)
        }
    }

    private fun showPauseScreen(showPauseScreen: Boolean) {
        pauseScreen.isVisible = sceneCanPause && showPauseScreen
    }

    private fun showGameOver(score: Int) {
        endofgameScreen.initialize(score, { restartGame() }, { finish() })
        endofgameScreen.visibility = View.VISIBLE
    }

    private fun onMuteButtonClick() {
        santaPreferences.toggleMuted()
        overlayMuteButton.isChecked = !santaPreferences.isMuted
        scorebarMuteButton.isChecked = !santaPreferences.isMuted
        dispatchSoundState()
    }

    override fun onStart() {
        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
class WebSceneActivityLandscape : WebSceneActivity()

@RequiresApi(Build.VERSION_CODES.M)
class WebSceneActivityPortrait : WebSceneActivity()
