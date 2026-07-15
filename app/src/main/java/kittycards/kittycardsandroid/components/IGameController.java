package kittycards.kittycardsandroid.components;

import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.GameState;
import kittycards.kittycardsandroid.model.Match;
import kittycards.kittycardsandroid.model.Player;
import kittycards.kittycardsandroid.network.Role;

/**
 * Defines the public operations for controlling one Kitty Cards match session.
 *
 * <p>The controller owns the active {@link Match}, validates requested player
 * actions, applies valid actions to the local game state and synchronizes those
 * actions with the connected remote device. Invalid player actions are ignored:
 * they do not change the game state, are not transmitted and do not trigger a
 * state-change notification.</p>
 *
 * <p>The controller also processes incoming remote actions and notifies
 * registered listeners when the locally represented session changes.</p>
 *
 * @author red_concrete
 * @author JellyMae
 */
public interface IGameController {

    /**
     * Creates and starts a new match with the supplied players.
     *
     * <p>The newly created match becomes the active match and its status is set
     * to running. Any state belonging to the previous match is replaced. On a
     * host device, the initial round setup and initial cards are generated and
     * transmitted to the remote device. On a guest device, the controller waits
     * for that setup to be received from the host.</p>
     *
     * <p>A state-change notification is triggered after the match has been
     * initialized.</p>
     *
     * @param playerOne the first player participating in the match
     * @param playerTwo the second player participating in the match
     */
    void startMatch(Player playerOne, Player playerTwo);

    /**
     * Returns the match currently managed by this controller.
     *
     * @return the active match, or {@code null} if no match session has been
     * started or the session has been reset
     */
    Match getMatch();

    /**
     * Returns the player controlled by the local device.
     *
     * @return the local player, or {@code null} if no local player has been set
     * or the session has been reset
     */
    Player getLocalPlayer();

    /**
     * Returns the other participant in the active match relative to the local
     * player.
     *
     * @return the remote player of the active match
     * @throws NullPointerException if no active match exists
     */
    Player getRemotePlayer();

    /**
     * Sets the player that is controlled by the local device.
     *
     * <p>Calling this method replaces any previously configured local player.</p>
     *
     * @param localPlayer the player controlled by the local device, or
     *                    {@code null} to clear the current assignment
     */
    void setLocalPlayer(Player localPlayer);

    /**
     * Sets the network role of the local device for the current session.
     *
     * <p>The role determines whether this controller creates and sends the
     * authoritative round setup as host or receives it as guest.</p>
     *
     * @param role the role of the local device
     * @throws NullPointerException if {@code role} is {@code null}
     */
    void setNetworkRole(Role role);

    /**
     * Selects the given card for the specified player.
     *
     * <p>The selection succeeds only while a match is running, the player
     * belongs to that match and the card is contained in that player's hand.
     * Selecting a new card replaces the player's previous selection.</p>
     *
     * <p>A successful selection is transmitted to the remote device and
     * triggers a state-change notification. If the selection is invalid, the
     * method performs no action.</p>
     *
     * @param player the player selecting a card
     * @param card   the card to select
     * @throws NullPointerException if {@code card} is {@code null} and
     *                              {@code player} belongs to the active match
     */
    void selectCard(Player player, Card card);

    /**
     * Clears the currently selected card of the specified player.
     *
     * <p>The action is permitted only while a match is running and the player
     * belongs to that match. If the player has no selected card, the method has
     * no observable effect: no state is changed, no network action is sent and
     * no state-change notification is triggered.</p>
     *
     * <p>A successful unselection is transmitted to the remote device and
     * triggers a state-change notification.</p>
     *
     * @param player the player whose selection is to be cleared
     * @throws NullPointerException if {@code player} is {@code null}
     */
    void unselectCard(Player player);

