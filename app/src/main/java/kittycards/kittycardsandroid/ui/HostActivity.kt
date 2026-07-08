package kittycards.kittycardsandroid.ui

import android.Manifest
import android.content.Intent
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

class HostActivity : AppCompatActivity() {

    private lateinit var networkManager: NetworkManager

    private val availableGuests = mutableListOf<NetworkDevice>()
    private var selectedGuest: NetworkDevice? = null

    private lateinit var roomPlayersContainer: LinearLayout
    private lateinit var availablePlayersContainer: LinearLayout
    private lateinit var startMatchButton: Button

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_host)

        networkManager = NetworkManager.getInstance(this)

        bindViews()
        applyWindowInsets()
        setupClickListeners()
        setupBackNavigation()
        //startHosting()
        renderRoom()
        renderAvailablePlayers()
    }

    private fun bindViews() {
        roomPlayersContainer = findViewById(R.id.roomPlayersContainer)
        availablePlayersContainer = findViewById(R.id.availablePlayersContainer)
        startMatchButton = findViewById(R.id.startMatchButton)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.hostRoot)) { view, insets ->
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
            closeRoomAndReturn()
        }

        startMatchButton.setOnClickListener {
            startMatch()
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun startHosting() {
        networkManager.hostMatch { connectedGuests ->
            runOnUiThread {
                availableGuests.clear()
                availableGuests.addAll(connectedGuests)

                selectedGuest?.let { guest ->
                    if (!availableGuests.contains(guest)) {
                        selectedGuest = null
                    }
                }

                renderRoom()
                renderAvailablePlayers()
            }
        }
    }

    private fun renderRoom() {
        roomPlayersContainer.removeAllViews()

        val hostName = getDeviceName()
        roomPlayersContainer.addView(
            createRoomPlayerRow(
                text = "$hostName (You)",
                backgroundColor = getColor(R.color.kc_host)
            )
        )

        selectedGuest?.let { guest ->
            roomPlayersContainer.addView(
                createRoomPlayerRow(
                    text = guest.deviceName() ?: "Unknown Guest",
                    backgroundColor = getColor(R.color.kc_guest)
                )
            )
        }

        //(vorerst auskommentiert zum Testen) startMatchButton.isEnabled = selectedGuest != null
        startMatchButton.isEnabled = true
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

    private fun createRoomPlayerRow(text: String, backgroundColor: Int): View {
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(getColor(android.R.color.black))
            setBackgroundColor(backgroundColor)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 0, 16, 0)

            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                52.dp()
            ).apply {
                bottomMargin = 10.dp()
            }
        }
    }

    private fun createAvailablePlayerRow(guest: NetworkDevice, index: Int): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(GameColorMapper.playerListColor(index))

            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                56.dp()
            ).apply {
                bottomMargin = 8
            }
        }

        val nameText = TextView(this).apply {
            text = guest.deviceName() ?: "Unknown Device"
            textSize = 18f
            setTextColor(getColor(android.R.color.black))
            setSingleLine(true)
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(16, 0, 12, 0)
            gravity = Gravity.CENTER_VERTICAL

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

            isEnabled = selectedGuest == null || selectedGuest == guest

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

        selectedGuest = guest
        networkManager.selectGuest(guest)

        renderRoom()
        renderAvailablePlayers()
    }

    private fun startMatch() {
        // Temporary for emulator/UI testing.
        // Real match start will be added later.
        openGameScreen()
    }

    private fun openGameScreen() {
        startActivity(
            Intent(this, GameActivity::class.java)
        )
    }

    private fun closeRoomAndReturn() {
        try {
            networkManager.disconnect()
        } catch (_: SecurityException) {
            // Bluetooth permission was denied or revoked.
        }

        finish()
    }

    private fun getDeviceName(): String {
        return Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
            ?: "Host Device"
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}