package kittycats.kittycatsandroid.model;

/**
 * Represents a Number Card with one of the four colors of {@link CardColor} and
 * a value between 1 and 6.
 *
 * @author JellyMae
 */
public class Card {

    private CardColor color;
    private int value;


    // --- Constructors ---

    /**
     * Creates a Number Card with one of the four colors of {@link CardColor} and a value between 1 and 6.
     *
     * @param color the color of the card
     * @param value the value of the card
     * @throws NullPointerException if {@code color} is {@code null}
     * @throws IllegalArgumentException if {@code value} is not between 1 and 6
     */
    public Card(CardColor color, int value) {
        if(color == null) {
            throw new NullPointerException("color cannot be null");
        }
        if(value < 1 || value > 6) {
            throw new IllegalArgumentException("value must be between 1 and 6");
        }

        this.color = color;
        this.value = value;
    }


    // --- Getters and Setters ---

    /**
     * Returns the color of the Number Card.
     *
     * @return the color of the Number Card
     */
    public CardColor getColor() {
        return color;
    }

    /**
     * Returns the value of the Number Card.
     *
     * @return the value of the Number Card
     */
    public int getValue() {
        return value;
    }
}
