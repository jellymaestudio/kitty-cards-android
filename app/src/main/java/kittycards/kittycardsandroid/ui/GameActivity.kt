package kittycards.kittycardsandroid.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kittycards.kittycardsandroid.R

/**
 * @author JellyMae
 */
class GameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_game)
    }
}