package kittycards.kittycardsandroid.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kittycards.kittycardsandroid.R
import kittycards.kittycardsandroid.model.GameColor
import kittycards.kittycardsandroid.ui.util.GameColorMapper
import android.content.Intent
import android.widget.Button

/**
 * @author JellyMae
 */
class HostActivity : AppCompatActivity() {

    private val playerRowColors = listOf(
        GameColor.YELLOW,
        GameColor.GREEN,
        GameColor.CYAN,
        GameColor.PURPLE
    )

    fun playerListColor(index: Int): GameColor {
        val colors = listOf(
            GameColor.YELLOW,
            GameColor.GREEN,
            GameColor.CYAN,
            GameColor.PURPLE
        )

        return colors[index % colors.size]
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_host)

        val backButton = findViewById<TextView>(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        val startMatchButton = findViewById<Button>(R.id.startMatchButton)

        startMatchButton.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        /*
        val availablePlayers = networkManager.getAvailablePlayers()

        availablePlayers.forEachIndexed { index, player ->
            addAvailablePlayer(player.name, index)
        }
         */

        addAvailablePlayer("Player#1234", 0)
        addAvailablePlayer("Luuk#5678", 1)
        addAvailablePlayer("Denia#9876", 2)
        addAvailablePlayer("Pixel 8", 3)

        addPlayerToRoom("Jil's Phone (You)")
        addPlayerToRoom("Pixel 8")
    }


    private fun addAvailablePlayer(name: String, index: Int) {
        val box = findViewById<LinearLayout>(R.id.availablePlayersBox)
        val gameColor = playerRowColors[index % playerRowColors.size]

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(GameColorMapper.toAndroidColor(gameColor))

            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                WRAP_CONTENT
            ).apply {
                topMargin = 6
            }
        }

        val nameText = TextView(this).apply {
            text = name
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val acceptText = TextView(this).apply {
            text = "✓"
            textSize = 18f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                48,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        row.addView(nameText)
        row.addView(acceptText)

        box.addView(row)
    }


    private fun addPlayerToRoom(name: String) {
        val box = findViewById<LinearLayout>(R.id.currentRoomBox)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 12)
            background = getDrawable(android.R.drawable.editbox_background)
        }

        val nameText = TextView(this).apply {
            text = name
            textSize = 18f
        }

        row.addView(nameText)

        box.addView(row)
    }
}