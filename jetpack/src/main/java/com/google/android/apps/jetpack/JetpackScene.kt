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

import android.view.KeyEvent
import com.google.android.apps.playgames.simpleengine.Logger
import com.google.android.apps.playgames.simpleengine.SceneManager
import com.google.android.apps.playgames.simpleengine.SmoothValue
import com.google.android.apps.playgames.simpleengine.game.GameObject
import java.util.ArrayList
import java.util.HashSet

class JetpackScene : BaseScene() {

    // player
    private lateinit var playerObj: GameObject

    // background
    private lateinit var background: GameObject

    // our object factory
    private lateinit var factory: JetpackObjectFactory

    // current difficulty level
    private var level = 1

    // player is currently injured
    private var playerHit = false

    // time remaining of player's injury
    private var playerHitTime = JetpackConfig.Time.HIT_TIME

    // total items collected
    private var itemsCollected = 0

    // item fall speed multipler (increases with level)
    private var fallMult = 3.0f

    // score multipler (increases with level)
    private var scoreMult = 1.0f

    private var spriteAngle = SmoothValue(
            0.0f,
            JetpackConfig.Player.SpriteAngle.MAX_CHANGE_RATE,
            -JetpackConfig.Player.SpriteAngle.MAX_ANGLE,
            JetpackConfig.Player.SpriteAngle.MAX_ANGLE,
            JetpackConfig.Player.SpriteAngle.FILTER_SAMPLES)

    private var playerTargetX = 0.0f
    private var playerTargetY = 0.0f

    // working array
    private var tmpList = ArrayList<GameObject>()

    // how long til we spawn the next item?
    private var spawnCountdown = JetpackConfig.Items.SPAWN_INTERVAL

    // cloud sprites
    private var cloudObj = arrayOfNulls<GameObject>(JetpackConfig.Clouds.COUNT)

    // time remaining
    override var displayedTime = JetpackConfig.Time.INITIAL

    // sfx IDs
    private lateinit var itemSfxSuccess: IntArray

    private val combo = Combo()

    // set of achievements we know we unlocked (to prevent repeated API calls)
    private val unlockedAchievements = HashSet<Int>()

    // achievement increments we are pending to send
    private var achPendingPresents = 0
    private var achPendingCandy = 0
    private var achPendingSeconds = 0f

    // countdown to next sending of incremental achievements
    private var incAchCountdown = JetpackConfig.Achievements.INC_ACH_SEND_INTERVAL

    // what pointer Id is the one that's steering the elf
    private var activePointerId = -1

    // accumulated play time
    private var playTime = 0.0f

    override val bgmResId = JetpackConfig.BGM_RES_ID

    // current combo
    private inner class Combo {

        internal var items = 0
        internal var countdown = 0.0f
        internal var centroidX: Float = 0.toFloat()
        internal var centroidY: Float = 0.toFloat()
        internal var points: Float = 0.toFloat()
        internal var timeRecovery: Float = 0.toFloat()

        internal fun reset() {
            items = 0
            timeRecovery = 0.0f
            points = timeRecovery
            centroidY = points
            centroidX = centroidY
            countdown = centroidX
        }
    }

    override fun makeNewScene(): BaseScene {
        return JetpackScene()
    }

    override fun onInstall() {
        super.onInstall()
        factory = JetpackObjectFactory(renderer, world)
        factory.requestTextures()
        background = factory.makeBackground()
        background.sendToBack()

        playerObj = factory.makePlayer()
        playerTargetX = 0.0f
        playerTargetY = renderer.bottom + JetpackConfig.Player.VERT_MOVEMENT_MARGIN
        val soundManager = SceneManager.getInstance().soundManager
        itemSfxSuccess = IntArray(3).apply {
            this[0] = soundManager.requestSfx(R.raw.jetpack_score1)
            this[1] = soundManager.requestSfx(R.raw.jetpack_score2)
            this[2] = soundManager.requestSfx(R.raw.jetpack_score3)
        }
        SceneManager.getInstance().vibrator
        // start paused
        startGameScreen()
    }

