package kittycards.kittycardsandroid.logic

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import kittycards.kittycardsandroid.components.IGameController
import kittycards.kittycardsandroid.components.INetworkManager
import kittycards.kittycardsandroid.model.Player
import kittycards.kittycardsandroid.network.GameAction
import kittycards.kittycardsandroid.network.NetworkDevice
import kittycards.kittycardsandroid.network.OnRoomConnectionListener
import kittycards.kittycardsandroid.network.Role
import kittycards.kittycardsandroid.network.event.NetworkEvent
import javax.inject.Inject

/**
 * Controls the lifecycle and state of the join lobby.
 *
 * The controller discovers hosted rooms, manages the connection and
 * acceptance process, handles lobby actions and prepares the game controller
 * when a match starts.
 *
 * It does not access Android views and does not perform navigation.
 *
 * @author JellyMae
 */
class JoinLobbyController @Inject constructor(
    private val networkManager: INetworkManager,
    private val gameController: IGameController
) {

    /**
     * Called whenever the visible join-lobby state changes.
     */
    var onStateChanged: ((JoinLobbyState) -> Unit)? = null

    /**
     * Called when the game has been prepared and the game screen can open.
     */
    var onOpenGameRequested: (() -> Unit)? = null

    /**
     * Called after the guest has left the lobby.
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

    private val availableRooms =
        mutableListOf<NetworkDevice>()

    private var selectedRoom: NetworkDevice? = null
    private var acceptedRoom: NetworkDevice? = null

    private var scanningStarted = false
    private var leavingScreen = false
    private var resettingRoomConnection = false

    @Volatile
    private var lobbyListenerRunning = false

    private var lobbyListenerThread: Thread? = null

    private val handler =
        Handler(Looper.getMainLooper())

    /**
     * Registers network listeners, starts the lobby action listener
     * and publishes the initial state.
     */
    fun initialize() {
        setupNetworkEventListener()
        setupRoomConnectionListener()
        startLobbyActionListener()
        publishState()
    }

    /**
     * Starts discovering available hosted rooms.
     */
    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (
            scanningStarted ||
            leavingScreen ||
            resettingRoomConnection
        ) {
            return
        }

        scanningStarted = true

        try {
            networkManager.joinMatch { updatedRooms ->
                updateAvailableRooms(updatedRooms)
            }
        } catch (_: SecurityException) {
            scanningStarted = false

            onError?.invoke(
                "Bluetooth permission is missing."
            )
        }
    }

    /**
     * Requests a connection to the selected hosted room.
     *
     * @param room room selected by the guest
     */
    @SuppressLint("MissingPermission")
    fun requestRoomJoin(room: NetworkDevice) {
        if (
            selectedRoom != null ||
            acceptedRoom != null ||
            leavingScreen ||
            resettingRoomConnection
        ) {
            return
        }

        try {
            networkManager.confirmRoom(room)
        } catch (_: SecurityException) {
            onError?.invoke(
                "Bluetooth permission is missing."
            )
            return
        }

        /*
         * The room is only marked as selected after confirmRoom()
         * has been invoked successfully.
         */
        selectedRoom = room
        publishState()
    }

    /**
     * Leaves the current room or scanning session and requests
     * that the UI close the screen.
     */
    @SuppressLint("MissingPermission")
    fun leaveRoom() {
        if (leavingScreen) {
            return
        }

        leavingScreen = true

        stopLobbyActionListener()
        handler.removeCallbacksAndMessages(null)

        try {
            networkManager.disconnect()
        } catch (_: SecurityException) {
            /*
             * The screen can still close when the permission was revoked.
             */
        } finally {
            scanningStarted = false
            onCloseScreenRequested?.invoke()
        }
    }

    /**
     * Removes listeners and stops background work owned by this controller.
     *
     * This method does not disconnect automatically because the Activity
     * may be destroyed while navigating into an active match.
     */
    fun cleanup() {
        stopLobbyActionListener()
        handler.removeCallbacksAndMessages(null)

        networkManager.setNetworkEventListener(null)
        networkManager.setRoomConnectionListener(null)

        onStateChanged = null
        onOpenGameRequested = null
        onCloseScreenRequested = null
        onError = null
        onWarning = null
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

    private fun setupRoomConnectionListener() {
        networkManager.setRoomConnectionListener(
            object : OnRoomConnectionListener {

                override fun onRoomConnected(
                    room: NetworkDevice
                ) {
                    /*
                     * The host is not displayed in the room box yet.
                     * That only happens after GUEST_ACCEPTED is received.
                     */
                }

                override fun onRoomDisconnected() {
                    handleRoomClosed()
                }
            }
        )
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

                    when (action.type()) {
                        GameAction.ActionType.GUEST_ACCEPTED -> {
                            handleGuestAccepted()
                        }

                        GameAction.ActionType.ROOM_CLOSED -> {
                            handleRoomClosed()
                        }

                        GameAction.ActionType.START_MATCH -> {
                            /*
                             * Stop this listener before the GameController
                             * starts consuming from the same network queue.
                             */
                            lobbyListenerRunning = false
                            handleStartMatch()
                            break
                        }

                        else -> {
                            // Other actions are ignored during the lobby phase.
                        }
                    }
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }.apply {
            name = "JoinLobbyActionListener"
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

    private fun handleGuestAccepted() {
        val pendingRoom =
            selectedRoom
                ?: return

        acceptedRoom = pendingRoom
        availableRooms.clear()

        publishState()
    }

    @SuppressLint("MissingPermission")
    private fun handleStartMatch() {
        if (leavingScreen) {
            return
        }

        if (acceptedRoom == null) {
            onError?.invoke(
                "The match cannot start because no host was accepted."
            )
            return
        }

        /*
         * The lobby listener stopped after receiving START_MATCH.
         * Only the stored thread reference must be cleared here.
         */
        lobbyListenerThread = null

        val hostPlayer = Player(
            0,
            "Host"
        )

        val guestPlayer = Player(
            1,
            "Guest"
        )

        gameController.setNetworkRole(Role.GUEST)
        gameController.setLocalPlayer(guestPlayer)

        /*
         * On the guest device this only initializes the local match.
         * Board colors and cards are received from the host afterwards.
         */
        gameController.startMatch(
            hostPlayer,
            guestPlayer
        )

        /*
         * From this point onward only the GameController consumes
         * incoming gameplay actions.
         */
        gameController.startListeningForActions()

        try {
            networkManager.sendGameChange(
                GameAction(
                    GameAction.ActionType.MATCH_READY
                )
            )
        } catch (_: SecurityException) {
            gameController.stopListeningForActions()

            onError?.invoke(
                "Bluetooth permission is missing."
            )
            return
        } catch (_: IllegalStateException) {
            gameController.stopListeningForActions()

            onError?.invoke(
                "The match could not be started."
            )
            return
        }

        onOpenGameRequested?.invoke()
    }

    @SuppressLint("MissingPermission")
    private fun handleRoomClosed() {
        if (
            leavingScreen ||
            resettingRoomConnection
        ) {
            return
        }

        resettingRoomConnection = true

        selectedRoom = null
        acceptedRoom = null
        availableRooms.clear()
        scanningStarted = false

        publishState()

        /*
         * ROOM_CLOSED may arrive before Android reports the physical
         * connection loss. Disconnect explicitly so the previous GATT
         * connection cannot remain internally active.
         */
        try {
            networkManager.disconnect()
        } catch (_: SecurityException) {
            /*
             * Continue resetting the lobby state even without permission.
             */
        }

        handler.postDelayed(
            {
                if (!leavingScreen) {
                    resettingRoomConnection = false
                    startScanning()
                }
            },
            500L
        )
    }

    private fun updateAvailableRooms(
        updatedRooms: List<NetworkDevice>
    ) {
        if (
            leavingScreen ||
            resettingRoomConnection
        ) {
            return
        }

        if (selectedRoom == null) {
            availableRooms.clear()
            availableRooms.addAll(updatedRooms)
        } else {
            /*
             * After selecting a room, scanning may stop. Keep the pending
             * room visible so the UI can continue displaying "...".
             */
            val pendingRoom = selectedRoom

            availableRooms.clear()
            availableRooms.addAll(updatedRooms)

            if (
                pendingRoom != null &&
                availableRooms.none { room ->
                    room == pendingRoom
                }
            ) {
                availableRooms.add(pendingRoom)
            }
        }

        publishState()
    }

    private fun publishState() {
        onStateChanged?.invoke(
            JoinLobbyState(
                availableRooms =
                    availableRooms.toList(),
                selectedRoom = selectedRoom,
                acceptedRoom = acceptedRoom
            )
        )
    }
}