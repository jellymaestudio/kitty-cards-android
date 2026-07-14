package kittycards.kittycardsandroid.logic;

import org.junit.Test;

import kittycards.kittycardsandroid.components.FakeNetworkManager;
import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.GameColor;
import kittycards.kittycardsandroid.model.MatchStatus;
import kittycards.kittycardsandroid.model.Player;
import kittycards.kittycardsandroid.network.GameAction;

import static org.junit.Assert.*;

import java.util.List;

public class GameControllerTest {

    private GameController createController() {
        return new GameController(new FakeNetworkManager());
    }

    // --- Construction and Initialization Tests ---

    @Test
    public void constructionShouldReturnGameControllerInstance() {
        GameController gameController = createController();

        assertNotNull(gameController);
    }

    @Test
    public void startMatchShouldCreateMatch() {
        GameController gameController = createController();
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        gameController.startMatch(playerOne, playerTwo);

        assertNotNull(gameController.getMatch());
    }

    @Test
    public void startMatchShouldSetPlayerOne() {
        GameController gameController = createController();
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        gameController.startMatch(playerOne, playerTwo);

        assertEquals(playerOne, gameController.getMatch().getPlayerOne());
    }

    @Test
    public void startMatchShouldSetPlayerTwo() {
        GameController gameController = createController();
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        gameController.startMatch(playerOne, playerTwo);

        assertEquals(playerTwo, gameController.getMatch().getPlayerTwo());
    }

    @Test
    public void startMatchShouldSetMatchStatusToRunning() {
        GameController gameController = createController();
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        gameController.startMatch(playerOne, playerTwo);

        assertEquals(MatchStatus.RUNNING, gameController.getMatch().getMatchStatus());
    }


    // --- Local Game-Actions Tests ---

    @Test
    public void selectCardShouldSelectGivenCard() {
        GameController gameController = createStartedController();
        Player player = gameController.getMatch().getPlayerOne();
        Card card = new Card(GameColor.PURPLE, 3);

        player.addCard(card);

        gameController.selectCard(player, card);

        assertEquals(card, player.getSelectedCard());
    }

    @Test
    public void unselectCardShouldClearSelectedCard() {
        GameController gameController = createStartedController();
        Player player = gameController.getMatch().getPlayerOne();
        Card card = new Card(GameColor.PURPLE, 3);

        player.addCard(card);
        player.selectCard(card);

        gameController.unselectCard(player);

        assertNull(player.getSelectedCard());
    }

    @Test
    public void drawCardShouldAddCardAndSwitchTurn() {
        GameController gameController = createStartedController();
        Player currentPlayer = gameController.getMatch().getGameState().getCurrentPlayer();
        Player otherPlayer = gameController.getMatch().getOtherPlayer(currentPlayer);

        int handSizeBefore = currentPlayer.getHandCardCount();

        gameController.drawCard(currentPlayer);

        assertEquals(handSizeBefore + 1, currentPlayer.getHandCardCount());
        assertEquals(otherPlayer, gameController.getMatch().getGameState().getCurrentPlayer());
    }

    @Test
    public void drawCardShouldNotDrawIfHandIsFull() {
        GameController gameController = createStartedController();
        Player currentPlayer = gameController.getMatch().getGameState().getCurrentPlayer();

        for (int i = 0; i < 10; i++) {
            currentPlayer.addCard(new Card(GameColor.PURPLE, 1));
        }

        gameController.drawCard(currentPlayer);

        assertEquals(10, currentPlayer.getHandCardCount());
    }

    @Test
    public void playCardShouldPlaceSelectedCardRemoveItAndSwitchTurn() {
        GameController gameController = createStartedController();
        Player currentPlayer = gameController.getMatch().getGameState().getCurrentPlayer();
        Player otherPlayer = gameController.getMatch().getOtherPlayer(currentPlayer);
        Card card = new Card(GameColor.PURPLE, 3);

        currentPlayer.addCard(card);
        currentPlayer.selectCard(card);

        gameController.getMatch().getGameState().getBoard().getField(0, 0).setColor(GameColor.GREY);

        gameController.playCard(currentPlayer, 0, 0);

        assertEquals(card, gameController.getMatch().getGameState().getBoard().getField(0, 0).getCard());
        assertFalse(currentPlayer.hasCard(card));
        assertNull(currentPlayer.getSelectedCard());
        assertEquals(otherPlayer, gameController.getMatch().getGameState().getCurrentPlayer());
    }

    @Test
    public void playCardShouldAddDoubleScoreIfCardMatchesFieldColor() {
        GameController gameController = createStartedController();
        Player currentPlayer = gameController.getMatch().getGameState().getCurrentPlayer();
        Card card = new Card(GameColor.PURPLE, 3);

        currentPlayer.addCard(card);
        currentPlayer.selectCard(card);

        gameController.getMatch().getGameState().getBoard().getField(0, 0).setColor(GameColor.PURPLE);

        gameController.playCard(currentPlayer, 0, 0);

        assertEquals(6, currentPlayer.getScore());
    }

    @Test
    public void playCardShouldNotChangeStateIfPlayerHasNoSelectedCard() {
        GameController gameController = createStartedController();
        Player currentPlayer = gameController.getMatch().getGameState().getCurrentPlayer();

        gameController.playCard(currentPlayer, 0, 0);

        assertTrue(gameController.getMatch().getGameState().getBoard().getField(0, 0).isEmpty());
        assertEquals(currentPlayer, gameController.getMatch().getGameState().getCurrentPlayer());
    }


