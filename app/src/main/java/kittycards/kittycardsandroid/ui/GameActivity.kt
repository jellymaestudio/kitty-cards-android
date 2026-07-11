package kittycards.kittycardsandroid.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
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


    private data class TestPlacedCard(
        val card: Card,
        val ownerIsHost: Boolean,
        val displayedScore: Int
    )

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
            GameColor.GREY, GameColor.GREY, GameColor.PURPLE,
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

    private fun testPlacedCard(row: Int, column: Int): TestPlacedCard? {
        return when {
            row == 0 && column == 0 -> TestPlacedCard(
                card = Card(GameColor.PURPLE, 1),
                ownerIsHost = true,
                displayedScore = 1
            )

            row == 1 && column == 2 -> TestPlacedCard(
                card = Card(GameColor.CYAN, 1),
                ownerIsHost = false,
                displayedScore = 0
            )

            row == 2 && column == 1 -> TestPlacedCard(
                card = Card(GameColor.GREEN, 6),
                ownerIsHost = true,
                displayedScore = 12
            )

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

    private fun createBoardFieldView(
        fieldColor: GameColor,
        placedCard: TestPlacedCard?
    ): View {
        /*
         * Ebene 1:
         * Dünner schwarzer Rand, der die einzelnen Felder voneinander trennt.
         */
        val blackBorder = FrameLayout(this).apply {
            setBackgroundColor(getColor(android.R.color.black))
            setPadding(1.dp(), 1.dp(), 1.dp(), 1.dp())

            layoutParams = GridLayout.LayoutParams().apply {
                width = 100.dp()
                height = 100.dp()
            }
        }

        /*
         * Ebene 2:
         * Der farbige Rahmen des Feldes.
         */
        val fieldColorFrame = FrameLayout(this).apply {
            setBackgroundColor(
                GameColorMapper.toAndroidColor(fieldColor)
            )
            setPadding(7.dp(), 7.dp(), 7.dp(), 7.dp())

            layoutParams = FrameLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            )
        }

        /*
         * Ebene 3:
         * Die eigentliche Feldfläche.
         *
         * Leer       -> weiß
         * Karte liegt -> Kartenfarbe
         */
        val cardArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL

            setBackgroundColor(
                placedCard?.let {
                    GameColorMapper.toAndroidColor(it.card.color)
                } ?: getColor(android.R.color.white)
            )

            layoutParams = FrameLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            )
        }

        if (placedCard != null) {
            val scoreText = TextView(this).apply {
                text = placedCard.displayedScore.toString()
                textSize = 17f
                gravity = Gravity.CENTER
                setTextColor(getColor(android.R.color.black))
                includeFontPadding = false

                layoutParams = LinearLayout.LayoutParams(
                    MATCH_PARENT,
                    28.dp()
                )
            }

            val kittyContainer = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    MATCH_PARENT,
                    0,
                    1f
                ).apply {
                    leftMargin = 6.dp()
                    rightMargin = 6.dp()
                    topMargin = 4.dp()
                    bottomMargin = 2.dp()
                }
            }

            val ownerColor = if (placedCard.ownerIsHost) {
                getColor(R.color.kc_host)
            } else {
                getColor(R.color.kc_guest)
            }

            val kittyFill = ImageView(this).apply {
                setImageResource(R.drawable.kitty_fill)
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = null

                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(ownerColor)
                )

                layoutParams = FrameLayout.LayoutParams(
                    54.dp(),
                    54.dp(),
                    Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                ).apply {
                    bottomMargin = 2.dp()
                }
            }

            val kittyOutline = ImageView(this).apply {
                setImageResource(R.drawable.kitty_card)
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = null

                layoutParams = FrameLayout.LayoutParams(
                    54.dp(),
                    54.dp(),
                    Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                ).apply {
                    bottomMargin = 2.dp()
                }
            }

            kittyContainer.addView(kittyFill)
            kittyContainer.addView(kittyOutline)

            cardArea.addView(scoreText)
            cardArea.addView(kittyContainer)
        }

        fieldColorFrame.addView(cardArea)
        blackBorder.addView(fieldColorFrame)

        return blackBorder
    }

    private fun createDrawPileView(): View {
        val blackBorder = FrameLayout(this).apply {
            setBackgroundColor(getColor(android.R.color.black))
            setPadding(1.dp(), 1.dp(), 1.dp(), 1.dp())

            layoutParams = GridLayout.LayoutParams().apply {
                width = 100.dp()
                height = 100.dp()
            }
        }

        val drawField = FrameLayout(this).apply {
            setBackgroundColor(getColor(android.R.color.white))

            layoutParams = FrameLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            )
        }

        val pileContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                76.dp(),
                76.dp(),
                Gravity.CENTER
            )
        }

        val pileImage = ImageView(this).apply {
            setImageResource(R.drawable.draw_pile)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = null

            layoutParams = FrameLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            )
        }

        val drawText = TextView(this).apply {
            text = "Draw"
            textSize = 13f
            gravity = Gravity.CENTER
            includeFontPadding = false
            setTextColor(getColor(android.R.color.white))

            layoutParams = FrameLayout.LayoutParams(
                48.dp(),
                32.dp(),
                Gravity.CENTER
            )

            // Verschiebt den Text in die Mitte der obersten Karte.
            translationX = 4.dp().toFloat()
            translationY = (-4).dp().toFloat()
        }

        pileContainer.addView(pileImage)
        pileContainer.addView(drawText)

        drawField.addView(pileContainer)
        blackBorder.addView(drawField)

        return blackBorder
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