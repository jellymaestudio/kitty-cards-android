package kittycards.kittycardsandroid.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kittycards.kittycardsandroid.R
import kittycards.kittycardsandroid.model.Card
import kittycards.kittycardsandroid.model.GameColor
import kittycards.kittycardsandroid.ui.util.GameColorMapper

class GameActivity : AppCompatActivity() {

    private lateinit var opponentHandContainer: LinearLayout
    private lateinit var playerHandContainer: LinearLayout
    private lateinit var boardContainer: GridLayout
    private lateinit var roundResultContainer: LinearLayout
    private lateinit var opponentScoreText: TextView
    private lateinit var playerScoreText: TextView
    private lateinit var turnInfoText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_game)

        bindViews()
        applyWindowInsets()
        renderTestScreen()
    }

    private fun bindViews() {
        opponentHandContainer = findViewById(R.id.opponentHandContainer)
        playerHandContainer = findViewById(R.id.playerHandContainer)
        boardContainer = findViewById(R.id.boardContainer)
        roundResultContainer = findViewById(R.id.roundResultContainer)
        opponentScoreText = findViewById(R.id.opponentScoreText)
        playerScoreText = findViewById(R.id.playerScoreText)
        turnInfoText = findViewById(R.id.turnInfoText)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gameRoot)) { view, insets ->
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

    private fun renderTestScreen() {
        renderOpponentHand(cardCount = 4)
        renderPlayerHand(
            listOf(
                Card(GameColor.YELLOW, 4),
                Card(GameColor.GREEN, 6),
                Card(GameColor.GREEN, 3),
                Card(GameColor.CYAN, 5),
                Card(GameColor.PURPLE, 1)
            )
        )

        renderRoundResults()
        renderBoard()

        opponentScoreText.text = "0"
        playerScoreText.text = "13"
        turnInfoText.visibility = View.GONE
    }

    private fun renderOpponentHand(cardCount: Int) {
        opponentHandContainer.removeAllViews()

        repeat(cardCount) {
            opponentHandContainer.addView(createHiddenCardView())
        }
    }

    private fun renderPlayerHand(cards: List<Card>) {
        playerHandContainer.removeAllViews()

        cards.sortedWith(
            compareBy<Card> { colorSortValue(it.color) }
                .thenByDescending { it.value }
        ).forEach { card ->
            playerHandContainer.addView(createVisibleCardView(card))
        }
    }

    private fun renderRoundResults() {
        roundResultContainer.removeAllViews()

        val colors = listOf(
            getColor(R.color.kc_guest),
            getColor(R.color.kc_host),
            getColor(android.R.color.white)
        )

        colors.forEachIndexed { index, color ->
            roundResultContainer.addView(
                View(this).apply {
                    setBackgroundColor(color)
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        MATCH_PARENT,
                        1f
                    ).apply {
                        if (index < colors.lastIndex) {
                            marginEnd = 2.dp()
                        }
                    }
                }
            )
        }
    }

    private fun renderBoard() {
        boardContainer.removeAllViews()

        val fieldColors = listOf(
            GameColor.PURPLE, GameColor.GREY, GameColor.PURPLE,
            GameColor.CYAN, GameColor.GREY, GameColor.GREEN,
            GameColor.GREY, GameColor.GREEN, GameColor.GREY
        )

        for (row in 0..2) {
            for (column in 0..2) {
                val index = row * 3 + column

                if (row == 1 && column == 1) {
                    boardContainer.addView(createDrawPileView())
                } else {
                    boardContainer.addView(
                        createBoardFieldView(
                            fieldColor = fieldColors[index],
                            placedCard = testPlacedCard(row, column)
                        )
                    )
                }
            }
        }
    }

    private fun testPlacedCard(row: Int, column: Int): Card? {
        return when {
            row == 0 && column == 0 -> Card(GameColor.PURPLE, 1)
            row == 1 && column == 2 -> Card(GameColor.CYAN, 0.coerceAtLeast(1))
            row == 2 && column == 1 -> Card(GameColor.GREEN, 6)
            else -> null
        }
    }

    private fun createHiddenCardView(): View {
        return View(this).apply {
            setBackgroundColor(getColor(R.color.kc_guest))
            layoutParams = LinearLayout.LayoutParams(
                48.dp(),
                80.dp()
            ).apply {
                marginEnd = 8.dp()
            }
        }
    }

    private fun createVisibleCardView(card: Card): View {
        val cardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(GameColorMapper.toAndroidColor(card.color))
            setPadding(4.dp(), 6.dp(), 4.dp(), 4.dp())

            layoutParams = LinearLayout.LayoutParams(
                56.dp(),
                88.dp()
            ).apply {
                marginEnd = 8.dp()
            }
        }

        val valueText = TextView(this).apply {
            text = card.value.toString()
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(getColor(android.R.color.black))

            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                28.dp()
            )
        }

        val kittyImage = ImageView(this).apply {
            setImageResource(R.drawable.kitty_card)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = null

            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                0,
                1f
            )
        }

        cardView.addView(valueText)
        cardView.addView(kittyImage)

        return cardView
    }

    private fun createBoardFieldView(fieldColor: GameColor, placedCard: Card?): View {
        val outer = LinearLayout(this).apply {
            setBackgroundColor(GameColorMapper.toAndroidColor(fieldColor))
            setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())

            layoutParams = GridLayout.LayoutParams().apply {
                width = 100.dp()
                height = 100.dp()
            }
        }

        val inner = TextView(this).apply {
            text = placedCard?.value?.toString() ?: ""
            textSize = 18f
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setTextColor(getColor(android.R.color.black))
            setBackgroundColor(
                placedCard?.let { GameColorMapper.toAndroidColor(it.color) }
                    ?: getColor(android.R.color.white)
            )
            setPadding(0, 8.dp(), 0, 0)

            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            )
        }

        outer.addView(inner)
        return outer
    }

    private fun createDrawPileView(): View {
        val outer = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(android.R.color.white))

            layoutParams = GridLayout.LayoutParams().apply {
                width = 100.dp()
                height = 100.dp()
            }
        }

        val pile = TextView(this).apply {
            text = "Draw"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(getColor(android.R.color.white))
            setBackgroundColor(getColor(R.color.kc_dark_grey))

            layoutParams = LinearLayout.LayoutParams(
                52.dp(),
                72.dp()
            )
        }

        outer.addView(pile)
        return outer
    }

    private fun colorSortValue(color: GameColor): Int {
        return when (color) {
            GameColor.YELLOW -> 0
            GameColor.GREEN -> 1
            GameColor.CYAN -> 2
            GameColor.PURPLE -> 3
            GameColor.GREY -> 4
        }
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}