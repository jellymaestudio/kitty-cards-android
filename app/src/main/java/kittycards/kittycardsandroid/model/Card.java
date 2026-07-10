package kittycards.kittycardsandroid.model;

import java.util.Objects;

/**
 * Represents a number card in Kitty Cards.
 * <p>
 * A card has one valid card color and a value between 1 and 6.
 * </p>
 *
 * @author JellyMae
 */
public class Card {

    private final GameColor color;
    private final int value;


    /**
     * Creates a new number card with the given color and value.
     *
     * @param color the color of the card
     * @param value the value of the card
     * @throws NullPointerException     if {@code color} is {@code null}
     * @throws IllegalArgumentException if {@code color} is white or
     *                                  if {@code value} is not between 1 and 6
     */
    public Card(GameColor color, int value) {
        if (color == null) {
            throw new NullPointerException("color cannot be null");
        }
        if (!color.isCardColor()) {
            throw new IllegalArgumentException("cards cannot be white");
        }
        if (value < 1 || value > 6) {
            throw new IllegalArgumentException("value must be between 1 and 6");
        }

        this.color = color;
        this.value = value;
    }


    /**
     * Returns the color of this card.
     *
     * @return the card color
     */
    public GameColor getColor() {
        return color;
    }

    /**
     * Returns the value of this card.
     *
     * @return the value of the card
     */
    public int getValue() {
        return value;
    }


    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Card other)) {
            return false;
        }

        return value == other.value && color == other.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, value);
    }
}
