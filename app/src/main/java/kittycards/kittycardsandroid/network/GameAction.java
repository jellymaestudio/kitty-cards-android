package kittycards.kittycardsandroid.network;

import kittycards.kittycardsandroid.model.Card;

/**
 * Object, that is transmitted to, or received from the other Device.
 *
 * @author red_concrete
 */
public class GameAction {
    private final ActionType type;
    private final Card card;
    private final int boardPositionColumn;
    private final int boardPositionRow;

    /**
     * Creates a GameAction without a card or board position.
     * Suitable for {@link ActionType#UNSELECT_CARD}.
     *
     * @param type the type of action; must not be {@code null}
     * @throws NullPointerException     if {@code type} is {@code null}
     * @throws IllegalArgumentException if {@code type} requires a card
     */
    public GameAction(ActionType type) {
        this(type, null, -1, -1);
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
        this(type, card, -1, -1);
    }

    /**
     * Creates a GameAction with all parameters explicitly set.
     * Suitable for {@link ActionType#PLAY_CARD}.
     *
     * @param type                the type of action; must not be {@code null}
     * @param card                the card relevant to the action; must not be {@code null} for {@link ActionType#DRAW_CARD}, {@link ActionType#SELECT_CARD} and {@link ActionType#PLAY_CARD}
     * @param boardPositionColumn the column of the board position; must be {@code >= 0} and {@code <= 2} for {@link ActionType#PLAY_CARD}
     * @param boardPositionRow    the row of the board position; must be {@code >= 0} and {@code <= 2} for {@link ActionType#PLAY_CARD}
     * @throws NullPointerException     if {@code type} is {@code null}
     * @throws IllegalArgumentException if {@code type} requires a card and {@code card} is {@code null},
     *                                  or if {@code type} is {@link ActionType#PLAY_CARD}
     *                                  and the board position is invalid
     */
    public GameAction(ActionType type, Card card, int boardPositionColumn, int boardPositionRow) {
        if (type == null)
            throw new NullPointerException("ActionType darf nicht null sein");

        if ((type == ActionType.DRAW_CARD || type == ActionType.SELECT_CARD || type == ActionType.PLAY_CARD) && card == null)
            throw new IllegalArgumentException(type + " erfordert eine Karte");

        if (type == ActionType.PLAY_CARD && (boardPositionColumn < 0 || boardPositionColumn > 2 || boardPositionRow < 0 || boardPositionRow > 2))
            throw new IllegalArgumentException("PLAY_CARD erfordert eine gültige Boardposition");

        this.type = type;
        this.card = card;
        this.boardPositionColumn = boardPositionColumn;
        this.boardPositionRow = boardPositionRow;
    }

    public ActionType getType() {
        return type;
    }

    public Card getCard() {
        return card;
    }

    public int getBoardPositionColumn() {
        return boardPositionColumn;
    }

    public int getBoardPositionRow() {
        return boardPositionRow;
    }

    public enum ActionType {

        DRAW_CARD, //must be sent along with the card drawn
        SELECT_CARD, //must be sent together with the selected card
        UNSELECT_CARD,
        PLAY_CARD; //must be specified together with the card being played and the board position


    }

}