    override fun isGameEnded(): Boolean {
        return gameEnded
    }

    override fun doFrame(deltaT: Float) {
        if (!paused) {
            if (!gameEnded) {
                playTime += deltaT
                updatePlayer(deltaT)
                if (!playerHit) {
                    detectCollectedItems()
                }
                updatePlayerHit(deltaT)
                updateTimeRemaining(deltaT)
                updateCombo(deltaT)
                checkLevelUp()
                achPendingSeconds += deltaT
            }
            updateClouds()
            updateCandy(deltaT)
            killMissedPresents()

            incAchCountdown -= deltaT
            sendIncrementalAchievements(false)

            spawnCountdown -= deltaT
            if (!isInGameEndingTransition && spawnCountdown < 0.0f) {
                spawnCountdown = JetpackConfig.Items.SPAWN_INTERVAL
                factory.makeRandomItem(
                        fallMult,
                        SceneManager.getInstance().largePresentMode,
                        displayedScore.value)
            }
            super.doFrame(deltaT)
        }
        if (inStartCountdown) {
            updatePlayer(deltaT)
            val newCount = startCountdownTimeRemaining - deltaT
            if (newCount <= 0) {
                inStartCountdown = false
                unPauseGame()
            } else if (newCount.toInt() < startCountdownTimeRemaining.toInt()) {
                digitFactory.setDigit(countdownDigitObj, Math.min(newCount.toInt() + 1, 3))
                countdownDigitObj?.bringToFront()
            }
            startCountdownTimeRemaining = newCount
        }
        if (gameEnded) {
            goToEndGame()
        }
    }

    override fun endGame() {
        isInGameEndingTransition = true

        // force send all incremental achievements
        sendIncrementalAchievements(true)

        // submit our score
        submitScore(JetpackConfig.LEADERBOARD, score)

        // Start end game activity
        onWidgetTriggered(BaseScene.MSG_GO_TO_END_GAME)
    }

    private fun updateTimeRemaining(deltaT: Float) {
        displayedTime -= deltaT
        if (displayedTime < 0.0f && !isInGameEndingTransition) {
            endGame()
        }
    }

    private fun updatePlayerHit(deltaT: Float) {
        playerHitTime -= deltaT
        if (playerHitTime < 0.0f && playerHit) {
            factory.recoverPlayerHit(playerObj)
            playerHit = false
        }
    }

    private fun sineWave(period: Float, amplitude: Float, t: Float): Float {
        return Math.sin(2.0 * Math.PI * t.toDouble() / period).toFloat() * amplitude
    }

    private fun updatePlayer(deltaT: Float) {
        spriteAngle.target = (playerObj.x - playerTargetX) *
                JetpackConfig.Player.SpriteAngle.ANGLE_CONST
        spriteAngle.update(deltaT)
        playerObj.getSprite(0)!!.rotation = spriteAngle.value
        playerObj.getSprite(1)!!.rotation = spriteAngle.value
        playerObj.getSprite(1)!!.width = JetpackConfig.Player.Fire.WIDTH * (1.0f + sineWave(
                JetpackConfig.Player.Fire.ANIM_PERIOD,
                JetpackConfig.Player.Fire.ANIM_AMPLITUDE,
                playTime))
        playerObj.getSprite(1)!!.height = java.lang.Float.NaN // proportional to width

        if (isTv) {
            // On TV, player moves based on its speed.
            playerObj.displaceBy(playerObj.velX * deltaT, playerObj.velY * deltaT)
        } else {
            playerObj.displaceTowards(
                    playerTargetX, playerTargetY, deltaT * JetpackConfig.Player.MAX_SPEED)
        }
    }

    private fun updateClouds() {
        var i = 0
        while (i < cloudObj.size) {
            var o: GameObject? = cloudObj[i]
            if (o == null) {
                o = factory.makeCloud()
                cloudObj[i] = o
                setupNewCloud(o)
            } else if (o.y < JetpackConfig.Clouds.DELETE_Y) {
                setupNewCloud(o)
            }
            i++
        }
    }

