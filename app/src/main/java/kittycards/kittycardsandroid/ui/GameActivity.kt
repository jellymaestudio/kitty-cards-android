package kittycards.kittycardsandroid.ui

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import kittycards.kittycardsandroid.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kittycards.kittycardsandroid.components.IGameController
import kittycards.kittycardsandroid.components.INetworkManager
import kittycards.kittycardsandroid.logic.GameSessionController
import kittycards.kittycardsandroid.model.Card
import kittycards.kittycardsandroid.model.Field
import kittycards.kittycardsandroid.model.GameColor
import kittycards.kittycardsandroid.model.MatchStatus
import kittycards.kittycardsandroid.model.RoundResult
import kittycards.kittycardsandroid.ui.util.GameColorMapper
import kittycards.kittycardsandroid.network.OnGameConnectionListener

@AndroidEntryPoint
class GameActivity : AppCompatActivity() {

    private lateinit var opponentHandContainer: LinearLayout
    private lateinit var playerHandContainer: LinearLayout
    private lateinit var boardContainer: GridLayout
    private lateinit var roundResultContainer: LinearLayout
    private lateinit var opponentScoreText: TextView
    private lateinit var playerScoreText: TextView
    private lateinit var turnInfoText: TextView
    private lateinit var opponentHandScrollView: HorizontalScrollView
    private lateinit var playerHandScrollView: HorizontalScrollView

    private var lastCurrentPlayerId: Int? = null
    private var matchEndHandled = false
    private var sessionEnding = false
    private var bluetoothReceiverRegistered = false

