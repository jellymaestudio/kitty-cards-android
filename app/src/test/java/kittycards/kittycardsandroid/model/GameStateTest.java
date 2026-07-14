package kittycards.kittycardsandroid.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GameStateTest {

    private static final List<GameColor> FIELD_COLORS = List.of(
            GameColor.YELLOW,
            GameColor.GREY,
            GameColor.GREEN,
            GameColor.CYAN,
            GameColor.GREY,
            GameColor.PURPLE,
            GameColor.GREY,
            GameColor.GREY
    );

    private Player startingPlayer;
    private Player secondPlayer;

    @BeforeEach
    void setUp() {
        startingPlayer = new Player(1, "Player One");
        secondPlayer = new Player(2, "Player Two");
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    @Test
    void defaultBoardConstructorAcceptsValidPlayers() {
        assertDoesNotThrow(
                () -> new GameState(startingPlayer, secondPlayer)
        );
    }

    @Test
    void predefinedBoardConstructorAcceptsValidArguments() {
        assertDoesNotThrow(
                () -> new GameState(
                        startingPlayer,
                        secondPlayer,
                        FIELD_COLORS
                )
        );
    }

    @Test
    void defaultBoardConstructorRejectsNullStartingPlayer() {
        assertThrows(
                NullPointerException.class,
                () -> new GameState(null, secondPlayer)
        );
    }

    @Test
    void defaultBoardConstructorRejectsNullSecondPlayer() {
        assertThrows(
                NullPointerException.class,
                () -> new GameState(startingPlayer, null)
        );
    }

    @Test
    void defaultBoardConstructorRejectsBothPlayersBeingNull() {
        assertThrows(
                NullPointerException.class,
                () -> new GameState(null, null)
        );
    }

    @Test
    void predefinedBoardConstructorRejectsNullStartingPlayer() {
        assertThrows(
                NullPointerException.class,
                () -> new GameState(
                        null,
                        secondPlayer,
                        FIELD_COLORS
                )
        );
    }

    @Test
    void predefinedBoardConstructorRejectsNullSecondPlayer() {
        assertThrows(
                NullPointerException.class,
                () -> new GameState(
                        startingPlayer,
                        null,
                        FIELD_COLORS
                )
        );
    }

    @Test
    void predefinedBoardConstructorRejectsNullFieldColors() {
        assertThrows(
                NullPointerException.class,
                () -> new GameState(
                        startingPlayer,
                        secondPlayer,
                        null
                )
        );
    }

    @Test
    void predefinedBoardConstructorRejectsBothPlayersAndColorsBeingNull() {
        assertThrows(
                NullPointerException.class,
                () -> new GameState(null, null, null)
        );
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    void defaultBoardConstructorCreatesBoard() {
        GameState gameState = new GameState(
                startingPlayer,
                secondPlayer
        );

        assertNotNull(gameState.getBoard());
    }

    @Test
    void predefinedBoardConstructorCreatesBoard() {
        GameState gameState = createGameState();

        assertNotNull(gameState.getBoard());
    }

    @Test
    void predefinedBoardConstructorUsesProvidedFieldColors() {
        GameState gameState = createGameState();

        assertEquals(
                FIELD_COLORS,
                gameState.getBoard().getFieldColors()
        );
    }

    @Test
    void predefinedBoardConstructorCreatesGreyCenterField() {
        GameState gameState = createGameState();

        assertEquals(
                GameColor.GREY,
                gameState.getBoard().getField(1, 1).getColor()
        );
    }

    @Test
    void newGameStateUsesStartingPlayerAsCurrentPlayer() {
        GameState gameState = createGameState();

        assertSame(
                startingPlayer,
                gameState.getCurrentPlayer()
        );
    }

    @Test
    void newGameStateIsNotOver() {
        GameState gameState = createGameState();

        assertFalse(gameState.isGameOver());
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    @Test
    void getStartingPlayerReturnsProvidedStartingPlayer() {
        GameState gameState = createGameState();

        assertSame(
                startingPlayer,
                gameState.getStartingPlayer()
        );
    }

    @Test
    void getSecondPlayerReturnsProvidedSecondPlayer() {
        GameState gameState = createGameState();

        assertSame(
                secondPlayer,
                gameState.getSecondPlayer()
        );
    }

    @Test
    void getCurrentPlayerInitiallyReturnsStartingPlayer() {
        GameState gameState = createGameState();

        assertSame(
                startingPlayer,
                gameState.getCurrentPlayer()
        );
    }

    @Test
    void getBoardReturnsSameBoardInstanceOnRepeatedAccess() {
        GameState gameState = createGameState();

        Board firstResult = gameState.getBoard();
        Board secondResult = gameState.getBoard();

        assertSame(firstResult, secondResult);
    }

    // -------------------------------------------------------------------------
    // setCurrentPlayer
    // -------------------------------------------------------------------------

    @Test
    void setCurrentPlayerAcceptsStartingPlayer() {
        GameState gameState = createGameState();

        gameState.setCurrentPlayer(startingPlayer);

        assertSame(
                startingPlayer,
                gameState.getCurrentPlayer()
        );
    }

    @Test
    void setCurrentPlayerAcceptsSecondPlayer() {
        GameState gameState = createGameState();

        gameState.setCurrentPlayer(secondPlayer);

        assertSame(
                secondPlayer,
                gameState.getCurrentPlayer()
        );
    }

    @Test
    void setCurrentPlayerCanSwitchBetweenPlayers() {
        GameState gameState = createGameState();

        gameState.setCurrentPlayer(secondPlayer);
        assertSame(secondPlayer, gameState.getCurrentPlayer());

        gameState.setCurrentPlayer(startingPlayer);
        assertSame(startingPlayer, gameState.getCurrentPlayer());
    }

    @Test
    void setCurrentPlayerCanSetSamePlayerAgain() {
        GameState gameState = createGameState();

        gameState.setCurrentPlayer(startingPlayer);

        assertSame(
                startingPlayer,
                gameState.getCurrentPlayer()
        );
    }

    @Test
    void setCurrentPlayerRejectsNull() {
        GameState gameState = createGameState();

        assertThrows(
                NullPointerException.class,
                () -> gameState.setCurrentPlayer(null)
        );
    }

    @Test
    void setCurrentPlayerRejectsForeignPlayer() {
        GameState gameState = createGameState();
        Player foreignPlayer = new Player(3, "Foreign Player");

        assertThrows(
                IllegalArgumentException.class,
                () -> gameState.setCurrentPlayer(foreignPlayer)
        );
    }

    @Test
    void rejectedNullCurrentPlayerDoesNotChangeCurrentPlayer() {
        GameState gameState = createGameState();

        assertThrows(
                NullPointerException.class,
                () -> gameState.setCurrentPlayer(null)
        );

        assertSame(
                startingPlayer,
                gameState.getCurrentPlayer()
        );
    }

    @Test
    void rejectedForeignPlayerDoesNotChangeCurrentPlayer() {
        GameState gameState = createGameState();
        Player foreignPlayer = new Player(3, "Foreign Player");

        assertThrows(
                IllegalArgumentException.class,
                () -> gameState.setCurrentPlayer(foreignPlayer)
        );

        assertSame(
                startingPlayer,
                gameState.getCurrentPlayer()
        );
    }

    @Test
    void setCurrentPlayerUsesPlayerIdentity() {
        GameState gameState = createGameState();
        Player playerWithSameIdAndName =
                new Player(startingPlayer.getId(), startingPlayer.getName());

        assertThrows(
                IllegalArgumentException.class,
                () -> gameState.setCurrentPlayer(playerWithSameIdAndName)
        );
    }

    // -------------------------------------------------------------------------
    // isGameOver
    // -------------------------------------------------------------------------

    @Test
    void isGameOverReturnsFalseForEmptyBoard() {
        GameState gameState = createGameState();

        assertFalse(gameState.isGameOver());
    }

    @ParameterizedTest(name = "One occupied field at ({0}, {1}) should not end game")
    @MethodSource("providePlayablePositions")
    void isGameOverReturnsFalseWhenOnlyOneFieldIsOccupied(
            int row,
            int column
    ) {
        GameState gameState = createGameState();

        occupyField(gameState, row, column, 1);

        assertFalse(gameState.isGameOver());
    }

    @Test
    void isGameOverReturnsFalseWhenSevenPlayableFieldsAreOccupied() {
        GameState gameState = createGameState();

        int occupiedFields = 0;

        for (int[] position : playablePositions()) {
            if (occupiedFields == 7) {
                break;
            }

            occupyField(
                    gameState,
                    position[0],
                    position[1],
                    occupiedFields % 6 + 1
            );

            occupiedFields++;
        }

        assertFalse(gameState.isGameOver());
    }

    @Test
    void isGameOverReturnsTrueWhenAllPlayableFieldsAreOccupied() {
        GameState gameState = createGameState();

        occupyAllPlayableFields(gameState);

        assertTrue(gameState.isGameOver());
    }

    @Test
    void isGameOverIgnoresEmptyCenterField() {
        GameState gameState = createGameState();

        occupyAllPlayableFields(gameState);

        assertTrue(gameState.getBoard().getField(1, 1).isEmpty());
        assertTrue(gameState.isGameOver());
    }

    @Test
    void isGameOverReturnsFalseWhenOnlyCenterFieldIsOccupied() {
        GameState gameState = createGameState();

        occupyField(gameState, 1, 1, 1);

        assertFalse(gameState.isGameOver());
    }

    @Test
    void isGameOverReturnsFalseWhenCenterAndSevenPlayableFieldsAreOccupied() {
        GameState gameState = createGameState();

        occupyField(gameState, 1, 1, 1);

        int occupiedFields = 0;

        for (int[] position : playablePositions()) {
            if (occupiedFields == 7) {
                break;
            }

            occupyField(
                    gameState,
                    position[0],
                    position[1],
                    occupiedFields % 6 + 1
            );

            occupiedFields++;
        }

        assertFalse(gameState.isGameOver());
    }

    @Test
    void isGameOverReturnsFalseAgainAfterBoardIsCleared() {
        GameState gameState = createGameState();

        occupyAllPlayableFields(gameState);
        assertTrue(gameState.isGameOver());

        gameState.getBoard().clearBoard();

        assertFalse(gameState.isGameOver());
    }

    @Test
    void isGameOverDoesNotChangeCurrentPlayer() {
        GameState gameState = createGameState();
        occupyAllPlayableFields(gameState);

        gameState.isGameOver();

        assertSame(
                startingPlayer,
                gameState.getCurrentPlayer()
        );
    }

    @Test
    void isGameOverDoesNotModifyBoard() {
        GameState gameState = createGameState();
        occupyAllPlayableFields(gameState);

        gameState.isGameOver();

        for (int[] position : playablePositions()) {
            assertFalse(
                    gameState.getBoard()
                            .getField(position[0], position[1])
                            .isEmpty()
            );
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private GameState createGameState() {
        return new GameState(
                startingPlayer,
                secondPlayer,
                FIELD_COLORS
        );
    }

    private void occupyAllPlayableFields(GameState gameState) {
        int value = 1;

        for (int[] position : playablePositions()) {
            occupyField(
                    gameState,
                    position[0],
                    position[1],
                    value
            );

            value = value % 6 + 1;
        }
    }

    private void occupyField(
            GameState gameState,
            int row,
            int column,
            int value
    ) {
        gameState.getBoard()
                .getField(row, column)
                .placeCard(
                        new Card(GameColor.YELLOW, value),
                        startingPlayer,
                        value
                );
    }

    private static List<int[]> playablePositions() {
        return List.of(
                new int[]{0, 0},
                new int[]{0, 1},
                new int[]{0, 2},
                new int[]{1, 0},
                new int[]{1, 2},
                new int[]{2, 0},
                new int[]{2, 1},
                new int[]{2, 2}
        );
    }

    private static Stream<Arguments> providePlayablePositions() {
        return playablePositions()
                .stream()
                .map(position ->
                        Arguments.of(position[0], position[1])
                );
    }
}