    /**
     * Places the specified player's selected card on a board field.
     *
     * <p>The placement succeeds only if all of the following conditions are
     * satisfied:</p>
     * <ul>
     *     <li>the match is running,</li>
     *     <li>it is the specified player's turn,</li>
     *     <li>the player has selected a card,</li>
     *     <li>the position is inside the board,</li>
     *     <li>the position is not the center draw field, and</li>
     *     <li>the target field is empty.</li>
     * </ul>
     *
     * <p>After a successful placement, the card is removed from the player's
     * hand, the player's selection is cleared, the resulting score is added and
     * the turn or round is advanced as required. The placement is transmitted
     * to the remote device and a state-change notification is triggered.</p>
     *
     * <p>If any condition is not satisfied, the method performs no action. In
     * particular, an occupied or out-of-bounds position and a missing prior
     * selection are handled as no-ops rather than by throwing an exception.</p>
     *
     * @param player the player attempting to place the selected card
     * @param row    the zero-based row of the target field
     * @param column the zero-based column of the target field
     * @throws NullPointerException if {@code player} is {@code null}
     */
    void playCard(Player player, int row, int column);

    /**
     * Generates and adds one new card to the specified player's hand.
     *
     * <p>Drawing succeeds only while a match is running, it is the specified
     * player's turn and the player's hand contains fewer than ten cards. This
     * game uses generated cards rather than a finite deck; therefore, there is
     * no deck-empty condition.</p>
     *
     * <p>After a successful draw, the turn changes to the other player. The
     * generated card is transmitted to the remote device and a state-change
     * notification is triggered. If drawing is not permitted, the method
     * performs no action.</p>
     *
     * @param player the player attempting to draw a card
     * @throws NullPointerException if {@code player} is {@code null}
     */
    void drawCard(Player player);

    /**
     * Registers the callback used to observe changes to the active
     * {@link GameState} or match status.
     *
     * <p>The supplied listener replaces any listener registered previously.
     * It is invoked after a local action has been applied successfully, after
     * an incoming remote action has been processed and after a match has been
     * started. Invalid local actions do not invoke it.</p>
     *
     * @param listener callback to invoke after a relevant state change, or
     *                 {@code null} to unregister the current callback
     */
    void setOnStateChangedListener(Runnable listener);

    /**
     * Registers the callback that is invoked when the remote participant aborts
     * the active match.
     *
     * <p>The supplied listener replaces any listener registered previously.
     * The listener is invoked once for each received match-aborted action.</p>
     *
     * @param listener callback to invoke when a remote abort is received, or
     *                 {@code null} to unregister the current callback
     */
    void setOnMatchAbortedListener(Runnable listener);

    /**
     * Starts processing incoming game actions on a background listener thread.
     *
     * <p>Queued incoming actions are retrieved in order and applied to the local
     * match until listening is stopped or the network role becomes
     * {@link Role#NOT_CONNECTED}. Calling this method while the listener is
     * already active has no effect and does not start another thread.</p>
     *
     * @throws IllegalStateException if the network role is
     *                               {@link Role#NOT_CONNECTED}
     */
    void startListeningForActions();

    /**
     * Stops processing incoming game actions.
     *
     * <p>The listener thread is interrupted so that a blocking wait for the next
     * action can finish. Calling this method while no listener is active has no
     * effect.</p>
     */
    void stopListeningForActions();

    /**
     * Reports whether the controller's action listener is currently active.
     *
     * @return {@code true} while incoming actions are being listened for;
     * otherwise {@code false}
     */
    boolean isListeningForActions();

    /**
     * Ends the current session and restores the controller to an unconfigured
     * session state.
     *
     * <p>This method stops the action listener and clears the active match, the
     * local player, network role, pending round setup data and registered
     * callbacks. After it returns, no further incoming actions are processed by
     * the stopped listener.</p>
     *
     * <p>Constructor-injected dependencies are retained and remain usable, so
     * the same controller instance can be configured for another session.</p>
     */
    void resetSession();
}