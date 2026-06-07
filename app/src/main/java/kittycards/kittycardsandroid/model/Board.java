package kittycards.kittycardsandroid.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the 3x3 game board.
 * <p>
 * The board consists of nine fields arranged in a grid.
 * The center field is always white and serves as the draw pile position.
 * All other fields are randomized at board creation and can hold one placed card.
 * </p>
 *
 * @author JellyMae
 */
public class Board {

    private final Field[][] fields;


    // --- Constructor ---

    /**
     * Creates a new game board and initializes all fields.
     * <p>
     * Field colors are randomized, except for the center field,
     * which is always white.
     * </p>
     */
    public Board() {
        fields = new Field[3][3];
        initializeFields();
    }


    // --- Getters ---

    /**
     * Checks whether the given position is on the board.
     *
     * @param row    the row to check
     * @param column the column to check
     * @return {@code true} if the position is on the board, otherwise {@code false}
     */
    public boolean isOnBoard(int row, int column) {
        return (row >= 0 && row <= 2) && (column >= 0 && column <= 2);
    }

    /**
     * Returns the field at the given board position.
     *
     * @param row    the row of the field
     * @param column the column of the field
     * @return the field at the given position
     * @throws IllegalArgumentException if the position is outside the board
     */
    public Field getField(int row, int column) {
        if (!isOnBoard(row, column)) {
            throw new IllegalArgumentException("position is not on the board");
        }

        return fields[row][column];
    }

    /**
     * Checks whether the given position is the center field.
     *
     * @param row    the row to check
     * @param column the column to check
     * @return {@code true} if the position is the center field, otherwise {@code false}
     */
    public boolean isCenterField(int row, int column) {
        return row == 1 && column == 1;
    }

    /**
     * Checks whether all playable fields on the board are occupied.
     * <p>
     * The center field is ignored because it is reserved as the draw position.
     *
     * @return {@code true} if all playable fields are occupied, otherwise {@code false}
     */
    public boolean isFull() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (!isCenterField(row, column) && fields[row][column].isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }


    // --- Board Management ---

    private void initializeFields() {
        List<GameColor> fieldColors = randomizeFieldColors();
        Collections.shuffle(fieldColors);

        int index = 0;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (isCenterField(row, column)) {
                    fields[1][1] = new Field(GameColor.WHITE, row, column);
                    continue;
                }

                fields[row][column] = new Field(fieldColors.get(index), row, column);
                index++;
            }
        }
    }

    private List<GameColor> randomizeFieldColors() {
        List<GameColor> fieldColors = new ArrayList<>();
        List<GameColor> coloredFields = new ArrayList<>();

        coloredFields.add(GameColor.RED);
        coloredFields.add(GameColor.YELLOW);
        coloredFields.add(GameColor.GREEN);
        coloredFields.add(GameColor.BLUE);

        for (int i = 0; i < 4; i++) {
            fieldColors.add(GameColor.WHITE);
            fieldColors.add(coloredFields.get((int) (Math.random() * coloredFields.size())));
        }

        return fieldColors;
    }

    /**
     * Clears all cards from the board.
     * <p>
     * Field colors and positions remain unchanged.
     * Only placed cards are removed.
     * </p>
     */
    public void clearBoard() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                fields[row][column].clearField();
            }
        }
    }
}
