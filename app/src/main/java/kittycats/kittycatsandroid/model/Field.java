package kittycats.kittycatsandroid.model;

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

    private final GameColor color;
    private final int row;
    private final int column;
    private Card card;


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
    }


    // --- Getters ---

    /**
     * Returns the color of this field.
     *
     * @return the field color
     */
    public GameColor getColor() {
        return color;
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
     * Checks whether this field is empty.
     *
     * @return {@code true} if no card is placed on this field, otherwise {@code false}
     */
    public boolean isEmpty() {
        return card == null;
    }


    // --- Field Management ---

    /**
     * Places a card on this field.
     *
     * @param card the card to place on this field
     * @throws NullPointerException     if {@code card} is {@code null}
     * @throws IllegalArgumentException if this field is already occupied
     */
    public void placeCard(Card card) {
        if (card == null) {
            throw new NullPointerException("card can not be null");
        }
        if (!isEmpty()) {
            throw new IllegalArgumentException("field is already occupied");
        }

        this.card = card;
    }

    /**
     * Removes the currently placed card from this field.
     * <p>
     * If the field is already empty, nothing changes.
     * </p>
     */
    public void clearField() {
        this.card = null;
    }
}