    // --- Remote-Actions Tests ---

    @Test
    public void handleRemoteActionDrawCardShouldAddCardToRemotePlayerAndSwitchTurn() {
        GameController gameController = createStartedController();
        Player localPlayer = gameController.getMatch().getPlayerOne();
        Player remotePlayer = gameController.getMatch().getPlayerTwo();

        gameController.setLocalPlayer(localPlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(remotePlayer);

        Card card = new Card(GameColor.PURPLE, 4);

        gameController.handleRemoteAction(
                new GameAction(GameAction.ActionType.DRAW_CARD, card)
        );

        assertTrue(remotePlayer.hasCard(card));
        assertEquals(localPlayer, gameController.getMatch().getGameState().getCurrentPlayer());
    }

    @Test
    public void handleRemoteActionSelectCardShouldSelectCardForRemotePlayer() {
        GameController gameController = createStartedController();
        Player remotePlayer = gameController.getMatch().getPlayerTwo();

        gameController.setLocalPlayer(gameController.getMatch().getPlayerOne());

        Card card = new Card(GameColor.PURPLE, 4);
        remotePlayer.addCard(card);

        gameController.handleRemoteAction(
                new GameAction(GameAction.ActionType.SELECT_CARD, card)
        );

        assertEquals(card, remotePlayer.getSelectedCard());
    }

    @Test
    public void handleRemoteActionUnselectCardShouldClearRemoteSelection() {
        GameController gameController = createStartedController();
        Player remotePlayer = gameController.getMatch().getPlayerTwo();

        gameController.setLocalPlayer(gameController.getMatch().getPlayerOne());

        Card card = new Card(GameColor.PURPLE, 4);
        remotePlayer.addCard(card);
        remotePlayer.selectCard(card);

        gameController.handleRemoteAction(
                new GameAction(GameAction.ActionType.UNSELECT_CARD)
        );

        assertNull(remotePlayer.getSelectedCard());
    }

    @Test
    public void handleRemoteActionPlayCardShouldPlaceRemoteCard() {
        GameController gameController = createStartedController();
        Player remotePlayer = gameController.getMatch().getPlayerTwo();

        gameController.setLocalPlayer(gameController.getMatch().getPlayerOne());
        gameController.getMatch().getGameState().setCurrentPlayer(remotePlayer);

        Card card = new Card(GameColor.PURPLE, 4);
        remotePlayer.addCard(card);
        remotePlayer.selectCard(card);

        gameController.handleRemoteAction(
                new GameAction(GameAction.ActionType.PLAY_CARD, card, 0, 0)
        );

        assertEquals(
                card,
                gameController.getMatch().getGameState().getBoard().getField(0, 0).getCard()
        );
        assertFalse(remotePlayer.hasCard(card));
    }


    // --- New Round-Synchronization Tests ---

    @Test
    public void handleRemoteActionSetStartingPlayerShouldStorePlayerOneAsNextStartingPlayer() {
        GameController gameController = createStartedController();

        gameController.handleRemoteAction(
                new GameAction(GameAction.ActionType.SET_STARTING_PLAYER, 0)
        );

        // Indirect test: After that, send 8 board colors and check whether PlayerOne starts
        sendEightBoardColors(gameController);

        assertEquals(
                gameController.getMatch().getPlayerOne(),
                gameController.getMatch().getGameState().getStartingPlayer()
        );
    }

    @Test
    public void handleRemoteActionSetStartingPlayerShouldStorePlayerTwoAsNextStartingPlayer() {
        GameController gameController = createStartedController();

        gameController.handleRemoteAction(
                new GameAction(GameAction.ActionType.SET_STARTING_PLAYER, 1)
        );

        sendEightBoardColors(gameController);

        assertEquals(
                gameController.getMatch().getPlayerTwo(),
                gameController.getMatch().getGameState().getStartingPlayer()
        );
    }

    @Test
    public void handleRemoteActionBoardColorsShouldCreateNextRoundWithReceivedColors() {
        GameController gameController = createStartedController();

        gameController.handleRemoteAction(
                new GameAction(GameAction.ActionType.SET_STARTING_PLAYER, 0)
        );

        List<GameColor> colors = sendEightBoardColors(gameController);

        assertEquals(colors, gameController.getMatch().getGameState().getBoard().getFieldColors());
    }



    // --- Helper Methods ---

    private GameController createStartedController() {
        GameController gameController = createController();

        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        gameController.startMatch(playerOne, playerTwo);

        return gameController;
    }

    private List<GameColor> sendEightBoardColors(GameController gameController) {
        List<GameColor> colors = List.of(
                GameColor.YELLOW,
                GameColor.GREEN,
                GameColor.CYAN,
                GameColor.PURPLE,
                GameColor.GREY,
                GameColor.YELLOW,
                GameColor.GREEN,
                GameColor.CYAN
        );

        for (GameColor color : colors) {
            gameController.handleRemoteAction(
                    new GameAction(
                            GameAction.ActionType.SET_BOARD_COLOR,
                            color,
                            0,
                            0
                    )
            );
        }

        return colors;
    }
}