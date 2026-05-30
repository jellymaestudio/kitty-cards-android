package kittycards.kittycardsandroid.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class GameColorTest {

    @Test
    public void getHexCodeShouldReturnCorrectHexCode() {
        assertEquals("#EB7878", GameColor.RED.getHexCode());
        assertEquals("#EBDC78", GameColor.YELLOW.getHexCode());
        assertEquals("#BDEB78", GameColor.GREEN.getHexCode());
        assertEquals("#78EBD6", GameColor.BLUE.getHexCode());
        assertEquals("#FFFFFF", GameColor.WHITE.getHexCode());
    }

    @Test
    public void isCardColorShouldReturnTrueForRedYellowGreenAndBlue() {
        assertTrue(GameColor.RED.isCardColor());
        assertTrue(GameColor.YELLOW.isCardColor());
        assertTrue(GameColor.GREEN.isCardColor());
        assertTrue(GameColor.BLUE.isCardColor());
    }

    @Test
    public void isCardColorShouldReturnFalseForWhite() {
        assertFalse(GameColor.WHITE.isCardColor());
    }
}