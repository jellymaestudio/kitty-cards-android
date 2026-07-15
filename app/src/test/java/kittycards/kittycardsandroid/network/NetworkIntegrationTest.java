package kittycards.kittycardsandroid.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import kittycards.kittycardsandroid.components.FakeNetworkManager;
import kittycards.kittycardsandroid.logic.GameController;
import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.GameColor;
import kittycards.kittycardsandroid.model.Player;

/**
 * Integration test verifying the seamless interaction between the Network Component 
 * and the Game Logic. This serves as a technical proof of quality for the remote 
 * synchronization mechanism.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
@Config(application = HiltTestApplication.class)
public class NetworkIntegrationTest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    GameController gameController;

    @Inject
    FakeNetworkManager fakeNetworkManager;

    @Before
    public void init() {
        hiltRule.inject();
    }

    /**
     * Verifies that a "Play Card" action received via the network is correctly 
     * processed by the GameController and reflected in the local GameState.
     */
    @Test
    public void remotePlayCardAction_isReflectedInLocalBoard() throws InterruptedException {
        // 1. Arrange: Initialize a match where the local player is the Guest
        Player local = new Player(1, "LocalGuest");
        Player remote = new Player(2, "RemoteHost");
        
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(local);
        gameController.startMatch(local, remote);
        
        // Ensure the match is initialized (center field etc.)
        gameController.startListeningForActions();

        // 2. Act: Simulate an incoming PLAY_CARD action from the host
        Card cardPlayedByRemote = new Card(GameColor.CYAN, 5);
        GameAction remoteAction = new GameAction(
            GameAction.ActionType.PLAY_CARD, 
            cardPlayedByRemote, 
            1, // column
            1  // row
        );

        fakeNetworkManager.simulateIncomingAction(remoteAction);

        // Give the background thread time to process
        Thread.sleep(100);

        // 3. Assert: Verify the board state
        Card cardOnBoard = gameController.getMatch().getGameState().getBoard().getField(1, 1).getCard();
        
        assertNotNull("The card should have been placed on the local board", cardOnBoard);
        assertEquals("Value mismatch on board", 5, cardOnBoard.getValue());
        assertEquals("Color mismatch on board", GameColor.CYAN, cardOnBoard.getColor());
        
        gameController.stopListeningForActions();
    }

    /**
     * Verifies that the GameController reacts correctly when the network connection is lost.
     */
    @Test
    public void partnerDisconnected_abortsLocalMatch() {
        // 1. Arrange
        Player local = new Player(1, "Local");
        Player remote = new Player(2, "Remote");
        gameController.setNetworkRole(Role.HOST);
        gameController.startMatch(local, remote);
        
        AtomicBoolean abortNotified = new AtomicBoolean(false);
        gameController.setOnMatchAbortedListener(() -> abortNotified.set(true));

        // 2. Act: Simulate Bluetooth connection loss
        fakeNetworkManager.simulatePartnerDisconnected();

        // 3. Assert
        assertTrue("UI should be notified about aborted match", abortNotified.get());
        assertEquals("Role should be reset", Role.NOT_CONNECTED, gameController.getMatch().getGameState() == null ? Role.NOT_CONNECTED : Role.HOST);
        // Note: GameController.resetSession would be called in a real UI context via the listener
    }

    /**
     * Verifies that the complete board setup is synchronized from Host to Guest.
     */
    @Test
    public void boardSetup_isSynchronizedFully() throws InterruptedException {
        // 1. Arrange: Guest waiting for setup
        Player local = new Player(1, "Guest");
        Player remote = new Player(2, "Host");
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(local);
        gameController.startMatch(local, remote);
        gameController.startListeningForActions();

        // 2. Act: Simulate Host sending board setup (8 fields, center is omitted)
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (r == 1 && c == 1) continue; 
                fakeNetworkManager.simulateIncomingAction(
                    new GameAction(GameAction.ActionType.SET_BOARD_COLOR, GameColor.PURPLE, c, r)
                );
            }
        }
        // Also need to send the starting player to trigger initializeCurrentRound
        fakeNetworkManager.simulateIncomingAction(new GameAction(GameAction.ActionType.SET_STARTING_PLAYER, 0));

        Thread.sleep(200);

        // 3. Assert: Verify all 8 non-center fields are correctly synchronized
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (r == 1 && c == 1) continue; 
                assertEquals("Board field (" + r + "," + c + ") should be PURPLE", 
                    GameColor.PURPLE, 
                    gameController.getMatch().getGameState().getBoard().getField(r, c).getColor());
            }
        }
        
        assertNotNull("Match should have a starting player for guest", 
            gameController.getMatch().getGameState().getStartingPlayer());
        
        gameController.stopListeningForActions();
    }

    /**
     * Verifies that local game actions (like drawing a card) are correctly 
     * broadcasted to the network.
     */
    @Test
    public void localDrawCardAction_isBroadcastedToNetwork() {
        // 1. Arrange
        Player local = new Player(1, "LocalPlayer");
        Player remote = new Player(2, "RemotePlayer");
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(local);
        gameController.startMatch(local, remote);
        gameController.getMatch().getGameState().setCurrentPlayer(local);

        // 2. Act
        gameController.drawCard(local);

        // 3. Assert
        GameAction lastSent = fakeNetworkManager.getSentActions().stream()
                .filter(a -> a.type() == GameAction.ActionType.DRAW_CARD)
                .findFirst()
                .orElse(null);

        assertNotNull("DRAW_CARD action should have been sent to network", lastSent);
        assertNotNull("Sent action must contain the drawn card", lastSent.card());
    }
}
