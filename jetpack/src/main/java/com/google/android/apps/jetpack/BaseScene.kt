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

package com.google.android.apps.jetpack

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import com.google.android.apps.playgames.simpleengine.Renderer
import com.google.android.apps.playgames.simpleengine.Scene
import com.google.android.apps.playgames.simpleengine.SceneManager
import com.google.android.apps.playgames.simpleengine.SmoothValue
import com.google.android.apps.playgames.simpleengine.game.GameObject
import com.google.android.apps.playgames.simpleengine.game.World
import com.google.android.apps.playgames.simpleengine.ui.Button
import com.google.android.apps.playgames.simpleengine.ui.SimpleUI
import com.google.android.apps.playgames.simpleengine.ui.Widget
import com.google.android.apps.santatracker.util.ImmersiveModeHelper
import java.util.Random

abstract class BaseScene : Scene(), Widget.WidgetTriggerListener, GameEndedListener {

    // digit object factory (to display score, etc)
    protected lateinit var digitFactory: DigitObjectFactory
    protected lateinit var objectFactory: GameObjectFactory

    protected lateinit var world: World
    protected lateinit var renderer: Renderer
    protected val random = Random()

    // score bar object
    private var scoreBarObj: GameObject? = null

    // score bar label object
    private var scoreBarLabelObj: GameObject? = null

    // score digit objects
    private var scoreDigitObj = arrayOfNulls<GameObject>(GameConfig.ScoreDisplay.DIGIT_COUNT)

    // timer digit objects
    private var timeDigitObj = arrayOfNulls<GameObject>(GameConfig.TimeDisplay.DIGIT_COUNT)

    // countdown objects
    var countdownDigitObj: GameObject? = null

    // player's current score
    override var score = 0
        protected set
    protected var displayedScore = SmoothValue(0.0f, GameConfig.ScoreDisplay.UPDATE_SPEED)

    // game ended?
    protected var gameEnded = false

    protected var isInGameEndingTransition = false

    // our UI (buttons, etc)
    private lateinit var simpleUI: SimpleUI

    // sfx IDs
    private var gameOverSfx: Int = 0

    // paused?
    protected var paused = false

    // in start countdown?
    protected var inStartCountdown = false
    protected var startCountdownTimeRemaining: Float = 0.toFloat()

    // back key pressed?
    private var backKeyPending = false

    // DPAD_CENTER key pressed?
    private var confirmKeyPending: Boolean = false
    private var confirmKeyEventTime: Long = 0

    // isRunning on Tv?
    protected var isTv: Boolean = false

    // pause button
    private var pauseButton: Button? = null

    // speaker on and mute buttons
    private var speakerOnButton: Button? = null
    private var speakerMuteButton: Button? = null

    // pause curtain, that is, the full screen object we display as a translucent
    // screen over the whole display when the game is paused
    private lateinit var pauseCurtain: GameObject

    // the big play button
    private lateinit var bigPlayButton: Button
    private var bigStartGameButton: Button? = null

    // quit button
    private var quitButton: Button? = null
    private var quitBarLabel: GameObject? = null

    // game objects that compose the Sign In ui
    private var signInBarObj: GameObject? = null
    private var signInButton: Button? = null
    private var signInTextObj: GameObject? = null

    // to be implemented by subclasses
    protected abstract val bgmResId: Int

    protected abstract val displayedTime: Float

    // are we signed in
    private var signedIn = false

    private val scoreDigitVisibleObj: Array<GameObject?>
        get() {
            val numDigits = Math.max(
                    (displayedScore.value.toInt() + 1).toString().length,
                    GameConfig.ScoreBar.MIN_DIGITS_VISIBLE)
            val scoreDigitVisibleObj = arrayOfNulls<GameObject>(numDigits)
            for (i in scoreDigitObj.size - numDigits until scoreDigitObj.size) {
                scoreDigitVisibleObj[i - (scoreDigitObj.size - numDigits)] = scoreDigitObj[i]
            }
            return scoreDigitVisibleObj
        }

    protected abstract fun makeNewScene(): BaseScene

