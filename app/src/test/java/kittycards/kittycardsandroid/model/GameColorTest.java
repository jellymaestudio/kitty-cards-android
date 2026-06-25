package kittycards.kittycardsandroid.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class GameColorTest {

    @Test
    public void getHexCodeShouldReturnCorrectHexCode() {
        assertEquals("#FFFCCF", GameColor.YELLOW.getHexCode());
        assertEquals("#D6FFD0", GameColor.GREEN.getHexCode());
        assertEquals("#D0FFF7", GameColor.CYAN.getHexCode());
        assertEquals("#F9DEFF", GameColor.PURPLE.getHexCode());
        assertEquals("#7D758D", GameColor.GREY.getHexCode());
    }

    @Test
    public void isCardColorShouldReturnTrueForRedYellowGreenAndBlue() {
        assertTrue(GameColor.PURPLE.isCardColor());
        assertTrue(GameColor.YELLOW.isCardColor());
        assertTrue(GameColor.GREEN.isCardColor());
        assertTrue(GameColor.CYAN.isCardColor());
    }

    @Test
    public void isCardColorShouldReturnFalseForWhite() {
        assertFalse(GameColor.GREY.isCardColor());
    }
}