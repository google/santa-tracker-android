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

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.core.os.postDelayed
import com.google.android.apps.jetpack.JetpackActivity.Companion.JETPACK_SCORE
import com.google.android.apps.playgames.common.PlayGamesActivity
import com.google.android.apps.playgames.simpleengine.Scene
import com.google.android.apps.playgames.simpleengine.SceneManager
import com.google.android.apps.santatracker.games.EndOfGameView
import com.google.android.apps.santatracker.invites.AppInvitesFragment

abstract class SceneActivity : PlayGamesActivity() {

    private var invitesFragment: AppInvitesFragment? = null
    private var gameEndedListener: GameEndedListener? = null

    protected abstract val gameScene: Scene

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            SceneManager.getInstance().requestNewScene(gameScene)
        }

        invitesFragment = AppInvitesFragment.getInstance(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        SceneManager.getInstance().onFocusChanged(hasFocus)
    }

    public override fun onPause() {
        super.onPause()
        SceneManager.getInstance().onPause()
    }

    public override fun onResume() {
        super.onResume()
        SceneManager.getInstance().onResume(this)
        if (SceneManager.getInstance().currentScene?.isGameEnded == true) {
            postGoToEndGame()
        }
    }

    override fun onSignInFailed() {
        super.onSignInFailed()

        // communicate to the BaseScene that we are no longer signed in
        val s = SceneManager.getInstance().currentScene
        if (s is BaseScene) {
            s.setSignedIn(false)
        }
    }

    override fun onSignInSucceeded() {
        super.onSignInSucceeded()

        // communicate to the BaseScene that we are no longer signed in
        val s = SceneManager.getInstance().currentScene
        if (s is BaseScene) {
            s.setSignedIn(true)
        }
    }

    override fun launchStartupActivity() {
        finish()
    }

    fun postQuitGame() {
        runOnUiThread { launchStartupActivity() }
    }

    fun postReturnWithScore(score: Int) {
        runOnUiThread { returnWithScore(score) }
    }

    private fun returnWithScore(score: Int) {
        val intent = this.intent
        intent.putExtra(JETPACK_SCORE, score)
        this.setResult(Activity.RESULT_OK, intent)
        finish()
    }

    fun setGameEndedListener(gameEndedListener: GameEndedListener) {
        this.gameEndedListener = gameEndedListener
    }

    fun postDelayedGoToEndGame(delay: Int) {
        runOnUiThread {
            Handler().postDelayed(delay.toLong()) {
                gameEndedListener?.let {
                    it.onGameEnded()
                    goToEndGame(it.score)
                }
            }
        }
    }

    fun postGoToEndGame() {
        runOnUiThread {
            gameEndedListener?.let {
                val score = it.score
                it.onGameEnded()
                goToEndGame(score)
            }
        }
    }

    private fun replay() {
        val gameView = findViewById<EndOfGameView>(R.id.jetpack_end_game_view)
        gameView.visibility = View.INVISIBLE
        SceneManager.getInstance().requestNewScene(gameScene)
    }

    private fun goToEndGame(score: Int) {
        // Show the end-game view
        val gameView = findViewById<EndOfGameView>(R.id.jetpack_end_game_view)
        gameView.initialize(
                score,
                { replay() },
                { returnWithScore(score) })
        gameView.visibility = View.VISIBLE
    }

    fun share() {
        invitesFragment?.sendGenericInvite()
    }
}
