package kittycards.kittycardsandroid.ui.util

import kittycards.kittycardsandroid.model.GameColor
import android.graphics.Color

object GameColorMapper {

    fun toAndroidColor(color: GameColor): Int {
        return Color.parseColor(color.hexCode)
    }
}