    private fun updateCombo(deltaT: Float) {
        combo.countdown -= deltaT
        if (combo.items > 0 && combo.countdown <= 0.0f) {
            endCombo()
        }
    }

    private fun isCandy(o: GameObject): Boolean {
        return o.type == TYPE_GOOD_ITEM && o.ivar[JetpackConfig.Items.IVAR_TYPE] == JetpackObjectFactory.ITEM_CANDY
    }

    private fun isPresent(o: GameObject): Boolean {
        return o.type == TYPE_GOOD_ITEM && o.ivar[JetpackConfig.Items.IVAR_TYPE] == JetpackObjectFactory.ITEM_PRESENT
    }

    private fun isCoal(o: GameObject): Boolean {
        return o.type == TYPE_BAD_ITEM && o.ivar[JetpackConfig.Items.IVAR_TYPE] == JetpackObjectFactory.ITEM_COAL
    }

    private fun updateCandy(deltaT: Float) {
        var i = 0
        while (i < world.gameObjects.size) {
            val o = world.gameObjects[i]
            if (isCandy(o)) {
                o.getSprite(0)!!.rotation += deltaT * JetpackConfig.Items.CANDY_ROTATE_SPEED
            }
            i++
        }
    }

    private fun setupNewCloud(o: GameObject) {
        o.displaceTo(
                renderer.left + random.nextFloat() * (renderer.right - renderer.left),
                JetpackConfig.Clouds.SPAWN_Y)
        o.velY =
                -(JetpackConfig.Clouds.SPEED_MIN + random.nextFloat() * (JetpackConfig.Clouds.SPEED_MAX - JetpackConfig.Clouds.SPEED_MIN))
    }

    private fun killMissedPresents() {
        var i = 0
        while (i < world.gameObjects.size) {
            val o = world.gameObjects[i]
            if ((o.type == TYPE_GOOD_ITEM || o.type == TYPE_BAD_ITEM) && o.y < JetpackConfig.Items.DELETE_Y) {
                o.dead = true
            }
            i++
        }
    }

    private fun addScore(score: Float) {
        this.score += score.toInt()
        unlockScoreBasedAchievements()
    }

    private fun addTime(time: Float) {
        displayedTime += time
        if (displayedTime > JetpackConfig.Time.MAX) {
            displayedTime = JetpackConfig.Time.MAX
        }
    }

    private fun pickUpItem(item: GameObject) {
        val value = item.ivar[JetpackConfig.Items.IVAR_BASE_VALUE]
        if (isCoal(item)) {
            factory.makePlayerHit(playerObj)
            playerHit = true
            playerHitTime = JetpackConfig.Time.HIT_TIME
            objectFactory.makeScorePopup(item.x, item.y, value, digitFactory)
            SceneManager.getInstance()
                    .soundManager
                    .playSfx(itemSfxSuccess[random.nextInt(itemSfxSuccess.size)])
            SceneManager.getInstance().vibrator.vibrate(JetpackConfig.Time.VIBRATE_SMALL)
            addTime(-JetpackConfig.Time.COAL_TIME_PENALTY)
        } else {
            if (isCandy(item)) {
                achPendingCandy++
            } else if (isPresent(item)) {
                achPendingPresents++
            }
            objectFactory.makeScorePopup(item.x, item.y, value, digitFactory)
            SceneManager.getInstance()
                    .soundManager
                    .playSfx(itemSfxSuccess[random.nextInt(itemSfxSuccess.size)])

            val timeRecovery = Math.max(JetpackConfig.Time.RECOVERED_BY_ITEM -
                    level * JetpackConfig.Time.RECOVERED_DECREASE_PER_LEVEL,
                    JetpackConfig.Time.RECOVERED_MIN)
            addTime(timeRecovery)

            combo.centroidX = (combo.centroidX * combo.items + item.x) / (combo.items + 1)
            combo.centroidY = (combo.centroidY * combo.items + item.y) / (combo.items + 1)
            combo.items++
            combo.countdown = JetpackConfig.Items.COMBO_INTERVAL
            combo.points += value
            combo.timeRecovery = timeRecovery
        }
        item.dead = true
        itemsCollected++

        addScore(value.toFloat())

        // play sfx
    }

