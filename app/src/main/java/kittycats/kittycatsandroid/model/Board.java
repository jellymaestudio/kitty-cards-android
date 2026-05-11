package kittycats.kittycatsandroid.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board {

    private final Field[][] fields;


    // --- Constructors ---

    public Board() {
        fields = new Field[3][3];
        initializeFields();
    }


    // --- Getters and Setters ---

    public Field getField(int row, int column) {
        if((row < 0 || row > 2) || (column < 0 || column > 2)) {
            throw new IllegalArgumentException("position is not on the board");
        }

        return fields[row][column];
    }

    public boolean isCenterField(int row, int column) {
        return row == 1 && column == 1;
    }

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
        List<FieldColor> fieldColors = randomizeFieldColors();
        Collections.shuffle(fieldColors);

        int index = 0;

        for(int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if(isCenterField(row, column)) {
                    fields[1][1] = new Field(FieldColor.WHITE, row, column);
                    continue;
                }

                fields[row][column] = new Field(fieldColors.get(index),  row, column);
                index++;
            }
        }
    }

    private List<FieldColor> randomizeFieldColors() {
        List<FieldColor> fieldColors = new ArrayList<>();
        List<FieldColor> coloredFields = new ArrayList<>();

        coloredFields.add(FieldColor.RED);
        coloredFields.add(FieldColor.YELLOW);
        coloredFields.add(FieldColor.GREEN);
        coloredFields.add(FieldColor.BLUE);

        for(int i = 0; i < 4; i++) {
            fieldColors.add(FieldColor.WHITE);
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
