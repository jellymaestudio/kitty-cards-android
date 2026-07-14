package kittycards.kittycardsandroid.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FieldTest {

    private Card card;
    private Player player;

    @BeforeEach
    void setUp() {
        card = new Card(GameColor.YELLOW, 3);
        player = new Player(1, "Player One");
    }

    @ParameterizedTest(name = "Field at ({1}, {2}) with color {0} should be valid")
    @MethodSource("provideValidFieldData")
    void constructorAcceptsValidColorAndPosition(
            GameColor color,
            int row,
            int column
    ) {
        assertDoesNotThrow(() -> new Field(color, row, column));
    }

    @Test
    void constructorRejectsNullColor() {
        assertThrows(
                NullPointerException.class,
                () -> new Field(null, 0, 0)
        );
    }

    @ParameterizedTest(name = "Row {0} should be rejected")
    @ValueSource(ints = {
            Integer.MIN_VALUE,
            -100,
            -1,
            3,
            4,
            100,
            Integer.MAX_VALUE
    })
    void constructorRejectsRowOutsideBoard(int invalidRow) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Field(GameColor.YELLOW, invalidRow, 0)
        );
    }

    @ParameterizedTest(name = "Column {0} should be rejected")
    @ValueSource(ints = {
            Integer.MIN_VALUE,
            -100,
            -1,
            3,
            4,
            100,
            Integer.MAX_VALUE
    })
    void constructorRejectsColumnOutsideBoard(int invalidColumn) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Field(GameColor.YELLOW, 0, invalidColumn)
        );
    }

    @ParameterizedTest(name = "Position ({0}, {1}) should be rejected")
    @MethodSource("provideInvalidPositions")
    void constructorRejectsInvalidRowAndColumn(
            int invalidRow,
            int invalidColumn
    ) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Field(
                        GameColor.YELLOW,
                        invalidRow,
                        invalidColumn
                )
        );
    }

    @ParameterizedTest(name = "getColor should return {0}")
    @EnumSource(GameColor.class)
    void getColorReturnsConstructorColor(GameColor color) {
        Field field = new Field(color, 0, 0);

        assertEquals(color, field.getColor());
    }

    @ParameterizedTest(name = "setColor should update color to {0}")
    @EnumSource(GameColor.class)
    void setColorUpdatesFieldColor(GameColor newColor) {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        field.setColor(newColor);

        assertEquals(newColor, field.getColor());
    }

    @Test
    void setColorRejectsNull() {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        assertThrows(
                NullPointerException.class,
                () -> field.setColor(null)
        );
    }

    @Test
    void setColorDoesNotChangeColorWhenNullIsRejected() {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        assertThrows(
                NullPointerException.class,
                () -> field.setColor(null)
        );

        assertEquals(GameColor.YELLOW, field.getColor());
    }

    @ParameterizedTest(name = "getRow should return {0}")
    @ValueSource(ints = {0, 1, 2})
    void getRowReturnsConstructorRow(int row) {
        Field field = new Field(GameColor.GREEN, row, 0);

        assertEquals(row, field.getRow());
    }

    @ParameterizedTest(name = "getColumn should return {0}")
    @ValueSource(ints = {0, 1, 2})
    void getColumnReturnsConstructorColumn(int column) {
        Field field = new Field(GameColor.CYAN, 0, column);

        assertEquals(column, field.getColumn());
    }

    @Test
    void newFieldContainsNoCard() {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        assertNull(field.getCard());
    }

    @Test
    void newFieldHasNoCardOwner() {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        assertEquals(-1, field.getCardOwnerId());
    }

    @Test
    void newFieldHasDisplayedScoreOfZero() {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        assertEquals(0, field.getDisplayedScore());
    }

    @Test
    void newFieldIsEmpty() {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        assertTrue(field.isEmpty());
    }

    @Test
    void placeCardStoresCard() {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        field.placeCard(card, player, 3);

        assertSame(card, field.getCard());
    }

    @Test
    void placeCardStoresPlayerIdAsOwner() {
        Player placingPlayer = new Player(42, "Player");
        Field field = new Field(GameColor.GREEN, 0, 0);

        field.placeCard(card, placingPlayer, 3);

        assertEquals(42, field.getCardOwnerId());
    }

    @ParameterizedTest(name = "Displayed score {0} should be stored")
    @ValueSource(ints = {
            0,
            1,
            3,
            6,
            12,
            100,
            Integer.MAX_VALUE
    })
    void placeCardStoresNonNegativeDisplayedScore(int displayedScore) {
        Field field = new Field(GameColor.PURPLE, 0, 0);

        field.placeCard(card, player, displayedScore);

        assertEquals(displayedScore, field.getDisplayedScore());
    }

    @Test
    void placeCardMarksFieldAsOccupied() {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        field.placeCard(card, player, 3);

        assertFalse(field.isEmpty());
    }

    @Test
    void placeCardRejectsNullCard() {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        assertThrows(
                NullPointerException.class,
                () -> field.placeCard(null, player, 3)
        );
    }

    @Test
    void placeCardRejectsNullPlayer() {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        assertThrows(
                NullPointerException.class,
                () -> field.placeCard(card, null, 3)
        );
    }

    @ParameterizedTest(name = "Displayed score {0} should be rejected")
    @ValueSource(ints = {
            Integer.MIN_VALUE,
            -100,
            -2,
            -1
    })
    void placeCardRejectsNegativeDisplayedScore(int displayedScore) {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> field.placeCard(card, player, displayedScore)
        );
    }

    @Test
    void failedPlacementWithNullCardLeavesFieldEmpty() {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        assertThrows(
                NullPointerException.class,
                () -> field.placeCard(null, player, 3)
        );

        assertFieldIsEmpty(field);
    }

    @Test
    void failedPlacementWithNullPlayerLeavesFieldEmpty() {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        assertThrows(
                NullPointerException.class,
                () -> field.placeCard(card, null, 3)
        );

        assertFieldIsEmpty(field);
    }

    @Test
    void failedPlacementWithNegativeScoreLeavesFieldEmpty() {
        Field field = new Field(GameColor.YELLOW, 0, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> field.placeCard(card, player, -1)
        );

        assertFieldIsEmpty(field);
    }

    @Test
    void placeCardRejectsPlacementOnOccupiedField() {
        Field field = new Field(GameColor.YELLOW, 0, 0);
        field.placeCard(card, player, 3);

        Card secondCard = new Card(GameColor.GREEN, 5);
        Player secondPlayer = new Player(2, "Player Two");

        assertThrows(
                IllegalArgumentException.class,
                () -> field.placeCard(secondCard, secondPlayer, 5)
        );
    }

    @Test
    void rejectedSecondPlacementDoesNotReplaceExistingCard() {
        Field field = new Field(GameColor.YELLOW, 0, 0);
        field.placeCard(card, player, 3);

        Card secondCard = new Card(GameColor.GREEN, 5);
        Player secondPlayer = new Player(2, "Player Two");

        assertThrows(
                IllegalArgumentException.class,
                () -> field.placeCard(secondCard, secondPlayer, 5)
        );

        assertSame(card, field.getCard());
        assertEquals(player.getId(), field.getCardOwnerId());
        assertEquals(3, field.getDisplayedScore());
        assertFalse(field.isEmpty());
    }

    @Test
    void clearFieldRemovesPlacedCard() {
        Field field = createOccupiedField();

        field.clearField();

        assertNull(field.getCard());
    }

    @Test
    void clearFieldResetsCardOwnerId() {
        Field field = createOccupiedField();

        field.clearField();

        assertEquals(-1, field.getCardOwnerId());
    }

    @Test
    void clearFieldResetsDisplayedScore() {
        Field field = createOccupiedField();

        field.clearField();

        assertEquals(0, field.getDisplayedScore());
    }

    @Test
    void clearFieldMarksFieldAsEmpty() {
        Field field = createOccupiedField();

        field.clearField();

        assertTrue(field.isEmpty());
    }

    @Test
    void clearFieldResetsAllCardRelatedData() {
        Field field = createOccupiedField();

        field.clearField();

        assertFieldIsEmpty(field);
    }

    @Test
    void clearFieldOnEmptyFieldDoesNotThrowException() {
        Field field = new Field(GameColor.CYAN, 2, 2);

        assertDoesNotThrow(field::clearField);
    }

    @Test
    void clearFieldOnEmptyFieldKeepsEmptyState() {
        Field field = new Field(GameColor.CYAN, 2, 2);

        field.clearField();

        assertFieldIsEmpty(field);
    }

    @Test
    void clearFieldDoesNotChangeFieldColorOrPosition() {
        Field field = new Field(GameColor.PURPLE, 2, 1);
        field.placeCard(card, player, 6);

        field.clearField();

        assertEquals(GameColor.PURPLE, field.getColor());
        assertEquals(2, field.getRow());
        assertEquals(1, field.getColumn());
    }

    @Test
    void fieldCanAcceptAnotherCardAfterBeingCleared() {
        Field field = createOccupiedField();
        Card secondCard = new Card(GameColor.GREEN, 6);
        Player secondPlayer = new Player(2, "Player Two");

        field.clearField();
        field.placeCard(secondCard, secondPlayer, 12);

        assertSame(secondCard, field.getCard());
        assertEquals(secondPlayer.getId(), field.getCardOwnerId());
        assertEquals(12, field.getDisplayedScore());
        assertFalse(field.isEmpty());
    }

    private Field createOccupiedField() {
        Field field = new Field(GameColor.YELLOW, 0, 0);
        field.placeCard(card, player, 3);
        return field;
    }

    private void assertFieldIsEmpty(Field field) {
        assertNull(field.getCard());
        assertEquals(-1, field.getCardOwnerId());
        assertEquals(0, field.getDisplayedScore());
        assertTrue(field.isEmpty());
    }

    private Stream<Arguments> provideValidFieldData() {
        return Stream.of(
                Arguments.of(GameColor.YELLOW, 0, 0),
                Arguments.of(GameColor.GREEN, 0, 2),
                Arguments.of(GameColor.CYAN, 1, 1),
                Arguments.of(GameColor.PURPLE, 2, 0),
                Arguments.of(GameColor.GREY, 2, 2)
        );
    }

    private Stream<Arguments> provideInvalidPositions() {
        return Stream.of(
                Arguments.of(-1, -1),
                Arguments.of(-1, 3),
                Arguments.of(3, -1),
                Arguments.of(3, 3),
                Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE),
                Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE)
        );
    }
}