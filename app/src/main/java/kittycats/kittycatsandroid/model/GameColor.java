package kittycats.kittycatsandroid.model;

/**
 * Represents the colors used in the game.
 * <p>
 * Red, yellow, green and blue can be used for cards and fields.
 * White can only be used for fields.
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


    // --- Constructor ---

    GameColor(String hexCode) {
        this.hexCode = hexCode;
    }


    // --- Getters ---

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
