package kittycards.kittycardsandroid.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import kittycards.kittycardsandroid.R

@AndroidEntryPoint
class LobbyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        enableEdgeToEdge()
        setContentView(R.layout.activity_lobby)

        applyWindowInsets()
        setupButtonListeners()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.lobbyRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }

    private fun setupButtonListeners() {
        findViewById<Button>(R.id.hostGameButton).setOnClickListener {
            startActivity(Intent(this, HostActivity::class.java))
        }

        findViewById<Button>(R.id.joinGameButton).setOnClickListener {
            startActivity(Intent(this, JoinActivity::class.java))
        }
    }
}