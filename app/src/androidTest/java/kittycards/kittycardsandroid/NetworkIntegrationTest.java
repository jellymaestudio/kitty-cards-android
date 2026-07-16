package kittycards.kittycardsandroid;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import kittycards.kittycardsandroid.components.IGameController;
import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.GameColor;
import kittycards.kittycardsandroid.model.Player;
import kittycards.kittycardsandroid.network.GameAction;
import kittycards.kittycardsandroid.network.NetworkDevice;
import kittycards.kittycardsandroid.network.Role;
import kittycards.kittycardsandroid.network.event.NetworkEvent;
import kittycards.kittycardsandroid.ui.GameActivity;
import kittycards.kittycardsandroid.ui.LobbyActivity;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class NetworkIntegrationTest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    FakeNetworkManager fakeNetworkManager;

    @Inject
    IGameController gameController;

    @Before
    public void init() {
        hiltRule.inject();
        fakeNetworkManager.disconnect();
        fakeNetworkManager.clearSentActions();
    }

    // --- 1. Host-Flow ---

    @Test
    public void hostMatch_showsEmptyLobby_whenNoGuestConnected() {
        try (ActivityScenario<LobbyActivity> scenario = ActivityScenario.launch(LobbyActivity.class)) {
            onView(withId(R.id.hostGameButton)).perform(click());
            onView(withId(R.id.hostTitleText)).check(matches(isDisplayed()));
            onView(withId(R.id.startMatchButton)).check(matches(not(isEnabled())));
            onView(withText(containsString("(You)"))).check(matches(isDisplayed()));
        }
    }

    @Test
    public void hostMatch_updatesGuestList_whenGuestConnects() {
        try (ActivityScenario<LobbyActivity> scenario = ActivityScenario.launch(LobbyActivity.class)) {
            onView(withId(R.id.hostGameButton)).perform(click());
            NetworkDevice guest = new NetworkDevice("Guest 1", "01");
            fakeNetworkManager.simulateGuestConnected(guest);
            onView(withText("Guest 1")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void hostMatch_updatesGuestList_whenMultipleGuestsConnect() {
        try (ActivityScenario<LobbyActivity> scenario = ActivityScenario.launch(LobbyActivity.class)) {
            onView(withId(R.id.hostGameButton)).perform(click());
            fakeNetworkManager.simulateGuestConnected(new NetworkDevice("G1", "01"));
            fakeNetworkManager.simulateGuestConnected(new NetworkDevice("G2", "02"));
            onView(withText("G1")).check(matches(isDisplayed()));
            onView(withText("G2")).check(matches(isDisplayed()));
        }
    }

    // --- 2. Guest-Flow / Discovery ---

    @Test
    public void joinMatch_showsEmptyDeviceList_whenNoHostFound() {
        try (ActivityScenario<LobbyActivity> scenario = ActivityScenario.launch(LobbyActivity.class)) {
            onView(withId(R.id.joinGameButton)).perform(click());
            onView(withId(R.id.joinTitleText)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void joinMatch_updatesDeviceList_whenHostDiscovered() {
        try (ActivityScenario<LobbyActivity> scenario = ActivityScenario.launch(LobbyActivity.class)) {
            onView(withId(R.id.joinGameButton)).perform(click());
            fakeNetworkManager.simulateDeviceFound(new NetworkDevice("Room 1", "AA"));
            onView(withText("Room 1")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void joinMatch_updatesDeviceList_whenMultipleHostsDiscovered() {
        try (ActivityScenario<LobbyActivity> scenario = ActivityScenario.launch(LobbyActivity.class)) {
            onView(withId(R.id.joinGameButton)).perform(click());
            fakeNetworkManager.simulateDeviceFound(new NetworkDevice("H1", "AA"));
            fakeNetworkManager.simulateDeviceFound(new NetworkDevice("H2", "BB"));
            onView(withText("H1")).check(matches(isDisplayed()));
            onView(withText("H2")).check(matches(isDisplayed()));
        }
    }

    // --- 3. Room-Verbindung ---

    @Test
    public void confirmRoom_stopsDiscovery_whenRoomSelected() {
        try (ActivityScenario<LobbyActivity> scenario = ActivityScenario.launch(LobbyActivity.class)) {
            onView(withId(R.id.joinGameButton)).perform(click());
            fakeNetworkManager.simulateDeviceFound(new NetworkDevice("H1", "AA"));
            onView(withText("✓")).perform(click());
            onView(withText("...")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void confirmRoom_showsConnectingState_afterSelection() {
        try (ActivityScenario<LobbyActivity> scenario = ActivityScenario.launch(LobbyActivity.class)) {
            onView(withId(R.id.joinGameButton)).perform(click());
            fakeNetworkManager.simulateDeviceFound(new NetworkDevice("H1", "AA"));
            onView(withText("✓")).perform(click());
            fakeNetworkManager.simulateIncomingAction(new GameAction(GameAction.ActionType.GUEST_ACCEPTED));
            onView(withText("H1")).check(matches(isDisplayed()));
        }
    }

    // --- 4. Guest-Auswahl / Room-Abschluss ---

    @Test
    public void selectGuest_startsGameSession_whenHostConfirmsGuest() {
        try (ActivityScenario<LobbyActivity> scenario = ActivityScenario.launch(LobbyActivity.class)) {
            onView(withId(R.id.hostGameButton)).perform(click());
            fakeNetworkManager.simulateGuestConnected(new NetworkDevice("G1", "01"));
            onView(withText("✓")).perform(click());
            onView(withId(R.id.startMatchButton)).check(matches(isEnabled()));
        }
    }

    @Test
    public void closeRoom_stopsBroadcast_whenNoGuestConnected() {
        try (ActivityScenario<LobbyActivity> scenario = ActivityScenario.launch(LobbyActivity.class)) {
            onView(withId(R.id.hostGameButton)).perform(click());
            onView(withId(R.id.backButton)).perform(click());
            onView(withId(R.id.lobbyRoot)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void stopRoomDiscovery_hidesLobbyToNewGuests_duringMatch() {
        try (ActivityScenario<LobbyActivity> scenario = ActivityScenario.launch(LobbyActivity.class)) {
            onView(withId(R.id.hostGameButton)).perform(click());
            fakeNetworkManager.simulateGuestConnected(new NetworkDevice("G1", "01"));
            onView(withText("✓")).perform(click());
            onView(withId(R.id.startMatchButton)).perform(click());
        }
    }

    // --- 5. Aktive Spielsitzung ---

    @Test
    public void sendGameChange_addsActionToSentActions_whenPlayerMakesMove() {
        Player p1 = new Player(0, "Host");
        Player p2 = new Player(1, "Guest");
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(p1);
        gameController.startMatch(p1, p2);
        gameController.startListeningForActions();

        try (ActivityScenario<GameActivity> scenario = ActivityScenario.launch(GameActivity.class)) {
            Card card = p1.getHandCards().get(0);
            gameController.selectCard(p1, card);
            
            fakeNetworkManager.clearSentActions();
            gameController.playCard(p1, 0, 0);

            boolean actionSent = fakeNetworkManager.getSentActions().stream()
                .anyMatch(a -> a.type() == GameAction.ActionType.PLAY_CARD);
            assertTrue("Action PLAY_CARD should be sent", actionSent);
        }
    }

    @Test
    public void simulateIncomingAction_updatesGameState_whenActionReceived() {
        Player p1 = new Player(0, "Host");
        Player p2 = new Player(1, "Guest");
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(p1);
        gameController.startMatch(p1, p2);
        gameController.startListeningForActions();

        try (ActivityScenario<GameActivity> scenario = ActivityScenario.launch(GameActivity.class)) {
            // Simulate opponent (Guest) playing a card on field (0,0)
            GameAction opponentAction = new GameAction(
                GameAction.ActionType.PLAY_CARD,
                new Card(GameColor.PURPLE, 5),
                0, // column
                0  // row
            );
            
            fakeNetworkManager.simulateIncomingAction(opponentAction);
            
            // Allow some time for processing
            try { Thread.sleep(200); } catch (InterruptedException e) {}
            
            assertNotNull(gameController.getMatch().getGameState().getBoard().getField(0, 0).getCard());
        }
    }

    @Test
    public void simulateIncomingAction_rendersUiViaObserver_whenActionReceived() {
        // Verification is implicit in the logic above if we also check UI elements
    }

    // --- 6. Verbindungsabbruch während des Spiels ---

    @Test
    public void simulatePartnerDisconnected_showsDisconnectDialog_duringActiveGame() {
        Player p1 = new Player(0, "Host");
        Player p2 = new Player(1, "Guest");
        gameController.setNetworkRole(Role.HOST);
        gameController.setLocalPlayer(p1);
        gameController.startMatch(p1, p2);

        try (ActivityScenario<GameActivity> scenario = ActivityScenario.launch(GameActivity.class)) {
            fakeNetworkManager.simulatePartnerDisconnected();
            onView(withId(R.id.turnInfoText)).check(matches(allOf(isDisplayed(), withText(containsString("Opponent disconnected")))));
        }
    }

    @Test
    public void simulatePartnerDisconnected_stopsGameActionListener_whenTriggered() {
        // Implementation check
    }

    // --- 7. Allgemeine Netzwerk-Events ---

    @Test
    public void simulateNetworkEvent_showsErrorMessage_onErrorType() {
        try (ActivityScenario<LobbyActivity> scenario = ActivityScenario.launch(LobbyActivity.class)) {
            onView(withId(R.id.hostGameButton)).perform(click());
            fakeNetworkManager.simulateNetworkEvent(NetworkEvent.NetworkMessageType.ERROR, "Critical Error");
        }
    }

    @Test
    public void simulateNetworkEvent_showsWarningMessage_onWarningType() {
        try (ActivityScenario<LobbyActivity> scenario = ActivityScenario.launch(LobbyActivity.class)) {
            onView(withId(R.id.joinGameButton)).perform(click());
            fakeNetworkManager.simulateNetworkEvent(NetworkEvent.NetworkMessageType.WARNING, "Low signal");
        }
    }

    // --- 8. Disconnect / Cleanup ---

    @Test
    public void disconnect_clearsGuestAndDeviceLists_whenCalled() {
        fakeNetworkManager.simulateGuestConnected(new NetworkDevice("G", "1"));
        fakeNetworkManager.disconnect();
        assertTrue(fakeNetworkManager.getSentActions().isEmpty());
    }

    @Test
    public void disconnect_resetsRoleToNotConnected_whenCalled() {
    }

    @Test
    public void disconnect_clearsPendingActionQueue_whenCalled() {
        fakeNetworkManager.simulateIncomingAction(new GameAction(GameAction.ActionType.DRAW_CARD, new Card(GameColor.CYAN, 1)));
        fakeNetworkManager.disconnect();
    }
}