    private val bluetoothStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (
                    intent?.action !=
                    BluetoothAdapter.ACTION_STATE_CHANGED
                ) {
                    return
                }

                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )

                if (
                    state == BluetoothAdapter.STATE_TURNING_OFF ||
                    state == BluetoothAdapter.STATE_OFF
                ) {
                    handleOpponentDisconnected()
                }
            }
        }

    @Inject lateinit var gameController: IGameController
    @Inject lateinit var networkManager: INetworkManager
    @Inject lateinit var gameSessionController: GameSessionController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_game)

        bindViews()
        applyWindowInsets()

        // gameController is injected

        // gameSessionController is injected

        setupSessionControllerCallbacks()

        gameController.setOnStateChangedListener {
            runOnUiThread {
                renderGameState()
            }
        }

        gameController.setOnMatchAbortedListener {
            runOnUiThread {
                handleOpponentDisconnected()
            }
        }

        setupGameConnectionListener()
        registerBluetoothStateReceiver()

        renderGameState()
    }

    private fun bindViews() {
        opponentHandContainer = findViewById(R.id.opponentHandContainer)
        playerHandContainer = findViewById(R.id.playerHandContainer)
        boardContainer = findViewById(R.id.boardContainer)
        roundResultContainer = findViewById(R.id.roundResultContainer)
        opponentScoreText = findViewById(R.id.opponentScoreText)
        playerScoreText = findViewById(R.id.playerScoreText)
        turnInfoText = findViewById(R.id.turnInfoText)
        opponentHandScrollView = findViewById(R.id.opponentHandScrollView)
        playerHandScrollView = findViewById(R.id.playerHandScrollView)
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

    private fun renderGameState() {
        val match = gameController.match ?: return
        val localPlayer = gameController.localPlayer ?: return
        val remotePlayer = gameController.remotePlayer
        val gameState = match.gameState

        val isLocalPlayersTurn =
            gameState.currentPlayer == localPlayer

        renderOpponentHand(
            cardCount = remotePlayer.handCardCount,
            isActivePlayer = !isLocalPlayersTurn
        )

        renderPlayerHand(
            cards = localPlayer.handCards,
            selectedCard = localPlayer.selectedCard,
            isActivePlayer = isLocalPlayersTurn
        )

        updateHandBoxSizes(isLocalPlayersTurn)

        renderRoundResults()
        renderBoard()

        opponentScoreText.text = remotePlayer.score.toString()
        playerScoreText.text = localPlayer.score.toString()

        playerScoreText.setBackgroundColor(
            getLocalPlayerColor()
        )

        opponentScoreText.setBackgroundColor(
            getRemotePlayerColor()
        )

        if (match.matchStatus == MatchStatus.FINISHED) {
            showMatchResult()
        } else {
            updateTurnMessage(
                currentPlayerId = gameState.currentPlayer.id,
                isLocalPlayersTurn = isLocalPlayersTurn
            )
        }
    }

    private fun renderOpponentHand(
        cardCount: Int,
        isActivePlayer: Boolean
    ) {
        opponentHandContainer.removeAllViews()

        repeat(cardCount) {
            opponentHandContainer.addView(
                createHiddenCardView(isActivePlayer)
            )
        }
    }

    private fun renderPlayerHand(
        cards: List<Card>,
        selectedCard: Card?,
        isActivePlayer: Boolean
    ) {
        playerHandContainer.removeAllViews()

        cards.sortedWith(
            compareBy<Card> { colorSortValue(it.color) }
                .thenByDescending { it.value }
        ).forEach { card ->
            playerHandContainer.addView(
                createVisibleCardView(
                    card = card,
                    isSelected = card == selectedCard,
                    isActivePlayer = isActivePlayer
                )
            )
        }
    }

    private fun renderRoundResults() {
        roundResultContainer.removeAllViews()

        val matchState = gameController.match
            ?.matchState
            ?: return

        val roundResults = matchState.roundResults

        for (roundIndex in 0 until matchState.maxRounds) {
            val result = roundResults.getOrNull(roundIndex)

            val resultView = when (result) {
                RoundResult.PLAYER_ONE_WIN -> {
                    createSolidRoundResultView(
                        getColor(R.color.kc_host)
                    )
                }

                RoundResult.PLAYER_TWO_WIN -> {
                    createSolidRoundResultView(
                        getColor(R.color.kc_guest)
                    )
                }

                RoundResult.DRAW -> {
                    createDrawRoundResultView()
                }

                null -> {
                    createSolidRoundResultView(
                        getColor(android.R.color.white)
                    )
                }
            }

            resultView.layoutParams = LinearLayout.LayoutParams(
                0,
                MATCH_PARENT,
                1f
            ).apply {
                if (roundIndex < matchState.maxRounds - 1) {
                    marginEnd = 2.dp()
                }
            }

            roundResultContainer.addView(resultView)
        }
    }

    private fun renderBoard() {
        boardContainer.removeAllViews()

        val board = gameController.match
            ?.gameState
            ?.board
            ?: return

        for (row in 0..2) {
            for (column in 0..2) {
                if (row == 1 && column == 1) {
                    boardContainer.addView(
                        createDrawPileView()
                    )
                } else {
                    val field = board.getField(row, column)

                    boardContainer.addView(
                        createBoardFieldView(
                            field = field,
                            row = row,
                            column = column
                        )
                    )
                }
            }
        }
    }

    private fun createHiddenCardView(
        isActivePlayer: Boolean
    ): View {
        val cardHeight =
            if (isActivePlayer) {
                80.dp()
            } else {
                68.dp()
            }

        return View(this).apply {
            setBackgroundColor(getRemotePlayerColor())

            layoutParams = LinearLayout.LayoutParams(
                48.dp(),
                cardHeight
            ).apply {
                marginEnd = 8.dp()
            }
        }
    }

    private fun createVisibleCardView(
        card: Card,
        isSelected: Boolean,
        isActivePlayer: Boolean
    ): View {
        val cardHeight =
            if (isActivePlayer) {
                88.dp()
            } else {
                76.dp()
            }

        val cardBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE

            setColor(
                GameColorMapper.toAndroidColor(card.color)
            )

            if (isSelected) {
                setStroke(
                    3.dp(),
                    getColor(android.R.color.white)
                )
            }
        }

        val cardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = cardBackground

            setPadding(
                4.dp(),
                6.dp(),
                4.dp(),
                4.dp()
            )

            layoutParams = LinearLayout.LayoutParams(
                56.dp(),
                cardHeight
            ).apply {
                marginEnd = 8.dp()
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                val localPlayer =
                    gameController.localPlayer
                        ?: return@setOnClickListener

                if (localPlayer.selectedCard == card) {
                    gameController.unselectCard(localPlayer)
                } else {
                    gameController.selectCard(
                        localPlayer,
                        card
                    )
                }
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
        field: Field,
        row: Int,
        column: Int
    ): View {
        val placedCard = field.card

        val blackBorder = FrameLayout(this).apply {
            setBackgroundColor(getColor(android.R.color.black))
            setPadding(1.dp(), 1.dp(), 1.dp(), 1.dp())

            layoutParams = GridLayout.LayoutParams().apply {
                width = 100.dp()
                height = 100.dp()
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                val localPlayer =
                    gameController.localPlayer
                        ?: return@setOnClickListener

                gameController.playCard(
                    localPlayer,
                    row,
                    column
                )
            }
        }

        val fieldColorFrame = FrameLayout(this).apply {
            setBackgroundColor(
                GameColorMapper.toAndroidColor(field.color)
            )
            setPadding(7.dp(), 7.dp(), 7.dp(), 7.dp())

            layoutParams = FrameLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            )
        }

        val cardArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL

            setBackgroundColor(
                placedCard?.let { card ->
                    GameColorMapper.toAndroidColor(card.color)
                } ?: getColor(android.R.color.white)
            )

            layoutParams = FrameLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            )
        }

        if (placedCard != null) {
            val scoreText = TextView(this).apply {
                text = field.displayedScore.toString()
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

            /*
             * Player 0 is always the host.
             * Player 1 is always the guest.
             */
            val ownerColor =
                if (field.cardOwnerId == 0) {
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

            isClickable = true
            isFocusable = true

            setOnClickListener {
                val localPlayer =
                    gameController.localPlayer
                        ?: return@setOnClickListener

                gameController.drawCard(localPlayer)
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

    private fun createSolidRoundResultView(
        color: Int
    ): View {
        return View(this).apply {
            setBackgroundColor(color)
        }
    }

    private fun createDrawRoundResultView(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL

            addView(
                View(this@GameActivity).apply {
                    setBackgroundColor(
                        getColor(R.color.kc_host)
                    )

                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        MATCH_PARENT,
                        1f
                    )
                }
            )

            addView(
                View(this@GameActivity).apply {
                    setBackgroundColor(
                        getColor(R.color.kc_guest)
                    )

                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        MATCH_PARENT,
                        1f
                    )
                }
            )
        }
    }

    private fun updateHandBoxSizes(
        isLocalPlayersTurn: Boolean
    ) {
        setHandBoxHeight(
            scrollView = playerHandScrollView,
            isActivePlayer = isLocalPlayersTurn
        )

        setHandBoxHeight(
            scrollView = opponentHandScrollView,
            isActivePlayer = !isLocalPlayersTurn
        )
    }

    private fun setHandBoxHeight(
        scrollView: HorizontalScrollView,
        isActivePlayer: Boolean
    ) {
        val targetHeight =
            if (isActivePlayer) {
                104.dp()
            } else {
                92.dp()
            }

        if (scrollView.layoutParams.height == targetHeight) {
            return
        }

        scrollView.layoutParams =
            scrollView.layoutParams.apply {
                height = targetHeight
            }

        scrollView.requestLayout()
    }

    private fun updateTurnMessage(
        currentPlayerId: Int,
        isLocalPlayersTurn: Boolean
    ) {
        /*
         * Nur auf einen tatsächlichen Zugwechsel reagieren.
         * Dadurch startet die Einblendung nicht bei jedem Neurendern erneut.
         */
        if (lastCurrentPlayerId == currentPlayerId) {
            return
        }

        lastCurrentPlayerId = currentPlayerId
        turnInfoText.removeCallbacks(hideTurnInfoRunnable)

        if (isLocalPlayersTurn) {
            turnInfoText.visibility = View.VISIBLE

            turnInfoText.postDelayed(
                hideTurnInfoRunnable,
                1_500L
            )
        } else {
            turnInfoText.visibility = View.GONE
        }
    }

    private val hideTurnInfoRunnable = Runnable {
        turnInfoText.visibility = View.GONE
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

    private fun showMatchResult() {
        if (matchEndHandled) {
            return
        }

        val match = gameController.match ?: return
        val localPlayer = gameController.localPlayer ?: return
        val matchState = match.matchState

        matchEndHandled = true
        sessionEnding = true

        turnInfoText.removeCallbacks(hideTurnInfoRunnable)

        turnInfoText.text = when {
            matchState.isDraw -> {
                "Match Draw!"
            }

            matchState.matchWinner == localPlayer -> {
                "You Win!"
            }

            else -> {
                "You Lose!"
            }
        }

        turnInfoText.visibility = View.VISIBLE

        gameSessionController.finishRegularSession(
            delayMillis = 3_000L
        )
    }

    private fun getLocalPlayerColor(): Int {
        return if (gameController.localPlayer?.id == 0) {
            getColor(R.color.kc_host)
        } else {
            getColor(R.color.kc_guest)
        }
    }

    private fun getRemotePlayerColor(): Int {
        return if (gameController.localPlayer?.id == 0) {
            getColor(R.color.kc_guest)
        } else {
            getColor(R.color.kc_host)
        }
    }

    private fun setupGameConnectionListener() {
        networkManager
            .setGameConnectionListener(
                object : OnGameConnectionListener {
                    override fun onGamePartnerDisconnected() {
                        handleOpponentDisconnected()
                    }
                }
            )
    }

    private fun setupSessionControllerCallbacks() {
        gameSessionController.onOpponentDisconnected = {
            sessionEnding = true
            showOpponentDisconnectedMessage()
        }

        gameSessionController.onSessionClosed = {
            openLobby()
        }
    }

    private fun showOpponentDisconnectedMessage() {
        turnInfoText.removeCallbacks(hideTurnInfoRunnable)
        turnInfoText.text = "Opponent disconnected"
        turnInfoText.visibility = View.VISIBLE
    }

    private fun openLobby() {
        val intent = Intent(
            this,
            LobbyActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        startActivity(intent)
        finish()
    }

    private fun handleOpponentDisconnected() {
        if (matchEndHandled) {
            return
        }

        gameSessionController.handleRemoteDisconnect(
            delayMillis = 2_000L
        )
    }

    private fun registerBluetoothStateReceiver() {
        if (bluetoothReceiverRegistered) {
            return
        }

        ContextCompat.registerReceiver(
            this,
            bluetoothStateReceiver,
            IntentFilter(
                BluetoothAdapter.ACTION_STATE_CHANGED
            ),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        bluetoothReceiverRegistered = true
    }

    override fun onDestroy() {
        turnInfoText.removeCallbacks(hideTurnInfoRunnable)

        networkManager
            .setGameConnectionListener(null)

        gameController.setOnStateChangedListener(null)
        gameController.setOnMatchAbortedListener(null)

        gameSessionController.cleanup()

        if (bluetoothReceiverRegistered) {
            unregisterReceiver(bluetoothStateReceiver)
            bluetoothReceiverRegistered = false
        }

        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()

        if (
            !isChangingConfigurations &&
            !sessionEnding &&
            !matchEndHandled &&
            !gameSessionController.isSessionEnding()
        ) {
            sessionEnding = true

            gameSessionController.abortLocalSession(
                sendDelayMillis = 400L
            )
        }
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}