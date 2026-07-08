package kittycards.kittycardsandroid.ui.util

import kittycards.kittycardsandroid.model.GameColor
import android.graphics.Color

object GameColorMapper {

    fun toAndroidColor(color: GameColor): Int {
        return Color.parseColor(color.hexCode)
    }

    fun playerListColor(index: Int): Int {
        val colors = listOf(
            GameColor.YELLOW,
            GameColor.GREEN,
            GameColor.CYAN,
            GameColor.PURPLE
        )

        return toAndroidColor(colors[index % colors.size])
    }
}