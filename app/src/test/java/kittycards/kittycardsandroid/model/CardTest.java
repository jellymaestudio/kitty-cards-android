package kittycards.kittycardsandroid.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class CardTest {

    @ParameterizedTest(name = "Creating a card with color {0} and value {1} should succeed")
    @MethodSource("provideValidCardData")
    void constructorAcceptsValidCardData(GameColor color, int value) {
        assertDoesNotThrow(() -> new Card(color, value));
    }

    @Test
    void constructorRejectsNullColor() {
        assertThrows(
                NullPointerException.class,
                () -> new Card(null, 1)
        );
    }

    @Test
    void constructorRejectsGreyAsCardColor() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Card(GameColor.GREY, 1)
        );
    }

    @ParameterizedTest(name = "Card value {0} should be rejected")
    @ValueSource(ints = {
            Integer.MIN_VALUE,
            -100,
            -1,
            0,
            7,
            8,
            100,
            Integer.MAX_VALUE
    })
    void constructorRejectsValuesOutsideValidRange(int invalidValue) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Card(GameColor.YELLOW, invalidValue)
        );
    }

    @ParameterizedTest(name = "getColor should return {0}")
    @EnumSource(
            value = GameColor.class,
            names = {"YELLOW", "GREEN", "CYAN", "PURPLE"}
    )
    void getColorReturnsConstructorColor(GameColor color) {
        Card card = new Card(color, 3);

        assertEquals(color, card.getColor());
    }

    @ParameterizedTest(name = "getValue should return {0}")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void getValueReturnsConstructorValue(int value) {
        Card card = new Card(GameColor.CYAN, value);

        assertEquals(value, card.getValue());
    }

    @Test
    void equalsReturnsTrueForSameReference() {
        Card card = new Card(GameColor.YELLOW, 4);

        assertEquals(card, card);
    }

    @Test
    void equalsReturnsTrueForCardsWithSameColorAndValue() {
        Card firstCard = new Card(GameColor.GREEN, 5);
        Card secondCard = new Card(GameColor.GREEN, 5);

        assertEquals(firstCard, secondCard);
    }

    @Test
    void equalsIsSymmetricForEqualCards() {
        Card firstCard = new Card(GameColor.PURPLE, 2);
        Card secondCard = new Card(GameColor.PURPLE, 2);

        assertEquals(firstCard, secondCard);
        assertEquals(secondCard, firstCard);
    }

    @Test
    void equalsIsTransitiveForEqualCards() {
        Card firstCard = new Card(GameColor.CYAN, 6);
        Card secondCard = new Card(GameColor.CYAN, 6);
        Card thirdCard = new Card(GameColor.CYAN, 6);

        assertEquals(firstCard, secondCard);
        assertEquals(secondCard, thirdCard);
        assertEquals(firstCard, thirdCard);
    }

    @Test
    void equalsReturnsFalseForDifferentColors() {
        Card firstCard = new Card(GameColor.YELLOW, 3);
        Card secondCard = new Card(GameColor.GREEN, 3);

        assertNotEquals(firstCard, secondCard);
    }

    @Test
    void equalsReturnsFalseForDifferentValues() {
        Card firstCard = new Card(GameColor.PURPLE, 2);
        Card secondCard = new Card(GameColor.PURPLE, 3);

        assertNotEquals(firstCard, secondCard);
    }

    @Test
    void equalsReturnsFalseForNull() {
        Card card = new Card(GameColor.YELLOW, 1);

        assertNotEquals(card, null);
    }

    @Test
    void equalsReturnsFalseForObjectOfDifferentType() {
        Card card = new Card(GameColor.YELLOW, 1);

        assertNotEquals(card, "YELLOW-1");
    }

    @Test
    void hashCodeIsEqualForEqualCards() {
        Card firstCard = new Card(GameColor.GREEN, 4);
        Card secondCard = new Card(GameColor.GREEN, 4);

        assertEquals(firstCard.hashCode(), secondCard.hashCode());
    }

    @Test
    void hashCodeIsConsistentForSameCard() {
        Card card = new Card(GameColor.CYAN, 5);

        int firstHashCode = card.hashCode();
        int secondHashCode = card.hashCode();

        assertEquals(firstHashCode, secondHashCode);
    }

    private static Stream<Arguments> provideValidCardData() {
        return Stream.of(
                Arguments.of(GameColor.YELLOW, 1),
                Arguments.of(GameColor.YELLOW, 6),
                Arguments.of(GameColor.GREEN, 1),
                Arguments.of(GameColor.GREEN, 6),
                Arguments.of(GameColor.CYAN, 1),
                Arguments.of(GameColor.CYAN, 6),
                Arguments.of(GameColor.PURPLE, 1),
                Arguments.of(GameColor.PURPLE, 6)
        );
    }
}