    override fun onInstall() {

        // are we signed in?
        val act = SceneManager.getInstance().activity as SceneActivity?
        act?.let {
            signedIn = act.isSignedIn
            val manger = act.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            isTv = Configuration.UI_MODE_TYPE_TELEVISION == manger.currentModeType
        }

        renderer = SceneManager.getInstance().renderer
        world = World(renderer)
        digitFactory = DigitObjectFactory(renderer, world)
        digitFactory.requestWhiteTextures(GameConfig.ScoreDisplay.DIGIT_SIZE)
        objectFactory = GameObjectFactory(renderer, world)
        objectFactory.requestTextures()

        simpleUI = SimpleUI(renderer)

        if (isTv) {
            digitFactory.makeDigitObjects(
                    GameConfig.ScoreDisplay.DIGIT_COUNT,
                    GameConfig.TYPE_DECOR,
                    renderer.getRelativePos(
                            GameConfig.ScoreDisplay.POS_X_REL, GameConfig.ScoreDisplay.POS_X_DELTA),
                    renderer.getRelativePos(
                            GameConfig.ScoreDisplay.POS_Y_REL_TV,
                            GameConfig.ScoreDisplay.POS_Y_DELTA_TV),
                    GameConfig.ScoreDisplay.DIGIT_SIZE,
                    GameConfig.ScoreDisplay.DIGIT_SPACING,
                    scoreDigitObj)
            bigPlayButton = objectFactory.makeBigPlayButton(this, MSG_RESUME)
            bigPlayButton.hide()
            simpleUI.add(bigPlayButton)

            val x = GameConfig.TimeDisplay.POS_X_DELTA + GameConfig.TimeDisplay.ICON_SIZE
            digitFactory.makeDigitObjects(
                    GameConfig.TimeDisplay.DIGIT_COUNT,
                    GameConfig.TYPE_DECOR,
                    renderer.getRelativePos(GameConfig.TimeDisplay.POS_X_REL, x),
                    renderer.getRelativePos(
                            GameConfig.TimeDisplay.POS_Y_REL_TV,
                            GameConfig.TimeDisplay.POS_Y_DELTA_TV),
                    GameConfig.TimeDisplay.DIGIT_SIZE,
                    GameConfig.TimeDisplay.DIGIT_SPACING,
                    timeDigitObj)

            pauseCurtain = objectFactory.makePauseCurtain()
            pauseCurtain.hide()
        } else {
            scoreBarObj = objectFactory.makeScoreBar()
            scoreBarLabelObj = objectFactory.makeScoreBarLabel()
            digitFactory.makeDigitObjects(
                    GameConfig.ScoreDisplay.DIGIT_COUNT,
                    GameConfig.TYPE_DECOR,
                    renderer.getRelativePos(
                            GameConfig.ScoreDisplay.POS_X_REL, GameConfig.ScoreDisplay.POS_X_DELTA),
                    renderer.getRelativePos(
                            GameConfig.ScoreDisplay.POS_Y_REL, GameConfig.ScoreDisplay.POS_Y_DELTA),
                    GameConfig.ScoreDisplay.DIGIT_SIZE,
                    GameConfig.ScoreDisplay.DIGIT_SPACING,
                    scoreDigitObj)
            hideObjects(scoreDigitObj)
            bringObjectsToFront(scoreDigitVisibleObj)
            val x = GameConfig.TimeDisplay.POS_X_DELTA + GameConfig.TimeDisplay.ICON_SIZE
            digitFactory.makeDigitObjects(
                    GameConfig.TimeDisplay.DIGIT_COUNT,
                    GameConfig.TYPE_DECOR,
                    renderer.getRelativePos(GameConfig.TimeDisplay.POS_X_REL, x),
                    renderer.getRelativePos(
                            GameConfig.TimeDisplay.POS_Y_REL, GameConfig.TimeDisplay.POS_Y_DELTA),
                    GameConfig.TimeDisplay.DIGIT_SIZE,
                    GameConfig.TimeDisplay.DIGIT_SPACING,
                    timeDigitObj)
            countdownDigitObj = digitFactory.makeDigitObject(
                    GameConfig.TYPE_DECOR, 0.0f, 0.0f, GameConfig.Countdown.DIGIT_SIZE)
            countdownDigitObj?.hide()
            quitButton = objectFactory.makeQuitButton(this, MSG_QUIT)
            quitBarLabel = objectFactory.makeQuitBarLabel()
            quitButton?.hide()
            quitBarLabel?.hide()
            simpleUI.add(quitButton)

            pauseButton = objectFactory.makePauseButton(this, MSG_PAUSE)
            simpleUI.add(pauseButton)

            speakerMuteButton = objectFactory.makeSpeakerMuteButton(this, MSG_UNMUTE)
            speakerOnButton = objectFactory.makeSpeakerOnButton(this, MSG_MUTE)
            simpleUI.add(speakerMuteButton)
            simpleUI.add(speakerOnButton)
            pauseCurtain = objectFactory.makePauseCurtain()
            pauseCurtain.hide()

            bigPlayButton = objectFactory.makeBigPlayButton(this, MSG_RESUME)
            bigPlayButton.hide()
            simpleUI.add(bigPlayButton)
            bigStartGameButton = objectFactory.makeBigPlayButton(this, MSG_START)
            bigStartGameButton?.hide()
            simpleUI.add(bigStartGameButton)
        }

        val soundManager = SceneManager.getInstance().soundManager
        if (soundManager != null) {
            soundManager.requestBackgroundMusic(bgmResId)
            gameOverSfx = soundManager.requestSfx(com.google.android.apps.playgames.R.raw.gameover)
            if (soundManager.isMuted) {
                muteSpeaker()
            } else {
                unmuteSpeaker()
            }
        }
    }

