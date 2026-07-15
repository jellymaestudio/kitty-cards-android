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
import dagger.hilt.android.AndroidEntryPoint
import kittycards.kittycardsandroid.R
import kittycards.kittycardsandroid.logic.HostLobbyController
import kittycards.kittycardsandroid.logic.HostLobbyState
import kittycards.kittycardsandroid.network.NetworkDevice
import kittycards.kittycardsandroid.ui.util.GameColorMapper
import javax.inject.Inject

@AndroidEntryPoint
class HostActivity : AppCompatActivity() {

    @Inject
    lateinit var hostLobbyController: HostLobbyController

    private lateinit var roomPlayersContainer: LinearLayout
    private lateinit var availablePlayersContainer: LinearLayout
    private lateinit var startMatchButton: Button

    private val bluetoothPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = requiredBluetoothPermissions()
                .all { permission ->
                    permissions[permission] == true
                }

            if (allGranted) {
                hostLobbyController.startHosting()
            } else {
                showError(
                    "Bluetooth permissions are required to host a game."
                )

                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_host)

        bindViews()
        applyWindowInsets()
        setupControllerCallbacks()
        setupClickListeners()
        setupBackNavigation()

        hostLobbyController.initialize()

        checkPermissionsAndStartHosting()
    }

    private fun bindViews() {
        roomPlayersContainer =
            findViewById(R.id.roomPlayersContainer)

        availablePlayersContainer =
            findViewById(R.id.availablePlayersContainer)

        startMatchButton =
            findViewById(R.id.startMatchButton)
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

    private fun setupControllerCallbacks() {
        hostLobbyController.onStateChanged = { state ->
            runOnUiThread {
                renderState(state)
            }
        }

        hostLobbyController.onOpenGameRequested = {
            runOnUiThread {
                openGameScreen()
            }
        }

        hostLobbyController.onCloseScreenRequested = {
            runOnUiThread {
                finish()
            }
        }

        hostLobbyController.onError = { message ->
            runOnUiThread {
                showError(message)
            }
        }

        hostLobbyController.onWarning = { message ->
            runOnUiThread {
                showWarning(message)
            }
        }
    }

    private fun setupClickListeners() {
        findViewById<TextView>(R.id.backButton)
            .setOnClickListener {
                hostLobbyController.closeRoom()
            }

        startMatchButton.setOnClickListener {
            hostLobbyController.requestMatchStart()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    hostLobbyController.closeRoom()
                }
            }
        )
    }

    private fun checkPermissionsAndStartHosting() {
        val missingPermissions =
            requiredBluetoothPermissions()
                .filter { permission ->
                    ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                }

        if (missingPermissions.isEmpty()) {
            hostLobbyController.startHosting()
        } else {
            bluetoothPermissionLauncher.launch(
                missingPermissions.toTypedArray()
            )
        }
    }

    private fun requiredBluetoothPermissions(): List<String> {
        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
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

    private fun renderState(state: HostLobbyState) {
        renderRoom(state.selectedGuest)

        renderAvailablePlayers(
            guests = state.availableGuests,
            guestSelected = state.selectedGuest != null
        )

        startMatchButton.isEnabled =
            state.canStartMatch
    }

    private fun renderRoom(
        selectedGuest: NetworkDevice?
    ) {
        roomPlayersContainer.removeAllViews()

        roomPlayersContainer.addView(
            createRoomPlayerRow(
                text = "${getDeviceName()} (You)",
                backgroundColor =
                    getColor(R.color.kc_host)
            )
        )

        selectedGuest?.let { guest ->
            roomPlayersContainer.addView(
                createRoomPlayerRow(
                    text = getGuestDisplayName(guest),
                    backgroundColor =
                        getColor(R.color.kc_guest)
                )
            )
        }
    }

    private fun renderAvailablePlayers(
        guests: List<NetworkDevice>,
        guestSelected: Boolean
    ) {
        availablePlayersContainer.removeAllViews()

        guests.forEachIndexed { index, guest ->
            availablePlayersContainer.addView(
                createAvailablePlayerRow(
                    guest = guest,
                    index = index,
                    guestSelected = guestSelected
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

            setTextColor(
                getColor(android.R.color.black)
            )

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
        index: Int,
        guestSelected: Boolean
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

            setTextColor(
                getColor(android.R.color.black)
            )

            setSingleLine(true)

            ellipsize =
                android.text.TextUtils.TruncateAt.END

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

            setTextColor(
                getColor(android.R.color.white)
            )

            setBackgroundColor(
                getColor(R.color.kc_dark_grey)
            )

            layoutParams = LinearLayout.LayoutParams(
                56.dp(),
                MATCH_PARENT
            )

            visibility =
                if (guestSelected) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }

            setOnClickListener {
                hostLobbyController.selectGuest(guest)
            }
        }

        row.addView(nameText)
        row.addView(acceptButton)

        return row
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

    private fun showError(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showWarning(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        hostLobbyController.cleanup()

        super.onDestroy()
    }

    private fun getDeviceName(): String {
        return Settings.Global.getString(
            contentResolver,
            Settings.Global.DEVICE_NAME
        ) ?: Build.MODEL
    }

    private fun getGuestDisplayName(
        guest: NetworkDevice
    ): String {
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