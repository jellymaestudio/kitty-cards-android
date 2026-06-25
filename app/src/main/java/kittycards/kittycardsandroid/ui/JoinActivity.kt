package kittycards.kittycardsandroid.ui

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kittycards.kittycardsandroid.R
import kittycards.kittycardsandroid.model.GameColor
import kittycards.kittycardsandroid.ui.util.GameColorMapper

/**
 * @author JellyMae
 */
class JoinActivity : AppCompatActivity() {

    /*private val playerRowColors = listOf(
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
    }*/


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_join)

        val backButton = findViewById<TextView>(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }


        /*addAvailableRooms("Player#1234", 0)
        addAvailableRooms("Luuk#5678", 1)
        addAvailableRooms("Denia#9876", 2)
        addAvailableRooms("Pixel 8", 3)

        addPlayerToRoom("Jil's Phone (You)")
        addPlayerToRoom("Pixel 8")*/
    }


    /*private fun addAvailableRooms(name: String, index: Int) {
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
    }*/
}