    override fun onUninstall() {}

    override fun doStandbyFrame(deltaT: Float) {}

    override fun doFrame(deltaT: Float) {
        var deltaT = deltaT
        if (paused) {
            deltaT = 0.0f
        }

        if (backKeyPending) {
            processBackKey()
        }

        if (confirmKeyPending) {
            // TODO: move a focus based on KeyEvent
            processCenterKey()
        }

        // If Activity lost focus and we're playing the game, pause
        if (!SceneManager.getInstance().shouldBePlaying() && !gameEnded && !paused) {
            pauseGame()
        }

        if (!gameEnded) {
            updateScore(deltaT)
            updateTime(deltaT)
        } else {
            updateScore(deltaT)
            checkSignInWidgetsNeeded()
        }

        world.doFrame(deltaT)
    }

    private fun processBackKey() {
        backKeyPending = false
        if (gameEnded || paused && isTv) {
            quitGame()
        } else if (paused) {
            unPauseGame()
        } else {
            pauseGame()
        }
    }

    private fun processCenterKey() {
        val currTime = System.currentTimeMillis()
        if (currTime - confirmKeyEventTime < CENTER_KEY_DELAY_MS) {
            bigPlayButton.setPressed(true)
        } else {
            confirmKeyPending = false
            bigPlayButton.setPressed(false)
            if (paused) {
                unPauseGame()
            } else if (gameEnded) {
                // re-start new game
                SceneManager.getInstance().requestNewScene(makeNewScene())
            }
        }
    }

    private fun updateScore(deltaT: Float) {
        if (gameEnded) {
            displayedScore.value = score.toFloat()
        } else {
            displayedScore.target = score.toFloat()
            displayedScore.update(deltaT)
        }
        digitFactory.setDigits(Math.round(displayedScore.value), scoreDigitObj)
        bringObjectsToFront(scoreDigitVisibleObj)
    }

    protected open fun endGame() {
        gameEnded = true
        // show the podium object
        objectFactory.makePodium()

        // move score to final position
        val x = renderer.getRelativePos(
                GameConfig.Podium.ScoreDisplay.X_REL,
                GameConfig.Podium.ScoreDisplay.X_DELTA)
        val y = renderer.getRelativePos(
                GameConfig.Podium.ScoreDisplay.Y_REL,
                GameConfig.Podium.ScoreDisplay.Y_DELTA)
        displaceObjectsTo(scoreDigitObj, x, y)
        bringObjectsToFront(scoreDigitVisibleObj)

        // hide time counter
        hideObjects(timeDigitObj)

        // make the "your score is" label
        objectFactory.makeScoreLabel()

        // create the end of game UI and add the "play again" button to it
        simpleUI.add(objectFactory.makeReturnToMapButton(this, MSG_RETURN_WITH_VALUE))

        if (isTv) {
            // TODO: tv specific ui layout
        } else {
            // hide the score bar
            scoreBarObj?.hide()
            scoreBarLabelObj?.hide()
            pauseButton?.hide()
            speakerMuteButton?.hide()
            speakerOnButton?.hide()
            // TODO: real message

            // create the sign in bar and sign in button
            if (!signedIn) {
                signInBarObj = objectFactory.makeSignInBar()
                signInTextObj = objectFactory.makeSignInText()
                signInButton = objectFactory.makeSignInButton(this, MSG_SIGN_IN)
                simpleUI.add(signInButton)
            }
        }

        // disable the background music
        SceneManager.getInstance().soundManager.enableBgm(false)

        // play the game over sfx
        SceneManager.getInstance().soundManager.playSfx(gameOverSfx)
    }

