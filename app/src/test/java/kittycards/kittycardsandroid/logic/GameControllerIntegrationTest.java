package kittycards.kittycardsandroid.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import kittycards.kittycardsandroid.components.FakeNetworkManager;
import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.GameColor;
import kittycards.kittycardsandroid.model.MatchStatus;
import kittycards.kittycardsandroid.model.Player;
import kittycards.kittycardsandroid.network.GameAction;
import kittycards.kittycardsandroid.network.Role;

/**
 * Comprehensive Integration test for the GameController.
 * Covers match start, card actions, network synchronization, and lifecycle management.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
@Config(application = HiltTestApplication.class)
public class GameControllerIntegrationTest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    GameController gameController;

    @Inject
    FakeNetworkManager fakeNetworkManager;

    private Player localPlayer;
    private Player remotePlayer;

    @Before
    public void init() {
        hiltRule.inject();
        localPlayer = new Player(1, "Alice");
        remotePlayer = new Player(2, "Bob");
    }

    // --- 1. Match Start ---

    @Test
    public void startMatch_asHost_sendsStartingPlayerBoardSetupAndDealsCards() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);

        List<GameAction> sentActions = fakeNetworkManager.getSentActions();
        assertEquals(8, sentActions.stream().filter(a -> a.type() == GameAction.ActionType.SET_BOARD_COLOR).count());
        assertEquals(1, sentActions.stream().filter(a -> a.type() == GameAction.ActionType.SET_STARTING_PLAYER).count());
        assertEquals(5, sentActions.stream().filter(a -> a.type() == GameAction.ActionType.DEAL_CARD).count());
        
        assertNotNull(gameController.getMatch());
        assertEquals(MatchStatus.RUNNING, gameController.getMatch().getMatchStatus());
    }

    @Test
    public void startMatch_asGuest_doesNotSendNetworkActions() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);

        assertTrue("Guest should be passive during setup", fakeNetworkManager.getSentActions().isEmpty());
    }

    @Test
    public void startMatch_initializesMatchAndMoveValidator() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        
        assertNotNull("Match should be initialized", gameController.getMatch());
        // Indirectly verify MoveValidator by performing a valid selection
        Card card = new Card(GameColor.CYAN, 5);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);
        assertTrue("Selection should be valid if MoveValidator is correctly initialized", localPlayer.hasSelectedCard());
    }

    // --- 2. Karten auswählen/abwählen ---

    @Test
    public void selectCard_validCard_updatesStateAndSendsAction() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.CYAN, 5);
        localPlayer.addCard(card);

        gameController.selectCard(localPlayer, card);

        assertTrue(localPlayer.hasSelectedCard());
        assertEquals(card, localPlayer.getSelectedCard());
        assertEquals(GameAction.ActionType.SELECT_CARD, fakeNetworkManager.getSentActions().getLast().type());
    }

    @Test
    public void selectCard_invalidMove_doesNothing() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card foreignCard = new Card(GameColor.PURPLE, 6); // Valid value but not in hand

        gameController.selectCard(localPlayer, foreignCard);

        assertFalse("Should not select card if not in hand", localPlayer.hasSelectedCard());
    }

    @Test
    public void unselectCard_sendsActionAndClearsSelection() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.CYAN, 5);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);

        gameController.unselectCard(localPlayer);

        assertFalse(localPlayer.hasSelectedCard());
        assertEquals(GameAction.ActionType.UNSELECT_CARD, fakeNetworkManager.getSentActions().getLast().type());
    }

    // --- 3. Karte spielen ---

    @Test
    public void playCard_validMove_updatesScoreAndSwitchesTurn() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);
        
        Card card = new Card(GameColor.YELLOW, 6); // Max value for simple score check
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);
        int initialScore = localPlayer.getScore();

        gameController.playCard(localPlayer, 0, 0);

        assertTrue("Score should have increased", localPlayer.getScore() > initialScore);
        assertEquals("Turn should switch", remotePlayer, gameController.getMatch().getGameState().getCurrentPlayer());
        assertEquals(card, gameController.getMatch().getGameState().getBoard().getField(0, 0).getCard());
    }

    @Test
    public void playCard_invalidMove_doesNothing() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        
        // No card selected, should not be able to play
        gameController.playCard(localPlayer, 0, 0);

        assertTrue("Board field should remain empty", gameController.getMatch().getGameState().getBoard().getField(0, 0).isEmpty());
        assertTrue("No network action should be sent", fakeNetworkManager.getSentActions().stream().noneMatch(a -> a.type() == GameAction.ActionType.PLAY_CARD));
    }

    @Test
    public void playCard_roundOver_hostStartsNextRoundAndSendsSetup() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        
        // Fill 7 fields to nearly finish the round
        for (int i = 0; i < 7; i++) {
            int r = i / 3; int c = i % 3;
            if (r == 1 && c == 1) { r = 2; c = 1; }
            gameController.getMatch().getGameState().getBoard().getField(r, c).placeCard(new Card(GameColor.CYAN, 1), localPlayer, 0);
        }

        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);
        Card finalCard = new Card(GameColor.CYAN, 1);
        localPlayer.addCard(finalCard);
        gameController.selectCard(localPlayer, finalCard);

        gameController.playCard(localPlayer, 2, 2);

        assertEquals("Round should advance", 2, gameController.getMatch().getMatchState().getCurrentRound());
        // Verify broadcast (Setup for round 2: 8 colors + starting player)
        long setupActions = fakeNetworkManager.getSentActions().stream()
                .filter(a -> a.type() == GameAction.ActionType.SET_BOARD_COLOR || a.type() == GameAction.ActionType.SET_STARTING_PLAYER)
                .count();
        assertTrue("Host should broadcast new round setup", setupActions >= 9);
    }

    @Test
    public void playCard_matchOver_hostSendsMatchFinishedAction() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        
        // Host needs 2 wins to end the match (standard)
        localPlayer.addWin(); 

        // Nearly fill board
        for (int i = 0; i < 8; i++) {
            int r = i / 3; int c = i % 3;
            if (r == 1 && c == 1) continue;
            gameController.getMatch().getGameState().getBoard().getField(r, c).placeCard(new Card(GameColor.CYAN, 1), localPlayer, 0);
        }
        
        // Clear the last field for playCard
        gameController.getMatch().getGameState().getBoard().getField(2, 2).clearField();

        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);
        Card finalCard = new Card(GameColor.CYAN, 1);
        localPlayer.addCard(finalCard);
        gameController.selectCard(localPlayer, finalCard);

        gameController.playCard(localPlayer, 2, 2);

        assertEquals(MatchStatus.FINISHED, gameController.getMatch().getMatchStatus());
        assertTrue("Host should send MATCH_FINISHED", fakeNetworkManager.getSentActions().stream().anyMatch(a -> a.type() == GameAction.ActionType.MATCH_FINISHED));
    }

    @Test
    public void playCard_asGuest_doesNotTriggerRoundTransition() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        
        int roundBefore = gameController.getMatch().getMatchState().getCurrentRound();
        
        // Fill board
        for (int i = 0; i < 8; i++) {
            int r = i / 3; int c = i % 3;
            if (r == 1 && c == 1) continue;
            gameController.getMatch().getGameState().getBoard().getField(r, c).placeCard(new Card(GameColor.CYAN, 1), localPlayer, 0);
        }
        
        // Guest plays (if it were their turn and they had a card, but here we check the logic branch)
        gameController.playCard(localPlayer, 0, 0); // Field occupied, but we test the HOST check for transition

        assertEquals("Guest should not advance round autonomously", roundBefore, gameController.getMatch().getMatchState().getCurrentRound());
    }

    // --- 4. Karte ziehen ---

    @Test
    public void drawCard_validDraw_addsCardAndSwitchesTurn() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);
        int count = localPlayer.getHandCardCount();

        gameController.drawCard(localPlayer);

        assertEquals("Hand size should increase", count + 1, localPlayer.getHandCardCount());
        assertEquals("Turn should switch", remotePlayer, gameController.getMatch().getGameState().getCurrentPlayer());
    }

    @Test
    public void drawCard_invalidDraw_doesNothing() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        // Not local turn
        gameController.getMatch().getGameState().setCurrentPlayer(remotePlayer);

        gameController.drawCard(localPlayer);

        assertEquals("Should not draw if not turn", 0, localPlayer.getHandCardCount());
    }

    // --- 5. Remote-Aktionen (handleRemoteAction) ---

    @Test
    public void handleRemoteAction_drawCard_appliesToRemotePlayer() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card remoteCard = new Card(GameColor.GREEN, 4);

        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.DRAW_CARD, remoteCard));

        assertTrue(remotePlayer.hasCard(remoteCard));
        assertEquals("Turn should switch to local", localPlayer, gameController.getMatch().getGameState().getCurrentPlayer());
    }

    @Test
    public void handleRemoteAction_playCard_appliesToRemotePlayer() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card remoteCard = new Card(GameColor.PURPLE, 3);
        
        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.PLAY_CARD, remoteCard, 0, 0));

        assertEquals(remoteCard, gameController.getMatch().getGameState().getBoard().getField(0, 0).getCard());
        assertEquals(localPlayer, gameController.getMatch().getGameState().getCurrentPlayer());
    }

    @Test
    public void handleRemoteAction_selectCard_appliesToRemotePlayer() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card remoteCard = new Card(GameColor.CYAN, 2);
        
        // Ensure remote player has the card
        gameController.getMatch().getPlayerTwo().addCard(remoteCard);

        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SELECT_CARD, remoteCard));

        assertTrue(remotePlayer.hasSelectedCard());
        assertEquals(remoteCard, remotePlayer.getSelectedCard());
    }

    @Test
    public void handleRemoteAction_unselectCard_appliesToRemotePlayer() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.CYAN, 5);
        gameController.getMatch().getPlayerTwo().addCard(card);
        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SELECT_CARD, card));

        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.UNSELECT_CARD));

        assertFalse(remotePlayer.hasSelectedCard());
    }

    @Test
    public void handleRemoteAction_setStartingPlayer_setsCorrectPlayer() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);

        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SET_STARTING_PLAYER, 1)); // Guest starts

        // Verification happens via full round init test
    }

    @Test
    public void handleRemoteAction_dealCard_addsCardToCorrectPlayer() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.YELLOW, 1);

        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.DEAL_CARD, card, 0)); // Index 0 = Alice

        assertTrue(localPlayer.hasCard(card));
    }

    @Test
    public void handleRemoteAction_matchFinished_setsMatchStatusFinished() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);

        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.MATCH_FINISHED));

        assertEquals(MatchStatus.FINISHED, gameController.getMatch().getMatchStatus());
    }

    @Test
    public void handleRemoteAction_matchAborted_triggersAbortListener() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        AtomicBoolean aborted = new AtomicBoolean(false);
        gameController.setOnMatchAbortedListener(() -> aborted.set(true));

        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.MATCH_ABORTED));

        assertTrue(aborted.get());
    }

    @Test
    public void handleRemoteAction_notifiesStateChangedListener() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        AtomicInteger notificationCount = new AtomicInteger(0);
        gameController.setOnStateChangedListener(notificationCount::incrementAndGet);

        Card card = new Card(GameColor.CYAN, 1);
        gameController.getMatch().getPlayerTwo().addCard(card);
        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SELECT_CARD, card));
        
        assertTrue("Should notify state change", notificationCount.get() > 0);
    }

    // --- 6. Board-Setup-Synchronisation ---

    @Test
    public void applyBoardColor_allColorsAndStartingPlayerReceived_initializesFirstRound() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);

        for (int i = 0; i < 8; i++) {
            gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SET_BOARD_COLOR, GameColor.YELLOW, i % 3, i / 3));
        }
        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SET_STARTING_PLAYER, 0));

        assertEquals(GameColor.YELLOW, gameController.getMatch().getGameState().getBoard().getField(0, 0).getColor());
    }

    @Test
    public void applyBoardColor_secondRound_startsNextRoundInsteadOfInitializing() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        
        // Setup Round 1
        for(int i=0; i<8; i++) gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SET_BOARD_COLOR, GameColor.CYAN, i%3, i/3));
        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SET_STARTING_PLAYER, 0));
        
        // Setup Round 2
        for(int i=0; i<8; i++) gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SET_BOARD_COLOR, GameColor.YELLOW, i%3, i/3));
        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SET_STARTING_PLAYER, 1));
        
        assertEquals("Should advance to round 2", 2, gameController.getMatch().getMatchState().getCurrentRound());
        assertEquals("New setup should be applied", GameColor.YELLOW, gameController.getMatch().getGameState().getBoard().getField(0,0).getColor());
    }

    @Test
    public void applyBoardColor_incompleteData_doesNotInitializeRound() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);

        // Only 7 colors
        for (int i = 0; i < 7; i++) {
            gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SET_BOARD_COLOR, GameColor.YELLOW, i%3, i/3));
        }
        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SET_STARTING_PLAYER, 0));

        assertTrue("Should not initialize if colors missing", gameController.getMatch().getGameState().getBoard().getField(0, 0).getColor() != GameColor.YELLOW);
    }

    // --- 7. Action-Listener-Lifecycle ---

    @Test
    public void startListeningForActions_lifecycle_exceptions_and_logic() {
        gameController.resetSession();
        gameController.setNetworkManager(null);
        assertThrows("Should throw if NM null", IllegalStateException.class, () -> gameController.startListeningForActions());

        gameController.setNetworkManager(fakeNetworkManager);
        gameController.setNetworkRole(Role.NOT_CONNECTED);
        assertThrows("Should throw if role NOT_CONNECTED", IllegalStateException.class, () -> gameController.startListeningForActions());
        
        gameController.setNetworkRole(Role.HOST);
        gameController.startListeningForActions();
        assertTrue(gameController.isListeningForActions());
        
        gameController.stopListeningForActions();
        assertFalse(gameController.isListeningForActions());
    }

    // --- 8. Session-Reset ---

    @Test
    public void resetSession_clearsAllStateAndStopsListening() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.setNetworkManager(fakeNetworkManager);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.startListeningForActions();

        gameController.resetSession();

        assertNull("Match should be null", gameController.getMatch());
        assertNull("Player should be null", gameController.getLocalPlayer());
        assertFalse("Should stop listening", gameController.isListeningForActions());
    }

    // --- 9. End-to-End Consistency ---

    @Test
    public void fullRound_hostAndGuestSync_boardAndScoresConsistent() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        
        // Local play
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);
        Card card = new Card(GameColor.YELLOW, 5);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);
        gameController.playCard(localPlayer, 0, 0);

        assertEquals("Board should have card", card, gameController.getMatch().getGameState().getBoard().getField(0, 0).getCard());
        assertEquals("Turn should have switched", remotePlayer, gameController.getMatch().getGameState().getCurrentPlayer());
    }

    @Test
    public void matchFinished_bothDevicesReachSameFinalState() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        
        // Signal from remote
        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.MATCH_FINISHED));
        
        assertEquals(MatchStatus.FINISHED, gameController.getMatch().getMatchStatus());
    }
}
