package kittycards.kittycardsandroid.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class BoardTest {

    @Test
    public void constructorShouldInitializeAllFields() {
        Board board = new Board();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                assertNotNull(board.getField(row, column));
            }
        }
    }

    @Test
    public void constructorShouldSetCenterFieldToWhite() {
        Board board = new Board();

        assertEquals(GameColor.WHITE, board.getField(1, 1).getColor());
    }

    @Test
    public void constructorShouldInitializeAllFieldsAsEmpty() {
        Board board = new Board();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                assertTrue(board.getField(row, column).isEmpty());
            }
        }
    }

    @Test
    public void constructorShouldSetCorrectFieldPositions() {
        Board board = new Board();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                Field field = board.getField(row, column);

                assertEquals(row, field.getRow());
                assertEquals(column, field.getColumn());
            }
        }
    }

    @Test
    public void constructorShouldCreateFourWhitePlayableFields() {
        Board board = new Board();
        int whitePlayableFields = 0;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (!board.isCenterField(row, column)
                        && board.getField(row, column).getColor() == GameColor.WHITE) {
                    whitePlayableFields++;
                }
            }
        }

        assertEquals(4, whitePlayableFields);
    }

    @Test
    public void constructorShouldCreateFourColoredPlayableFields() {
        Board board = new Board();
        int coloredPlayableFields = 0;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (!board.isCenterField(row, column)
                        && board.getField(row, column).getColor().isCardColor()) {
                    coloredPlayableFields++;
                }
            }
        }

        assertEquals(4, coloredPlayableFields);
    }

    @Test
    public void getFieldShouldReturnCorrectField() {
        Board board = new Board();

        Field field = board.getField(2, 1);

        assertEquals(2, field.getRow());
        assertEquals(1, field.getColumn());
    }

    @Test
    public void getFieldShouldThrowExceptionIfRowIsNegative() {
        Board board = new Board();

        assertThrows(IllegalArgumentException.class, () -> board.getField(-1, 0));
    }

    @Test
    public void getFieldShouldThrowExceptionIfRowIsTooHigh() {
        Board board = new Board();

        assertThrows(IllegalArgumentException.class, () -> board.getField(3, 0));
    }

    @Test
    public void getFieldShouldThrowExceptionIfColumnIsNegative() {
        Board board = new Board();

        assertThrows(IllegalArgumentException.class, () -> board.getField(0, -1));
    }

    @Test
    public void getFieldShouldThrowExceptionIfColumnIsTooHigh() {
        Board board = new Board();

        assertThrows(IllegalArgumentException.class, () -> board.getField(0, 3));
    }

    @Test
    public void isCenterFieldShouldReturnTrueForCenterField() {
        Board board = new Board();

        assertTrue(board.isCenterField(1, 1));
    }

    @Test
    public void isCenterFieldShouldReturnFalseForNonCenterFields() {
        Board board = new Board();

        assertFalse(board.isCenterField(0, 0));
        assertFalse(board.isCenterField(0, 1));
        assertFalse(board.isCenterField(2, 2));
    }

    @Test
    public void isFullShouldReturnFalseForNewBoard() {
        Board board = new Board();

        assertFalse(board.isFull());
    }

    @Test
    public void isFullShouldReturnTrueIfAllPlayableFieldsAreOccupied() {
        Board board = new Board();

        fillPlayableFields(board);

        assertTrue(board.isFull());
    }

    @Test
    public void isFullShouldIgnoreCenterField() {
        Board board = new Board();

        fillPlayableFields(board);

        assertTrue(board.getField(1, 1).isEmpty());
        assertTrue(board.isFull());
    }

    @Test
    public void clearBoardShouldRemoveAllPlacedCards() {
        Board board = new Board();

        fillPlayableFields(board);
        board.clearBoard();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                assertTrue(board.getField(row, column).isEmpty());
            }
        }
    }

    @Test
    public void clearBoardShouldKeepFieldColorsUnchanged() {
        Board board = new Board();
        GameColor[][] colorsBeforeClear = new GameColor[3][3];

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                colorsBeforeClear[row][column] = board.getField(row, column).getColor();
            }
        }

        fillPlayableFields(board);
        board.clearBoard();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                assertEquals(colorsBeforeClear[row][column], board.getField(row, column).getColor());
            }
        }
    }

    private void fillPlayableFields(Board board) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (!board.isCenterField(row, column)) {
                    board.getField(row, column).placeCard(new Card(GameColor.RED, 1));
                }
            }
        }
    }
}