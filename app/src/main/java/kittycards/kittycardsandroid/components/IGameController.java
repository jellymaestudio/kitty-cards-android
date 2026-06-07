package kittycards.kittycardsandroid.components;

import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.Player;

/**
 * Controls the local player's in-game actions during an active match.
 * <p>
 * Each method represents a player interaction triggered from the UI.
 * Implementations are responsible for validating the action against the current
 * {@link kittycards.kittycardsandroid.model.GameState}, applying it locally,
 * encoding it as a {@link kittycards.kittycardsandroid.network.GameAction},
 * and forwarding it to the remote device via the
 * {@link kittycards.kittycardsandroid.components.INetworkManager}, if it didn't already come from the remote device.
 *
 * @author red_concrete
 * @author JellyMae
 */
public interface IGameController {

    /**
     * Should be called when the guest device has been connected, to initiate the Match and the GameBoard
     */
    void startMatch(Player playerOne, Player playerTwo);

    /**
     * Marks a card in hand of player as selected.
     * A selected card can subsequently be played via {@link #playCard(Player, int, int)}.
     * The selected card must be transmitted to the remote device.
     *
     * @param player the Player who selects the card
     * @param card   the card to be selected
     */
    void selectCard(Player player, Card card);

    /**
     * Unselects the currently selected card.
     * Has no effect if no card is currently selected.
     * The unselection must be transmitted to the remote device.
     *
     * @param player the Player
     */
    void unselectCard(Player player);

    /**
     * Places the currently selected card of the currentPlayer onto the board at (row, column).
     * Requires a card to be selected by the currentPlayer via {@link #selectCard(Player, Card)} )} beforehand.
     * The action must be transmitted to the remote device.
     *
     * @param row    the row of the field onto which the card is placed.
     * @param column the column of the field onto which the card is placed.
     */
    void playCard(Player player, int row, int column);

    /**
     * Draws a card from the game pile and adds it to the currentPlayer's hand.
     * Ends the turn.
     * The drawn card must be transmitted to the remote device.
     */
    void drawCard(Player player);

    /**
     * Registers a listener that is notified whenever the {@link kittycards.kittycardsandroid.model.GameState}
     * or the match status changes.
     * <p>
     * Since {@link IGameController} is a singleton, the UI cannot directly observe state mutations.
     * Instead, the UI component (e.g. the game Activity or Fragment) registers a callback here
     * to be notified and trigger a re-render accordingly.
     * <p>
     * The listener is invoked after every state-mutating operation, including actions initiated
     * locally (e.g. {@link #drawCard(Player player)}, {@link #playCard(Player, int, int)}) as well as actions received
     * from the remote device via {@link INetworkManager#fetchNextAction()}.
     * <p>
     * Only one listener can be registered at a time. A subsequent call replaces the previous listener.
     * Pass {@code null} to unregister.
     * <p>
     * Example usage:
     * <pre>{@code
     * gameController.setOnStateChangedListener(() -> runOnUiThread(this::updateUI));
     * }</pre>
     *
     * @param listener the {@link Runnable} to invoke on every state change, or {@code null} to unregister.
     */
    void setOnStateChangedListener(Runnable listener);


}