package kittycards.kittycardsandroid.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class GameColorTest {

    @ParameterizedTest(name = "{0} should have hex code {1}")
    @MethodSource("provideColorsAndExpectedHexCodes")
    void getHexCodeReturnsExpectedHexCode(
            GameColor color,
            String expectedHexCode
    ) {
        assertEquals(expectedHexCode, color.getHexCode());
    }

    @ParameterizedTest(name = "{0} should be a valid card color")
    @EnumSource(
            value = GameColor.class,
            names = {"YELLOW", "GREEN", "CYAN", "PURPLE"}
    )
    void isCardColorReturnsTrueForCardColors(GameColor color) {
        assertTrue(color.isCardColor());
    }

    @Test
    void isCardColorReturnsFalseForGrey() {
        assertFalse(GameColor.GREY.isCardColor());
    }

    @Test
    void gameColorContainsExactlyFiveValues() {
        assertEquals(5, GameColor.values().length);
    }

    @Test
    void gameColorContainsAllExpectedValuesInDeclaredOrder() {
        GameColor[] expectedColors = {
                GameColor.YELLOW,
                GameColor.GREEN,
                GameColor.CYAN,
                GameColor.PURPLE,
                GameColor.GREY
        };

        assertEquals(
                java.util.List.of(expectedColors),
                java.util.List.of(GameColor.values())
        );
    }

    private static Stream<Arguments> provideColorsAndExpectedHexCodes() {
        return Stream.of(
                Arguments.of(GameColor.YELLOW, "#FFFCCF"),
                Arguments.of(GameColor.GREEN, "#D6FFD0"),
                Arguments.of(GameColor.CYAN, "#D0FFF7"),
                Arguments.of(GameColor.PURPLE, "#F9DEFF"),
                Arguments.of(GameColor.GREY, "#7D758D")
        );
    }
}