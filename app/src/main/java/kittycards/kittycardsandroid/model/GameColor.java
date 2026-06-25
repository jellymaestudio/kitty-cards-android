package kittycards.kittycardsandroid.model;

/**
 * Represents all colors used in the game.
 * <p>
 * Yellow, green, cyan and purple can be used for both cards and board fields.
 * Grey is reserved exclusively for neutral board fields, including the center
 * draw field.
 * </p>
 *
 * @author JellyMae
 */
public enum GameColor {
    YELLOW("#FFFCCF"),
    GREEN("#D6FFD0"),
    CYAN("#D0FFF7"),
    PURPLE("#F9DEFF"),
    GREY("#7D758D");


    private final String hexCode;


    GameColor(String hexCode) {
        this.hexCode = hexCode;
    }


    /**
     * Returns the hexadecimal color code of this game color.
     *
     * @return the hex code of this color
     */
    public String getHexCode() {
        return hexCode;
    }

    /**
     * Checks whether this color can be used for a card.
     *
     * @return {@code true} if this color can be used for a card, otherwise {@code false}
     */
    public boolean isCardColor() {
        return this != GREY;
    }
}
