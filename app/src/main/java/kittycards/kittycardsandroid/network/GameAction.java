package kittycards.kittycardsandroid.network;

import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.GameColor;

/**
 * Object, that is transmitted to, or received from the other Device.
 *
 * @author red_concrete
 */
public record GameAction(ActionType type, Card card, GameColor boardColor, int boardPositionColumn,
                         int boardPositionRow) {
    /**
     * Creates a GameAction without a card or board position.
     * Suitable for {@link ActionType#UNSELECT_CARD}.
     *
     * @param type the type of action; must not be {@code null}
     * @throws NullPointerException     if {@code type} is {@code null}
     * @throws IllegalArgumentException if {@code type} requires a card
     */
    public GameAction(ActionType type) {
        this(type, null, null, -1, -1);
    }

    /**
     * Creates a GameAction with a card but without a board position.
     * Suitable for {@link ActionType#DRAW_CARD} and {@link ActionType#SELECT_CARD}.
     *
     * @param type the type of action; must not be {@code null}
     * @param card the card relevant to the action; must not be {@code null} for
     *             {@link ActionType#DRAW_CARD} and {@link ActionType#SELECT_CARD}
     * @throws NullPointerException     if {@code type} is {@code null}
     * @throws IllegalArgumentException if {@code type} requires a card and {@code card} is {@code null},
     *                                  or if {@code type} requires a board position
     */
    public GameAction(ActionType type, Card card) {
        this(type, card, null, -1, -1);
    }

    /**
     * Creates a GameAction with a board color and position, but without a card.
     * Suitable for {@link ActionType#SET_BOARD_COLOR}.
     *
     * @param type                the type of action; must not be {@code null}
     * @param boardColor          the color to be applied at the given board position;
     *                            must not be {@code null} for {@link ActionType#SET_BOARD_COLOR}
     * @param boardPositionColumn the column of the board position
     * @param boardPositionRow    the row of the board position
     * @throws NullPointerException     if {@code type} is {@code null}
     * @throws IllegalArgumentException if {@code type} is {@link ActionType#SET_BOARD_COLOR}
     *                                  and {@code boardColor} is {@code null}
     * @throws IllegalArgumentException if {@code type} is {@link ActionType#SET_BOARD_COLOR}
     *                                  and {@code boardColor} is {@code null},
     *                                  or if the board position is invalid
     */
    public GameAction(ActionType type, GameColor boardColor, int boardPositionColumn, int boardPositionRow) {
        this(type, null, boardColor, boardPositionColumn, boardPositionRow);
    }

    /**
     * Creates a GameAction with a card and board position.
     * Suitable for {@link ActionType#PLAY_CARD}.
     *
     * @param type                the type of action; must not be {@code null}
     * @param card                the card to be played; must not be {@code null}
     * @param boardPositionColumn the column of the board position; must be {@code >= 0} and {@code <= 2}
     * @param boardPositionRow    the row of the board position; must be {@code >= 0} and {@code <= 2}
     * @throws NullPointerException     if {@code type} is {@code null}
     * @throws IllegalArgumentException if {@code card} is {@code null}
     * @throws IllegalArgumentException if the board position is invalid
     */
    public GameAction(ActionType type, Card card, int boardPositionColumn, int boardPositionRow) {
        this(type, card, null, boardPositionColumn, boardPositionRow);
    }

    /**
     * Creates a GameAction with all parameters explicitly set.
     *
     * @param type                the type of action; must not be {@code null}
     * @param card                the card relevant to the action; required for
     *                            {@link ActionType#DRAW_CARD},
     *                            {@link ActionType#SELECT_CARD} and
     *                            {@link ActionType#PLAY_CARD}
     * @param boardColor          the board color relevant to the action; required for
     *                            {@link ActionType#SET_BOARD_COLOR}
     * @param boardPositionColumn the column of the board position; must be {@code >= 0}
     *                            and {@code <= 2} for
     *                            {@link ActionType#PLAY_CARD} and
     *                            {@link ActionType#SET_BOARD_COLOR}
     * @param boardPositionRow    the row of the board position; must be {@code >= 0}
     *                            and {@code <= 2} for
     *                            {@link ActionType#PLAY_CARD} and
     *                            {@link ActionType#SET_BOARD_COLOR}
     * @throws NullPointerException     if {@code type} is {@code null}
     * @throws IllegalArgumentException if required parameters for the given
     *                                  {@code type} are missing or invalid
     */

    public GameAction {
        if (type == null)
            throw new NullPointerException("ActionType darf nicht null sein");

        if ((type == ActionType.DRAW_CARD || type == ActionType.SELECT_CARD || type == ActionType.PLAY_CARD) && card == null)
            throw new IllegalArgumentException(type + " erfordert eine Karte");

        if ((type == ActionType.PLAY_CARD || type == ActionType.SET_BOARD_COLOR) && (boardPositionColumn < 0 || boardPositionColumn > 2 || boardPositionRow < 0 || boardPositionRow > 2))
            throw new IllegalArgumentException(type.name() + " erfordert eine gültige Boardposition");

        if (type == ActionType.SET_BOARD_COLOR && boardColor == null)
            throw new IllegalArgumentException("SET_BOARD_COLOR erfordert eine Farbe");
    }

    public enum ActionType {

        DRAW_CARD, //must be sent along with the card drawn
        SELECT_CARD, //must be sent together with the selected card
        UNSELECT_CARD,
        PLAY_CARD, //must be specified together with the card being played and the board position
        SET_BOARD_COLOR //must be specified together with the color being set and the board position
    }
}