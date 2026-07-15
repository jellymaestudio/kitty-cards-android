package kittycards.kittycardsandroid.logic

import android.annotation.SuppressLint
import kittycards.kittycardsandroid.components.IGameController
import kittycards.kittycardsandroid.components.INetworkManager
import kittycards.kittycardsandroid.model.Player
import kittycards.kittycardsandroid.network.GameAction
import kittycards.kittycardsandroid.network.NetworkDevice
import kittycards.kittycardsandroid.network.Role
import kittycards.kittycardsandroid.network.event.NetworkEvent
import javax.inject.Inject

/**
 * Controls the lifecycle and state of the host lobby.
 *
 * The controller starts and closes hosted rooms, manages available guests,
 * coordinates the match-start handshake and prepares the game controller.
 *
 * It does not access Android views and does not perform navigation.
 *
 * @author JellyMae
 */
class HostLobbyController @Inject constructor(
    private val networkManager: INetworkManager,
    private val gameController: IGameController
) {

    /**
     * Called whenever the visible host-lobby state changes.
     */
    var onStateChanged: ((HostLobbyState) -> Unit)? = null

    /**
     * Called when the match has been prepared and the game screen can open.
     */
    var onOpenGameRequested: (() -> Unit)? = null

    /**
     * Called after the hosted room has been closed.
     */
    var onCloseScreenRequested: (() -> Unit)? = null

    /**
     * Called when an error should be displayed by the UI.
     */
    var onError: ((String) -> Unit)? = null

    /**
     * Called when a non-fatal warning should be displayed by the UI.
     */
    var onWarning: ((String) -> Unit)? = null

    private val availableGuests = mutableListOf<NetworkDevice>()

    private var selectedGuest: NetworkDevice? = null

    private var hostingStarted = false
    private var leavingScreen = false
    private var matchStartRequested = false

    @Volatile
    private var lobbyListenerRunning = false

    private var lobbyListenerThread: Thread? = null

    /**
     * Registers the required network listeners and publishes the initial state.
     */
    fun initialize() {
        setupNetworkEventListener()
        publishState()
    }

    /**
     * Starts hosting a discoverable match room.
     */
    @SuppressLint("MissingPermission")
    fun startHosting() {
        if (hostingStarted || leavingScreen) {
            return
        }

        hostingStarted = true

        try {
            networkManager.hostMatch { connectedGuests ->
                updateAvailableGuests(connectedGuests)
            }
        } catch (_: SecurityException) {
            hostingStarted = false

            onError?.invoke(
                "Bluetooth permission is missing."
            )
        }
    }

    /**
     * Accepts a guest for the hosted match.
     *
     * @param guest guest selected by the host
     */
    @SuppressLint("MissingPermission")
    fun selectGuest(guest: NetworkDevice) {
        if (
            selectedGuest != null ||
            leavingScreen ||
            matchStartRequested
        ) {
            return
        }

        try {
            networkManager.selectGuest(guest)
        } catch (_: SecurityException) {
            onError?.invoke(
                "Bluetooth permission is missing."
            )
            return
        }

        selectedGuest = guest
        availableGuests.remove(guest)

        publishState()
    }

    /**
     * Starts the match handshake with the selected guest.
     */
    @SuppressLint("MissingPermission")
    fun requestMatchStart() {
        if (
            selectedGuest == null ||
            matchStartRequested ||
            leavingScreen
        ) {
            return
        }

        matchStartRequested = true
        publishState()

        try {
            /*
             * The room must disappear from other guests' discovery results,
             * while the selected guest connection remains active.
             */
            networkManager.stopRoomDiscovery()

            /*
             * The host must listen for MATCH_READY before sending START_MATCH.
             */
            startLobbyActionListener()

            networkManager.sendGameChange(
                GameAction(
                    GameAction.ActionType.START_MATCH
                )
            )
        } catch (_: SecurityException) {
            matchStartRequested = false
            publishState()

            onError?.invoke(
                "Bluetooth permission is missing."
            )
        } catch (_: IllegalStateException) {
            matchStartRequested = false
            publishState()

            onError?.invoke(
                "The match could not be started."
            )
        }
    }

    /**
     * Closes the hosted room and requests that the UI close the screen.
     */
    @SuppressLint("MissingPermission")
    fun closeRoom() {
        if (leavingScreen) {
            return
        }

        leavingScreen = true
        stopLobbyActionListener()

        try {
            networkManager.closeRoom()
        } catch (_: SecurityException) {
            /*
             * The screen can still close when the permission was revoked.
             */
        } finally {
            hostingStarted = false
            onCloseScreenRequested?.invoke()
        }
    }

    /**
     * Removes listeners and stops background work owned by this controller.
     *
     * This does not close the room because the Activity may be destroyed while
     * navigating into an active match.
     */
    fun cleanup() {
        stopLobbyActionListener()

        networkManager.setNetworkEventListener(null)

        onStateChanged = null
        onOpenGameRequested = null
        onCloseScreenRequested = null
        onError = null
        onWarning = null
    }

    private fun updateAvailableGuests(
        connectedGuests: List<NetworkDevice>
    ) {
        val acceptedGuest = selectedGuest

        if (
            acceptedGuest != null &&
            connectedGuests.none { guest ->
                guest == acceptedGuest
            }
        ) {
            selectedGuest = null
            matchStartRequested = false
        }

        availableGuests.clear()
        availableGuests.addAll(
            connectedGuests.filter { guest ->
                guest != selectedGuest
            }
        )

        publishState()
    }

    private fun startLobbyActionListener() {
        if (lobbyListenerRunning) {
            return
        }

        lobbyListenerRunning = true

        lobbyListenerThread = Thread {
            while (lobbyListenerRunning) {
                try {
                    val action =
                        networkManager.fetchNextAction()

                    if (
                        action.type() ==
                        GameAction.ActionType.MATCH_READY
                    ) {
                        lobbyListenerRunning = false
                        handleMatchReady()
                        break
                    }
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }.apply {
            name = "HostLobbyActionListener"
            start()
        }
    }

    private fun stopLobbyActionListener() {
        lobbyListenerRunning = false

        lobbyListenerThread?.let { thread ->
            if (thread != Thread.currentThread()) {
                thread.interrupt()
            }
        }

        lobbyListenerThread = null
    }

    private fun handleMatchReady() {
        if (
            leavingScreen ||
            !matchStartRequested
        ) {
            return
        }

        lobbyListenerThread = null

        val hostPlayer = Player(
            0,
            "Host"
        )

        val guestPlayer = Player(
            1,
            "Guest"
        )

        gameController.setNetworkRole(Role.HOST)
        gameController.setLocalPlayer(hostPlayer)

        gameController.startMatch(
            hostPlayer,
            guestPlayer
        )

        /*
         * From this point onward, only the GameController consumes
         * incoming gameplay actions.
         */
        gameController.startListeningForActions()

        onOpenGameRequested?.invoke()
    }

    private fun setupNetworkEventListener() {
        networkManager.setNetworkEventListener { event ->
            when (event.type) {
                NetworkEvent.NetworkMessageType.ERROR -> {
                    onError?.invoke(event.message)
                }

                NetworkEvent.NetworkMessageType.WARNING -> {
                    onWarning?.invoke(event.message)
                }

                NetworkEvent.NetworkMessageType.INFO -> {
                    // No visible message is required.
                }
            }
        }
    }

    private fun publishState() {
        onStateChanged?.invoke(
            HostLobbyState(
                availableGuests =
                    availableGuests.toList(),
                selectedGuest = selectedGuest,
                matchStartRequested =
                    matchStartRequested
            )
        )
    }
}