    private fun displaceObjectsTo(objs: Array<GameObject?>, x: Float, y: Float) {
        val first = objs[0] ?: throw IllegalStateException()
        val deltaX = x - first.x
        val deltaY = y - first.y
        var i = 0
        while (i < objs.size) {
            objs[i]?.displaceBy(deltaX, deltaY)
            i++
        }
    }

    private fun bringObjectsToFront(objs: Array<GameObject?>) {
        var i = 0
        while (i < objs.size) {
            objs[i]?.show()
            objs[i]?.bringToFront()
            i++
        }
    }

    private fun hideObjects(objs: Array<GameObject?>) {
        var i = 0
        while (i < objs.size) {
            objs[i]?.hide()
            i++
        }
    }

    private fun updateTime(deltaT: Float) {
        var seconds = Math.ceil(displayedTime.toDouble()).toInt()
        seconds = if (seconds < 0) 0 else if (seconds > 99) 99 else seconds
        digitFactory.setDigits(seconds, timeDigitObj)
        bringObjectsToFront(timeDigitObj)
    }

    override fun onScreenResized(width: Int, height: Int) {}

    override fun onPointerDown(pointerId: Int, x: Float, y: Float) {
        super.onPointerDown(pointerId, x, y)
        simpleUI.onPointerDown(pointerId, x, y)
    }

    override fun onPointerUp(pointerId: Int, x: Float, y: Float) {
        super.onPointerUp(pointerId, x, y)
        simpleUI.onPointerUp(pointerId, x, y)
    }

    protected fun startGameScreen() {
        paused = true
        SceneManager.getInstance().soundManager.stopSound()
        if (isTv) {
            pauseCurtain.show()
            pauseCurtain.bringToFront()

            bigPlayButton.show()
            bigPlayButton.bringToFront()
        } else {
            pauseButton?.hide()

            pauseCurtain.show()
            pauseCurtain.bringToFront()

            quitButton?.hide()
            quitBarLabel?.hide()
            speakerOnButton?.bringToFront()
            speakerMuteButton?.bringToFront()
            speakerMuteButton?.setEnabled(true)
            speakerOnButton?.setEnabled(true)
            bigStartGameButton?.show()
            bigStartGameButton?.bringToFront()
        }

        if (SceneManager.getInstance().activity != null) {
            SceneManager.getInstance()
                    .activity
                    .runOnUiThread {
                        ImmersiveModeHelper.setImmersiveStickyWithActionBar(
                                SceneManager.getInstance().activity.window)
                    }
        }
    }

    private fun pauseGame() {
        if (!paused && !gameEnded) {
            paused = true
            SceneManager.getInstance().soundManager.stopSound()

            if (isTv) {
                pauseCurtain.show()
                pauseCurtain.bringToFront()

                bigPlayButton.show()
                bigPlayButton.bringToFront()
            } else {
                pauseButton?.hide()

                pauseCurtain.show()
                pauseCurtain.bringToFront()

                bigPlayButton.show()
                bigPlayButton.bringToFront()

                quitButton?.show()
                quitBarLabel?.show()
                quitButton?.bringToFront()
                quitBarLabel?.bringToFront()

                speakerMuteButton?.setEnabled(false)
                speakerOnButton?.setEnabled(false)
            }

            if (SceneManager.getInstance().activity != null) {
                SceneManager.getInstance()
                        .activity
                        .runOnUiThread {
                            val activity = SceneManager.getInstance().activity
                            if (activity != null) {
                                ImmersiveModeHelper.setImmersiveStickyWithActionBar(
                                        activity.window)
                            }
                        }
            }
        }
    }

    private fun muteSpeaker() {
        if (!gameEnded) {
            SceneManager.getInstance().soundManager.mute()
            speakerOnButton?.hide()
            speakerMuteButton?.show()
            speakerMuteButton?.bringToFront()
        }
    }

    private fun unmuteSpeaker() {
        if (!gameEnded) {
            SceneManager.getInstance().soundManager.unmute()
            speakerMuteButton?.hide()
            speakerOnButton?.show()
            speakerOnButton?.bringToFront()
        }
    }

    private fun startCountdown() {
        inStartCountdown = true
        startCountdownTimeRemaining = GameConfig.Countdown.TIME.toFloat()
        bigStartGameButton?.hide()
        digitFactory.setDigit(countdownDigitObj, GameConfig.Countdown.TIME)
        countdownDigitObj?.show()
        countdownDigitObj?.bringToFront()
    }

