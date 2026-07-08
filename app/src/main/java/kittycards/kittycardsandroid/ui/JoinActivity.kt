package kittycards.kittycardsandroid.ui

import android.Manifest
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kittycards.kittycardsandroid.R
import kittycards.kittycardsandroid.network.NetworkDevice
import kittycards.kittycardsandroid.network.NetworkManager
import kittycards.kittycardsandroid.ui.util.GameColorMapper

class JoinActivity : AppCompatActivity() {

    private lateinit var networkManager: NetworkManager

    private val availableRooms = mutableListOf<NetworkDevice>()
    private var selectedRoom: NetworkDevice? = null

    private lateinit var roomPlayersContainer: LinearLayout
    private lateinit var availableRoomsContainer: LinearLayout

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_join)

        networkManager = NetworkManager.getInstance(this)

        bindViews()
        applyWindowInsets()
        setupClickListeners()
        setupBackNavigation()

        // startScanning()

        renderRoom()
        renderAvailableRooms()
    }

    private fun bindViews() {
        roomPlayersContainer = findViewById(R.id.roomPlayersContainer)
        availableRoomsContainer = findViewById(R.id.availableRoomsContainer)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.joinRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

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

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startScanning() {
        networkManager.joinMatch { updatedRooms ->
            runOnUiThread {
                availableRooms.clear()
                availableRooms.addAll(updatedRooms)

                renderAvailableRooms()
            }
        }
    }

    private fun renderRoom() {
        roomPlayersContainer.removeAllViews()

        roomPlayersContainer.addView(
            createRoomPlayerRow(
                text = "${getDeviceName()} (You)",
                backgroundColor = getColor(R.color.kc_guest)
            )
        )

        selectedRoom?.let { room ->
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

        availableRooms
            .filter { it != selectedRoom }
            .forEachIndexed { index, room ->
                availableRoomsContainer.addView(
                    createAvailableRoomRow(
                        room = room,
                        index = index
                    )
                )
            }
    }

    private fun createRoomPlayerRow(text: String, backgroundColor: Int): View {
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(getColor(android.R.color.black))
            setBackgroundColor(backgroundColor)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16.dp(), 0, 16.dp(), 0)

            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                52.dp()
            ).apply {
                bottomMargin = 10.dp()
            }
        }
    }

    private fun createAvailableRoomRow(room: NetworkDevice, index: Int): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(GameColorMapper.playerListColor(index))

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
            setPadding(16.dp(), 0, 12.dp(), 0)
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                0,
                MATCH_PARENT,
                1f
            )
        }

        val joinButton = Button(this).apply {
            text = if (selectedRoom == room) "..." else "✓"
            textSize = 22f
            setTextColor(getColor(android.R.color.white))
            setBackgroundColor(getColor(R.color.kc_dark_grey))

            layoutParams = LinearLayout.LayoutParams(
                56.dp(),
                MATCH_PARENT
            )

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

        selectedRoom = room

        try {
            networkManager.confirmRoom(room)
        } catch (_: SecurityException) {
            // Bluetooth permission was denied or revoked.
        }

        renderRoom()
        renderAvailableRooms()
    }

    private fun leaveRoomAndReturn() {
        try {
            networkManager.disconnect()
        } catch (_: SecurityException) {
            // Bluetooth permission was denied or revoked.
        }

        finish()
    }

    private fun getDeviceName(): String {
        return Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
            ?: "Guest Device"
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}