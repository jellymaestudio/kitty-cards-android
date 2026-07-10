package kittycards.kittycardsandroid.model;

/**
 * Represents a single field on the game board.
 * <p>
 * A field has a fixed color and a fixed position on the {@link Board}.
 * It can either be empty or contain one placed {@link Card}.
 * </p>
 *
 * @author JellyMae
 */
public class Field {

    private GameColor color;
    private final int row;
    private final int column;
    private Card card;
    private int cardOwnerId;
    private int displayedScore;


    // --- Constructor ---

    /**
     * Creates a field with the given color and position.
     *
     * @param color  the color of the field
     * @param row    the row of the field on the board
     * @param column the column of the field on the board
     * @throws NullPointerException     if {@code color} is {@code null}
     * @throws IllegalArgumentException if {@code row} or {@code column} is outside the board range from 0 to 2
     */
    public Field(GameColor color, int row, int column) {
        if (color == null) {
            throw new NullPointerException("color cannot be null");
        }
        if ((column < 0 || column > 2) || (row < 0 || row > 2)) {
            throw new IllegalArgumentException("position is not on the board");
        }

        this.color = color;
        this.row = row;
        this.column = column;
        this.card = null;
        this.cardOwnerId = -1;
        this.displayedScore = 0;
    }


    // --- Getters and Setters ---

    /**
     * Returns the color of this field.
     *
     * @return the field color
     */
    public GameColor getColor() {
        return color;
    }

    /**
     * Updates the color of this field.
     *
     * @param color the new field color
     * @throws NullPointerException if color is {@code null}
     */
    public void setColor(GameColor color) {
        if (color == null) {
            throw new NullPointerException("color cannot be null");
        }

        this.color = color;
    }

    /**
     * Returns the row of this field on the board.
     *
     * @return the row of this field
     */
    public int getRow() {
        return row;
    }

    /**
     * Returns the column of this field on the board.
     *
     * @return the column of this field
     */
    public int getColumn() {
        return column;
    }

    /**
     * Returns the card currently placed on this field.
     *
     * @return the placed card, or {@code null} if the field is empty
     */
    public Card getCard() {
        return card;
    }

    /**
     * Returns the id of the player who placed the card on this field.
     *
     * @return the owner's player id, or {@code -1} if the field is empty
     */
    public int getCardOwnerId() {
        return cardOwnerId;
    }

    /**
     * Returns the score displayed on the placed card.
     *
     * @return the displayed score, or {@code 0} if the field is empty
     */
    public int getDisplayedScore() {
        return displayedScore;
    }

    /**
     * Checks whether this field is empty.
     *
     * @return {@code true} if no card is placed on this field, otherwise {@code false}
     */
    public boolean isEmpty() {
        return card == null;
    }


    // --- Field Management ---

    /**
     * Places a card on this field and stores its owner and resulting score.
     *
     * @param card           the card to place
     * @param player         the player placing the card
     * @param displayedScore the score achieved by placing the card
     * @throws NullPointerException     if {@code card} or {@code player} is {@code null}
     * @throws IllegalArgumentException if the field is occupied or the score is negative
     */
    public void placeCard(Card card, Player player, int displayedScore) {
        if (card == null) {
            throw new NullPointerException("card cannot be null");
        }
        if (player == null) {
            throw new NullPointerException("player cannot be null");
        }
        if (!isEmpty()) {
            throw new IllegalArgumentException("field is already occupied");
        }
        if (displayedScore < 0) {
            throw new IllegalArgumentException("displayed score cannot be negative");
        }

        this.card = card;
        this.cardOwnerId = player.getId();
        this.displayedScore = displayedScore;
    }

    /**
     * Removes the currently placed card from this field.
     * <p>
     * If the field is already empty, nothing changes.
     * </p>
     */
    public void clearField() {
        this.card = null;
        this.cardOwnerId = -1;
        this.displayedScore = 0;
    }
}
