package kittycards.kittycardsandroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import kittycards.kittycardsandroid.components.IGameController;
import kittycards.kittycardsandroid.model.Player;
import kittycards.kittycardsandroid.model.MatchStatus;

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
    }

    @Test
    public void testMatchStartAndInitialState() {
        Player p1 = new Player(1, "Alice");
        Player p2 = new Player(2, "Bob");

        gameController.startMatch(p1, p2);

        assertNotNull(gameController);
        // Assuming GameController has a way to get the match via IGameController 
        // or we cast it for testing (though ideally IGameController should have it)
        // Let's check IGameController again.
    }
}
