package kittycards.kittycardsandroid.components;

import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.Match;
import kittycards.kittycardsandroid.model.Player;
import kittycards.kittycardsandroid.network.Role;

/**
 * Defines the public operations of the game controller.
 *
 * <p>The controller manages the active match state, validates and applies
 * player actions, processes remote actions and notifies observing
 * components about state changes.</p>
 *
 * <p>Implementations may use an {@link INetworkManager} to synchronize
 * game actions with a remote device.</p>
 *
 * @author red_concrete
 * @author JellyMae
 */
public interface IGameController {

    /**
     * Starts a new match using the provided players.
     *
     * @param playerOne the first player
     * @param playerTwo the second player
     */
    void startMatch(Player playerOne, Player playerTwo);

    /**
     * Returns the currently active match.
     *
     * @return the active match, or {@code null} if no match exists
     */
    Match getMatch();

    /**
     * Returns the player controlled by the local device.
     *
     * @return the local player, or {@code null} if none is configured
     */
    Player getLocalPlayer();

    /**
     * Returns the remote player of the current match.
     *
     * @return the remote player
     */
    Player getRemotePlayer();

    /**
     * Sets the player controlled by the local device.
     *
     * @param localPlayer the local player
     */
    void setLocalPlayer(Player localPlayer);

    /**
     * Sets the network role used for the current match.
     *
     * @param role the current network role
     */
    void setNetworkRole(Role role);


    /**
     * Marks a card in hand of player as selected.
     * A selected card can subsequently be played via {@link #playCard(Player, int, int)}.
     * The selected card must be transmitted to the remote device.
     *
     * @param player the player selecting the card
     * @param card   the card to select
     */
    void selectCard(Player player, Card card);

    /**
     * Unselects the currently selected card.
     * Has no effect if no card is currently selected.
     * The unselection must be transmitted to the remote device.
     *
     * @param player the player whose card selection is cleared
     */
    void unselectCard(Player player);

    /**
     * Places the selected card of the specified player onto the given board position.
     * Requires a card to be selected by the currentPlayer via {@link #selectCard(Player, Card)} beforehand.
     * The action must be transmitted to the remote device.
     *
     * @param player the player performing the action
     * @param row    the target row
     * @param column the target column
     */
    void playCard(Player player, int row, int column);

    /**
     * Draws a new card for the specified player.
     * The drawn card must be transmitted to the remote device.
     *
     * @param player the player drawing a card
     */
    void drawCard(Player player);


    /**
     * Registers a listener that is notified whenever the {@link kittycards.kittycardsandroid.model.GameState}
     * or the match status changes.
     * <p>
     * Since the game state is managed by the controller, UI components cannot
     * directly observe state mutations.
     * Instead, the UI component (e.g. the game Activity or Fragment) registers
     * a callback here to be notified and trigger a re-render accordingly.
     *
     * @param listener the {@link Runnable} to invoke on every state change, or {@code null} to unregister.
     */
    void setOnStateChangedListener(Runnable listener);

    /**
     * Registers a listener that is called when the remote player aborts
     * the active match.
     *
     * @param listener callback to execute, or {@code null} to unregister
     */
    void setOnMatchAbortedListener(Runnable listener);

    /**
     * Starts listening for incoming game actions.
     *
     * Only one action listener may be active at a time.
     */
    void startListeningForActions();

    /**
     * Stops the listener for incoming game actions.
     */
    void stopListeningForActions();

    /**
     * Returns whether the controller is currently listening for actions.
     *
     * @return {@code true} if the action listener is active
     */
    boolean isListeningForActions();

    /**
     * Clears all state belonging to the current match session.
     *
     * The injected dependencies of the controller remain available.
     */
    void resetSession();
}