    private fun detectCollectedItems() {
        world.detectCollisions(playerObj, tmpList, true)
        var i = 0
        while (i < tmpList.size) {
            val o = tmpList[i]
            if (o.type == TYPE_GOOD_ITEM || o.type == TYPE_BAD_ITEM) {
                pickUpItem(o)
            }
            i++
        }
    }

    private fun endCombo() {
        if (combo.items > 1) {
            factory.makeComboPopup(combo.items, combo.centroidX, combo.centroidY)

            // give bonus
            addScore(combo.points * combo.items)
            addTime(combo.timeRecovery * combo.items)

            // unlock combo-based achievements
            unlockComboBasedAchievements(combo.items)
        }
        combo.reset()
    }

    override fun onPointerDown(pointerId: Int, x: Float, y: Float) {
        super.onPointerDown(pointerId, x, y)
        if (activePointerId < 0) {
            activePointerId = pointerId
        }
    }

    override fun onPointerUp(pointerId: Int, x: Float, y: Float) {
        super.onPointerUp(pointerId, x, y)
        if (activePointerId == pointerId) {
            activePointerId = -1
        }
    }

    override fun onPointerMove(pointerId: Int, x: Float, y: Float, deltaX: Float, deltaY: Float) {
        super.onPointerMove(pointerId, x, y, deltaX, deltaY)

        // if paused, do nothing.
        if (paused) {
            return
        }

        // if no finger owns the steering of the elf, adopt this one.
        if (activePointerId < 0) {
            activePointerId = pointerId
        }
    }

    override fun onKeyDown(keyCode: Int, repeatCount: Int) {

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BUTTON_A -> onConfirmKeyPressed()
            KeyEvent.KEYCODE_BUTTON_B -> onBackKeyPressed()
        }

