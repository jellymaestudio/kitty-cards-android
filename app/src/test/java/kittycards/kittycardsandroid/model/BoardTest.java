package kittycards.kittycardsandroid.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class BoardTest {

    private static final List<GameColor> PREDEFINED_COLORS = List.of(
            GameColor.YELLOW,
            GameColor.GREY,
            GameColor.GREEN,
            GameColor.CYAN,
            GameColor.GREY,
            GameColor.PURPLE,
            GameColor.GREY,
            GameColor.GREY
    );

    private Board board;
    private Player player;

    @BeforeEach
    void setUp() {
        board = new Board(PREDEFINED_COLORS);
        player = new Player(1, "Player One");
    }

    @Test
    void defaultConstructorCreatesBoardWithoutThrowing() {
        assertDoesNotThrow(() -> new Board());
    }

    @Test
    void predefinedColorConstructorAcceptsEightColors() {
        assertDoesNotThrow(() -> new Board(PREDEFINED_COLORS));
    }

    @Test
    void predefinedColorConstructorRejectsNullList() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Board(null)
        );
    }

    @ParameterizedTest(name = "List size {0} should be rejected")
    @ValueSource(ints = {0, 1, 7, 9, 10, 100})
    void predefinedColorConstructorRejectsListWithInvalidSize(int size) {
        List<GameColor> colors = new ArrayList<>();

        for (int index = 0; index < size; index++) {
            colors.add(GameColor.YELLOW);
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> new Board(colors)
        );
    }

    @Test
    void predefinedColorConstructorAssignsColorsInBoardOrder() {
        assertEquals(GameColor.YELLOW, board.getField(0, 0).getColor());
        assertEquals(GameColor.GREY, board.getField(0, 1).getColor());
        assertEquals(GameColor.GREEN, board.getField(0, 2).getColor());
        assertEquals(GameColor.CYAN, board.getField(1, 0).getColor());
        assertEquals(GameColor.GREY, board.getField(1, 2).getColor());
        assertEquals(GameColor.PURPLE, board.getField(2, 0).getColor());
        assertEquals(GameColor.GREY, board.getField(2, 1).getColor());
        assertEquals(GameColor.GREY, board.getField(2, 2).getColor());
    }

    @Test
    void predefinedColorConstructorAlwaysSetsCenterFieldToGrey() {
        List<GameColor> colorsWithoutGrey = List.of(
                GameColor.YELLOW,
                GameColor.GREEN,
                GameColor.CYAN,
                GameColor.PURPLE,
                GameColor.YELLOW,
                GameColor.GREEN,
                GameColor.CYAN,
                GameColor.PURPLE
        );

        Board createdBoard = new Board(colorsWithoutGrey);

        assertEquals(GameColor.GREY, createdBoard.getField(1, 1).getColor());
    }

    @Test
    void predefinedColorConstructorDoesNotConsumeColorForCenterField() {
        List<GameColor> colors = List.of(
                GameColor.YELLOW,
                GameColor.GREEN,
                GameColor.CYAN,
                GameColor.PURPLE,
                GameColor.YELLOW,
                GameColor.GREEN,
                GameColor.CYAN,
                GameColor.PURPLE
        );

        Board createdBoard = new Board(colors);

        assertEquals(GameColor.PURPLE, createdBoard.getField(1, 0).getColor());
        assertEquals(GameColor.YELLOW, createdBoard.getField(1, 2).getColor());
    }

    @Test
    void getFieldColorsReturnsAllEightNonCenterColorsInBoardOrder() {
        assertEquals(PREDEFINED_COLORS, board.getFieldColors());
    }

    @Test
    void getFieldColorsDoesNotIncludeCenterFieldColor() {
        Board createdBoard = new Board(List.of(
                GameColor.YELLOW,
                GameColor.GREEN,
                GameColor.CYAN,
                GameColor.PURPLE,
                GameColor.YELLOW,
                GameColor.GREEN,
                GameColor.CYAN,
                GameColor.PURPLE
        ));

        List<GameColor> fieldColors = createdBoard.getFieldColors();

        assertEquals(8, fieldColors.size());
        assertFalse(fieldColors.contains(GameColor.GREY));
    }

    @Test
    void defaultBoardReturnsEightNonCenterFieldColors() {
        Board randomBoard = new Board();

        assertEquals(8, randomBoard.getFieldColors().size());
    }

    @ParameterizedTest(name = "Position ({0}, {1}) should be on board")
    @MethodSource("provideAllBoardPositions")
    void isOnBoardReturnsTrueForValidPosition(int row, int column) {
        assertTrue(board.isOnBoard(row, column));
    }

    @ParameterizedTest(name = "Position ({0}, {1}) should be outside board")
    @MethodSource("provideInvalidBoardPositions")
    void isOnBoardReturnsFalseForInvalidPosition(int row, int column) {
        assertFalse(board.isOnBoard(row, column));
    }

    @ParameterizedTest(name = "getField should return field at ({0}, {1})")
    @MethodSource("provideAllBoardPositions")
    void getFieldReturnsFieldAtRequestedPosition(int row, int column) {
        Field field = board.getField(row, column);

        assertNotNull(field);
        assertEquals(row, field.getRow());
        assertEquals(column, field.getColumn());
    }

    @ParameterizedTest(name = "getField should reject position ({0}, {1})")
    @MethodSource("provideInvalidBoardPositions")
    void getFieldRejectsPositionOutsideBoard(int row, int column) {
        assertThrows(
                IllegalArgumentException.class,
                () -> board.getField(row, column)
        );
    }

    @Test
    void getFieldReturnsSameFieldForRepeatedAccess() {
        Field firstResult = board.getField(0, 0);
        Field secondResult = board.getField(0, 0);

        assertEquals(firstResult, secondResult);
    }

    @Test
    void allBoardPositionsContainInitializedFields() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                assertNotNull(board.getField(row, column));
            }
        }
    }

    @ParameterizedTest(name = "Position ({0}, {1}) should not be center")
    @MethodSource("providePlayableBoardPositions")
    void isCenterFieldReturnsFalseForPlayableFields(int row, int column) {
        assertFalse(board.isCenterField(row, column));
    }

    @Test
    void isCenterFieldReturnsTrueForCenterPosition() {
        assertTrue(board.isCenterField(1, 1));
    }

    @ParameterizedTest(name = "Outside position ({0}, {1}) should not be center")
    @MethodSource("provideInvalidBoardPositions")
    void isCenterFieldReturnsFalseForPositionsOutsideBoard(int row, int column) {
        assertFalse(board.isCenterField(row, column));
    }

    @Test
    void centerFieldHasCorrectPosition() {
        Field centerField = board.getField(1, 1);

        assertEquals(1, centerField.getRow());
        assertEquals(1, centerField.getColumn());
    }

    @Test
    void centerFieldIsGrey() {
        assertEquals(GameColor.GREY, board.getField(1, 1).getColor());
    }

    @Test
    void centerFieldIsInitiallyEmpty() {
        assertTrue(board.getField(1, 1).isEmpty());
    }

    @Test
    void newBoardIsNotFull() {
        assertFalse(board.isFull());
    }

    @Test
    void boardWithOneOccupiedFieldIsNotFull() {
        occupyField(board.getField(0, 0), 1);

        assertFalse(board.isFull());
    }

    @Test
    void boardWithSevenOccupiedPlayableFieldsIsNotFull() {
        int value = 1;

        for (int[] position : playablePositions()) {
            if (position[0] == 2 && position[1] == 2) {
                continue;
            }

            occupyField(board.getField(position[0], position[1]), value);
            value = value % 6 + 1;
        }

        assertFalse(board.isFull());
    }

    @Test
    void boardWithAllEightPlayableFieldsOccupiedIsFull() {
        occupyAllPlayableFields();

        assertTrue(board.isFull());
    }

    @Test
    void isFullIgnoresEmptyCenterField() {
        occupyAllPlayableFields();

        assertTrue(board.getField(1, 1).isEmpty());
        assertTrue(board.isFull());
    }

    @Test
    void occupyingOnlyCenterFieldDoesNotMakeBoardFull() {
        occupyField(board.getField(1, 1), 1);

        assertFalse(board.isFull());
    }

    @Test
    void clearBoardRemovesCardsFromAllPlayableFields() {
        occupyAllPlayableFields();

        board.clearBoard();

        for (int[] position : playablePositions()) {
            assertTrue(board.getField(position[0], position[1]).isEmpty());
        }
    }

    @Test
    void clearBoardAlsoClearsCenterFieldWhenOccupied() {
        occupyField(board.getField(1, 1), 1);

        board.clearBoard();

        assertTrue(board.getField(1, 1).isEmpty());
    }

    @Test
    void clearBoardMakesFullBoardNotFull() {
        occupyAllPlayableFields();
        assertTrue(board.isFull());

        board.clearBoard();

        assertFalse(board.isFull());
    }

    @Test
    void clearBoardPreservesFieldColors() {
        List<GameColor> colorsBeforeClear = new ArrayList<>(board.getFieldColors());
        occupyAllPlayableFields();

        board.clearBoard();

        assertEquals(colorsBeforeClear, board.getFieldColors());
        assertEquals(GameColor.GREY, board.getField(1, 1).getColor());
    }

    @Test
    void clearBoardPreservesFieldPositions() {
        occupyAllPlayableFields();

        board.clearBoard();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                Field field = board.getField(row, column);

                assertEquals(row, field.getRow());
                assertEquals(column, field.getColumn());
            }
        }
    }

    @Test
    void clearBoardResetsCardOwnerAndDisplayedScore() {
        Field field = board.getField(0, 0);
        field.placeCard(new Card(GameColor.YELLOW, 4), player, 8);

        board.clearBoard();

        assertEquals(-1, field.getCardOwnerId());
        assertEquals(0, field.getDisplayedScore());
        assertTrue(field.isEmpty());
    }

    @Test
    void clearBoardDoesNotThrowWhenBoardIsAlreadyEmpty() {
        assertDoesNotThrow(board::clearBoard);
    }

    @Test
    void boardCanBeFilledAgainAfterBeingCleared() {
        occupyAllPlayableFields();
        board.clearBoard();

        occupyAllPlayableFields();

        assertTrue(board.isFull());
    }

    private void occupyAllPlayableFields() {
        int value = 1;

        for (int[] position : playablePositions()) {
            occupyField(board.getField(position[0], position[1]), value);
            value = value % 6 + 1;
        }
    }

    private void occupyField(Field field, int value) {
        field.placeCard(
                new Card(GameColor.YELLOW, value),
                player,
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

    private static Stream<Arguments> provideAllBoardPositions() {
        return Stream.of(
                Arguments.of(0, 0),
                Arguments.of(0, 1),
                Arguments.of(0, 2),
                Arguments.of(1, 0),
                Arguments.of(1, 1),
                Arguments.of(1, 2),
                Arguments.of(2, 0),
                Arguments.of(2, 1),
                Arguments.of(2, 2)
        );
    }

    private static Stream<Arguments> providePlayableBoardPositions() {
        return playablePositions()
                .stream()
                .map(position -> Arguments.of(position[0], position[1]));
    }

    private static Stream<Arguments> provideInvalidBoardPositions() {
        return Stream.of(
                Arguments.of(-1, 0),
                Arguments.of(0, -1),
                Arguments.of(3, 0),
                Arguments.of(0, 3),
                Arguments.of(-1, -1),
                Arguments.of(-1, 3),
                Arguments.of(3, -1),
                Arguments.of(3, 3),
                Arguments.of(Integer.MIN_VALUE, 0),
                Arguments.of(0, Integer.MIN_VALUE),
                Arguments.of(Integer.MAX_VALUE, 0),
                Arguments.of(0, Integer.MAX_VALUE)
        );
    }
}