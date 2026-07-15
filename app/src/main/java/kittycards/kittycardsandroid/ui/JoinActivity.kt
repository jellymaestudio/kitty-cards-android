package kittycards.kittycardsandroid.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kittycards.kittycardsandroid.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kittycards.kittycardsandroid.components.IGameController
import kittycards.kittycardsandroid.components.INetworkManager
import kittycards.kittycardsandroid.model.Player
import kittycards.kittycardsandroid.network.GameAction
import kittycards.kittycardsandroid.network.NetworkDevice
import kittycards.kittycardsandroid.network.event.NetworkEvent
import kittycards.kittycardsandroid.ui.util.GameColorMapper
import kittycards.kittycardsandroid.network.OnRoomConnectionListener
import kittycards.kittycardsandroid.network.Role

@AndroidEntryPoint
class JoinActivity : AppCompatActivity() {

    @Inject lateinit var networkManager: INetworkManager
    @Inject lateinit var gameController: IGameController

    private val availableRooms = mutableListOf<NetworkDevice>()

    /*
     * The room to which a connection is currently being established.
     *
     * Important:
     * This host is not yet automatically part of the room box.
     * That happens later, after GUEST_ACCEPTED.
     */
    private var selectedRoom: NetworkDevice? = null
    private var acceptedRoom: NetworkDevice? = null

    @Volatile
    private var lobbyListenerRunning = false

    private var lobbyListenerThread: Thread? = null

    private lateinit var roomPlayersContainer: LinearLayout
    private lateinit var availableRoomsContainer: LinearLayout

    private var scanningStarted = false
    private var leavingScreen = false
    private var resettingRoomConnection = false

    private val bluetoothPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = requiredBluetoothPermissions()
                .all { permission -> permissions[permission] == true }

            if (allGranted) {
                startScanning()
            } else {
                Toast.makeText(
                    this,
                    "Bluetooth permissions are required to join a game.",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_join)

        // networkManager is injected

        bindViews()
        applyWindowInsets()
        setupClickListeners()
        setupBackNavigation()
        setupNetworkEventListener()
        setupRoomConnectionListener()
        startLobbyActionListener()

        renderRoom()
        renderAvailableRooms()

        checkPermissionsAndStartScanning()
    }