        if (!paused) {
            val absVelocity =
                    JetpackConfig.Player.WIDTH * (1.5f + repeatCount.toFloat() * JetpackConfig.Input.KEY_SENSIVITY)
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT -> {
                    playerTargetX -= 0.17f
                    playerObj.velX = -absVelocity
                }
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    playerTargetX += 0.17f
                    playerObj.velX = absVelocity
                }
            }
            // don't let the player wander off screen
            limitPlayerMovement()
        }
    }

    override fun onKeyUp(keyCode: Int) {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                // if it's going up, stop it.
                playerTargetY = playerObj.y
                if (playerObj.velY > 0) {
                    playerObj.velY = 0f
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // if it's going down, stop it.
                playerTargetY = playerObj.y
                if (playerObj.velY < 0) {
                    playerObj.velY = 0f
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                // if it's going left, stop it.
                playerTargetX = playerObj.x
                if (playerObj.velX < 0) {
                    playerObj.velX = 0f
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // if it's going right, stop it.
                playerTargetX = playerObj.x
                if (playerObj.velX > 0) {
                    playerObj.velX = 0f
                }
            }
        }

        // don't let the player wander off screen
        limitPlayerMovement()
    }

    override fun onSensorChanged(x: Float, y: Float, accuracy: Int) {
        playerTargetX += JetpackConfig.Input.Sensor.transformX(x)
        // don't let the player wander off screen
        limitPlayerMovement()
    }

    private fun limitPlayerMovement() {
        val minX = renderer.left + JetpackConfig.Player.HORIZ_MOVEMENT_MARGIN
        val maxX = renderer.right - JetpackConfig.Player.HORIZ_MOVEMENT_MARGIN
        val minY = renderer.bottom + JetpackConfig.Player.VERT_MOVEMENT_MARGIN
        val maxY = renderer.top - JetpackConfig.Player.VERT_MOVEMENT_MARGIN

        if (isTv) {
            playerObj.velX = when {
                playerObj.x + playerObj.velX < minX -> minX - playerObj.x
                playerObj.x + playerObj.velX > maxX -> maxX - playerObj.x
                else -> playerObj.velX
            }

            playerObj.velY = when {
                playerObj.y + playerObj.velY < minY -> minY - playerObj.y
                playerObj.y + playerObj.velY > maxY -> maxY - playerObj.y
                else -> playerObj.velY
            }
        } else {
            playerTargetX =
                    if (playerTargetX < minX) minX else if (playerTargetX > maxX) maxX else playerTargetX
            playerTargetY =
                    if (playerTargetY < minY) minY else if (playerTargetY > maxY) maxY else playerTargetY
        }
    }

    private fun checkLevelUp() {
        val dueLevel = itemsCollected / JetpackConfig.Progression.ITEMS_PER_LEVEL
        while (level < dueLevel) {
            level++
            Logger.d("Level up! Now at level $level")
            fallMult *= JetpackConfig.Items.FALL_SPEED_LEVEL_MULT
            scoreMult *= JetpackConfig.Progression.SCORE_LEVEL_MULT
        }
    }

    private fun unlockScoreBasedAchievements() {
        var i = 0
        while (i < JetpackConfig.Achievements.SCORE_ACHS.size) {
            if (score >= JetpackConfig.Achievements.SCORE_FOR_ACH[i]) {
                unlockAchievement(JetpackConfig.Achievements.SCORE_ACHS[i])
            }
            i++
        }
    }

    private fun unlockComboBasedAchievements(comboSize: Int) {
        var i = 0
        while (i < JetpackConfig.Achievements.COMBO_ACHS.size) {
            // COMBO_ACHS[n] is the achievement to unlock for a combo of size n + 2
            if (comboSize >= i + 2) {
                unlockAchievement(JetpackConfig.Achievements.COMBO_ACHS[i])
            }
            i++
        }
    }

    private fun sendIncrementalAchievements(force: Boolean) {
        if (!force && incAchCountdown > 0.0f) {
            // it's not time to send yet
            return
        }
        if (SceneManager.getInstance().activity == null) {
            // no Activity (maybe we're in the background), so can't send yet
            return
        }

        if (achPendingCandy > 0) {
            incrementAchievements(JetpackConfig.Achievements.TOTAL_CANDY_ACHS, achPendingCandy)
            achPendingCandy = 0
        }
        if (achPendingPresents > 0) {
            incrementAchievements(
                    JetpackConfig.Achievements.TOTAL_PRESENTS_ACHS, achPendingPresents)
            achPendingPresents = 0
        }
        if (achPendingSeconds >= 1.0f) {
            val seconds = Math.floor(achPendingSeconds.toDouble()).toInt()
            incrementAchievements(JetpackConfig.Achievements.TOTAL_TIME_ACHS, seconds)
            achPendingSeconds -= seconds.toFloat()
        }

        // submit score as well, since we're at it.
        submitScore(JetpackConfig.LEADERBOARD, score)

        // reset countdown
        incAchCountdown = JetpackConfig.Achievements.INC_ACH_SEND_INTERVAL
    }

    private fun unlockAchievement(resId: Int) {
        val act = SceneManager.getInstance().activity as SceneActivity
        if (!unlockedAchievements.contains(resId)) {
            act.postUnlockAchievement(resId)
            unlockedAchievements.add(resId)
        }
    }

    private fun incrementAchievements(resId: IntArray, steps: Int) {
        for (i in resId) {
            incrementAchievement(i, steps)
        }
    }

    private fun incrementAchievement(resId: Int, steps: Int) {
        val act = SceneManager.getInstance().activity as SceneActivity
        if (steps > 0) {
            act.postIncrementAchievement(resId, steps)
        }
    }

    private fun submitScore(resId: Int, score: Int) {
        val act = SceneManager.getInstance().activity as SceneActivity
        act.postSubmitScore(resId, score.toLong())
    }

    companion object {
        // GameObject types:
        internal const val TYPE_PLAYER = 0
        internal const val TYPE_GOOD_ITEM = 1
        internal const val TYPE_BAD_ITEM = 2
    }
}
