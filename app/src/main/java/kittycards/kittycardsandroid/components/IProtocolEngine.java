package kittycards.kittycardsandroid.components;

import kittycards.kittycardsandroid.network.GameAction;

/**
 * Handles encoding and decoding of {@link GameAction} objects
 * for network transmission.
 *
 * <p>Byte layout:</p>
 * <pre>
 * [0] ActionType ordinal
 * [1] Card value                  (-1 if no card)
 * [2] CardColor ordinal           (-1 if no card)
 * [3] Board color ordinal         (-1 if not applicable)
 * [4] Board column
 * [5] Board row
 * [6] Context-sensitive integer
 * </pre>
 *
 * @author red_concrete
 */
public interface IProtocolEngine {
    /**
     * Encodes a {@link GameAction} into a 6-byte array for network transmission.
     *
     * @param action the action to encode; must not be {@code null}
     * @return a 7-byte array representing the action
     * @throws NullPointerException if {@code action} is {@code null}
     */
    byte[] encodeGameAction(GameAction action);

    /**
     * Decodes a 6-byte array received over the network into a {@link GameAction}.
     *
     * @param bytes the byte array to decode; must not be {@code null} and must have a length of exactly 6
     * @return the decoded {@link GameAction}
     * @throws NullPointerException     if {@code bytes} is {@code null}
     * @throws IllegalArgumentException if {@code bytes} does not have a length of 6,
     *                                  or if the encoded ordinals are out of range
     */
    GameAction decodeGameAction(byte[] bytes);
}
