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
import kittycards.kittycardsandroid.logic.JoinLobbyController
import kittycards.kittycardsandroid.logic.JoinLobbyState
import kittycards.kittycardsandroid.network.NetworkDevice
import kittycards.kittycardsandroid.ui.util.GameColorMapper
import javax.inject.Inject

@AndroidEntryPoint
class JoinActivity : AppCompatActivity() {

    @Inject
    lateinit var joinLobbyController: JoinLobbyController

    private lateinit var roomPlayersContainer: LinearLayout
    private lateinit var availableRoomsContainer: LinearLayout

    private val bluetoothPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = requiredBluetoothPermissions()
                .all { permission ->
                    permissions[permission] == true
                }

            if (allGranted) {
                joinLobbyController.startScanning()
            } else {
                showError(
                    "Bluetooth permissions are required to join a game."
                )

                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_join)

        bindViews()
        applyWindowInsets()
        setupControllerCallbacks()
        setupClickListeners()
        setupBackNavigation()

        joinLobbyController.initialize()

        checkPermissionsAndStartScanning()
    }

    private fun bindViews() {
        roomPlayersContainer =
            findViewById(R.id.roomPlayersContainer)

        availableRoomsContainer =
            findViewById(R.id.availableRoomsContainer)
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

    private fun setupControllerCallbacks() {
        joinLobbyController.onStateChanged = { state ->
            runOnUiThread {
                renderState(state)
            }
        }

        joinLobbyController.onOpenGameRequested = {
            runOnUiThread {
                openGameScreen()
            }
        }

        joinLobbyController.onCloseScreenRequested = {
            runOnUiThread {
                finish()
            }
        }

        joinLobbyController.onError = { message ->
            runOnUiThread {
                showError(message)
            }
        }

        joinLobbyController.onWarning = { message ->
            runOnUiThread {
                showWarning(message)
            }
        }
    }

    private fun setupClickListeners() {
        findViewById<TextView>(R.id.backButton)
            .setOnClickListener {
                joinLobbyController.leaveRoom()
            }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    joinLobbyController.leaveRoom()
                }
            }
        )
    }

    private fun checkPermissionsAndStartScanning() {
        val missingPermissions =
            requiredBluetoothPermissions()
                .filter { permission ->
                    ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                }

        if (missingPermissions.isEmpty()) {
            joinLobbyController.startScanning()
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

    private fun renderState(state: JoinLobbyState) {
        renderRoom(state.acceptedRoom)

        renderAvailableRooms(
            rooms = state.availableRooms,
            selectedRoom = state.selectedRoom,
            roomAccepted = state.acceptedRoom != null
        )
    }

    private fun renderRoom(
        acceptedRoom: NetworkDevice?
    ) {
        roomPlayersContainer.removeAllViews()

        roomPlayersContainer.addView(
            createRoomPlayerRow(
                text = "${getDeviceName()} (You)",
                backgroundColor =
                    getColor(R.color.kc_guest)
            )
        )

        acceptedRoom?.let { room ->
            roomPlayersContainer.addView(
                createRoomPlayerRow(
                    text =
                        room.deviceName()
                            ?: "Unknown Host",
                    backgroundColor =
                        getColor(R.color.kc_host)
                )
            )
        }
    }

    private fun renderAvailableRooms(
        rooms: List<NetworkDevice>,
        selectedRoom: NetworkDevice?,
        roomAccepted: Boolean
    ) {
        availableRoomsContainer.removeAllViews()

        if (roomAccepted) {
            return
        }

        rooms.forEachIndexed { index, room ->
            availableRoomsContainer.addView(
                createAvailableRoomRow(
                    room = room,
                    index = index,
                    selectedRoom = selectedRoom
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

    private fun createAvailableRoomRow(
        room: NetworkDevice,
        index: Int,
        selectedRoom: NetworkDevice?
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
            text =
                room.deviceName()
                    ?: "Unknown Room"

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

        val isPendingRoom =
            selectedRoom == room

        val anotherRoomIsPending =
            selectedRoom != null &&
                    !isPendingRoom

        val joinButton = Button(this).apply {
            text =
                if (isPendingRoom) {
                    "..."
                } else {
                    "✓"
                }

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
                if (anotherRoomIsPending) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }

            isEnabled =
                selectedRoom == null

            setOnClickListener {
                joinLobbyController.requestRoomJoin(room)
            }
        }

        row.addView(nameText)
        row.addView(joinButton)

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
        joinLobbyController.cleanup()

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