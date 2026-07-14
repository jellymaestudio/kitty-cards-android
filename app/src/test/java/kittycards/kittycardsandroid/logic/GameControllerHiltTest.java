package kittycards.kittycardsandroid.logic;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import javax.inject.Inject;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import kittycards.kittycardsandroid.components.INetworkManager;
import kittycards.kittycardsandroid.components.IProtocolEngine;
import kittycards.kittycardsandroid.model.Player;
import org.robolectric.annotation.Config;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
@Config(application = HiltTestApplication.class)
public class GameControllerHiltTest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    GameController gameController;

    @BindValue
    INetworkManager networkManager = mock(INetworkManager.class);

    @BindValue
    IProtocolEngine protocolEngine = mock(IProtocolEngine.class);

    @Before
    public void init() {
        hiltRule.inject();
    }

    @Test
    public void testDrawCardSendsActionToNetwork() {
        Player p1 = new Player(1, "Alice");
        Player p2 = new Player(2, "Bob");
        gameController.startMatch(p1, p2);
        gameController.getMatch().getGameState().setCurrentPlayer(p1);

        gameController.drawCard(p1);

        org.mockito.Mockito.verify(networkManager).sendGameChange(org.mockito.Mockito.any());
    }
}
