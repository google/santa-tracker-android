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

package com.google.android.apps.santatracker.dasherdancer

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.LruCache
import android.view.GestureDetector.OnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.view.postDelayed
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.apps.santatracker.common.CheckableImageButton
import com.google.android.apps.santatracker.data.SantaPreferences
import com.google.android.apps.santatracker.games.OnDemandActivity
import com.google.android.apps.santatracker.games.PlayGamesFragment
import com.google.android.apps.santatracker.games.SignInListener
import com.google.android.apps.santatracker.util.ImmersiveModeHelper
import com.google.android.apps.santatracker.util.MeasurementManager
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.apps.santatracker.util.play
import com.google.android.gms.games.Games
import com.google.firebase.analytics.FirebaseAnalytics
import com.squareup.seismic.ShakeDetector
import com.squareup.seismic.ShakeDetector.Listener
import java.util.HashSet

class DasherDancerActivity : OnDemandActivity(), OnGestureListener, OnScaleGestureListener,
        Handler.Callback, Listener, SensorEventListener, AnimatorListener, OnPageChangeListener,
        SignInListener {

    private val soundIds = arrayOf(
            intArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1), // santa
            intArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1), // elf
            intArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1), // reindeer
            intArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1) // snowman
    )

    private lateinit var progressAnimator: ObjectAnimator
    private lateinit var activityManager: ActivityManager
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var detector: ShakeDetector
    private lateinit var soundPool: SoundPool
    private lateinit var memoryCache: LruCache<Int, Drawable?>
    private lateinit var pager: NoSwipeViewPager
    private lateinit var handler: Handler
    private var loadBitmapsTask: LoadBitmapsTask? = null
    private var loadCharacterTask: LoadCharacterResourcesTask? = null
    private var animator: ObjectAnimator? = null
    private var playingRest = false
    private var animCanceled = false
    private var animPlaying = false
    private var scaling = false
    private var initialized = false
    private var soundId = -1
    private var canTouch = false

    private lateinit var muteButton: CheckableImageButton
    private lateinit var santaPreferences: SantaPreferences

    // Bitmap downsampling options
    private lateinit var options: BitmapFactory.Options
    private var downSamplingAttempts: Int = 0

    private var gamesFragment: PlayGamesFragment? = null

    // For achievements
    private lateinit var achievements: Array<HashSet<Int>>

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dasher_dancer)

        santaPreferences = SantaPreferences(this)

        muteButton = findViewById(R.id.mute_button)
        muteButton.isChecked = !santaPreferences.isMuted
        muteButton.setOnClickListener {
            // Toggle the state
            santaPreferences.toggleMuted()
            // Now update the vie
            muteButton.isChecked = !santaPreferences.isMuted
            onMuteChanged(santaPreferences.isMuted)
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        MeasurementManager.recordScreenView(
                firebaseAnalytics, getString(R.string.analytics_screen_dasher))

        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        memoryCache = object : LruCache<Int, Drawable?>(240) {
            override fun entryRemoved(
                evicted: Boolean,
                key: Int?,
                oldValue: Drawable?,
                newValue: Drawable?
            ) {
                if (oldValue != null && oldValue !== newValue) {
                    if (oldValue is InsetDrawableCompat) {
                        val drawable = oldValue.drawable
                        if (drawable is BitmapDrawable) {
                            drawable.bitmap.recycle()
                        }
                    }
                }
            }
        }

        // Initialize default Bitmap options
        options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
            inSampleSize = resources.getInteger(R.integer.res)
            if (activityManager.isLowRamDevice) {
                SantaLog.w(TAG, "isLowRamDevice: downsampling default bitmap options")
                inSampleSize *= 2
            }
        }

        val adapter = CharacterAdapter(characters)
        pager = findViewById<View>(R.id.character_pager) as NoSwipeViewPager
        pager.let {
            it.adapter = adapter
            it.setGestureDetectorListeners(this, this, this)
            it.setOnPageChangeListener(this)
        }

        handler = Handler(mainLooper, this)
        detector = ShakeDetector(this)
        soundPool = SoundPool(4, AudioManager.STREAM_MUSIC, 0)
        achievements = arrayOf(HashSet(), HashSet(), HashSet(), HashSet())
        progressAnimator =
                ObjectAnimator.ofFloat(findViewById(R.id.progress), "rotation", 360f).apply {
                    duration = 4000
                    start()
                }
        gamesFragment = PlayGamesFragment.getInstance(this, this)
        ImmersiveModeHelper.setImmersiveSticky(window)
        ImmersiveModeHelper.installSystemUiVisibilityChangeListener(window)
    }

    public override fun onResume() {
        super.onResume()
        val manager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        manager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL)
        detector.start(manager)

        if (initialized) {
            // Start the animation for the first character.
            pager.postDelayed(300) {
                loadAnimation(true,
                        characters[pager.currentItem].getDuration(Character.ANIM_IDLE),
                        characters[pager.currentItem].getFrameIndices(Character.ANIM_IDLE),
                        characters[pager.currentItem].getFrames(Character.ANIM_IDLE))
            }
        } else {
            loadCharacterTask?.cancel(true)
            loadCharacterTask = LoadCharacterResourcesTask(pager.currentItem)
            loadCharacterTask?.execute()
        }
    }

    public override fun onPause() {
        super.onPause()

        detector.stop()

        val manager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        manager.unregisterListener(this)

        animator?.cancel()
        val character = pager.findViewWithTag<FrameAnimationView>(pager.currentItem)
        character?.setImageDrawable(null)
    }

    /**
     * Finishes the activity.
     *
     * @param view
     */
    fun onNavClick(view: View) {
        loadBitmapsTask?.cancel(true)
        animator?.cancel()
        finish()
    }

    /**
     * Starts the CharacterActivity for result. That result is an integer that corresponds to the
     * index of an entry in characters.
     *
     * @param view
     */
    fun onChangeClick(view: View) {
        loadBitmapsTask?.cancel(true)
        loadCharacterTask?.cancel(true)
        animator?.cancel()
        val character = pager.findViewWithTag<FrameAnimationView>(pager.currentItem)
        character.setImageDrawable(null)
        character.setFrames(null, null)
        character.invalidate()
        val intent = Intent(this, CharacterActivity::class.java)
        startActivityForResult(intent, sCharacterRequestCode)
    }

    /** Moves the view pager to the next character to the left of the current position.  */
    fun onLeftClick(view: View) {
        val currentPosition = pager.currentItem
        if (currentPosition != 0) {
            characterSelectedHelper(currentPosition - 1, true)
        }
    }

    /** Moves the view pager to the next character to the right of the current position.  */
    fun onRightClick(view: View) {
        val currentPosition = pager.currentItem
        if (currentPosition != pager.adapter!!.count - 1) {
            characterSelectedHelper(currentPosition + 1, true)
        }
    }

    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent) {
        // Ignore this.
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        if (!animPlaying) {
            soundId = soundIds[pager.currentItem][Character.ANIM_TAP]
        }

        MeasurementManager.recordCustomEvent(
                firebaseAnalytics,
                getString(R.string.analytics_category_interaction),
                characters[pager.currentItem].characterName,
                getString(R.string.analytics_action_tap))

        updateGestureAchievements(Character.ANIM_TAP)
        loadAnimation(
                false,
                characters[pager.currentItem].getDuration(Character.ANIM_TAP),
                characters[pager.currentItem].getFrameIndices(Character.ANIM_TAP),
                characters[pager.currentItem].getFrames(Character.ANIM_TAP))
        return true
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        // Ignore
    }

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val xDelta = Math.abs(e1.x - e2.x)
        val yDelta = Math.abs(e1.y - e2.y)
        if (xDelta > yDelta) {
            // Moving side to side.
            if (e1.x > e2.x) {
                // Moving left.
                if (!animPlaying) {
                    soundId = soundIds[pager.currentItem][Character.ANIM_SWIPE_LEFT]
                }
                MeasurementManager.recordCustomEvent(
                        firebaseAnalytics,
                        getString(R.string.analytics_category_interaction),
                        characters[pager.currentItem].characterName,
                        getString(R.string.analytics_action_swipe_left))
                updateGestureAchievements(Character.ANIM_SWIPE_LEFT)
                loadAnimation(
                        false,
                        characters[pager.currentItem].getDuration(Character.ANIM_SWIPE_LEFT),
                        characters[pager.currentItem].getFrameIndices(
                                Character.ANIM_SWIPE_LEFT),
                        characters[pager.currentItem].getFrames(Character.ANIM_SWIPE_LEFT))
            } else if (e2.x > e1.x) {
                // Moving right.
                if (!animPlaying) {
                    soundId = soundIds[pager.currentItem][Character.ANIM_SWIPE_RIGHT]
                }
                MeasurementManager.recordCustomEvent(
                        firebaseAnalytics,
                        getString(R.string.analytics_category_interaction),
                        characters[pager.currentItem].characterName,
                        getString(R.string.analytics_action_swipe_right))
                updateGestureAchievements(Character.ANIM_SWIPE_RIGHT)
                loadAnimation(
                        false,
                        characters[pager.currentItem].getDuration(
                                Character.ANIM_SWIPE_RIGHT),
                        characters[pager.currentItem].getFrameIndices(
                                Character.ANIM_SWIPE_RIGHT),
                        characters[pager.currentItem].getFrames(Character.ANIM_SWIPE_RIGHT))
            }
        } else {
            // We are moving up and down
            if (e1.y > e2.y) {
                // Moving up.
                if (!animPlaying) {
                    soundId = soundIds[pager.currentItem][Character.ANIM_SWIPE_UP]
                }
                MeasurementManager.recordCustomEvent(
                        firebaseAnalytics,
                        getString(R.string.analytics_category_interaction),
                        characters[pager.currentItem].characterName,
                        getString(R.string.analytics_action_swipe_up))
                updateGestureAchievements(Character.ANIM_SWIPE_UP)
                loadAnimation(
                        false,
                        characters[pager.currentItem].getDuration(Character.ANIM_SWIPE_UP),
                        characters[pager.currentItem].getFrameIndices(
                                Character.ANIM_SWIPE_UP),
                        characters[pager.currentItem].getFrames(Character.ANIM_SWIPE_UP))
            } else if (e2.y > e1.y) {
                // Moving down.
                if (!animPlaying) {
                    soundId = soundIds[pager.currentItem][Character.ANIM_SWIPE_DOWN]
                }
                MeasurementManager.recordCustomEvent(
                        firebaseAnalytics,
                        getString(R.string.analytics_category_interaction),
                        characters[pager.currentItem].characterName,
                        getString(R.string.analytics_action_swipe_down))
                updateGestureAchievements(Character.ANIM_SWIPE_DOWN)
                loadAnimation(
                        false,
                        characters[pager.currentItem].getDuration(Character.ANIM_SWIPE_DOWN),
                        characters[pager.currentItem].getFrameIndices(
                                Character.ANIM_SWIPE_DOWN),
                        characters[pager.currentItem].getFrames(Character.ANIM_SWIPE_DOWN))
            }
        }
        return false
    }

    override fun handleMessage(msg: Message): Boolean {
        loadAnimation(
                true,
                characters[pager.currentItem].getDuration(Character.ANIM_IDLE),
                characters[pager.currentItem].getFrameIndices(Character.ANIM_IDLE),
                characters[pager.currentItem].getFrames(Character.ANIM_IDLE))
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {

        return false
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        scaling = true
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        scaling = false
        if (detector.scaleFactor > 1) {
            // Pinch in
            if (!animPlaying) {
                soundId = soundIds[pager.currentItem][Character.ANIM_PINCH_IN]
            }
            MeasurementManager.recordCustomEvent(
                    firebaseAnalytics,
                    getString(R.string.analytics_category_interaction),
                    characters[pager.currentItem].characterName,
                    getString(R.string.analytics_action_pinch_in))
            updateGestureAchievements(Character.ANIM_PINCH_IN)
            loadAnimation(
                    false,
                    characters[pager.currentItem].getDuration(Character.ANIM_PINCH_IN),
                    characters[pager.currentItem].getFrameIndices(Character.ANIM_PINCH_IN),
                    characters[pager.currentItem].getFrames(Character.ANIM_PINCH_IN))
        } else if (detector.scaleFactor < 1) {
            // Pinch out
            if (!animPlaying) {
                soundId = soundIds[pager.currentItem][Character.ANIM_PINCH_OUT]
            }
            MeasurementManager.recordCustomEvent(
                    firebaseAnalytics,
                    getString(R.string.analytics_category_interaction),
                    characters[pager.currentItem].characterName,
                    getString(R.string.analytics_action_pinch_out))
            updateGestureAchievements(Character.ANIM_PINCH_OUT)
            loadAnimation(
                    false,
                    characters[pager.currentItem].getDuration(Character.ANIM_PINCH_OUT),
                    characters[pager.currentItem].getFrameIndices(Character.ANIM_PINCH_OUT),
                    characters[pager.currentItem].getFrames(Character.ANIM_PINCH_OUT))
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Ignore this.
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Ignore this.
    }

    override fun hearShake() {
        if (!animPlaying) {
            soundId = soundIds[pager.currentItem][Character.ANIM_SHAKE]
        }
        MeasurementManager.recordCustomEvent(
                firebaseAnalytics,
                getString(R.string.analytics_category_interaction),
                characters[pager.currentItem].characterName,
                getString(R.string.analytics_action_shake))
        updateGestureAchievements(Character.ANIM_SHAKE)
        loadAnimation(
                false,
                characters[pager.currentItem].getDuration(Character.ANIM_SHAKE),
                characters[pager.currentItem].getFrameIndices(Character.ANIM_SHAKE),
                characters[pager.currentItem].getFrames(Character.ANIM_SHAKE))
    }

    private fun onMuteChanged(isMuted: Boolean) {
        if (isMuted) {
            soundPool.autoPause()
        } else {
            soundPool.autoResume()
        }
    }

    /**
     * Helper method to load and start animations. Takes care of canceling any ongoing animations,
     * and will return without executing anything if animPlaying is true or scaling is true.
     *
     * @param playingRest
     * @param animationTime
     * @param frameIndices
     * @param frameResourceIds
     */
    private fun loadAnimation(
        playingRest: Boolean,
        animationTime: Long,
        frameIndices: IntArray,
        frameResourceIds: IntArray
    ) {
        if (!playingRest && (animPlaying || scaling) || !canTouch) {
            return
        }
        animPlaying = !playingRest
        this.playingRest = playingRest
        if (loadBitmapsTask != null) {
            loadBitmapsTask?.cancel(true)
            animator?.cancel()
        }

        loadBitmapsTask = LoadBitmapsTask(animationTime, frameIndices, frameResourceIds)
        loadBitmapsTask?.execute()
    }

    /**
     * Load and cache all sounds for a given character.
     *
     * @param characterIndex index of the character in the array, like [.CHARACTER_ID_SANTA].
     */
    private fun loadSoundsForCharacter(characterIndex: Int) {
        for (animationId in Character.ALL_ANIMS) {
            // No need to load sounds twice
            if (soundIds[characterIndex][animationId] != -1) {
                continue
            }

            val soundResource = characters[characterIndex].getSoundResource(animationId)
            if (soundResource != -1) {
                soundIds[characterIndex][animationId] = soundPool.load(this, soundResource, 1)
            }
        }
    }

    override fun onSignInFailed() {}

    override fun onSignInSucceeded() {}

    @Throws(DasherDancerActivity.BitmapLoadException::class)
    private fun tryLoadBitmap(@DrawableRes resourceId: Int): Drawable {
        try {
            BitmapFactory.decodeResource(resources, resourceId, options)?.let { bmp ->
                val bitmapDrawable = BitmapDrawable(resources, bmp)
                val p = ResourceOffsets.getOffsets(resourceId)
                val x = Math.round(p.x / options.inSampleSize.toFloat())
                val y = Math.round(p.y / options.inSampleSize.toFloat())
                val w = Math.round(ResourceOffsets.ORIG_SIZE.x / options.inSampleSize.toFloat())
                val h = Math.round(ResourceOffsets.ORIG_SIZE.y / options.inSampleSize.toFloat())
                return InsetDrawableCompat(
                        bitmapDrawable, x, y, w - bmp.width - x, h - bmp.height - y)
            }
        } catch (oom: OutOfMemoryError) {
            SantaLog.w(TAG, "Out of memory error, inSampleSize=" + options.inSampleSize)
            if (downSamplingAttempts < MAX_DOWNSAMPLING_ATTEMPTS) {
                options.inSampleSize *= 2
                downSamplingAttempts++
            }
        }

        throw BitmapLoadException("Failed to load resource ID: $resourceId")
    }

    /**
     * Load all of the resources for a given Character, then begin playing the "IDLE" animation. for
     * that character.
     */
    private inner class LoadCharacterResourcesTask internal constructor(
        private val characterIndex: Int
    ) : RetryableAsyncTask<Void?, Void?, Void?>() {

        private val character: Character = characters[characterIndex]

        override fun doInBackground(vararg params: Void?): Void? {
            canTouch = false
            // See if we can free up any memory before we allocate some ourselves.
            // Request garbage collection.
            System.gc()

            // Load all sounds for this character
            loadSoundsForCharacter(characterIndex)

            // Load all animations types for this character
            for (animation in Character.ALL_ANIMS) {
                for (resourceId in character.getFrames(animation)) {
                    if (isCancelled) {
                        break
                    }

                    if (memoryCache.get(resourceId) == null) {
                        try {
                            val bitmap = tryLoadBitmap(resourceId)
                            memoryCache.put(resourceId, bitmap)
                        } catch (e: BitmapLoadException) {
                            SantaLog.e(TAG, "LoadCharacterResourcesTask: failed", e)

                            // Retry the task
                            return retrySelf(params as Array<Void?>)
                        }

                        if (isCancelled) {
                            // Remove the BMP we just added
                            // The check and remove should be atomic so we synchronize
                            // (There could be an evict going on so make sure it's still there...
                            synchronized(memoryCache) {
                                if (memoryCache.get(resourceId) != null) {
                                    memoryCache.remove(resourceId)
                                }
                            }
                        }
                    }
                }
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            if (isCancelled) {
                return
            }

            findViewById<View>(R.id.progress).visibility = View.GONE

            val currentCharacter = characters[pager.currentItem]
            val frames =
                    arrayOfNulls<Drawable>(currentCharacter.getFrames(Character.ANIM_IDLE).size)
            for (i in frames.indices) {
                frames[i] = memoryCache.get(currentCharacter.getFrames(Character.ANIM_IDLE)[i])
            }

            val characterView = pager.findViewWithTag<FrameAnimationView>(pager.currentItem)
            characterView.setFrames(frames, currentCharacter.getFrameIndices(Character.ANIM_IDLE))

            playingRest = true
            animator = ObjectAnimator.ofInt(
                    characterView,
                    "frameIndex",
                    0,
                    currentCharacter.getFrameIndices(Character.ANIM_IDLE).size - 1)
            animator?.let {
                it.duration = currentCharacter.getDuration(Character.ANIM_IDLE)
                it.addListener(this@DasherDancerActivity)
                it.start()
            }
            initialized = true
            canTouch = true
        }

        public override fun shouldRetry(): Boolean {
            return downSamplingAttempts < MAX_DOWNSAMPLING_ATTEMPTS
        }

        public override fun onPrepareForRetry() {
            // Clear all frames for this character
            for (animation in Character.ALL_ANIMS) {
                for (resourceId in character.getFrames(animation)) {
                    memoryCache.remove(resourceId)
                }
            }

            // Try to retry
            System.gc()
        }
    }

    /** AsyncTask that loads bitmaps for animation and starts the animation upon completion.  */
    private inner class LoadBitmapsTask(
        private val mDuration: Long,
        private val mFrameIndices: IntArray,
        private val mFrames: IntArray
    ) : RetryableAsyncTask<Void?, Void?, Array<Drawable?>>() {

        override fun doInBackground(vararg params: Void?): Array<Drawable?>? {
            val bitmaps = arrayOfNulls<Drawable>(mFrames.size)
            for (i in mFrames.indices) {
                if (isCancelled) {
                    break
                }

                val id = mFrames[i]
                if (memoryCache.get(id) == null) {
                    try {
                        bitmaps[i] = tryLoadBitmap(id)
                    } catch (e: BitmapLoadException) {
                        SantaLog.e(TAG, "LoadBitmapsTask: failed", e)
                        return retrySelf(params as Array<Void?>)
                    }

                    memoryCache.put(id, bitmaps[i])
                    if (isCancelled) {
                        synchronized(memoryCache) {
                            if (memoryCache.get(id) != null) {
                                memoryCache.remove(id)
                            }
                        }
                    }
                } else {
                    bitmaps[i] = memoryCache.get(mFrames[i])
                }
            }
            return bitmaps
        }

        override fun onPostExecute(result: Array<Drawable?>?) {
            if (result == null || isCancelled) {
                return
            }

            val character = pager.findViewWithTag<FrameAnimationView>(pager.currentItem)
            character.setFrames(result, mFrameIndices)
            animator?.cancel()
            animator = ObjectAnimator.ofInt(character, "frameIndex", 0, mFrameIndices.size - 1)
            animator?.let {
                it.duration = mDuration
                it.addListener(this@DasherDancerActivity)
                it.start()
            }
            if (soundId != -1 && !santaPreferences.isMuted) {
                soundPool.play(soundId)
                soundId = -1
            }
        }

        public override fun shouldRetry(): Boolean {
            return downSamplingAttempts < MAX_DOWNSAMPLING_ATTEMPTS
        }

        public override fun onPrepareForRetry() {
            // Remove all frames this task should load
            for (id in mFrames) {
                memoryCache.remove(id)
            }

            // See if we can now GC
            System.gc()
        }
    }

    override fun onAnimationStart(animation: Animator) {
        animCanceled = false
        if (!playingRest) {
            animPlaying = true
        }
    }

    override fun onAnimationEnd(animation: Animator) {
        if (animCanceled) {
            return
        }
        animPlaying = false // yoda
        if (playingRest) {
            // We are at rest, so play the idle animation again.
            val character = pager.findViewWithTag<View>(pager.currentItem) as FrameAnimationView
            animator = ObjectAnimator.ofInt(
                    character,
                    "frameIndex",
                    0,
                    characters[0].getFrameIndices(Character.ANIM_IDLE).size)
            animator?.let {
                it.duration = characters[0].getDuration(Character.ANIM_IDLE)
                it.addListener(this@DasherDancerActivity)
                it.start()
            }
        } else {
            // We finished an animation triggered by a gesture, so start the idle animation again.
            handler.sendEmptyMessage(1)
        }
    }

    override fun onAnimationCancel(animation: Animator) {
        animCanceled = true
        animPlaying = false
    }

    override fun onAnimationRepeat(animation: Animator) {
        // Ignore
    }

    override fun onPageScrollStateChanged(arg0: Int) {
        // Ignore
    }

    override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {
        // Ignore
    }

    override fun onPageSelected(arg0: Int) {}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == sCharacterRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                initialized = false
                val character = pager.findViewWithTag<View>(pager.currentItem) as FrameAnimationView
                character.setImageDrawable(null)
                data?.extras?.let {
                    val position = it.getInt(EXTRA_CHARACTER_ID)
                    characterSelectedHelper(position, false)
                }
            }
        }
    }

    override fun onBackPressed() {
        // If we are backing out of the game, clear the cache to free memory.
        soundPool.release()
        memoryCache.evictAll()
        // Request garbage collection.
        System.gc()
        super.onBackPressed()
    }

    public override fun onDestroy() {
        soundPool.release()
        memoryCache.evictAll()
        // Request garbage collection.
        System.gc()
        super.onDestroy()
    }

    private fun characterSelectedHelper(position: Int, smoothScroll: Boolean) {
        loadBitmapsTask?.cancel(true)
        loadCharacterTask?.cancel(true)
        animator?.cancel()

        when {
            position == 0 -> {
                findViewById<View>(R.id.left_button).visibility = View.GONE
                findViewById<View>(R.id.right_button).visibility = View.VISIBLE
            }
            position + 1 == pager.adapter!!.count -> {
                findViewById<View>(R.id.right_button).visibility = View.GONE
                findViewById<View>(R.id.left_button).visibility = View.VISIBLE
            }
            else -> {
                findViewById<View>(R.id.left_button).visibility = View.VISIBLE
                findViewById<View>(R.id.right_button).visibility = View.VISIBLE
            }
        }

        MeasurementManager.recordCustomEvent(
                firebaseAnalytics,
                getString(R.string.analytics_category_character),
                getString(R.string.analytics_action_character_change),
                characters[position].characterName)

        pager.postDelayed(100) {
            // Show progress
            pager.setCurrentItem(position, smoothScroll)
            findViewById<View>(R.id.progress).visibility = View.VISIBLE
            progressAnimator.start()
            (pager.findViewWithTag<View>(pager.currentItem) as ImageView)
                    .setImageDrawable(null)
            memoryCache.evictAll()
            // Request garbage collection.
            System.gc()
            loadCharacterTask?.cancel(true)

            loadCharacterTask = LoadCharacterResourcesTask(position)
            loadCharacterTask?.execute()
        }
    }

    private fun updateGestureAchievements(type: Int) {
        val character = pager.currentItem
        achievements[character].add(type)
        if (achievements[character].size == 8) {
            if (gamesFragment!!.isSignedIn) {
                when (character) {
                    CHARACTER_ID_SANTA -> {
                        Games.Achievements.unlock(
                                gamesFragment!!.gamesApiClient,
                                getString(R.string.achievement_santas_dance_party))
                        MeasurementManager.recordAchievement(
                                firebaseAnalytics,
                                getString(R.string.achievement_santas_dance_party),
                                getString(R.string.analytics_screen_dasher))
                    }
                    CHARACTER_ID_ELF -> {
                        Games.Achievements.unlock(
                                gamesFragment!!.gamesApiClient,
                                getString(R.string.achievement_elfs_dance_party))
                        MeasurementManager.recordAchievement(
                                firebaseAnalytics,
                                getString(R.string.achievement_elfs_dance_party),
                                getString(R.string.analytics_screen_dasher))
                    }
                    CHARACTER_ID_REINDEER -> {
                        Games.Achievements.unlock(
                                gamesFragment!!.gamesApiClient,
                                getString(R.string.achievement_rudolphs_dance_party))
                        MeasurementManager.recordAchievement(
                                firebaseAnalytics,
                                getString(R.string.achievement_rudolphs_dance_party),
                                getString(R.string.analytics_screen_dasher))
                    }
                    CHARACTER_ID_SNOWMAN -> {
                        Games.Achievements.unlock(
                                gamesFragment!!.gamesApiClient,
                                getString(R.string.achievement_snowmans_dance_party))
                        MeasurementManager.recordAchievement(
                                firebaseAnalytics,
                                getString(R.string.achievement_snowmans_dance_party),
                                getString(R.string.analytics_screen_dasher))
                    }
                }
            }
        }
    }

    /** Convenience class for exception when loading Bitmaps.  */
    private class BitmapLoadException(msg: String) : Exception(msg)

    companion object {

        private const val TAG = "DasherDancer"

        /**
         * Extra key used to pass back the character id that should be selected, set by the
         * CharacterActivity.
         */
        const val EXTRA_CHARACTER_ID = "extra_character_id"

        // Character ids, which are also indices in the characters array.
        const val CHARACTER_ID_SANTA = 0
        const val CHARACTER_ID_ELF = 1
        const val CHARACTER_ID_REINDEER = 2
        const val CHARACTER_ID_SNOWMAN = 3

        /** Number of times to try downsampling before giving up  */
        const val MAX_DOWNSAMPLING_ATTEMPTS = 3

        /** Request code for calling CharacterActivity for result.  */
        private const val sCharacterRequestCode = 1

        /**
         * Our array of playable characters. Add more characters here an create new CHARACTER_ID_*
         * static variables.
         */
        private val characters = arrayOf(Santa(), Elf(), Reindeer(), Snowman())
    }
}
