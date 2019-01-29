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

package com.google.android.apps.santatracker.rocketsleigh

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Vibrator
import android.util.DisplayMetrics
import android.util.Pair
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import com.google.android.apps.santatracker.AudioConstants
import com.google.android.apps.santatracker.common.CheckableImageButton
import com.google.android.apps.santatracker.data.SantaPreferences
import com.google.android.apps.santatracker.games.OnDemandActivity
import com.google.android.apps.santatracker.invites.AppInvitesFragment
import com.google.android.apps.santatracker.util.ImmersiveModeHelper
import com.google.android.apps.santatracker.util.MeasurementManager
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.apps.santatracker.util.play
import com.google.firebase.analytics.FirebaseAnalytics
import java.text.NumberFormat
import java.util.Random
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import com.google.android.apps.santatracker.R as appR

class RocketSleighActivity : OnDemandActivity() {

    private val executor = Executors.newFixedThreadPool(4)
    private lateinit var bitmapCache: BitmapCache
    private lateinit var viewPool: ViewPool

    private var levels: List<Level>? = null

    private lateinit var elfImage: ImageView
    private lateinit var thrustImage: ImageView
    private lateinit var elfLayout: LinearLayout
    private lateinit var plus100Image: ImageView
    private lateinit var plus500Image: ImageView
    private lateinit var obstacleLayout: LinearLayout
    private lateinit var obstacleScroll: GameScrollLayout
    private lateinit var backgroundLayout: LinearLayout
    private lateinit var backgroundScroll: GameScrollLayout
    private lateinit var foregroundLayout: LinearLayout
    private lateinit var foregroundScroll: GameScrollLayout
    private lateinit var scoreText: TextView
    private lateinit var playPauseButton: ImageView
    private lateinit var controlView: View
    private var countdownView: TextView? = null
    private lateinit var bigPlayButtonLayout: View
    private lateinit var bigPlayButton: ImageButton
    private lateinit var exitView: ImageView
    private lateinit var introVideo: VideoView
    private lateinit var introControl: View
    private lateinit var muteButton: CheckableImageButton

    private var plus100Anim = AlphaAnimation(1.0f, 0.0f).apply {
        duration = 1000
        fillBefore = true
        fillAfter = true
    }
    private var plus500Anim = AlphaAnimation(1.0f, 0.0f).apply {
        duration = 1000
        fillBefore = true
        fillAfter = true
    }

    private var elfBitmap: Bitmap? = null
    private var burnBitmap: Bitmap? = null
    private var thrustBitmap: Bitmap? = null
    private var smokeBitmap: Bitmap? = null
    private var currentTrailBitmap: Bitmap? = null

    /** 0 - 100% ... 3 - 25% 4 - Parachute elf. */
    private var elfState = 0
    private var elfIsHit = false
    private var elfHitTime: Long = 0
    /** Pixels from the left edge */
    private var elfPosX = 100f
    /** Pixels from the top edge */
    private var elfPosY = 200f
    /** Horizontal speed. */
    private var elfVelX = 0.3f
    /** Vertical speed. */
    private var elfVelY = 0.0f
    /** Acceleration due to thrust if user is touching screen. */
    private var elfAccelY = 0.0f

    /** Vertical acceleration in pixel velocity per second of thrust. */
    private var thrustAccelY: Float = 0.toFloat()
    /** Vertical acceleration due to gravity in pixel velocity per second. */
    private var gravityAccelY: Float = 0.toFloat()
    private var lastTime: Long = 0
    private var elfScale: Float = 0.toFloat()

    private var countDownTimer: CountDownTimer? = null

    private lateinit var santaPreferences: SantaPreferences

    // Achievements
    private val gameRecord = object {
        var hit = false
        var hitLevel = false
        var clearLevel = false
        var presentBonus = false
    }

    private var mRainingPresents = false

    private var slotWidth: Int = 0
    /** This is the width of an ornament "slot".  Obstacles can span multiple slots. */
    private var random = Random(System.currentTimeMillis())

    private var scaleY = 1.0f
    private var scaleX = 1.0f

    private var scoreLabel: String? = null

    private var screenHeight = 0
    private var screenWidth = 0
    private var finishBackgroundWidth = 0

    private var isTv = true
    private var isPlaying = false
    private var moviePlaying = false
    private var countdownStarted = false
    /** There are six levels. */
    private var levelIndex = 0
    private var score: Long = 0
    /** 5 in a row gets a bonus... */
    private var presentCount = 0
    /** 5 copies of backgrounds per level */
    private var backgroundCount = 0
    /** Some level transitions have transition images. */
    private var transitionImagesCount = 0

    private var mLastFrameTime: Long = 0

    private lateinit var vibrator: Vibrator

    private var handler: Handler? = null

    private var bgmPlayer: MediaPlayer? = null

    // For sound effects
    private var soundPool: SoundPool? = null
    private val sounds = object {
        var crash1 = 0
        var crash2 = 0
        var crash3 = 0
        var gameOver = 0
        var jetThrust = 0
        var levelUp = 0
        var scoreBig = 0
        var scoreSmall = 0
        var jetThrustStream = 0
    }

    private lateinit var measurement: FirebaseAnalytics
    private var invitesFragment: AppInvitesFragment? = null

    private var woodsObstacleIndex = AtomicInteger(0)
    private var caveObstacleIndex = AtomicInteger(0)
    private var factoryObstacleIndex = AtomicInteger(0)

    private val gameLoop = Runnable { processFrame() }

    private var lastObstacle = 0

    private val onClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.play_pause_button -> if (isPlaying) {
                pause()
            } else {
                play()
            }
            R.id.big_play_button -> if (!isPlaying) {
                bigPlayButtonLayout.visibility = View.GONE
                doCountdown()
            }
            R.id.exit -> finish()
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SantaLog.d(TAG, "onCreate() : $savedInstanceState")

        setContentView(R.layout.activity_jet_pack_elf)

        santaPreferences = SantaPreferences(this)

        // App Invites
        invitesFragment = AppInvitesFragment.getInstance(this)

        // App Measurement
        measurement = FirebaseAnalytics.getInstance(this)
        MeasurementManager.recordScreenView(
                measurement, getString(R.string.analytics_screen_rocket))

        ImmersiveModeHelper.setImmersiveSticky(window)
        ImmersiveModeHelper.installSystemUiVisibilityChangeListener(window)

        introVideo = findViewById(R.id.intro_view)
        introControl = findViewById(R.id.intro_control_view)

