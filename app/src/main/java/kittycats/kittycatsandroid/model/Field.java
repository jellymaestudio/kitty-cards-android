package kittycats.kittycatsandroid.model;

/**
 * Represents a Field with one of the five colors of {@link GameColor} and
 * a position on the {@link Board}.
 *
 * @author JellyMae
 */
public class Field {

    private final GameColor color;
    private final int row;
    private final int column;
    private Card card;



    // --- Constructors ---

    /**
     * Creates a Field with one of the five colors of {@link GameColor} and a position on the {@link Board}.
     *
     * @param color the color of the field
     * @param column the column of the field on the board
     * @param row the row of the field on the board
     * @throws NullPointerException if {@code color} is {@code null}
     * @throws IllegalArgumentException if {@code column} or {@code row} is not between 0 and 2
     */
    public Field(GameColor color, int row, int column) {
        if(color == null) {
            throw new NullPointerException("color cannot be null");
        }
        if((column < 0 || column > 2) || (row < 0 || row > 2)) {
            throw new IllegalArgumentException("position is not on the board");
        }

        this.color = color;
        this.row = row;
        this.column = column;
        this.card = null;
    }



    // --- Getters and Setters ---

    /**
     * Returns the color of the Field.
     *
     * @return the color of the Field
     */
    public GameColor getColor() {
        return color;
    }

    public int getRow() {
        return row;
    }

    /**
     * Returns the column of the Field on the {@link Board}.
     *
     * @return the column of the Field
     */
    public int getColumn() {
        return column;
    }

    public Card getCard() {
        return card;
    }

    public boolean isEmpty() {
        return card == null;
    }



    // --- Operations ---

    public void placeCard(Card card) {
        if(card == null) {
            throw new NullPointerException("card can not be null");
        }
        if(!isEmpty()) {
            throw new IllegalArgumentException("field is already occupied");
        }

        this.card = card;
    }

    public void clearField() {
        this.card = null;
    }
}
