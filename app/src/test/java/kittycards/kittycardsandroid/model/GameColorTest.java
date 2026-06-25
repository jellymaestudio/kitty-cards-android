package kittycards.kittycardsandroid.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class GameColorTest {

    @Test
    public void getHexCodeShouldReturnCorrectHexCode() {
        assertEquals("#EB7878", GameColor.PURPLE.getHexCode());
        assertEquals("#EBDC78", GameColor.YELLOW.getHexCode());
        assertEquals("#BDEB78", GameColor.GREEN.getHexCode());
        assertEquals("#78EBD6", GameColor.CYAN.getHexCode());
        assertEquals("#FFFFFF", GameColor.GREY.getHexCode());
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