    protected fun unPauseGame() {
        if (!paused) {
            return
        }
        paused = false
        SceneManager.getInstance().soundManager.enableBgm(true)
        SceneManager.getInstance().soundManager.resumeSound()
        if (isTv) {
            pauseCurtain.hide()
            bigPlayButton.hide()
        } else {
            pauseButton?.show()
            pauseCurtain.hide()
            quitButton?.hide()
            quitBarLabel?.hide()
            bigPlayButton.hide()
            countdownDigitObj?.hide()
            speakerMuteButton?.setEnabled(true)
            speakerOnButton?.setEnabled(true)
        }

        if (SceneManager.getInstance().activity != null) {
            SceneManager.getInstance()
                    .activity
                    .runOnUiThread {
                        ImmersiveModeHelper.setImmersiveSticky(
                                SceneManager.getInstance().activity.window)
                    }
        }
    }

    override fun onPointerMove(pointerId: Int, x: Float, y: Float, deltaX: Float, deltaY: Float) {
        simpleUI.onPointerMove(pointerId, x, y, deltaX, deltaY)
    }

    override fun onWidgetTriggered(message: Int) {
        when (message) {
            MSG_RETURN_WITH_VALUE -> returnWithScore()
            MSG_REPLAY -> SceneManager.getInstance().requestNewScene(makeNewScene())
            MSG_SIGN_IN -> {
                (SceneManager.getInstance().activity as SceneActivity).beginUserInitiatedSignIn()
            }
            MSG_PAUSE -> pauseGame()
            MSG_RESUME -> unPauseGame()
            MSG_QUIT -> quitGame()
            MSG_SHARE -> share()
            MSG_MUTE -> muteSpeaker()
            MSG_UNMUTE -> unmuteSpeaker()
            MSG_START -> startCountdown()
            MSG_GO_TO_END_GAME -> goToEndGameWithDelay(GameConfig.EndGame.DELAY)
        }
    }

    private fun quitGame() {
        val act = SceneManager.getInstance().activity
        if (act != null && act is SceneActivity) {
            act.postQuitGame()
        }
    }

    private fun returnWithScore() {
        val act = SceneManager.getInstance().activity
        if (act != null && act is SceneActivity) {
            act.postReturnWithScore(score)
        }
    }

    private fun goToEndGameWithDelay(delay: Int) {
        val act = SceneManager.getInstance().activity
        (act as SceneActivity).setGameEndedListener(this)
        act.postDelayedGoToEndGame(delay)
    }

    protected fun goToEndGame() {
        val act = SceneManager.getInstance().activity
        if (act != null && act is SceneActivity) {
            act.setGameEndedListener(this)
            act.postGoToEndGame()
        }
    }

    override fun onGameEnded() {
        gameEnded = true
    }

    private fun share() {
        val act = SceneManager.getInstance().activity
        if (act != null && act is SceneActivity) {
            act.share()
        }
    }

    private fun checkSignInWidgetsNeeded() {
        if (signedIn) {
            signInBarObj?.hide()
            signInTextObj?.hide()
            signInButton?.hide()
        }
    }

    // Caution: Called from the UI thread!
    fun setSignedIn(signedIn: Boolean) {
        this.signedIn = signedIn
    }

    // Caution: Called from the UI thread!
    fun onBackKeyPressed(): Boolean {
        // raise a flag and process later (on the game thread)
        backKeyPending = true
        return true
    }

    // Caution: Called from the UI thread!
    fun onConfirmKeyPressed(): Boolean {
        // raise a flag and process later (on the game thread)
        if (confirmKeyPending) {
            return true
        }

        confirmKeyPending = true
        confirmKeyEventTime = System.currentTimeMillis()
        return true
    }

    companion object {

        // widget trigger messages
        private const val MSG_RETURN_WITH_VALUE = 1001
        private const val MSG_SIGN_IN = 1002
        private const val MSG_PAUSE = 1003
        private const val MSG_RESUME = 1004
        private const val MSG_QUIT = 1005
        private const val MSG_SHARE = 1006
        private const val MSG_REPLAY = 1007
        private const val MSG_MUTE = 1008
        private const val MSG_UNMUTE = 1009
        private const val MSG_START = 1010
        const val MSG_GO_TO_END_GAME = 1011
        private const val CENTER_KEY_DELAY_MS: Long = 500
    }
}
