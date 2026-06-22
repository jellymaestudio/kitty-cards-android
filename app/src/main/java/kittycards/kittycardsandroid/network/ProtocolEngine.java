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
        if (action == null) throw new NullPointerException("action must not be null");

        byte[] bytes = new byte[7];
        bytes[0] = (byte) action.type().ordinal();
        bytes[1] = action.card() != null ? (byte) action.card().getValue() : -1;
        bytes[2] = action.card() != null ? (byte) action.card().getColor().ordinal() : -1;
        bytes[3] = action.boardColor() != null ? (byte) action.boardColor().ordinal() : -1;
        bytes[4] = (byte) action.boardPositionColumn();
        bytes[5] = (byte) action.boardPositionRow();
        bytes[6] = (byte) action.contextSensitiveInt();
        return bytes;
    }

    @Override
    public GameAction decodeGameAction(byte[] bytes) {
        if (bytes == null) throw new NullPointerException("bytes must not be null");
        if (bytes.length != 7)
            throw new IllegalArgumentException("bytes must have exactly 7 elements, is: " + bytes.length);

        if (bytes[0] < 0 || bytes[0] >= GameAction.ActionType.values().length)
            throw new IllegalArgumentException("Invalid ActionType ordinal: " + bytes[0]);

        if (bytes[2] != -1 && (bytes[2] < 0 || bytes[2] >= GameColor.values().length))
            throw new IllegalArgumentException("Invalid CardColor ordinal: " + bytes[2]);
        if (bytes[2] != -1 && !GameColor.values()[bytes[2]].isCardColor())
            throw new IllegalArgumentException("GameColor is not a card color: " + bytes[2]);
        if (bytes[3] != -1 && (bytes[3] < 0 || bytes[3] >= GameColor.values().length))
            throw new IllegalArgumentException("Invalid BoardColor ordinal: " + bytes[3]);

        GameAction.ActionType actionType = GameAction.ActionType.values()[bytes[0]];
        Card card = (bytes[1] != -1 && bytes[2] != -1)
                ? new Card(GameColor.values()[bytes[2]], bytes[1])
                : null;
        GameColor boardColor = bytes[3] != -1 ? GameColor.values()[bytes[3]] : null;
        int boardPositionColumn = bytes[4];
        int boardPositionRow = bytes[5];
        int contextSensitiveInt = bytes[6];

        return new GameAction(actionType, card, boardColor, boardPositionColumn, boardPositionRow, contextSensitiveInt);
    }
}