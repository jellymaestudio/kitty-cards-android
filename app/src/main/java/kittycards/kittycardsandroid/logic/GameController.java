package kittycards.kittycardsandroid.logic;

import android.annotation.SuppressLint;

import java.util.ArrayList;
import java.util.List;

import kittycards.kittycardsandroid.components.IGameController;
import kittycards.kittycardsandroid.components.INetworkManager;
import kittycards.kittycardsandroid.model.Board;
import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.Field;
import kittycards.kittycardsandroid.model.GameColor;
import kittycards.kittycardsandroid.model.GameState;
import kittycards.kittycardsandroid.model.Match;
import kittycards.kittycardsandroid.model.MatchStatus;
import kittycards.kittycardsandroid.model.Player;
import kittycards.kittycardsandroid.network.GameAction;
import kittycards.kittycardsandroid.network.Role;

/**
 * Controls the game flow of a Kitty Cards match.
 * <p>
 * The GameController is responsible for validating player actions,
 * updating the game state, handling turn changes and coordinating
 * network communication between connected devices.
 * <p>
 * Implemented as a singleton to provide a single shared controller
 * instance throughout the application.
 *
 * @author JellyMae
 */
public class GameController implements IGameController {

    // --- Fields ---

    private static GameController INSTANCE;

    private Match match;
    private Player localPlayer;
    private volatile Role role = Role.NOT_CONNECTED;
    private final List<GameColor> receivedBoardColors = new ArrayList<>();
    private Player receivedStartingPlayer;
    private MoveValidator moveValidator;
    private INetworkManager networkManager;
    private Runnable onStateChangedListener;
    private Runnable onMatchAbortedListener;
    private static final int STARTING_PLAYER_INITIAL_CARDS = 2;
    private static final int SECOND_PLAYER_INITIAL_CARDS = 3;
    private boolean initialRoundSetupReceived;
    private volatile boolean listeningForActions;
    private Thread actionListenerThread;


    // --- Constructor ---

    private GameController() {

    }


    // --- Singleton ---

