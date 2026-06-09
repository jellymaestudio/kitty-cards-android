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
    public byte[] encodeGameAction(GameAction action) {
        if (action == null) throw new NullPointerException("action darf nicht null sein");

        byte[] bytes = new byte[6];
        bytes[0] = (byte) action.type().ordinal();
        bytes[1] = action.card() != null ? (byte) action.card().getValue() : -1;
        bytes[2] = action.card() != null ? (byte) action.card().getColor().ordinal() : -1;
        bytes[3] = action.boardColor() != null ? (byte) action.boardColor().ordinal() : -1;
        bytes[4] = (byte) action.boardPositionColumn();
        bytes[5] = (byte) action.boardPositionRow();
        return bytes;
    }

    @Override
    public GameAction decodeGameAction(byte[] bytes) {
        if (bytes == null) throw new NullPointerException("bytes darf nicht null sein");
        if (bytes.length != 6)
            throw new IllegalArgumentException("bytes muss genau 6 Elemente haben, ist: " + bytes.length);

        if (bytes[0] < 0 || bytes[0] >= GameAction.ActionType.values().length)
            throw new IllegalArgumentException("Ungültiger ActionType-Ordinal: " + bytes[0]);

        if (bytes[2] != -1 && (bytes[2] < 0 || bytes[2] >= GameColor.values().length))
            throw new IllegalArgumentException("Ungültiger CardColor-Ordinal: " + bytes[2]);
        if (bytes[2] != -1 && !GameColor.values()[bytes[2]].isCardColor())
            throw new IllegalArgumentException("GameColor ist keine Kartenfarbe: " + bytes[2]);
        if (bytes[3] != -1 && (bytes[3] < 0 || bytes[3] >= GameColor.values().length))
            throw new IllegalArgumentException("Ungültiger BoardColor-Ordinal: " + bytes[3]);

        GameAction.ActionType actionType = GameAction.ActionType.values()[bytes[0]];
        Card card = (bytes[1] != -1 && bytes[2] != -1)
                ? new Card(GameColor.values()[bytes[2]], bytes[1])
                : null;
        GameColor boardColor = bytes[3] != -1 ? GameColor.values()[bytes[3]] : null;
        int boardPositionColumn = bytes[4];
        int boardPositionRow = bytes[5];

        return new GameAction(actionType, card, boardColor, boardPositionColumn, boardPositionRow);
    }
}