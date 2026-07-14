package kittycards.kittycardsandroid.logic

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import kittycards.kittycardsandroid.network.GameAction
import kittycards.kittycardsandroid.network.NetworkManager

import javax.inject.Inject

/**
 * Coordinates the lifecycle of an active network game session.
 *
 * This controller is responsible for stopping game communication,
 * disconnecting the network session and resetting the game controller.
 *
 * It does not access Android views and does not perform navigation.
 *
 * @author JellyMae
 */
class GameSessionController @Inject constructor(
    private val gameController: GameController,
    private val networkManager: NetworkManager
) {

    /**
     * Called when the remote player disconnects.
     *
     * The UI can use this callback to display an appropriate message.
     */
    var onOpponentDisconnected: (() -> Unit)? = null

    /**
     * Called after the game session has been completely cleaned up.
     *
     * The Activity can use this callback to navigate back to the lobby.
     */
    var onSessionClosed: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())

    private var sessionEnding = false

    /**
     * Returns whether the current session is already being closed.
     */
    fun isSessionEnding(): Boolean {
        return sessionEnding
    }

    /**
     * Ends a regularly completed match after the supplied delay.
     *
     * The delay allows the UI to display the match result first.
     *
     * @param delayMillis delay before cleaning up the session
     */
    fun finishRegularSession(
        delayMillis: Long = 3_000L
    ) {
        if (sessionEnding) {
            return
        }

        sessionEnding = true

        handler.postDelayed(
            {
                cleanupSession()
                onSessionClosed?.invoke()
            },
            delayMillis
        )
    }

    /**
     * Handles a lost connection to the remote player.
     *
     * The opponent-disconnected callback is invoked immediately.
     * Session cleanup follows after the supplied delay.
     *
     * @param delayMillis delay before cleaning up the session
     */
    fun handleRemoteDisconnect(
        delayMillis: Long = 2_000L
    ) {
        if (sessionEnding) {
            return
        }

        sessionEnding = true

        /*
         * Stop the game action listener immediately so that it cannot
         * consume actions from a later lobby or match session.
         */
        gameController.stopListeningForActions()

        onOpponentDisconnected?.invoke()

        handler.postDelayed(
            {
                cleanupSession()
                onSessionClosed?.invoke()
            },
            delayMillis
        )
    }

    /**
     * Aborts the current match because the local player leaves the game.
     *
     * MATCH_ABORTED is sent first. The local session is cleaned up shortly
     * afterwards so the BLE queue has time to transmit the action.
     */
    @SuppressLint("MissingPermission")
    fun abortLocalSession(
        sendDelayMillis: Long = 400L
    ) {
        if (sessionEnding) {
            return
        }

        sessionEnding = true

        try {
            networkManager.sendGameChange(
                GameAction(
                    GameAction.ActionType.MATCH_ABORTED
                )
            )
        } catch (_: SecurityException) {
            // Continue with local cleanup.
        } catch (_: IllegalStateException) {
            // The connection may already be unavailable.
        }

        handler.postDelayed(
            {
                cleanupSession()
                onSessionClosed?.invoke()
            },
            sendDelayMillis
        )
    }

    /**
     * Immediately cleans up the current game session.
     *
     * This method is shared by regular match completion and connection loss.
     */
    @SuppressLint("MissingPermission")
    private fun cleanupSession() {
        /*
         * Remove the network callback before disconnecting intentionally.
         * Otherwise the local disconnect could be reported as another
         * unexpected connection loss.
         */
        networkManager.setGameConnectionListener(null)

        gameController.stopListeningForActions()

        try {
            networkManager.disconnect()
        } catch (_: SecurityException) {
            /*
             * The local game session must still be reset when the Bluetooth
             * permission was revoked while the match was running.
             */
        }

        gameController.resetSession()
    }

    /**
     * Removes pending callbacks and references held by this controller.
     *
     * Call this when the owning Activity is destroyed.
     */
    fun cleanup() {
        handler.removeCallbacksAndMessages(null)

        onOpponentDisconnected = null
        onSessionClosed = null
    }
}