    /**
     * Returns the singleton instance of the GameController.
     * Creates the instance if it does not already exist.
     *
     * @return the GameController instance
     */
    public static GameController getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GameController();
        }

        return INSTANCE;
    }


    // --- Configuration ---

    /**
     * Returns the currently active match.
     *
     * @return the active match
     */
    public Match getMatch() {
        return match;
    }

    /**
     * Sets the player controlled by the local device.
     *
     * @param localPlayer the local player
     */
    public void setLocalPlayer(Player localPlayer) {
        this.localPlayer = localPlayer;
    }

    /**
     * Returns the player controlled by the remote device.
     *
     * @return the remote player
     */
    public Player getRemotePlayer() {
        return match.getOtherPlayer(localPlayer);
    }

    /**
     * Returns the player controlled by the local device.
     *
     * @return the local player
     */
    public Player getLocalPlayer() {
        return localPlayer;
    }


    @Override
    public void setOnStateChangedListener(Runnable listener) {
        this.onStateChangedListener = listener;
    }

    public void setOnMatchAbortedListener(Runnable listener) {
        this.onMatchAbortedListener = listener;
    }

    /**
     * Sets the NetworkManager used for sending and receiving game actions.
     *
     * @param networkManager the network manager
     */
    public void setNetworkManager(INetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /**
     * Sets the role of this device within the network connection.
     *
     * @param role the network role
     */
    public void setNetworkRole(Role role) {
        this.role = role;
    }

    /**
     * Returns whether the controller currently listens for network actions.
     *
     * @return true if the action listener is running
     */
    public boolean isListeningForActions() {
        return listeningForActions;
    }


    // --- Public Game Actions ---

    @Override
    public void startMatch(Player playerOne, Player playerTwo) {
        applyStartMatch(playerOne, playerTwo);

        if (role == Role.HOST) {
            sendStartingPlayer();
            sendBoardSetup();
            dealInitialCards();
        }

        notifyStateChanged();
    }


    @Override
    public void selectCard(Player player, Card card) {
        if (!moveValidator.canSelectCard(player, card)) {
            return;
        }

        sendGameAction(new GameAction(GameAction.ActionType.SELECT_CARD, card));
        applySelectCard(player, card);
        notifyStateChanged();
    }

    @Override
    public void unselectCard(Player player) {
        sendGameAction(new GameAction(GameAction.ActionType.UNSELECT_CARD));
        applyUnselectCard(player);
        notifyStateChanged();
    }


    @Override
    public void playCard(Player player, int row, int column) {
        if (!moveValidator.canPlayCard(player, row, column)) {
            return;
        }

        Card selectedCard = player.getSelectedCard();

        sendGameAction(new GameAction(
                GameAction.ActionType.PLAY_CARD,
                selectedCard,
                column,
                row
        ));

        applyPlayCard(player, selectedCard, row, column);
        notifyStateChanged();
    }


    @Override
    public void drawCard(Player player) {
        if (!moveValidator.canDrawCard(player)) {
            return;
        }

        Card card = generateCard();

        sendGameAction(new GameAction(
                GameAction.ActionType.DRAW_CARD,
                card
        ));

        applyDrawCard(player, card);
        notifyStateChanged();
    }


    // --- Remote Action Handling ---

    /**
     * Starts the background listener for incoming game actions.
     *
     * <p>Only one listener thread may run at a time.</p>
     *
     * @throws IllegalStateException if the controller has not been configured
     *                               for a network match
     */
    public synchronized void startListeningForActions() {
        if (listeningForActions) {
            return;
        }

        if (networkManager == null) {
            throw new IllegalStateException(
                    "NetworkManager must be set before starting the action listener"
            );
        }

        if (role == Role.NOT_CONNECTED) {
            throw new IllegalStateException(
                    "Network role must be set before starting the action listener"
            );
        }

        listeningForActions = true;

        actionListenerThread = new Thread(() -> {
            try {
                while (
                        listeningForActions
                                && role != Role.NOT_CONNECTED
                ) {
                    GameAction action =
                            networkManager.fetchNextAction();

                    /*
                     * The session may have been stopped while this thread
                     * was waiting for the next action.
                     */
                    if (
                            !listeningForActions
                                    || role == Role.NOT_CONNECTED
                    ) {
                        break;
                    }

                    handleRemoteAction(action);
                }
            } catch (InterruptedException exception) {
                /*
                 * Interrupting is the expected way to stop the blocking
                 * fetchNextAction() call.
                 */
                Thread.currentThread().interrupt();
            } finally {
                synchronized (GameController.this) {
                    listeningForActions = false;

                    if (
                            actionListenerThread
                                    == Thread.currentThread()
                    ) {
                        actionListenerThread = null;
                    }
                }
            }
        }, "GameActionListener");

        actionListenerThread.start();
    }

    /**
     * Stops the background listener for incoming game actions.
     *
     * <p>The listener thread is interrupted so that a blocking
     * fetchNextAction() call returns immediately.</p>
     */
    public synchronized void stopListeningForActions() {
        listeningForActions = false;

        if (actionListenerThread != null) {
            actionListenerThread.interrupt();
            actionListenerThread = null;
        }
    }

    /**
     * Clears all data belonging to the current game session.
     *
     * <p>This method must be called when a match ends or is aborted,
     * before another lobby or match session is started.</p>
     */
    public synchronized void resetSession() {
        stopListeningForActions();

        role = Role.NOT_CONNECTED;

        match = null;
        localPlayer = null;
        moveValidator = null;

        receivedBoardColors.clear();
        receivedStartingPlayer = null;
        initialRoundSetupReceived = false;

        onStateChangedListener = null;
        onMatchAbortedListener = null;
        networkManager = null;
    }

    /**
     * Processes a game action received from the remote device and applies
     * the corresponding state changes locally.
     *
     * @param action the received game action
     */
    public void handleRemoteAction(GameAction action) {
        switch (action.type()) {
            case DRAW_CARD:
                applyDrawCard(getRemotePlayer(), action.card());
                break;

            case PLAY_CARD:
                applyPlayCard(
                        getRemotePlayer(),
                        action.card(),
                        action.boardPositionRow(),
                        action.boardPositionColumn()
                );
                break;

            case SELECT_CARD:
                applySelectCard(getRemotePlayer(), action.card());
                break;

            case UNSELECT_CARD:
                applyUnselectCard(getRemotePlayer());
                break;

            case SET_BOARD_COLOR:
                applyBoardColor(action.boardColor());
                break;

            case SET_STARTING_PLAYER:
                if (action.contextSensitiveInt() == 0) {
                    receivedStartingPlayer = match.getPlayerOne();
                } else {
                    receivedStartingPlayer = match.getPlayerTwo();
                }
                break;

            case DEAL_CARD:
                Player targetPlayer =
                        action.contextSensitiveInt() == 0
                                ? match.getPlayerOne()
                                : match.getPlayerTwo();

                targetPlayer.addCard(action.card());
                break;

            case MATCH_FINISHED:
                match.finishMatch();
                break;

            case MATCH_ABORTED:
                if (onMatchAbortedListener != null) {
                    onMatchAbortedListener.run();
                }
                break;
        }

        notifyStateChanged();
    }


    // --- Local Apply Methods ---

    private void applyStartMatch(Player playerOne, Player playerTwo) {
        this.match = new Match(playerOne, playerTwo);
        this.moveValidator = new MoveValidator(getMatch());
        this.match.setMatchStatus(MatchStatus.RUNNING);

        receivedBoardColors.clear();
        receivedStartingPlayer = null;
        initialRoundSetupReceived = role == Role.HOST;
    }


    private void applySelectCard(Player player, Card card) {
        player.selectCard(card);
    }

    private void applyUnselectCard(Player player) {
        player.unselectCard();
    }


    private void applyPlayCard(Player player, Card card, int row, int column) {
        GameState gameState = match.getGameState();
        Field chosenField = gameState.getBoard().getField(row, column);

        int score = calculateScore(card, chosenField);

        chosenField.placeCard(card, player, score);
        player.addScore(score);
        player.unselectCard();
        player.removeCard(card);

        if (gameState.isGameOver()) {
            if (role == Role.HOST) {
                match.startNextRound();

                if (match.getMatchStatus() == MatchStatus.FINISHED) {
                    sendGameAction(new GameAction(GameAction.ActionType.MATCH_FINISHED));
                }
                else {
                    sendStartingPlayer();
                    sendBoardSetup();
                    dealInitialCards();
                }
            }
            return;
        }

        switchTurn();
    }


    private void applyDrawCard(Player player, Card card) {
        player.addCard(card);
        switchTurn();
    }


    private void applyBoardColor(GameColor color) {
        receivedBoardColors.add(color);

        if (receivedBoardColors.size() != 8 || receivedStartingPlayer == null) {
            return;
        }

        if (!initialRoundSetupReceived) {
            match.initializeCurrentRound(
                    receivedBoardColors,
                    receivedStartingPlayer
            );

            initialRoundSetupReceived = true;
        } else {
            match.startNextRound(
                    receivedBoardColors,
                    receivedStartingPlayer
            );
        }

        receivedBoardColors.clear();
        receivedStartingPlayer = null;
    }


    // --- Helper Methods ---

    private Card generateCard() {
        int value = (int) (Math.random() * 6) + 1;
        int colorIndex = (int) (Math.random() * (GameColor.values().length - 1));
        GameColor color = GameColor.values()[colorIndex];

        return new Card(color, value);
    }

    private void dealInitialCards() {
        GameState gameState = match.getGameState();

        dealCards(
                gameState.getStartingPlayer(),
                STARTING_PLAYER_INITIAL_CARDS
        );

        dealCards(
                gameState.getSecondPlayer(),
                SECOND_PLAYER_INITIAL_CARDS
        );
    }

    private void dealCards(Player player, int amount) {
        int playerIndex = player == match.getPlayerOne() ? 0 : 1;

        for (int i = 0; i < amount; i++) {
            Card card = generateCard();

            player.addCard(card);

            sendGameAction(new GameAction(
                    GameAction.ActionType.DEAL_CARD,
                    card,
                    playerIndex
            ));
        }
    }

    private int calculateScore(Card card, Field field) {
        if (card == null || field == null) {
            throw new NullPointerException("card and field cannot be null");
        }

        GameColor cardColor = card.getColor();
        GameColor fieldColor = field.getColor();

        if (fieldColor == GameColor.GREY) {
            return card.getValue();
        }

        if (cardColor == fieldColor) {
            return card.getValue() * 2;
        }

        return 0;
    }

    private void switchTurn() {
        GameState gameState = match.getGameState();

        gameState.setCurrentPlayer(
                match.getOtherPlayer(gameState.getCurrentPlayer())
        );
    }

    private void notifyStateChanged() {
        if (onStateChangedListener != null) {
            onStateChangedListener.run();
        }
    }

    @SuppressLint("MissingPermission")
    private void sendGameAction(GameAction action) {
        if (networkManager != null) {
            networkManager.sendGameChange(action);
        }
    }

    private void sendBoardSetup() {
        Board board = match.getGameState().getBoard();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (!board.isCenterField(row, column)) {
                    sendGameAction(new GameAction(
                            GameAction.ActionType.SET_BOARD_COLOR,
                            board.getField(row, column).getColor(),
                            column,
                            row
                    ));
                }
            }
        }
    }

    private void sendStartingPlayer() {
        Player startingPlayer = match.getGameState().getStartingPlayer();

        int playerIndex = startingPlayer == match.getPlayerOne() ? 0 : 1;

        sendGameAction(new GameAction(
                GameAction.ActionType.SET_STARTING_PLAYER,
                playerIndex
        ));
    }
}