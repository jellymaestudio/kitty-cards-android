package kittycats.kittycatsandroid.components;

import kittycats.kittycatsandroid.network.GameAction;

/**
 * Handles encoding and decoding of {@link GameAction} objects for network transmission.
 * <p>
 * Converts a {@link GameAction} into a compact 5-byte array and back,
 * allowing game actions to be sent between devices over the network.
 * <p>
 * Byte layout:
 * <pre>
 * [0] ActionType ordinal
 * [1] Card value        (-1 if no card)
 * [2] CardColor ordinal (-1 if no card)
 * [3] Board column      (-1 if not applicable)
 * [4] Board row         (-1 if not applicable)
 * </pre>
 *
 * @author red_concrete
 */
public interface IProtocolEngine {
    /**
     * Encodes a {@link GameAction} into a 5-byte array for network transmission.
     *
     * @param action the action to encode; must not be {@code null}
     * @return a 5-byte array representing the action
     * @throws NullPointerException if {@code action} is {@code null}
     */
    byte[] encode(GameAction action);

    /**
     * Decodes a 5-byte array received over the network into a {@link GameAction}.
     *
     * @param bytes the byte array to decode; must not be {@code null} and must have a length of exactly 5
     * @return the decoded {@link GameAction}
     * @throws NullPointerException     if {@code bytes} is {@code null}
     * @throws IllegalArgumentException if {@code bytes} does not have a length of 5,
     *                                  or if the encoded ordinals are out of range
     */
    GameAction decode(byte[] bytes);
}
