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

class HostActivity : AppCompatActivity() {

    private lateinit var networkManager: NetworkManager

    private val availableGuests = mutableListOf<NetworkDevice>()
    private var selectedGuest: NetworkDevice? = null

    private lateinit var roomPlayersContainer: LinearLayout
    private lateinit var availablePlayersContainer: LinearLayout
    private lateinit var startMatchButton: Button

    private var hostingStarted = false
    private var leavingScreen = false

    private val bluetoothPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = requiredBluetoothPermissions()
                .all { permission -> permissions[permission] == true }

            if (allGranted) {
                startHosting()
            } else {
                Toast.makeText(
                    this,
                    "Bluetooth permissions are required to host a game.",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_host)

        networkManager = NetworkManager.getInstance(applicationContext)

        bindViews()
        applyWindowInsets()
        setupClickListeners()
        setupBackNavigation()
        setupNetworkEventListener()

        renderRoom()
        renderAvailablePlayers()

        startMatchButton.isEnabled = false

        checkPermissionsAndStartHosting()
    }

    private fun bindViews() {
        roomPlayersContainer = findViewById(R.id.roomPlayersContainer)
        availablePlayersContainer = findViewById(R.id.availablePlayersContainer)
        startMatchButton = findViewById(R.id.startMatchButton)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.hostRoot)
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
            closeRoomAndReturn()
        }

        startMatchButton.setOnClickListener {
            // Phase A:
            // Match start will be implemented after host/join connection works.
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    closeRoomAndReturn()
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
                    // Information events do not need to be displayed.
                }
            }
        }
    }

    private fun checkPermissionsAndStartHosting() {
        val missingPermissions = requiredBluetoothPermissions()
            .filter { permission ->
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            }

        if (missingPermissions.isEmpty()) {
            startHosting()
        } else {
            bluetoothPermissionLauncher.launch(
                missingPermissions.toTypedArray()
            )
        }
    }

    private fun requiredBluetoothPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun startHosting() {
        if (hostingStarted) {
            return
        }

        hostingStarted = true

        try {
            networkManager.hostMatch { connectedGuests ->
                updateAvailableGuests(connectedGuests)
            }
        } catch (_: SecurityException) {
            hostingStarted = false

            Toast.makeText(
                this,
                "Bluetooth permission is missing.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateAvailableGuests(
        connectedGuests: List<NetworkDevice>
    ) {
        val acceptedGuest = selectedGuest

        if (
            acceptedGuest != null &&
            connectedGuests.none { guest -> guest == acceptedGuest }
        ) {
            selectedGuest = null
        }

        availableGuests.clear()
        availableGuests.addAll(
            connectedGuests.filter { guest -> guest != selectedGuest }
        )

        renderRoom()
        renderAvailablePlayers()
    }

    private fun renderRoom() {
        roomPlayersContainer.removeAllViews()

        roomPlayersContainer.addView(
            createRoomPlayerRow(
                text = "${getDeviceName()} (You)",
                backgroundColor = getColor(R.color.kc_host)
            )
        )

        selectedGuest?.let { guest ->
            roomPlayersContainer.addView(
                createRoomPlayerRow(
                    text = getGuestDisplayName(guest),
                    backgroundColor = getColor(R.color.kc_guest)
                )
            )
        }

        startMatchButton.isEnabled = selectedGuest != null
    }

    private fun renderAvailablePlayers() {
        availablePlayersContainer.removeAllViews()

        availableGuests.forEachIndexed { index, guest ->
            availablePlayersContainer.addView(
                createAvailablePlayerRow(
                    guest = guest,
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

    private fun createAvailablePlayerRow(
        guest: NetworkDevice,
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
            text = getGuestDisplayName(guest)
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

        val acceptButton = Button(this).apply {
            text = "✓"
            textSize = 22f
            setTextColor(getColor(android.R.color.white))
            setBackgroundColor(getColor(R.color.kc_dark_grey))

            layoutParams = LinearLayout.LayoutParams(
                56.dp(),
                MATCH_PARENT
            )

            visibility = if (selectedGuest == null) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }

            setOnClickListener {
                acceptGuest(guest)
            }
        }

        row.addView(nameText)
        row.addView(acceptButton)

        return row
    }

    private fun acceptGuest(guest: NetworkDevice) {
        if (selectedGuest != null) {
            return
        }

        try {
            networkManager.selectGuest(guest)
        } catch (_: SecurityException) {
            Toast.makeText(
                this,
                "Bluetooth permission is missing.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        selectedGuest = guest
        availableGuests.remove(guest)

        renderRoom()
        renderAvailablePlayers()
    }

    private fun closeRoomAndReturn() {
        if (leavingScreen) {
            return
        }

        leavingScreen = true

        try {
            networkManager.disconnect()
        } catch (_: SecurityException) {
            // The Activity can still close if permission was revoked.
        }

        hostingStarted = false
        finish()
    }

    override fun onDestroy() {
        networkManager.setNetworkEventListener(null)

        super.onDestroy()
    }

    private fun getDeviceName(): String {
        return Settings.Global.getString(
            contentResolver,
            Settings.Global.DEVICE_NAME
        ) ?: Build.MODEL
    }

    private fun getGuestDisplayName(guest: NetworkDevice): String {
        val deviceName = guest.deviceName()

        return if (!deviceName.isNullOrBlank()) {
            deviceName
        } else {
            "Guest ${guest.deviceAddress().takeLast(5)}"
        }
    }

    private fun Int.dp(): Int {
        return (
                this * resources.displayMetrics.density
                ).toInt()
    }
}