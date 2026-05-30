package kittycards.kittycardsandroid.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class GameStateTest {

    @Test
    public void constructorShouldCreateBoard() {
        Player startingPlayer = new Player(1, "Player One");
        Player secondPlayer = new Player(2, "Player Two");

        GameState gameState = new GameState(startingPlayer, secondPlayer);

        assertNotNull(gameState.getBoard());
    }

    @Test
    public void constructorShouldSetStartingPlayer() {
        Player startingPlayer = new Player(1, "Player One");
        Player secondPlayer = new Player(2, "Player Two");

        GameState gameState = new GameState(startingPlayer, secondPlayer);

        assertEquals(startingPlayer, gameState.getStartingPlayer());
    }

    @Test
    public void constructorShouldSetSecondPlayer() {
        Player startingPlayer = new Player(1, "Player One");
        Player secondPlayer = new Player(2, "Player Two");

        GameState gameState = new GameState(startingPlayer, secondPlayer);

        assertEquals(secondPlayer, gameState.getSecondPlayer());
    }

    @Test
    public void constructorShouldSetCurrentPlayerToStartingPlayer() {
        Player startingPlayer = new Player(1, "Player One");
        Player secondPlayer = new Player(2, "Player Two");

        GameState gameState = new GameState(startingPlayer, secondPlayer);

        assertEquals(startingPlayer, gameState.getCurrentPlayer());
    }

    @Test
    public void constructorShouldThrowExceptionIfStartingPlayerIsNull() {
        Player secondPlayer = new Player(2, "Player Two");

        assertThrows(NullPointerException.class, () -> new GameState(null, secondPlayer));
    }

    @Test
    public void constructorShouldThrowExceptionIfSecondPlayerIsNull() {
        Player startingPlayer = new Player(1, "Player One");

        assertThrows(NullPointerException.class, () -> new GameState(startingPlayer, null));
    }

    @Test
    public void setCurrentPlayerShouldChangeCurrentPlayer() {
        Player startingPlayer = new Player(1, "Player One");
        Player secondPlayer = new Player(2, "Player Two");
        GameState gameState = new GameState(startingPlayer, secondPlayer);

        gameState.setCurrentPlayer(secondPlayer);

        assertEquals(secondPlayer, gameState.getCurrentPlayer());
    }

    @Test
    public void setCurrentPlayerShouldThrowExceptionIfCurrentPlayerIsNull() {
        Player startingPlayer = new Player(1, "Player One");
        Player secondPlayer = new Player(2, "Player Two");
        GameState gameState = new GameState(startingPlayer, secondPlayer);

        assertThrows(NullPointerException.class, () -> gameState.setCurrentPlayer(null));
    }

    @Test
    public void setCurrentPlayerShouldThrowExceptionIfPlayerIsNotPartOfGame() {
        Player startingPlayer = new Player(1, "Player One");
        Player secondPlayer = new Player(2, "Player Two");
        Player otherPlayer = new Player(3, "Other Player");
        GameState gameState = new GameState(startingPlayer, secondPlayer);

        assertThrows(IllegalArgumentException.class, () -> gameState.setCurrentPlayer(otherPlayer));
    }

    @Test
    public void isGameOverShouldReturnFalseIfBoardIsNotFull() {
        Player startingPlayer = new Player(1, "Player One");
        Player secondPlayer = new Player(2, "Player Two");
        GameState gameState = new GameState(startingPlayer, secondPlayer);

        assertFalse(gameState.isGameOver());
    }

    @Test
    public void isGameOverShouldReturnTrueIfBoardIsFull() {
        Player startingPlayer = new Player(1, "Player One");
        Player secondPlayer = new Player(2, "Player Two");
        GameState gameState = new GameState(startingPlayer, secondPlayer);

        fillPlayableFields(gameState.getBoard());

        assertTrue(gameState.isGameOver());
    }

    private void fillPlayableFields(Board board) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (!board.isCenterField(row, column)) {
                    board.getField(row, column).placeCard(new Card(GameColor.RED, 1));
                }
            }
        }
    }
}