    private fun bindViews() {
        roomPlayersContainer = findViewById(R.id.roomPlayersContainer)
        availableRoomsContainer = findViewById(R.id.availableRoomsContainer)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.joinRoot)
        ) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                systemBars.left + 24,
                systemBars.top + 24,
                systemBars.right + 24,
                systemBars.bottom + 24
            )

            insets
        }
    }

    private fun setupClickListeners() {
        findViewById<TextView>(R.id.backButton).setOnClickListener {
            leaveRoomAndReturn()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    leaveRoomAndReturn()
                }
            }
        )
    }

    private fun setupNetworkEventListener() {
        networkManager.setNetworkEventListener { event ->
            when (event.type) {
                NetworkEvent.NetworkMessageType.ERROR -> {
                    Toast.makeText(
                        this,
                        event.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                NetworkEvent.NetworkMessageType.WARNING -> {
                    Toast.makeText(
                        this,
                        event.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                NetworkEvent.NetworkMessageType.INFO -> {
                    // No visible message required.
                }
            }
        }
    }

    private fun setupRoomConnectionListener() {
        networkManager.setRoomConnectionListener(
            object : OnRoomConnectionListener {

                override fun onRoomConnected(room: NetworkDevice) {
                    /*
                     * No visible changes are necessary in Phase A.
                     * The host is not displayed in the
                     * Room box until after GUEST_ACCEPTED.
                     */
                }

                override fun onRoomDisconnected() {
                    handleRoomClosed()
                }
            }
        )
    }

    private fun checkPermissionsAndStartScanning() {
        val missingPermissions = requiredBluetoothPermissions()
            .filter { permission ->
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            }

        if (missingPermissions.isEmpty()) {
            startScanning()
        } else {
            bluetoothPermissionLauncher.launch(
                missingPermissions.toTypedArray()
            )
        }
    }

    private fun requiredBluetoothPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun startScanning() {
        if (scanningStarted) {
            return
        }

        scanningStarted = true

        try {
            networkManager.joinMatch { updatedRooms ->
                runOnUiThread {
                    updateAvailableRooms(updatedRooms)
                }
            }
        } catch (_: SecurityException) {
            scanningStarted = false

            Toast.makeText(
                this,
                "Bluetooth permission is missing.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun startLobbyActionListener() {
        if (lobbyListenerRunning) {
            return
        }

        lobbyListenerRunning = true

        lobbyListenerThread = Thread {
            while (lobbyListenerRunning) {
                try {
                    val action = networkManager.fetchNextAction()

                    when (action.type()) {
                        GameAction.ActionType.GUEST_ACCEPTED -> {
                            runOnUiThread {
                                handleGuestAccepted()
                            }
                        }

                        GameAction.ActionType.ROOM_CLOSED -> {
                            runOnUiThread {
                                handleRoomClosed()
                            }
                        }

                        GameAction.ActionType.START_MATCH -> {
                            /*
                             * Stop the lobby listener before the GameController starts
                             * consuming actions from the same NetworkManager queue.
                             */
                            lobbyListenerRunning = false

                            runOnUiThread {
                                handleStartMatch()
                            }

                            break
                        }

                        else -> {
                            // Other actions are not handled during the lobby phase.
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
        val pendingRoom = selectedRoom ?: return

        acceptedRoom = pendingRoom
        availableRooms.clear()

        renderRoom()
        renderAvailableRooms()
    }

    private fun handleStartMatch() {
        if (leavingScreen) {
            return
        }

        val hostRoom = acceptedRoom

        if (hostRoom == null) {
            Toast.makeText(
                this,
                "The match cannot start because no host was accepted.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        /*
         * The lobby listener already stopped itself after receiving
         * START_MATCH. We only clear the stored thread reference here.
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

        // gameController is injected

        gameController.setNetworkRole(Role.GUEST)
        gameController.setLocalPlayer(guestPlayer)

        /*
         * On the guest device this only initializes the local match.
         * It does not send board colors or cards because the role is GUEST.
         */
        gameController.startMatch(
            hostPlayer,
            guestPlayer
        )

        /*
         * From this point onward, the GameController is the only consumer
         * of incoming gameplay actions.
         */
        gameController.startListeningForActions()

        try {
            networkManager.sendGameChange(
                GameAction(
                    GameAction.ActionType.MATCH_READY
                )
            )
        } catch (_: SecurityException) {
            Toast.makeText(
                this,
                "Bluetooth permission is missing.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        openGameScreen()
    }

    private fun handleRoomClosed() {
        if (leavingScreen || resettingRoomConnection) {
            return
        }

        resettingRoomConnection = true

        selectedRoom = null
        acceptedRoom = null
        availableRooms.clear()

        renderRoom()
        renderAvailableRooms()

        scanningStarted = false

        /*
         * Close the old GATT connection explicitly.
         *
         * ROOM_CLOSED may arrive before Android reports the physical
         * BLE disconnection. Without this cleanup, the previous
         * connection can remain internally active.
         */
        try {
            networkManager.disconnect()
        } catch (_: SecurityException) {
            // Continue with the UI reset even if permission was revoked.
        }

        availableRoomsContainer.postDelayed(
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
        /*
         * As long as no room has been selected yet, we will
         * copy the current scan list in its entirety.
         */
        if (selectedRoom == null) {
            availableRooms.clear()
            availableRooms.addAll(updatedRooms)
        } else {
            /*
             * After the selection is made, BleGuest stops the scan.
             * The selected room remains visible so that
             * “...” can continue to be displayed there.
             */
            val pendingRoom = selectedRoom

            availableRooms.clear()
            availableRooms.addAll(updatedRooms)

            if (
                pendingRoom != null &&
                availableRooms.none { room -> room == pendingRoom }
            ) {
                availableRooms.add(pendingRoom)
            }
        }

        renderAvailableRooms()
    }

    private fun renderRoom() {
        roomPlayersContainer.removeAllViews()

        roomPlayersContainer.addView(
            createRoomPlayerRow(
                text = "${getDeviceName()} (You)",
                backgroundColor = getColor(R.color.kc_guest)
            )
        )

        acceptedRoom?.let { room ->
            roomPlayersContainer.addView(
                createRoomPlayerRow(
                    text = room.deviceName() ?: "Unknown Host",
                    backgroundColor = getColor(R.color.kc_host)
                )
            )
        }
    }

    private fun renderAvailableRooms() {
        availableRoomsContainer.removeAllViews()

        if (acceptedRoom != null) {
            return
        }

        availableRooms.forEachIndexed { index, room ->
            availableRoomsContainer.addView(
                createAvailableRoomRow(
                    room = room,
                    index = index
                )
            )
        }
    }

    private fun createRoomPlayerRow(
        text: String,
        backgroundColor: Int
    ): View {
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(getColor(android.R.color.black))
            setBackgroundColor(backgroundColor)
            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                16.dp(),
                0,
                16.dp(),
                0
            )

            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                52.dp()
            ).apply {
                bottomMargin = 10.dp()
            }
        }
    }

    private fun createAvailableRoomRow(
        room: NetworkDevice,
        index: Int
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(
                GameColorMapper.playerListColor(index)
            )

            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                56.dp()
            ).apply {
                bottomMargin = 8.dp()
            }
        }

        val nameText = TextView(this).apply {
            text = room.deviceName() ?: "Unknown Room"
            textSize = 18f
            setTextColor(getColor(android.R.color.black))
            setSingleLine(true)
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                16.dp(),
                0,
                12.dp(),
                0
            )

            layoutParams = LinearLayout.LayoutParams(
                0,
                MATCH_PARENT,
                1f
            )
        }

        val isPendingRoom = selectedRoom == room
        val anotherRoomIsPending =
            selectedRoom != null && !isPendingRoom

        val joinButton = Button(this).apply {
            text = if (isPendingRoom) "..." else "✓"
            textSize = 22f
            setTextColor(getColor(android.R.color.white))
            setBackgroundColor(getColor(R.color.kc_dark_grey))

            layoutParams = LinearLayout.LayoutParams(
                56.dp(),
                MATCH_PARENT
            )

            /*
             * Once a room has been selected:
             * - its button displays “...”
             * - all other buttons disappear
             */
            visibility = if (anotherRoomIsPending) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }

            isEnabled = selectedRoom == null

            setOnClickListener {
                requestRoomJoin(room)
            }
        }

        row.addView(nameText)
        row.addView(joinButton)

        return row
    }

    private fun requestRoomJoin(room: NetworkDevice) {
        if (selectedRoom != null) {
            return
        }

        try {
            networkManager.confirmRoom(room)
        } catch (_: SecurityException) {
            Toast.makeText(
                this,
                "Bluetooth permission is missing.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        /*
         * Only after the method has been successfully called do we
         * set the local UI state to “waiting.”
         */
        selectedRoom = room
        renderAvailableRooms()
    }

    private fun leaveRoomAndReturn() {
        if (leavingScreen) {
            return
        }

        leavingScreen = true
        stopLobbyActionListener()

        try {
            networkManager.disconnect()
        } catch (_: SecurityException) {
            // The Activity can still close if permission was revoked.
        }

        scanningStarted = false
        finish()
    }

    private fun openGameScreen() {
        startActivity(
            Intent(
                this,
                GameActivity::class.java
            )
        )

        finish()
    }

    override fun onDestroy() {
        stopLobbyActionListener()

        networkManager.setNetworkEventListener(null)
        networkManager.setRoomConnectionListener(null)

        super.onDestroy()
    }

    private fun getDeviceName(): String {
        return Settings.Global.getString(
            contentResolver,
            Settings.Global.DEVICE_NAME
        ) ?: Build.MODEL
    }

    private fun Int.dp(): Int {
        return (
                this * resources.displayMetrics.density
                ).toInt()
    }
}