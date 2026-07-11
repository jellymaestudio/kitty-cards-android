package kittycards.kittycardsandroid.ui

import android.Manifest
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
import kittycards.kittycardsandroid.network.NetworkDevice
import kittycards.kittycardsandroid.network.NetworkManager
import kittycards.kittycardsandroid.network.event.NetworkEvent
import kittycards.kittycardsandroid.ui.util.GameColorMapper
import kittycards.kittycardsandroid.network.OnRoomConnectionListener

class JoinActivity : AppCompatActivity() {

    private lateinit var networkManager: NetworkManager

    private val availableRooms = mutableListOf<NetworkDevice>()

    /*
     * Der Raum, zu dem gerade eine Verbindung aufgebaut wird.
     *
     * Wichtig:
     * Dieser Host ist noch nicht automatisch Teil der Room-Box.
     * Das passiert erst später nach GUEST_ACCEPTED.
     */
    private var selectedRoom: NetworkDevice? = null

    private lateinit var roomPlayersContainer: LinearLayout
    private lateinit var availableRoomsContainer: LinearLayout

    private var scanningStarted = false
    private var leavingScreen = false

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

        networkManager = NetworkManager.getInstance(applicationContext)

        bindViews()
        applyWindowInsets()
        setupClickListeners()
        setupBackNavigation()
        setupNetworkEventListener()
        setupRoomConnectionListener()

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
                     * In Phase A ist noch keine sichtbare Änderung nötig.
                     * Der Host wird erst nach GUEST_ACCEPTED in der
                     * Room-Box angezeigt.
                     */
                }

                override fun onRoomDisconnected() {
                    if (leavingScreen) {
                        return
                    }

                    selectedRoom = null
                    availableRooms.clear()

                    renderRoom()
                    renderAvailableRooms()

                    scanningStarted = false

                    /*
                     * Kurze Verzögerung, damit BleGuest seine alte
                     * Verbindung vollständig bereinigen kann.
                     */
                    availableRoomsContainer.postDelayed(
                        {
                            if (!leavingScreen) {
                                startScanning()
                            }
                        },
                        300L
                    )
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

    private fun updateAvailableRooms(
        updatedRooms: List<NetworkDevice>
    ) {
        /*
         * Solange noch kein Raum gewählt wurde, übernehmen wir
         * vollständig die aktuelle Scan-Liste.
         */
        if (selectedRoom == null) {
            availableRooms.clear()
            availableRooms.addAll(updatedRooms)
        } else {
            /*
             * Nach der Auswahl stoppt BleGuest den Scan.
             * Der ausgewählte Raum bleibt sichtbar, damit dort
             * weiterhin "..." angezeigt werden kann.
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

        /*
         * In Phase A steht hier nur der aktuelle Guest.
         * Der Host wird erst nach GUEST_ACCEPTED ergänzt.
         */
        roomPlayersContainer.addView(
            createRoomPlayerRow(
                text = "${getDeviceName()} (You)",
                backgroundColor = getColor(R.color.kc_guest)
            )
        )
    }

    private fun renderAvailableRooms() {
        availableRoomsContainer.removeAllViews()

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
             * Sobald ein Raum gewählt wurde:
             * - dessen Button zeigt "..."
             * - alle anderen Buttons verschwinden
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
         * Erst nach dem erfolgreichen Methodenaufruf setzen wir
         * den lokalen UI-Zustand auf "wartend".
         */
        selectedRoom = room
        renderAvailableRooms()
    }

    private fun leaveRoomAndReturn() {
        if (leavingScreen) {
            return
        }

        leavingScreen = true

        try {
            networkManager.disconnect()
        } catch (_: SecurityException) {
            // The Activity can still close if permission was revoked.
        }

        scanningStarted = false
        finish()
    }

    override fun onDestroy() {
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