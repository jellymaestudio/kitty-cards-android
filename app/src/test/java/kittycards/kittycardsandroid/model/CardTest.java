package kittycards.kittycardsandroid.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class CardTest {

    @Test
    public void constructorShouldSetColor() {
        Card card = new Card(GameColor.RED, 3);

        assertEquals(GameColor.RED, card.getColor());
    }

    @Test
    public void constructorShouldSetValue() {
        Card card = new Card(GameColor.BLUE, 5);

        assertEquals(5, card.getValue());
    }

    @Test
    public void constructorShouldThrowExceptionIfColorIsNull() {
        assertThrows(NullPointerException.class, () -> new Card(null, 3));
    }

    @Test
    public void constructorShouldThrowExceptionIfColorIsWhite() {
        assertThrows(IllegalArgumentException.class, () -> new Card(GameColor.WHITE, 3));
    }

    @Test
    public void constructorShouldThrowExceptionIfValueIsLowerThanOne() {
        assertThrows(IllegalArgumentException.class, () -> new Card(GameColor.RED, 0));
    }

    @Test
    public void constructorShouldThrowExceptionIfValueIsHigherThanSix() {
        assertThrows(IllegalArgumentException.class, () -> new Card(GameColor.RED, 7));
    }

    @Test
    public void constructorShouldAllowMinimumValue() {
        Card card = new Card(GameColor.YELLOW, 1);

        assertEquals(1, card.getValue());
    }

    @Test
    public void constructorShouldAllowMaximumValue() {
        Card card = new Card(GameColor.GREEN, 6);

        assertEquals(6, card.getValue());
    }
}