        if (savedInstanceState == null) {
            if (!santaPreferences.isMuted) {
                playBackgroundMusic()
            }

            var nomovie = false
            if (intent.getBooleanExtra("nomovie", false)) {
                nomovie = true
            }
            if (!nomovie) {
                introControl.setOnClickListener { endIntro() }
                val path = "android.resource://$packageName/${R.raw.intro_wipe}"
                introVideo.setVideoURI(path.toUri())
                introVideo.setOnPreparedListener {
                    if (santaPreferences.isMuted) {
                        it.setVolume(0f, 0f)
                    }
                }
                introVideo.setOnCompletionListener {
                    endIntro()
                }
                introVideo.setOnErrorListener { _, what, extra ->
                    endIntro()
                    // TODO: Add a custom error here when we add crashlytics
                    true
                }
                introVideo.start()
                moviePlaying = true
            } else {
                introControl.setOnClickListener(null)
                introControl.visibility = View.GONE
                introVideo.visibility = View.GONE
            }
        } else {
            introControl.setOnClickListener(null)
            introControl.visibility = View.GONE
            introVideo.visibility = View.GONE
        }

        // For hit indication.
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        handler = Handler() // Get the main UI handler for posting update events

        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)

        screenHeight = dm.heightPixels
        screenWidth = dm.widthPixels
        slotWidth = screenWidth / SLOTS_PER_SCREEN

        // Setup the background/foreground
        backgroundLayout = findViewById(R.id.background_layout)
        backgroundScroll = findViewById(R.id.background_scroll)
        foregroundLayout = findViewById(R.id.foreground_layout)
        foregroundScroll = findViewById(R.id.foreground_scroll)

        // Need to vertically scale background to fit the screen. Check the image size
        // compared to screen size and scale appropriately.  We will also use the matrix to
        // translate as we move through the level.
        val bmp = AppCompatResources.getDrawable(this, R.drawable.bg_jet_pack_1)!!
        scaleY = dm.heightPixels.toFloat() / bmp.intrinsicHeight.toFloat()
        // Ensure that a single bitmap is 2 screens worth of time.  (Stock xxhdpi image is
        // 3840x1080)
        scaleX = (dm.widthPixels * 2).toFloat() / bmp.intrinsicWidth.toFloat()

        bitmapCache = BitmapCache(this, executor, scaleX, scaleY)
        bitmapCache.preload(R.drawable.bg_jet_pack_1, splitSecondary = true)

        viewPool = ViewPool(this)

        // Load the initial background view
        levels = RocketSleigh.generateLevels(random)
        addNextImages(0, false)
        addNextImages(0, false)

        // We need the bitmaps, so we do pre-load here synchronously.
        preloadObstacles(0, levels!![0].obstacles)

        woodsObstacleIndex.set(0)
        caveObstacleIndex.set(0)
        factoryObstacleIndex.set(0)

        // Setup the elf
        elfImage = findViewById(R.id.elf_image)
        thrustImage = findViewById(R.id.thrust_image)
        elfLayout = findViewById(R.id.elf_container)
        loadElfImages()
        updateElf(false)
        // Elf should be the same height relative to the height of the screen on any platform.
        elfScale = dm.heightPixels.toFloat() * 0.123f / elfBitmap!!.height.toFloat()

        elfPosX = (dm.widthPixels * 15 / 100).toFloat() // 15% Into the screen
        elfPosY = (dm.heightPixels - elfBitmap!!.height.toFloat() * elfScale) / 2 // About 1/2 way down.
        elfVelX = dm.widthPixels.toFloat() / 3000.0f // We start at 3 seconds for a full screen to scroll.
        gravityAccelY = (2 * dm.heightPixels).toFloat() / Math.pow(
                1.2 * 1000.0,
                2.0).toFloat() // a = 2*d/t^2 Where d = height in pixels and t = 1.2
        // seconds
        thrustAccelY = (2 * dm.heightPixels).toFloat() / Math.pow(
                0.7 * 1000.0,
                2.0).toFloat() // a = 2*d/t^2 Where d = height in pixels and t = 0.7
        // seconds

        // Setup the control view
        controlView = findViewById(R.id.control_view)

        scoreLabel = getString(com.google.android.apps.santatracker.common.R.string.score)
        scoreText = findViewById(R.id.score_text)
        scoreText.text = "0"

        playPauseButton = findViewById(R.id.play_pause_button)
        exitView = findViewById(R.id.exit)

        muteButton = findViewById(R.id.mute_button)
        muteButton.isChecked = !santaPreferences.isMuted
        muteButton.setOnClickListener {
            // Toggle the state
            santaPreferences.toggleMuted()
            // Now update the vie
            muteButton.isChecked = !santaPreferences.isMuted
            onMuteChanged(santaPreferences.isMuted)
        }

        // Is Tv?
        isTv = TvUtil.isTv(this)
        if (isTv) {
            scoreText.text = scoreLabel!! + ": 0"
            playPauseButton.visibility = View.GONE
            exitView.visibility = View.GONE
            // move scoreLayout position to the Top-Right corner.
            val scoreLayout = findViewById<View>(R.id.score_layout)
            val params = scoreLayout.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.TOP or Gravity.LEFT

            val marginTop = resources.getDimensionPixelOffset(R.dimen.overscan_margin_top)
            val marginLeft = resources.getDimensionPixelOffset(R.dimen.overscan_margin_left)

            params.setMargins(marginLeft, marginTop, 0, 0)
            scoreLayout.layoutParams = params
            scoreLayout.background = null
            scoreLayout.findViewById<View>(R.id.score_text_seperator).visibility = View.GONE
        } else {
            playPauseButton.isEnabled = false
            playPauseButton.setOnClickListener(onClickListener)
            exitView.setOnClickListener(onClickListener)
        }

        bigPlayButtonLayout = findViewById(R.id.big_play_button_layout)
        bigPlayButtonLayout.setOnTouchListener { _, _ ->
            // No interaction with the screen below this one.
            true
        }
        bigPlayButton = findViewById(R.id.big_play_button)
        bigPlayButton.setOnClickListener(onClickListener)

        // For showing points when getting gifts.
        plus100Image = findViewById(R.id.plus_100)
        plus500Image = findViewById(R.id.plus_500)

        // Get the obstacle layouts ready.  No obstacles on the first screen of a level.
        // Prime with a screen full of obstacles.
        obstacleLayout = findViewById(R.id.obstacles_layout)
        obstacleScroll = findViewById(R.id.obstacles_scroll)

        // Initialize the gift bitmaps.  These are used repeatedly so we keep them loaded.
        for (drawableId in RocketSleigh.giftBoxes) {
            bitmapCache.preload(drawableId)
        }

        // Add starting obstacles.  First screen has presents.  Next 3 get obstacles.
        addFirstScreenPresents()
        addNextObstacles(0, 3)

        // Setup the sound pool
        soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(
                        AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .build())
                .build()
                .also { pool ->
                    sounds.crash1 = pool.load(this, R.raw.jp_crash_1, 1)
                    sounds.crash2 = pool.load(this, R.raw.jp_crash_2, 1)
                    sounds.crash3 = pool.load(this, R.raw.jp_crash_3, 1)
                    sounds.gameOver = pool.load(this, R.raw.jp_game_over, 1)
                    sounds.jetThrust = pool.load(this, R.raw.jp_jet_thrust, 1)
                    sounds.levelUp = pool.load(this, R.raw.jp_level_up, 1)
                    sounds.scoreBig = pool.load(this, R.raw.jp_score_big, 1)
                    sounds.scoreSmall = pool.load(this, R.raw.jp_score_small, 1)
                    sounds.jetThrustStream = 0
                }

        if (!moviePlaying) {
            doCountdown()
        }
    }

    private fun stopBackgroundMusic() {
        bgmPlayer?.run {
            stop()
            release()
        }
        bgmPlayer = null
    }

    private fun playBackgroundMusic() {
        if (bgmPlayer?.isPlaying == true) {
            return
        }
        bgmPlayer = MediaPlayer().apply {
            val path = "android.resource://$packageName/${appR.raw.santatracker_musicloop}"
            setDataSource(this@RocketSleighActivity, path.toUri())
            setVolume(AudioConstants.DEFAULT_BACKGROUND_VOLUME,
                    AudioConstants.DEFAULT_BACKGROUND_VOLUME)
            isLooping = true
            prepare()
            start()
        }
    }

    private fun onMuteChanged(isMuted: Boolean) {
        if (isMuted) {
            stopBackgroundMusic()
            soundPool?.autoPause()
        } else {
            playBackgroundMusic()
            soundPool?.autoResume()
        }
    }

    public override fun onPause() {
        if (moviePlaying) {
            // We are only here if home or lock is pressed or another app (phone)
            // interrupts.  We just go to the pause screen and start the game when
            // we come back.
            introVideo.stopPlayback()
            introVideo.visibility = View.GONE
            introControl.setOnClickListener(null)
            introControl.visibility = View.GONE
            moviePlaying = false
            isPlaying = true // this will make pause() show the pause button.
        } else if (countdownStarted) {
            countdownView?.visibility = View.GONE
            countDownTimer?.cancel()
            countdownStarted = false
            isPlaying = true // this will make pause() show the pause button.
        }
        pause()

        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        invitesFragment?.getInvite(object : AppInvitesFragment.GetInvitationCallback {
            override fun onInvitation(invitationId: String, deepLink: String) {
                SantaLog.d(TAG, "onInvitation:$deepLink")
            }
        }, false)
    }

    public override fun onDestroy() {
        super.onDestroy()
        releaseResources()
    }

    private fun releaseResources() {
        stopBackgroundMusic()

        if (soundPool != null) {
            if (sounds.jetThrustStream > 0) {
                soundPool?.stop(sounds.jetThrustStream)
                sounds.jetThrustStream = 0
            }
            soundPool?.run {
                unload(sounds.crash1)
                unload(sounds.crash2)
                unload(sounds.crash3)
                unload(sounds.gameOver)
                unload(sounds.jetThrust)
                unload(sounds.levelUp)
                unload(sounds.scoreBig)
                unload(sounds.scoreSmall)
                release()
            }
            soundPool = null
        }

        elfImage.setImageDrawable(null)
        thrustImage.setImageDrawable(null)
        plus100Image.setImageDrawable(null)
        plus500Image.setImageDrawable(null)
        backgroundLayout.removeAllViews()
        foregroundLayout.removeAllViews()
        obstacleLayout.removeAllViews()
        bitmapCache.releaseAll()
    }

    override fun onConfigurationChanged(config: Configuration) {
        // We are eating the config changes so that we don't get destroyed/recreated and again
        // destroyed/recreated when the lock button is pressed!
        SantaLog.e(TAG, "Config change: $config")
        super.onConfigurationChanged(config)
    }

    override fun onBackPressed() {
        if (moviePlaying) {
            introVideo.stopPlayback()
            introVideo.visibility = View.GONE
            introControl.setOnClickListener(null)
            introControl.visibility = View.GONE
            moviePlaying = false
            super.onBackPressed()
        } else if (countdownStarted) {
            countDownTimer?.run { cancel() }
            countDownTimer = null
            countdownStarted = false
            super.onBackPressed()
        } else if (isPlaying) {
            pause()
        } else {
            super.onBackPressed()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_BUTTON_A -> {
                if (isPlaying) {
                    elfAccelY = thrustAccelY
                    if (!elfIsHit) {
                        updateElfThrust(1)
                    }
                    soundPool?.let {
                        if (!santaPreferences.isMuted) {
                            sounds.jetThrustStream = it.play(sounds.jetThrust, priority = 1)
                        }
                    }
                } else if (!countdownStarted && !moviePlaying) {
                    // game is paused. resume it.
                    bigPlayButton.isPressed = true
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_BUTTON_A -> {
                if (isPlaying) {
                    elfAccelY = 0.0f
                    if (!elfIsHit) {
                        updateElfThrust(0)
                    }
                    if (sounds.jetThrustStream > 0) {
                        soundPool?.stop(sounds.jetThrustStream)
                        sounds.jetThrustStream = 0
                    }
                } else if (moviePlaying) {
                    endIntro()
                } else if (bigPlayButton.isPressed) {
                    bigPlayButton.isPressed = false
                    bigPlayButton.performClick()
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_B -> {
                onBackPressed()
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun endIntro() {
        moviePlaying = false
        introControl.setOnClickListener(null)
        introControl.visibility = View.GONE
        introVideo.visibility = View.GONE
        doCountdown()
    }

    private fun processFrame() {
        val newTime = System.currentTimeMillis()
        var time = newTime - lastTime

        var end = false

        if (time > 60) {
            SantaLog.e(TAG, "Frame time took too long! Time: $time Last process frame: " +
                    "$mLastFrameTime Count: $backgroundCount Level: $levelIndex")
            // We don't want to jump too far so, if real time is > 60 treat it as 33.
            // On screen, this will seem to slow down instead of "jump".
            time = 33
        }

        // Score is based on time + presents.  Right now 100 point per second played.
        // No presents yet
        if (levelIndex < 6) {
            score += time
        }

        if (isTv) {
            scoreText.text = scoreLabel.toString() + ": " + NumberFormat.getNumberInstance().format(score / 10)
        } else {
            scoreText.text = NumberFormat.getNumberInstance().format(score / 10)
        }

        val scroll = elfVelX * time

        // Do collision detection first...
        // The elf can't collide if it is within 2 seconds of colliding previously.
        if (elfIsHit) {
            if (newTime - elfHitTime > 2000) {
                // Move to next state.
                if (elfState < 4) {
                    elfState++
                    MeasurementManager.recordCustomEvent(
                            measurement,
                            getString(R.string.analytics_screen_rocket),
                            getString(R.string.analytics_action_rocket_hit), null,
                            elfState.toLong())
                    if (elfState == 4) {
                        if (!santaPreferences.isMuted) {
                            soundPool?.play(sounds.gameOver, priority = 2)
                        }
                        // No more control...
                        controlView.setOnTouchListener(null)
                        elfAccelY = 0.0f

                        if (sounds.jetThrustStream != 0) {
                            soundPool?.stop(sounds.jetThrustStream)
                        }
                    }
                }
                updateElf(false)
                elfIsHit = false
            }
        } else if (elfState == 4) {
            // Don't do any collision detection for parachute elf.  Just let him fall...
        } else {
            // Find the obstacle(s) we might be colliding with.  It can only be one of the first 3
            // obstacles.
            for (i in 0..2) {
                // Break if no more obstacles
                val view = obstacleLayout.getChildAt(i) ?: break

                val tmp = IntArray(2)
                view.getLocationOnScreen(tmp)

                // If the start of this view is past the center of the elf, we are done
                if (tmp[0] > elfPosX) {
                    break
                }

                if (view is RelativeLayout) {
                    // this is an obstacle layout.
                    val topView = view.findViewById<View>(R.id.top_view)
                    val bottomView = view.findViewById<View>(R.id.bottom_view)
                    if (topView != null && topView.visibility == View.VISIBLE) {
                        topView.getLocationOnScreen(tmp)
                        val obsRect = Rect(tmp[0], tmp[1], tmp[0] + topView.width, tmp[1] + topView.height)
                        if (obsRect.contains(elfPosX.toInt(), elfPosY.toInt() + elfBitmap!!.height / 2)) {
                            handleCollision()
                        }
                    }
                    if (!elfIsHit) {
                        if (bottomView != null && bottomView.visibility == View.VISIBLE) {
                            bottomView.getLocationOnScreen(tmp)
                            val obsRect = Rect(tmp[0], tmp[1], tmp[0] + bottomView.width, tmp[1] + bottomView.height)
                            if (obsRect.contains(elfPosX.toInt(), elfPosY.toInt() + elfBitmap!!.height / 2)) {
                                // Special case for the mammoth obstacle...
                                if (bottomView.tag != null) {
                                    if ((elfPosX - tmp[0]) / bottomView.width.toFloat() > 0.25f) {
                                        // We are over the mammoth not the spike.  lower the top of
                                        // the rect and test again.
                                        obsRect.top = (tmp[1] + bottomView.height.toFloat() * 0.18f).toInt()
                                        if (obsRect.contains(elfPosX.toInt(), elfPosY.toInt() + elfBitmap!!.height / 2)) {
                                            handleCollision()
                                        }
                                    }
                                } else {
                                    handleCollision()
                                }
                            }
                        }
                    }
                } else if (view is FrameLayout) {
                    // Present view
                    if (view.visibility == View.VISIBLE) {
                        val presentView = view.getChildAt(0) as ImageView
                        presentView.getLocationOnScreen(tmp)
                        val presentRect = Rect(
                                tmp[0],
                                tmp[1],
                                tmp[0] + presentView.width,
                                tmp[1] + presentView.height)
                        elfLayout.getLocationOnScreen(tmp)
                        val elfRect = Rect(
                                tmp[0],
                                tmp[1],
                                tmp[0] + elfLayout.width,
                                tmp[1] + elfLayout.height)
                        if (elfRect.intersect(presentRect)) {
                            // We got a present!
                            presentCount++
                            if (presentCount < 4) {
                                if (!santaPreferences.isMuted) {
                                    soundPool?.play(sounds.scoreSmall, priority = 2,
                                            volume = AudioConstants.DEFAULT_SOUND_EFFECT_VOLUME / 2f)
                                }
                                score += 1000 // 100 points.  Score is 10x displayed score.
                                plus100Image.visibility = View.VISIBLE
                                if (elfPosY > screenHeight / 2) {
                                    plus100Image.y = elfPosY - (elfLayout.height + plus100Image.height)
                                } else {
                                    plus100Image.y = elfPosY + elfLayout.height
                                }
                                plus100Image.x = elfPosX
                                if (plus100Anim.hasStarted()) {
                                    plus100Anim.reset()
                                }
                                plus100Image.startAnimation(plus100Anim)
                            } else {
                                if (!santaPreferences.isMuted) {
                                    soundPool?.play(sounds.scoreBig, priority = 2,
                                            volume = AudioConstants.DEFAULT_SOUND_EFFECT_VOLUME / 2f)
                                }
                                score += 5000 // 500 points.  Score is 10x displayed score.
                                if (!mRainingPresents) {
                                    presentCount = 0
                                }
                                plus500Image.visibility = View.VISIBLE
                                if (elfPosY > screenHeight / 2) {
                                    plus500Image.y = elfPosY - (elfLayout.height + plus100Image.height)
                                } else {
                                    plus500Image.y = elfPosY + elfLayout.height
                                }
                                plus500Image.x = elfPosX
                                if (plus500Anim.hasStarted()) {
                                    plus500Anim.reset()
                                }
                                plus500Image.startAnimation(plus500Anim)
                                gameRecord.presentBonus = true
                            }
                            view.visibility = View.INVISIBLE
                        } else if (elfRect.left > presentRect.right) {
                            presentCount = 0
                        }
                    }
                }
            }
        }

        if (foregroundLayout.childCount > 0) {
            val currentX = foregroundScroll.scrollX
            val view = foregroundLayout.getChildAt(0)
            var newX = currentX + scroll.toInt()
            if (newX > view.width) {
                newX -= view.width
                viewPool.recycle(foregroundLayout, view)
            }
            foregroundScroll.scrollX = newX
        }

        // Scroll obstacle views
        if (obstacleLayout.childCount > 0) {
            val currentX = obstacleScroll.scrollX
            val view = obstacleLayout.getChildAt(0)
            var newX = currentX + scroll.toInt()
            if (newX > view.width) {
                newX -= view.width
                viewPool.recycle(obstacleLayout, view)
            }
            obstacleScroll.scrollX = newX
        }

        // Scroll the background and foreground
        if (backgroundLayout.childCount > 0) {
            val currentX = backgroundScroll.scrollX
            val view = backgroundLayout.getChildAt(0)
            var newX = currentX + scroll.toInt()
            if (newX > view.width) {
                newX -= view.width
                if (view is ImageView) {
                    view.setImageDrawable(null)
                }
                viewPool.recycle(backgroundLayout, view)
                if (view.tag != null) {
                    val pair = view.tag as Pair<Int, Int>
                    val type = pair.first
                    val level = pair.second
                    val (background, _, entryTransition, exitTransition) = levels!![level]
                    if (type == 0) {
                        if (!bitmapCache.release(background, false)) {
                            bitmapCache.release(background, true)
                        }
                    } else if (type == 1) {
                        bitmapCache.release(exitTransition, false)
                    } else if (type == 2) {
                        bitmapCache.release(entryTransition, false)
                    }
                }
                if (backgroundCount == 5) {
                    if (levelIndex < 6) {
                        // Pre-fetch next levels backgrounds
                        // end level uses the index 1 background...
                        val levelIndex = if (levelIndex == 5) 1 else levelIndex + 1
                        val (_, _, _, exitTransition) = levels!![this.levelIndex]
                        val (background, _, entryTransition) = levels!![levelIndex]
                        bitmapCache.preload(background, splitSecondary = true)
                        // Exit transitions are for the current level...
                        bitmapCache.preload(exitTransition)
                        bitmapCache.preload(entryTransition)

                        addNextImages(this.levelIndex, true)
                        addNextObstacles(this.levelIndex, 2)
                    }
                    // Fetch first set of obstacles if the next level changes from woods to cave or
                    // cave to factory
                    if (levelIndex == 1) {
                        // Next level will be caves.  Get bitmaps for the first 20 obstacles.
                        preloadObstacles(0, levels!![2].obstacles)
                    } else if (levelIndex == 3) {
                        // Next level will be factory.  Get bitmaps for the first 20 obstacles.
                        preloadObstacles(0, levels!![4].obstacles)
                    }
                    backgroundCount++
                } else if (backgroundCount == 7) {
                    // Add transitions and/or next level
                    if (levelIndex < 5) {
                        addNextTransitionImages(levelIndex + 1)
                        if (transitionImagesCount > 0) {
                            addNextObstacleSpacer(transitionImagesCount)
                        }
                        addNextImages(levelIndex + 1, false)
                        // First screen of each new level has no obstacles
                        if (levelIndex % 2 == 1) {
                            addNextObstacleSpacer(1)
                            addNextObstacles(levelIndex + 1, 1)
                        } else {
                            addNextObstacles(levelIndex + 1, 2)
                        }
                    } else if (levelIndex == 5) {
                        addNextTransitionImages(levelIndex + 1)
                        if (transitionImagesCount > 0) {
                            addNextObstacleSpacer(transitionImagesCount)
                        }
                        finishBackgroundWidth = addFinalImages()
                    }
                    backgroundCount++
                } else if (backgroundCount == 9) {
                    // Either the transition or the next level is showing
                    if (this.transitionImagesCount > 0) {
                        transitionImagesCount--
                    } else {
                        when (levelIndex) {
                            1 -> // Destroy the wood obstacle bitmaps
                                executor.execute {
                                    for ((top, bottom, back) in RocketSleigh.woodsObstacles) {
                                        bitmapCache.release(top, false)
                                        bitmapCache.release(bottom, false)
                                        bitmapCache.release(back, false)
                                    }
                                }
                            3 -> // Destroy the cave obstacle bitmaps
                                executor.execute {
                                    for ((top, bottom, back) in RocketSleigh.caveObstacles) {
                                        bitmapCache.release(top, false)
                                        bitmapCache.release(bottom, false)
                                        bitmapCache.release(back, false)
                                    }
                                }
                            5 -> // Destroy the factory obstacle bitmaps
                                executor.execute {
                                    for ((top, bottom, back) in RocketSleigh.factoryObstacles) {
                                        bitmapCache.release(top, false)
                                        bitmapCache.release(bottom, false)
                                        bitmapCache.release(back, false)
                                    }
                                }
                        }
                        levelIndex++

                        // Add an event for clearing this level - note we don't increment levelIndex
                        // as it's 0-based and we're tracking the previous level.
                        MeasurementManager.recordCustomEvent(
                                measurement,
                                getString(R.string.analytics_screen_rocket),
                                getString(R.string.analytics_action_rocket_level), null,
                                levelIndex.toLong())

                        // Achievements
                        if (!gameRecord.hitLevel) {
                            gameRecord.clearLevel = true
                        }
                        gameRecord.hitLevel = false
                        if (levelIndex == 5) {
                            plus100Image.isSelected = true
                            plus500Image.isSelected = true
                        } else if (levelIndex == 6) {
                            plus100Image.isSelected = false
                            plus500Image.isSelected = false
                        }
                        if (levelIndex < 6) {
                            if (!santaPreferences.isMuted) {
                                soundPool?.play(sounds.levelUp, priority = 2)
                            }
                            addNextImages(levelIndex, false)
                            addNextObstacles(levelIndex, 2)
                        }
                        backgroundCount = 0
                    }
                } else {
                    if (backgroundCount % 2 == 1) {
                        if (levelIndex < 6) {
                            addNextImages(levelIndex, false)
                            addNextObstacles(levelIndex, 2)
                        }
                    }
                    backgroundCount++
                }

                if (BuildConfig.DEBUG) {
                    SantaLog.d(TAG, "levelIndex: $levelIndex, backgroundCount: $backgroundCount")
                }
            }
            backgroundScroll.scrollX = newX
            if (levelIndex == 6 && backgroundCount == 4 && newX > finishBackgroundWidth * 0.42) {
                end = true
            }
        }

        // Check on the elf
        var hitBottom = false

        val deltaY = elfVelY * time
        elfPosY = elfLayout.y + deltaY
        when {
            elfPosY < 0.0f -> {
                elfPosY = 0.0f
                elfVelY = 0.0f
            }
            elfPosY > screenHeight - elfLayout.height -> {
                elfPosY = (screenHeight - elfLayout.height).toFloat()
                elfVelY = 0.0f
                hitBottom = true
            }
            else -> // Remember -Y is up!
                elfVelY += gravityAccelY * time - elfAccelY * time
        }
        elfLayout.y = elfPosY

        // Rotate the elf to indicate thrust, dive.
        val rot = (Math.atan((elfVelY / elfVelX).toDouble()) * 120.0 / Math.PI).toFloat()
        elfLayout.rotation = rot

        elfImage.invalidate()

        // Update the time and spawn the next call to processFrame.
        lastTime = newTime
        mLastFrameTime = System.currentTimeMillis() - newTime
        if (!end) {
            if (elfState < 4 || !hitBottom) {
                if (mLastFrameTime < 16) {
                    handler?.postDelayed(gameLoop, 16 - mLastFrameTime)
                } else {
                    handler?.post(gameLoop)
                }
            } else {
                endGame()
            }
        } else {
            // Whatever the final stuff is, do it here.
            playPauseButton.isEnabled = false
            playPauseButton.visibility = View.INVISIBLE
            endGame()
        }
    }

    private fun handleCollision() {
        // Achievements
        gameRecord.hit = true
        gameRecord.hitLevel = true

        // Collision!
        elfIsHit = true
        elfHitTime = System.currentTimeMillis()
        updateElf(true)
        vibrator.vibrate(500)

        if (!santaPreferences.isMuted) {
            when (elfState) {
                0 -> soundPool?.play(sounds.crash1, priority = 2)
                1 -> soundPool?.play(sounds.crash2, priority = 2)
                2, 3 -> soundPool?.play(sounds.crash3, priority = 2)
            }
        }
    }

    private fun doCountdown() {
        countdownStarted = true
        playPauseButton.isEnabled = false
        // Start the countdown
        if (countdownView == null) {
            countdownView = findViewById(R.id.countdown_text)
        }
        countdownView?.let { cv ->
            cv.visibility = View.VISIBLE
            cv.setTextColor(Color.WHITE)
            cv.text = "3"
            countDownTimer = object : CountDownTimer(3500, 100) {
                override fun onTick(millisUntilFinished: Long) {
                    val time = ((millisUntilFinished + 500) / 1000).toInt()
                    when (time) {
                        3 -> cv.text = "3"
                        2 -> cv.text = "2"
                        1 -> cv.text = "1"
                        0 -> cv.text = "Go!"
                    }
                }

                override fun onFinish() {
                    countdownStarted = false
                    playPauseButton.isEnabled = true
                    play()
                    cv.visibility = View.GONE
                }
            }.also {
                it.start()
            }
        }
    }
    // 0 - spacer, 1 - upper obstacle, 2 - lower obstacle.  These are flags so top + bottom is 3.

    private fun addFirstScreenPresents() {
        // First 4 slots have no nothing.
        for (i in 0 until Math.min(4, SLOTS_PER_SCREEN)) {
            val holder = viewPool.obtainSpace()
            val lp = LinearLayout.LayoutParams(slotWidth, screenHeight)
            obstacleLayout.addView(holder.space, lp)
        }

        // Generate a SIN like pattern;
        val boxHeight = bitmapCache.fetch(RocketSleigh.giftBoxes[0]).height
        val center = ((screenHeight - boxHeight) / 2).toFloat()
        val presentHeight = boxHeight.toFloat()
        val heights = floatArrayOf(
                center,
                center - presentHeight,
                center - 1.5f * presentHeight,
                center - presentHeight,
                center,
                center + presentHeight,
                center + 1.5f * presentHeight,
                center + presentHeight,
                center)
        // Add presents to the end
        val obstacleLp = LinearLayout.LayoutParams(slotWidth, LinearLayout.LayoutParams.MATCH_PARENT)
        for (i in 0 until SLOTS_PER_SCREEN - 4) {
            // Which one?
            val bmp = bitmapCache.fetch(RocketSleigh.randomGift(random))
            val holder = viewPool.obtainGift()
            holder.image.setImageBitmap(bmp)

            // Position the present
            val left = ((slotWidth - bmp.width) / 2).toFloat()
            val top = heights[i % heights.size]
            holder.image.translationX = left
            holder.image.translationY = top

            obstacleLayout.addView(holder.frame, obstacleLp)
        }

        // Account for rounding errors in slotWidth
        val extra = screenWidth - SLOTS_PER_SCREEN * slotWidth
        if (extra > 0) {
            // Add filler to ensure sync with background/foreground scrolls!
            val lp = LinearLayout.LayoutParams(extra, LinearLayout.LayoutParams.MATCH_PARENT)
            obstacleLayout.addView(viewPool.obtainSpace().space, lp)
        }

        lastObstacle = 0
    }

    private fun addFinalPresentRun() {
        // Two spacers at the beginning.
        var lp = LinearLayout.LayoutParams(slotWidth, screenHeight)
        obstacleLayout.addView(viewPool.obtainSpace().space, lp)
        obstacleLayout.addView(viewPool.obtainSpace().space, lp)

        // All of these presents are 500 points (but only if you're awesome)
        if (elfState == 0) {
            mRainingPresents = true
        }

        // SIN wave of presents in the middle
        val center = (screenHeight / 2).toFloat()
        val amplitude = (screenHeight / 4).toFloat()

        val count = 3 * SLOTS_PER_SCREEN - 4

        val box = bitmapCache.fetch(RocketSleigh.giftBoxes[0])
        for (i in 0 until count) {
            val x = ((slotWidth - box.width) / 2).toFloat()
            val y = center + amplitude * Math.sin(2.0 * Math.PI * i.toDouble() / count.toDouble()).toFloat()
            val bmp = bitmapCache.fetch(RocketSleigh.randomGift(random))
            val holder = viewPool.obtainGift()
            holder.image.setImageBitmap(bmp)
            holder.image.translationX = x
            holder.image.translationY = y
            obstacleLayout.addView(holder.frame, lp)
        }

        // Two spacers at the end.
        obstacleLayout.addView(viewPool.obtainSpace().space, lp)
        obstacleLayout.addView(viewPool.obtainSpace().space, lp)

        // Account for rounding errors in slotWidth
        val extra = 3 * screenWidth - 3 * SLOTS_PER_SCREEN * slotWidth
        if (extra > 0) {
            // Add filler to ensure sync with background/foreground scrolls!
            lp = LinearLayout.LayoutParams(extra, LinearLayout.LayoutParams.MATCH_PARENT)
            obstacleLayout.addView(viewPool.obtainSpace().space, lp)
        }
    }

    private fun addNextObstacleSpacer(screens: Int) {
        if (screens > 0) {
            val lp = LinearLayout.LayoutParams(screenWidth * screens, screenHeight)
            obstacleLayout.addView(viewPool.obtainSpace().space, lp)
            lastObstacle = 0
        }
    }

    private fun addNextObstacles(level: Int, screens: Int) {
        when {
            level < 2 -> addObstacles(level, screens, woodsObstacleIndex)
            level < 4 -> addObstacles(level, screens, caveObstacleIndex)
            else -> addObstacles(level, screens, factoryObstacleIndex)
        }
    }

    private fun addObstacles(level: Int, screens: Int, index: AtomicInteger) {
        val totalSlots = screens * SLOTS_PER_SCREEN
        var i = 0
        while (i < totalSlots) {
            // Any given "slot" has a 1 in 3 chance of having an obstacle
            if (random.nextInt(3) == 0) {
                val holder = viewPool.obtainObstacle(obstacleLayout)

                // Which obstacle?
                var width = 0
                val obstacles = levels!![level].obstacles
                index.get().let { indexValue ->
                    if (indexValue % 20 == 0) {
                        preloadObstacles(indexValue + 20, obstacles)
                    }
                }
                if (index.incrementAndGet() >= obstacles.size) {
                    index.set(0)
                }
                val (top1, bottom1, back1) = obstacles[index.get()]
                if (back1 != 0) {
                    val bmp = bitmapCache.fetch(back1)
                    width = bmp.width
                    holder.back.setImageBitmap(bmp)
                    holder.back.visibility = View.VISIBLE
                } else {
                    holder.back.visibility = View.GONE
                }

                var currentObstacle = 0 // Same values as lastObstacle
                if (top1 != 0) {
                    currentObstacle = currentObstacle or 1
                    val bmp = bitmapCache.fetch(top1)
                    width = Math.max(width, bmp.width)
                    holder.top.setImageBitmap(bmp)
                    holder.top.visibility = View.VISIBLE
                } else {
                    holder.top.visibility = View.GONE
                }

                if (bottom1 != 0) {
                    currentObstacle = currentObstacle or 2
                    val bmp = bitmapCache.fetch(bottom1)
                    width = Math.max(width, bmp.width)
                    holder.bottom.setImageBitmap(bmp)
                    holder.bottom.visibility = View.VISIBLE
                } else {
                    holder.bottom.visibility = View.GONE
                }
                val slots = width / slotWidth + 2

                // If last obstacle had a top and this is a bottom or vice versa, insert a space
                if (lastObstacle and 0x1 > 0) {
                    if (currentObstacle and 0x2 > 0) {
                        addSpaceOrPresent(slotWidth)
                        i++
                    }
                } else if (lastObstacle and 0x2 > 0) {
                    if (currentObstacle and 0x1 > 0) {
                        addSpaceOrPresent(slotWidth)
                        i++
                    }
                }

                // If the new obstacle is too wide for the remaining space, skip it and fill spacer
                // instead
                if (i + slots > totalSlots) {
                    addSpaceOrPresent(slotWidth * (totalSlots - i))
                    i = totalSlots
                } else {
                    lastObstacle = currentObstacle
                    val lp = LinearLayout.LayoutParams(
                            slots * slotWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
                    obstacleLayout.addView(holder.obstacle, lp)
                    i += slots
                }
            } else {
                addSpaceOrPresent(slotWidth)
                i++
            }
        }

        // Account for rounding errors in slotWidth
        val extra = screens * screenWidth - totalSlots * slotWidth
        if (extra > 0) {
            // Add filler to ensure sync with background/foreground scrolls!
            val lp = LinearLayout.LayoutParams(extra, LinearLayout.LayoutParams.MATCH_PARENT)
            obstacleLayout.addView(viewPool.obtainSpace().space, lp)
        }
    }

    private fun addSpaceOrPresent(width: Int) {
        if (width <= 0) {
            return
        }
        lastObstacle = 0
        val lp = LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.MATCH_PARENT)
        // 1/3 chance of a present.
        if (random.nextInt(3) == 0) {
            // Present!

            // Which one?
            val bmp = bitmapCache.fetch(RocketSleigh.randomGift(random))
            val holder = viewPool.obtainGift()
            holder.image.setImageBitmap(bmp)

            // Position the present
            val left = random.nextInt(width / 2) + width / 4 -
                    (bmp.width.toFloat() * scaleX).toInt() / 2
            val top = random.nextInt(screenHeight / 2) + screenHeight / 4 -
                    (bmp.height.toFloat() * scaleY).toInt() / 2
            holder.image.translationX = left.toFloat()
            holder.image.translationY = top.toFloat()

            obstacleLayout.addView(holder.frame, lp)
        } else {
            // Space
            obstacleLayout.addView(viewPool.obtainSpace().space, lp)
        }
    }

    private fun addNextImages(level: Int, recycle: Boolean) {
        if (level >= levels!!.size) {
            return
        }
        val currentLevel = levels!![level]

        // Add the background image
        val bg1 = viewPool.obtainBackground()
        bg1.image.setImageBitmap(bitmapCache.fetch(currentLevel.background, secondary = false))
        if (recycle) {
            bg1.image.tag = Pair(0, level)
        }
        val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        backgroundLayout.addView(bg1.image, lp)

        val bg2 = viewPool.obtainBackground()
        bg2.image.setImageBitmap(bitmapCache.fetch(currentLevel.background, secondary = true))
        if (recycle) {
            bg2.image.tag = Pair(0, level)
        }
        backgroundLayout.addView(bg2.image, lp)

        // Add the foreground image
        if (currentLevel.foreground == 0) {
            val view = View(this)
            foregroundLayout.addView(view, LinearLayout.LayoutParams(screenWidth * 2, 10))
        } else {
            val fg = viewPool.obtainForeground()
            fg.image.setBackgroundResource(currentLevel.foreground)
            if (recycle) {
                fg.image.tag = level
            }
            foregroundLayout.addView(fg.image, LinearLayout.LayoutParams(
                    screenWidth * 2, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
    }

    // This is the level we are moving TO.
    private fun addNextTransitionImages(level: Int) {
        transitionImagesCount = 0
        val ls = levels!!
        if (level > 0 && level - 1 < ls.size) {
            val (_, _, _, exitTransition) = ls[level - 1]
            if (exitTransition != 0) {
                // Add the exit transition image
                val holder = viewPool.obtainBackground()
                holder.image.tag = Pair(1, level - 1)
                // This is being background loaded.  Should already be loaded, but if not, wait.
                holder.image.setImageBitmap(bitmapCache.fetch(exitTransition))
                var lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                backgroundLayout.addView(holder.image, lp)

                // No foreground on transitions.  Transition images are a single screen long
                val view = View(this)
                lp = LinearLayout.LayoutParams(screenWidth, 10)
                foregroundLayout.addView(view, lp)
                transitionImagesCount++
            }
        }
        if (level > 0 && level < ls.size) {
            val (_, _, entryTransition) = ls[level]
            if (entryTransition != 0) {
                // Add the exit transition image
                val holder = viewPool.obtainBackground()
                holder.image.tag = Pair(2, level)
                // This is being background loaded.  Should already be loaded, but if not, wait.
                holder.image.setImageBitmap(bitmapCache.fetch(entryTransition))
                var lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                backgroundLayout.addView(holder.image, lp)
                // No foreground on transitions.  Transition images are a single screen long
                val view = View(this)
                lp = LinearLayout.LayoutParams(screenWidth, 10)
                foregroundLayout.addView(view, lp)
                transitionImagesCount++
            }
        }
    }

    private fun addFinalImages(): Int {
        addNextImages(1, false)
        addNextImages(1, false)

        // Add presents
        addFinalPresentRun()

        // Add final screen.  This is a two screen background.
        val iv = ImageView(this)
        iv.tag = true
        val bmp = bitmapCache.fetch(R.drawable.bg_finish, false)
        iv.setImageBitmap(bmp)
        var lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        backgroundLayout.addView(iv, lp)
        val view = View(this)
        lp = LinearLayout.LayoutParams(screenWidth * 2, 10)
        foregroundLayout.addView(view, lp)
        addNextObstacleSpacer(2)
        return bmp.width
    }

    // Load the level 1 images right now since we need to display them.
    // Load the rest on a thread.
    // We preload all of these because the transitions can be quick.
    // They are not very big, relatively speaking.
    private fun loadElfImages() {
        for (id in RocketSleigh.elf) {
            bitmapCache.preload(id)
        }
        for (id in RocketSleigh.elfHit) {
            bitmapCache.preload(id)
        }
        for (id in RocketSleigh.elfBurn) {
            bitmapCache.preload(id)
        }
        for (id in RocketSleigh.elfThrust) {
            bitmapCache.preload(id)
        }
        for (id in RocketSleigh.elfSmoke) {
            bitmapCache.preload(id)
        }
    }

    private fun updateElf(hit: Boolean) {
        var thrustWidth = 0.0f
        if (hit) {
            // Just update the elf drawable
            elfImage.setImageDrawable(null)
            elfBitmap!!.recycle()
            elfBitmap = bitmapCache.fetch(RocketSleigh.elfHit[elfState])
            elfImage.setImageBitmap(elfBitmap)
            updateElfThrust(2)
            thrustWidth = currentTrailBitmap!!.width.toFloat() * elfScale
        } else {
            // New state for elf recycle, reload and reset.
            elfImage.setImageDrawable(null)
            if (elfBitmap != null) {
                elfBitmap!!.recycle()
            }
            thrustImage.setImageDrawable(null)
            if (burnBitmap != null) {
                burnBitmap!!.recycle()
            }
            if (thrustBitmap != null) {
                thrustBitmap!!.recycle()
            }
            if (smokeBitmap != null) {
                smokeBitmap!!.recycle()
            }
            if (elfState < 4) {
                burnBitmap = bitmapCache.fetch(RocketSleigh.elfBurn[elfState])
                thrustBitmap = bitmapCache.fetch(RocketSleigh.elfThrust[elfState])
                smokeBitmap = bitmapCache.fetch(RocketSleigh.elfSmoke[elfState])
                elfBitmap = bitmapCache.fetch(RocketSleigh.elf[elfState])
                if (elfAccelY > 0.0f) {
                    updateElfThrust(1)
                } else {
                    updateElfThrust(0)
                }
                thrustWidth = currentTrailBitmap!!.width.toFloat() * elfScale
            } else {
                elfBitmap = bitmapCache.fetch(RocketSleigh.elf[4])
                thrustImage.visibility = View.GONE
            }
            elfImage.setImageBitmap(elfBitmap)
        }
        val offset = thrustWidth + elfBitmap!!.width.toFloat() / 2.0f
        elfLayout.x = elfPosX - offset
        elfLayout.y = elfPosY
        elfLayout.pivotX = offset
        elfLayout.pivotY = elfBitmap!!.height.toFloat() * 3.0f / 4.0f
        val rot = (Math.atan((elfVelY / elfVelX).toDouble()) * 120.0 / Math.PI).toFloat()
        elfLayout.rotation = rot
        elfLayout.invalidate()
    }

    // 0 - burn, 1 - thrust, 2 - smoke, 3 - gone
    private fun updateElfThrust(type: Int) {
        currentTrailBitmap = when (type) {
            0 -> burnBitmap
            1 -> thrustBitmap
            2 -> smokeBitmap
            3 -> null
            else -> null
        }
        if (currentTrailBitmap != null) {
            thrustImage.setImageBitmap(currentTrailBitmap)
        }
    }

    private fun pause() {
        if (isPlaying) {
            bigPlayButtonLayout.visibility = View.VISIBLE
            if (!isTv) {
                exitView.visibility = View.VISIBLE
            }
        }

        isPlaying = false
        handler?.removeCallbacks(gameLoop)
        controlView.setOnTouchListener(null)
        playPauseButton.setImageResource(R.drawable.play_button_jp)

        if (sounds.jetThrustStream > 0) {
            soundPool!!.stop(sounds.jetThrustStream)
            sounds.jetThrustStream = 0
        }

        bgmPlayer?.pause()

        ImmersiveModeHelper.setImmersiveStickyWithActionBar(window)
    }

    private fun play() {
        isPlaying = true
        bigPlayButtonLayout.visibility = View.GONE
        lastTime = System.currentTimeMillis()
        controlView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                elfAccelY = thrustAccelY
                if (!elfIsHit) {
                    updateElfThrust(1)
                }
                if (!santaPreferences.isMuted) {
                    sounds.jetThrustStream = soundPool!!.play(sounds.jetThrust, priority = 1)
                }
            } else if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                elfAccelY = 0.0f
                if (!elfIsHit) {
                    updateElfThrust(0)
                }
                if (sounds.jetThrustStream > 0) {
                    soundPool!!.stop(sounds.jetThrustStream)
                    sounds.jetThrustStream = 0
                }
            }
            true
        }
        handler?.post(gameLoop)
        playPauseButton.setImageResource(R.drawable.pause_button_jp)
        exitView.visibility = View.GONE

        if (!santaPreferences.isMuted) {
            playBackgroundMusic()
        }

        ImmersiveModeHelper.setImmersiveSticky(window)
    }

    private fun endGame() {
        isPlaying = false
        val intent = Intent(this.applicationContext, EndGameActivity::class.java)
        intent.putExtra("score", score / 10)

        MeasurementManager.recordCustomEvent(
                measurement,
                getString(R.string.analytics_screen_rocket),
                getString(R.string.analytics_action_rocket_final_score), null,
                score / 10)

        if (gameRecord.clearLevel) {
            intent.putExtra(getString(R.string.achievement_safe_tapper), true)
        }
        if (!gameRecord.hit) {
            intent.putExtra(getString(R.string.achievement_untouchable), true)
        }
        if (gameRecord.presentBonus) {
            intent.putExtra(getString(R.string.achievement_hidden_presents), true)
        }
        if (score / 10 > 10000) {
            intent.putExtra(getString(R.string.achievement_rocket_junior_score_10000), true)
        }
        if (score / 10 > 30000) {
            intent.putExtra(getString(R.string.achievement_rocket_intermediate_score_30000), true)
        }
        if (score / 10 > 50000) {
            intent.putExtra(getString(R.string.achievement_rocket_pro_score_50000), true)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun preloadObstacles(startIndex: Int, obstacles: List<Obstacle>) {
        for (i in startIndex until startIndex + 20) {
            val (top, bottom, back) = obstacles[i]
            bitmapCache.preload(top)
            bitmapCache.preload(bottom)
            bitmapCache.preload(back)
        }
    }

    companion object {
        private const val TAG = "RocketSleighActivity"
        private const val SLOTS_PER_SCREEN = 10
    }
}
