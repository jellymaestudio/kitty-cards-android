package kittycats.kittycatsandroid.components;

import kittycats.kittycatsandroid.model.Card;
import kittycats.kittycatsandroid.model.Player;

/**
 * Controls the local player's in-game actions during an active match.
 * <p>
 * Each method represents a player interaction triggered from the UI.
 * Implementations are responsible for validating the action against the current
 * {@link kittycats.kittycatsandroid.model.GameState}, applying it locally,
 * encoding it as a {@link kittycats.kittycatsandroid.network.GameAction},
 * and forwarding it to the remote device via the
 * {@link kittycats.kittycatsandroid.components.INetworkManager}, if it didn't already come from the remote device.
 *
 * @author red_concrete
 */
public interface IGameController {

    /**
     * Should be called when the guest device has been connected, to initiate the Match and the GameBoard
     */
    void startMatch();
    /**
     * Draws a card from the game pile and adds it to the currentPlayer's hand.
     * Ends the turn.
     * The drawn card must be transmitted to the remote device.
     */
    void drawCard();

    /**
     * Marks a card in hand of player as selected.
     * A selected card can subsequently be played via {@link #playCard(int, int)}.
     * The selected card must be transmitted to the remote device.
     * @param player the Player who selects the card
     * @param card the card to be selected
     */
    void selectCard(Player player, Card card);

    /**
     * Unselects the currently selected card.
     * Has no effect if no card is currently selected.
     * The unselection must be transmitted to the remote device.
     * @param player the Player
     */
    void unselectCard(Player player);

    /**
     * Places the currently selected card of the currentPlayer onto the board at (row, column).
     * Requires a card to be selected by the currentPlayer via {@link #selectCard(Player, Card)} )} beforehand.
     * The action must be transmitted to the remote device.
     * @param row the row of the field onto which the card is placed.
     * @param column the column of the field onto which the card is placed.
     */
    void playCard(int row, int column);


}