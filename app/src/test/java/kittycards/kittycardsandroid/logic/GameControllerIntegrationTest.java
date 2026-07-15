package kittycards.kittycardsandroid.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
 * Comprehensive Integration test for the GameController using a FakeNetworkManager.
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
        
        gameController.resetSession();
        gameController.setLocalPlayer(localPlayer);
        fakeNetworkManager.clearSentActions();
    }

    // -------------------------------------------------------------------------
    // startMatch
    // -------------------------------------------------------------------------

    @Test
    public void startMatch_setsActiveMatchWithRunningStatus() {
        gameController.startMatch(localPlayer, remotePlayer);
        
        assertNotNull(gameController.getMatch());
        assertEquals(MatchStatus.RUNNING, gameController.getMatch().getMatchStatus());
    }

    @Test
    public void startMatch_asHost_generatesAndTransmitsInitialSetup() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);

        List<GameAction> sentActions = fakeNetworkManager.getSentActions();
        assertEquals(8, sentActions.stream().filter(a -> a.type() == GameAction.ActionType.SET_BOARD_COLOR).count());
        assertEquals(1, sentActions.stream().filter(a -> a.type() == GameAction.ActionType.SET_STARTING_PLAYER).count());
        assertEquals(5, sentActions.stream().filter(a -> a.type() == GameAction.ActionType.DEAL_CARD).count());
    }

    @Test
    public void startMatch_asHost_sentActionsContainDealCardPerInitialCard() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);

        List<GameAction> dealActions = fakeNetworkManager.getSentActions().stream()
                .filter(a -> a.type() == GameAction.ActionType.DEAL_CARD)
                .toList();
        
        assertEquals(5, dealActions.size());
        for (GameAction action : dealActions) {
            assertNotNull(action.card());
        }
    }

    @Test
    public void startMatch_asHost_sentActionsContainSetStartingPlayer() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);

        assertTrue(fakeNetworkManager.getSentActions().stream()
                .anyMatch(a -> a.type() == GameAction.ActionType.SET_STARTING_PLAYER));
    }

    @Test
    public void startMatch_triggersStateChangedListener() {
        AtomicInteger count = new AtomicInteger(0);
        gameController.setOnStateChangedListener(count::incrementAndGet);
        
        gameController.startMatch(localPlayer, remotePlayer);

        assertEquals(1, count.get());
    }

    // -------------------------------------------------------------------------
    // selectCard
    // -------------------------------------------------------------------------

    @Test
    public void selectCard_succeedsWhenCardInHandAndMatchRunning() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.CYAN, 5);
        localPlayer.addCard(card);

        gameController.selectCard(localPlayer, card);

        assertTrue(localPlayer.hasSelectedCard());
        assertEquals(card, localPlayer.getSelectedCard());
    }

    @Test
    public void selectCard_transmitsActionOnSuccess() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.CYAN, 5);
        localPlayer.addCard(card);

        gameController.selectCard(localPlayer, card);

        GameAction lastAction = fakeNetworkManager.getSentActions().getLast();
        assertEquals(GameAction.ActionType.SELECT_CARD, lastAction.type());
        assertEquals(card, lastAction.card());
    }

    @Test
    public void selectCard_sentActionHasTypeSelectCard() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.CYAN, 5);
        localPlayer.addCard(card);

        gameController.selectCard(localPlayer, card);

        assertEquals(GameAction.ActionType.SELECT_CARD, fakeNetworkManager.getSentActions().getLast().type());
    }

    @Test
    public void selectCard_sentActionContainsSelectedCard() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.CYAN, 5);
        localPlayer.addCard(card);

        gameController.selectCard(localPlayer, card);

        assertEquals(card, fakeNetworkManager.getSentActions().getLast().card());
    }

    @Test
    public void selectCard_doesNotTransmitOnFailure() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        fakeNetworkManager.clearSentActions();
        
        gameController.selectCard(localPlayer, new Card(GameColor.PURPLE, 1)); // Not in hand

        assertTrue(fakeNetworkManager.getSentActions().isEmpty());
    }

    // -------------------------------------------------------------------------
    // unselectCard
    // -------------------------------------------------------------------------

    @Test
    public void unselectCard_clearsExistingSelection() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.CYAN, 5);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);

        gameController.unselectCard(localPlayer);

        assertFalse(localPlayer.hasSelectedCard());
    }

    @Test
    public void unselectCard_doesNotTransmitWhenNothingSelected() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        fakeNetworkManager.clearSentActions();

        gameController.unselectCard(localPlayer);

        assertTrue(fakeNetworkManager.getSentActions().isEmpty());
    }

    @Test
    public void unselectCard_transmitsOnSuccess() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.CYAN, 5);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);

        gameController.unselectCard(localPlayer);

        GameAction lastAction = fakeNetworkManager.getSentActions().getLast();
        assertEquals(GameAction.ActionType.UNSELECT_CARD, lastAction.type());
        assertNull(lastAction.card());
    }

    @Test
    public void unselectCard_sentActionHasTypeUnselectCard() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.CYAN, 5);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);

        gameController.unselectCard(localPlayer);

        assertEquals(GameAction.ActionType.UNSELECT_CARD, fakeNetworkManager.getSentActions().getLast().type());
    }

    @Test
    public void unselectCard_sentActionCardFieldIsNull() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.CYAN, 5);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);

        gameController.unselectCard(localPlayer);

        assertNull(fakeNetworkManager.getSentActions().getLast().card());
    }

    // -------------------------------------------------------------------------
    // playCard
    // -------------------------------------------------------------------------

    @Test
    public void playCard_succeedsWhenAllConditionsMet() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);
        
        Card card = new Card(GameColor.YELLOW, 6);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);

        gameController.playCard(localPlayer, 0, 0);

        assertEquals(card, gameController.getMatch().getGameState().getBoard().getField(0, 0).getCard());
        assertEquals(remotePlayer, gameController.getMatch().getGameState().getCurrentPlayer());
    }

    @Test
    public void playCard_noopWhenNoCardSelected() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        fakeNetworkManager.clearSentActions();

        gameController.playCard(localPlayer, 0, 0);

        assertTrue(gameController.getMatch().getGameState().getBoard().getField(0, 0).isEmpty());
        assertTrue(fakeNetworkManager.getSentActions().isEmpty());
    }

    @Test
    public void playCard_noopWhenRowOrColumnNegative() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);
        Card card = new Card(GameColor.YELLOW, 6);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);
        fakeNetworkManager.clearSentActions();

        gameController.playCard(localPlayer, -1, 0);

        assertTrue(fakeNetworkManager.getSentActions().isEmpty());
    }

    @Test
    public void playCard_noopWhenRowOrColumnGreaterThanTwo() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);
        Card card = new Card(GameColor.YELLOW, 6);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);
        fakeNetworkManager.clearSentActions();

        gameController.playCard(localPlayer, 3, 0);

        assertTrue(fakeNetworkManager.getSentActions().isEmpty());
    }

    @Test
    public void playCard_noopWhenPositionIsCenterField() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);
        Card card = new Card(GameColor.YELLOW, 6);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);
        fakeNetworkManager.clearSentActions();

        // Center field is (1, 1)
        gameController.playCard(localPlayer, 1, 1);

        assertTrue(fakeNetworkManager.getSentActions().isEmpty());
    }

    @Test
    public void playCard_transmitsOnSuccess() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);
        Card card = new Card(GameColor.YELLOW, 6);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);

        gameController.playCard(localPlayer, 0, 0);

        GameAction lastAction = fakeNetworkManager.getSentActions().getLast();
        assertEquals(GameAction.ActionType.PLAY_CARD, lastAction.type());
        assertEquals(0, lastAction.boardPositionRow());
        assertEquals(0, lastAction.boardPositionColumn());
        assertEquals(card, lastAction.card());
    }

    @Test
    public void playCard_sentActionHasTypePlayCard() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);
        Card card = new Card(GameColor.YELLOW, 6);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);

        gameController.playCard(localPlayer, 0, 0);

        assertEquals(GameAction.ActionType.PLAY_CARD, fakeNetworkManager.getSentActions().getLast().type());
    }

    @Test
    public void playCard_sentActionContainsCorrectBoardPosition() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);
        Card card = new Card(GameColor.YELLOW, 6);
        localPlayer.addCard(card);
        gameController.selectCard(localPlayer, card);

        gameController.playCard(localPlayer, 2, 1);

        GameAction lastAction = fakeNetworkManager.getSentActions().getLast();
        assertEquals(2, lastAction.boardPositionRow());
        assertEquals(1, lastAction.boardPositionColumn());
    }

    // -------------------------------------------------------------------------
    // drawCard
    // -------------------------------------------------------------------------

    @Test
    public void drawCard_addsGeneratedCardToHand() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);
        int initialCount = localPlayer.getHandCardCount();

        gameController.drawCard(localPlayer);

        assertEquals(initialCount + 1, localPlayer.getHandCardCount());
    }

    @Test
    public void drawCard_transmitsOnSuccess() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);

        gameController.drawCard(localPlayer);

        GameAction lastAction = fakeNetworkManager.getSentActions().getLast();
        assertEquals(GameAction.ActionType.DRAW_CARD, lastAction.type());
        assertNotNull(lastAction.card());
    }

    @Test
    public void drawCard_sentActionHasTypeDrawCard() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);

        gameController.drawCard(localPlayer);

        assertEquals(GameAction.ActionType.DRAW_CARD, fakeNetworkManager.getSentActions().getLast().type());
    }

    @Test
    public void drawCard_sentActionContainsGeneratedCard() {
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(localPlayer);
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(localPlayer);

        gameController.drawCard(localPlayer);

        Card drawnCard = localPlayer.getHandCards().get(localPlayer.getHandCardCount() - 1);
        assertEquals(drawnCard, fakeNetworkManager.getSentActions().getLast().card());
    }

    // -------------------------------------------------------------------------
    // handleRemoteAction
    // -------------------------------------------------------------------------

    @Test
    public void setOnMatchAbortedListener_invokedOnRemoteAbortAction() {
        gameController.startMatch(localPlayer, remotePlayer);
        AtomicBoolean aborted = new AtomicBoolean(false);
        gameController.setOnMatchAbortedListener(() -> aborted.set(true));

        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.MATCH_ABORTED));

        assertTrue(aborted.get());
    }

    @Test
    public void incomingMatchAbortedAction_triggersOnMatchAbortedListener() {
        gameController.startMatch(localPlayer, remotePlayer);
        AtomicBoolean aborted = new AtomicBoolean(false);
        gameController.setOnMatchAbortedListener(() -> aborted.set(true));

        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.MATCH_ABORTED));

        assertTrue(aborted.get());
    }

    @Test
    public void incomingSelectCardAction_updatesRemotePlayerSelection() {
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.PURPLE, 3);
        remotePlayer.addCard(card);

        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SELECT_CARD, card));

        assertTrue(remotePlayer.hasSelectedCard());
        assertEquals(card, remotePlayer.getSelectedCard());
    }

    @Test
    public void incomingUnselectCardAction_clearsRemotePlayerSelection() {
        gameController.startMatch(localPlayer, remotePlayer);
        Card card = new Card(GameColor.PURPLE, 3);
        remotePlayer.addCard(card);
        remotePlayer.selectCard(card);

        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.UNSELECT_CARD));

        assertFalse(remotePlayer.hasSelectedCard());
    }

    @Test
    public void incomingPlayCardAction_appliesPlacementToLocalBoard() {
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(remotePlayer);
        Card card = new Card(GameColor.PURPLE, 3);
        remotePlayer.addCard(card);
        remotePlayer.selectCard(card);

        // Action constructor: (type, card, col, row)
        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.PLAY_CARD, card, 0, 2));

        assertEquals(card, gameController.getMatch().getGameState().getBoard().getField(2, 0).getCard());
    }

    @Test
    public void incomingDrawCardAction_addsCardToRemotePlayerHand() {
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.getMatch().getGameState().setCurrentPlayer(remotePlayer);
        Card card = new Card(GameColor.GREEN, 4);

        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.DRAW_CARD, card));

        assertTrue(remotePlayer.hasCard(card));
    }

    @Test
    public void incomingDealCardAction_addsInitialCardToCorrectPlayer() {
        gameController.startMatch(localPlayer, remotePlayer);
        Card card1 = new Card(GameColor.YELLOW, 1);
        Card card2 = new Card(GameColor.CYAN, 2);

        // contextSensitiveInt 0 -> playerOne (localPlayer here), 1 -> playerTwo (remotePlayer)
        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.DEAL_CARD, card1, 0));
        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.DEAL_CARD, card2, 1));

        assertTrue(localPlayer.hasCard(card1));
        assertTrue(remotePlayer.hasCard(card2));
    }

    @Test
    public void incomingSetStartingPlayerAction_setsCurrentPlayer() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.startMatch(localPlayer, remotePlayer);

        // Host sends Starting Player THEN Board Setup
        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SET_STARTING_PLAYER, 1));

        // Send 8 colors
        for (int i = 0; i < 8; i++) {
            gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SET_BOARD_COLOR, GameColor.CYAN, 0, 0));
        }

        assertEquals(remotePlayer, gameController.getMatch().getGameState().getCurrentPlayer());
    }

    @Test
    public void incomingStartMatchSetup_asGuest_initializesMatchFromHostData() {
        gameController.setNetworkRole(Role.GUEST);
        gameController.startMatch(localPlayer, remotePlayer);
        
        // Send starting player FIRST
        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SET_STARTING_PLAYER, 0));

        // Send 8 colors
        for (int i = 0; i < 8; i++) {
            int row = i < 3 ? 0 : (i < 5 ? 1 : 2);
            int col = i % 3;
            if (row == 1 && col == 1) col = 2; // skip center
            gameController.handleRemoteAction(new GameAction(GameAction.ActionType.SET_BOARD_COLOR, GameColor.CYAN, col, row));
        }
        
        assertNotNull(gameController.getMatch().getGameState());
        assertEquals(localPlayer, gameController.getMatch().getGameState().getCurrentPlayer());
    }

    @Test
    public void setOnStateChangedListener_invokedAfterRemoteActionProcessed() {
        gameController.startMatch(localPlayer, remotePlayer);
        AtomicInteger count = new AtomicInteger(0);
        gameController.setOnStateChangedListener(count::incrementAndGet);

        gameController.handleRemoteAction(new GameAction(GameAction.ActionType.UNSELECT_CARD));
        
        assertTrue(count.get() > 0);
    }

    // -------------------------------------------------------------------------
    // resetSession
    // -------------------------------------------------------------------------

    @Test
    public void resetSession_clearsMatch() {
        gameController.startMatch(localPlayer, remotePlayer);
        gameController.resetSession();

        assertNull(gameController.getMatch());
    }

    @Test
    public void resetSession_clearsLocalPlayer() {
        gameController.setLocalPlayer(localPlayer);
        gameController.resetSession();

        assertNull(gameController.getLocalPlayer());
    }

    @Test
    public void resetSession_stopsActionListener() {
        gameController.setNetworkRole(Role.HOST);
        gameController.startListeningForActions();

        gameController.resetSession();

        assertFalse(gameController.isListeningForActions());
    }
}
