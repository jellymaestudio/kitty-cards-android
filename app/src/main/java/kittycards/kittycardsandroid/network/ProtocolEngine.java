package kittycards.kittycardsandroid.network;

import kittycards.kittycardsandroid.components.IProtocolEngine;
import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.GameColor;

/**
 *
 * @author red_concrete
 */
public class ProtocolEngine implements IProtocolEngine {
    @Override
    public byte[] encode(GameAction action) {
        if (action == null) throw new NullPointerException("action darf nicht null sein");

        byte[] bytes = new byte[5];
        bytes[0] = (byte) action.getType().ordinal();
        bytes[1] = action.getCard() != null ? (byte) action.getCard().getValue() : -1;
        bytes[2] = action.getCard() != null ? (byte) action.getCard().getColor().ordinal() : -1;
        bytes[3] = (byte) action.getBoardPositionColumn();
        bytes[4] = (byte) action.getBoardPositionRow();
        return bytes;
    }

    @Override
    public GameAction decode(byte[] bytes) {
        if (bytes == null) throw new NullPointerException("bytes darf nicht null sein");
        if (bytes.length != 5)
            throw new IllegalArgumentException("bytes muss genau 5 Elemente haben, ist: " + bytes.length);

        if (bytes[0] < 0 || bytes[0] >= GameAction.ActionType.values().length)
            throw new IllegalArgumentException("Ungültiger ActionType-Ordinal: " + bytes[0]);

        if (bytes[2] != -1 && (bytes[2] < 0 || bytes[2] >= GameColor.values().length))
            throw new IllegalArgumentException("Ungültiger CardColor-Ordinal: " + bytes[2]);


        GameAction.ActionType actionType = GameAction.ActionType.values()[bytes[0]];
        Card card = (bytes[1] != -1 && bytes[2] != -1)
                ? new Card(GameColor.values()[bytes[2]], bytes[1])
                : null;
        int boardPositionColumn = bytes[3];
        int boardPositionRow = bytes[4];

        return new GameAction(actionType, card, boardPositionColumn, boardPositionRow);
    }
}