package kittycards.kittycardsandroid.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class FieldTest {

    @Test
    public void constructorShouldSetColor() {
        Field field = new Field(GameColor.PURPLE, 0, 2);

        assertEquals(GameColor.PURPLE, field.getColor());
    }

    @Test
    public void constructorShouldSetRow() {
        Field field = new Field(GameColor.CYAN, 2, 1);

        assertEquals(2, field.getRow());
    }

    @Test
    public void constructorShouldSetColumn() {
        Field field = new Field(GameColor.GREEN, 1, 2);

        assertEquals(2, field.getColumn());
    }

    @Test
    public void constructorShouldInitializeFieldAsEmpty() {
        Field field = new Field(GameColor.YELLOW, 1, 1);

        assertTrue(field.isEmpty());
        assertNull(field.getCard());
    }

    @Test
    public void constructorShouldThrowExceptionIfColorIsNull() {
        assertThrows(NullPointerException.class, () -> new Field(null, 0, 0));
    }

    @Test
    public void constructorShouldThrowExceptionIfRowIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Field(GameColor.PURPLE, -1, 0));
    }

    @Test
    public void constructorShouldThrowExceptionIfRowIsTooHigh() {
        assertThrows(IllegalArgumentException.class, () -> new Field(GameColor.PURPLE, 3, 0));
    }

    @Test
    public void constructorShouldThrowExceptionIfColumnIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Field(GameColor.PURPLE, 0, -1));
    }

    @Test
    public void constructorShouldThrowExceptionIfColumnIsTooHigh() {
        assertThrows(IllegalArgumentException.class, () -> new Field(GameColor.PURPLE, 0, 3));
    }

    @Test
    public void placeCardShouldPlaceCardOnEmptyField() {
        Field field = new Field(GameColor.GREY, 0, 0);
        Card card = new Card(GameColor.PURPLE, 4);

        field.placeCard(card);

        assertFalse(field.isEmpty());
        assertEquals(card, field.getCard());
    }

    @Test
    public void placeCardShouldThrowExceptionIfCardIsNull() {
        Field field = new Field(GameColor.GREY, 0, 0);

        assertThrows(NullPointerException.class, () -> field.placeCard(null));
    }

    @Test
    public void placeCardShouldThrowExceptionIfFieldIsAlreadyOccupied() {
        Field field = new Field(GameColor.GREY, 0, 0);
        Card firstCard = new Card(GameColor.PURPLE, 2);
        Card secondCard = new Card(GameColor.CYAN, 5);

        field.placeCard(firstCard);

        assertThrows(IllegalArgumentException.class, () -> field.placeCard(secondCard));
    }

    @Test
    public void clearFieldShouldRemovePlacedCard() {
        Field field = new Field(GameColor.GREY, 2, 2);
        Card card = new Card(GameColor.GREEN, 6);

        field.placeCard(card);
        field.clearField();

        assertTrue(field.isEmpty());
        assertNull(field.getCard());
    }

    @Test
    public void clearFieldShouldKeepFieldEmptyIfAlreadyEmpty() {
        Field field = new Field(GameColor.GREY, 1, 1);

        field.clearField();

        assertTrue(field.isEmpty());
        assertNull(field.getCard());
    }
}