package kittycards.kittycardsandroid.logic;

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

/**
 *
 *
 * @author JellyMae
 * @author red_concrete
 */
public class GameController implements IGameController {

    /**
     * Speichert die eine gemeinsame Instanz von GameController (?)
     */
    private static GameController INSTANCE;
    private Match match;
    private Player localPlayer;
    private Role role = Role.NOT_CONNECTED;
    private final List<GameColor> receivedBoardColors = new ArrayList<>();
    private MoveValidator moveValidator;
    private INetworkManager networkManager;
    private Runnable onStateChangedListener;


    // --- Constructor ---

    /**
     * Constructor
     */
    private GameController() {

    }


    // --- Getters and Setters ---

    /**
     * Bedeutet: Wenn es noch keinen GameController gibt: Erstelle einen.
     * Sonst: Gib mir den vorhandenen zurück.
     *
     * @return die GameController Instanz
     */
    public static GameController getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GameController();
        }

        return INSTANCE;
    }

    public Match getMatch() {
        return match;
    }

    public void setLocalPlayer(Player localPlayer) {
        this.localPlayer = localPlayer;
    }

    public Player getRemotePlayer() {
        return match.getOtherPlayer(localPlayer);
    }

    @Override
    public void setOnStateChangedListener(Runnable listener) {
        this.onStateChangedListener = listener;
    }

    public void setNetworkManager(INetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public void setNetworkRole(Role role) {
        this.role = role;
    }


    // --- Assistance Methods ---

    private Card generateCard() {
        int value = (int) (Math.random() * 6) + 1;
        int colorIndex = (int) (Math.random() * (GameColor.values().length - 1));
        GameColor color = GameColor.values()[colorIndex];

        return new Card(color, value);
    }

    private int calculateScore(Card card, Field field) {
        GameColor cardColor = card.getColor();
        GameColor fieldColor = field.getColor();

        if(fieldColor == GameColor.WHITE) {
            return card.getValue();
        }
        else if(cardColor == fieldColor) {
            return card.getValue() * 2;
        }
        else {
            return 0;
        }
    }

    private void switchTurn() {
        GameState gameState = match.getGameState();

        gameState.setCurrentPlayer(
                match.getOtherPlayer(gameState.getCurrentPlayer())
        );
    }

    private void notifyStateChanged() {
        if(onStateChangedListener != null) {
            onStateChangedListener.run();
        }
    }

    // Sendet eine GameAction ans Network, aber nur wenn networkManager != null ist
    private void sendGameAction(GameAction action) {
        if(networkManager != null) {
            networkManager.sendGameChange(action);
        }
    }

    // Startet einen neuen Thread. Dieser ruft dauerhaft fetchNextAction() auf und wartet
    // auf neue Nachrichten vom anderen Gerät. Wenn eine Nachricht kommt, wird
    // handleRemoteAction(action) ausgeführt.
    public void startListeningForActions() {
        new Thread(() -> {
            while (role != Role.NOT_CONNECTED) {
                try {
                    GameAction action = networkManager.fetchNextAction();
                    handleRemoteAction(action);
                } catch (InterruptedException e) {
                    role = Role.NOT_CONNECTED;
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    // Schaut, welche Action empfangen wurde:
    //
    //  DRAW_CARD       → applyDrawCard(...)
    //  PLAY_CARD       → applyPlayCard(...)
    //  SELECT_CARD     → applySelectCard(...)
    //  UNSELECT_CARD   → applyUnselectCard(...)
    //  SET_BOARD_COLOR → applyBoardColor(...)
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
        }

        notifyStateChanged();
    }

    private void applyBoardColor(GameColor color) {
        receivedBoardColors.add(color);

        if (receivedBoardColors.size() == 8) {
            match.startNextRound(receivedBoardColors);
            receivedBoardColors.clear();
        }
    }


    // --- GameController Management ---

    @Override
    public void startMatch(Player playerOne, Player playerTwo) {
        applyStartMatch(playerOne, playerTwo);

        if (role == Role.HOST) {
            sendBoardSetup();
        }

        notifyStateChanged();
    }

    private void applyStartMatch(Player playerOne, Player playerTwo) {
        this.match = new Match(playerOne, playerTwo);
        this.moveValidator = new MoveValidator(getMatch());
        this.match.setMatchStatus(MatchStatus.RUNNING);
    }


    @Override
    public void selectCard(Player player, Card card) {
        if(!moveValidator.canSelectCard(player, card)) {
            return;
        }

        applySelectCard(player, card);

        sendGameAction(new GameAction(GameAction.ActionType.SELECT_CARD, card));
        notifyStateChanged();
    }

    private void applySelectCard(Player player, Card card) {
        player.selectCard(card);
    }


    @Override
    public void unselectCard(Player player) {
        applyUnselectCard(player);

        sendGameAction(new GameAction(GameAction.ActionType.UNSELECT_CARD));
        notifyStateChanged();
    }

    private void applyUnselectCard(Player player) {
        player.unselectCard();
    }


    @Override
    public void playCard(Player player, int row, int column) {
        if(!moveValidator.canPlayCard(player, row, column)) {
            return;
        }

        Card selectedCard = player.getSelectedCard();

        applyPlayCard(player, selectedCard, row, column);

        sendGameAction(new GameAction(
                GameAction.ActionType.PLAY_CARD,
                selectedCard,
                column,
                row
        ));

        notifyStateChanged();
    }

    // Führt den Kartenzug wirklich aus: Karte aufs Feld, Punkte berechnen, Karte entfernen,
    // ggf. Zug wechseln. Falls das Board voll ist, startet aktuell nur der Host die nächste
    // Runde und sendet danach Boardfarben.
    private void applyPlayCard(Player player, Card card, int row, int column) {
        GameState gameState = match.getGameState();
        Field chosenField = gameState.getBoard().getField(row, column);

        chosenField.placeCard(card);
        player.addScore(calculateScore(card, chosenField));
        player.unselectCard();
        player.removeCard(card);

        if (gameState.isGameOver()) {
            if ( role == Role.HOST) {
                match.startNextRound();
                sendBoardSetup();
            }
            return;
        }

        switchTurn();
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


    @Override
    public void drawCard(Player player) {
        if(!moveValidator.canDrawCard(player)) {
            return;
        }

        Card card = generateCard();

        applyDrawCard(player, card);

        sendGameAction(new GameAction(
                GameAction.ActionType.DRAW_CARD,
                card
        ));

        notifyStateChanged();
    }

    private void applyDrawCard(Player player, Card card) {
        player.addCard(card);
        switchTurn();
    }
}