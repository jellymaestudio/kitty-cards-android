package kittycards.kittycardsandroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import kittycards.kittycardsandroid.components.IGameController;
import kittycards.kittycardsandroid.logic.GameController;
import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.GameColor;
import kittycards.kittycardsandroid.model.MatchStatus;
import kittycards.kittycardsandroid.model.Player;
import kittycards.kittycardsandroid.network.Role;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class GameLogicIntegrationTest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    IGameController gameController;

    @Before
    public void init() {
        hiltRule.inject();
        if (gameController instanceof GameController gc) {
            gc.resetSession();
        }
    }

    @After
    public void tearDown() {
        if (gameController instanceof GameController gc) {
            gc.resetSession();
        }
    }

    @Test
    public void testFullMatchCycle_StartAndFirstTurn() {
        Player p1 = new Player(1, "Alice");
        Player p2 = new Player(2, "Bob");

        if (gameController instanceof GameController gc) {
            gc.setNetworkRole(Role.HOST);
            gc.setLocalPlayer(p1);
            
            // 1. Start Match
            gc.startMatch(p1, p2);

            assertNotNull("Match should be initialized", gc.getMatch());
            assertEquals("Match should be running", MatchStatus.RUNNING, gc.getMatch().getMatchStatus());
            assertNotNull("Game state should be initialized", gc.getMatch().getGameState());
            
            // 2. Play a card (Local turn)
            Player currentPlayer = gc.getMatch().getGameState().getCurrentPlayer();
            Card card = new Card(GameColor.CYAN, 7);
            currentPlayer.addCard(card);
            gc.selectCard(currentPlayer, card);
            
            int initialScore = currentPlayer.getScore();
            gc.playCard(currentPlayer, 0, 0);
            
            // Assertions after turn
            assertTrue("Score should have increased", currentPlayer.getScore() > initialScore);
            assertEquals("Card should be on board", card, gc.getMatch().getGameState().getBoard().getField(0, 0).getCard());
            assertTrue("Hand card should be removed", currentPlayer.getHandCards().isEmpty());
            
            Player nextPlayer = gc.getMatch().getGameState().getCurrentPlayer();
            assertTrue("Turn should have switched", nextPlayer != currentPlayer);
        }
    }
}
