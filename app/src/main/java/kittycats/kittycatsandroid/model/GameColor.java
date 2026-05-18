package kittycats.kittycatsandroid.model;

/**
 * Represents all colors used in the game.
 * <p>
 * The colors red, yellow, green, and blue can be used for both cards and fields.
 * White is reserved exclusively for board fields, such as neutral or draw pile fields.
 * </p>
 *
 * @author JellyMae
 */
public enum GameColor {
    RED("#EB7878"),
    YELLOW("#EBDC78"),
    GREEN("#BDEB78"),
    BLUE("#78EBD6"),
    WHITE("#FFFFFF");


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
        return this != WHITE;
    }
}
