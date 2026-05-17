package kittycats.kittycatsandroid.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the 3x3 game board.
 * <p>
 * The center field is always white and is used as the draw pile position.
 * All other fields can hold one card.
 *
 * @author JellyMae
 */
public class Board {

    private final Field[][] fields;



    // --- Constructors ---

    public Board() {
        fields = new Field[3][3];
        initializeFields();
    }



    // --- Getters and Setters ---

    /**
     * Returns the field at the given board position.
     *
     * @param row the row of the field
     * @param column the column of the field
     * @return the field at the given position
     * @throws IllegalArgumentException if the position is outside the board
     */
    public Field getField(int row, int column) {
        if((row < 0 || row > 2) || (column < 0 || column > 2)) {
            throw new IllegalArgumentException("position is not on the board");
        }

        return fields[row][column];
    }

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
        for(int row = 0; row < 3; row++) {
            for(int column = 0; column < 3; column++) {
                if(!isCenterField(row, column) && fields[row][column].isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }



    // --- Operations ---

    private void initializeFields() {
        List<GameColor> fieldColors = randomizeFieldColors();
        Collections.shuffle(fieldColors);

        int index = 0;

        for(int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if(isCenterField(row, column)) {
                    fields[1][1] = new Field(GameColor.WHITE, row, column);
                    continue;
                }

                fields[row][column] = new Field(fieldColors.get(index),  row, column);
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

        for(int i = 0; i < 4; i++) {
            fieldColors.add(GameColor.WHITE);
            fieldColors.add(coloredFields.get((int)(Math.random() * coloredFields.size())));
        }

        return fieldColors;
    }

    public void clearBoard() {
        for(int row = 0; row < 3; row++) {
            for(int column = 0; column < 3; column++) {
                fields[row][column].clearField();
            }
        }
    }
}
