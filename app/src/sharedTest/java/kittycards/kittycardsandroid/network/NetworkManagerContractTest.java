package kittycards.kittycardsandroid.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import kittycards.kittycardsandroid.components.INetworkManager;

/**
 * Abstract contract test for INetworkManager.
 * Any implementation of INetworkManager must pass these tests.
 */
public abstract class NetworkManagerContractTest {

    protected INetworkManager networkManager;

    /**
     * Factory method to be implemented by concrete test classes.
     * @return an instance of the INetworkManager to be tested.
     */
    protected abstract INetworkManager createManager();

    @Before
    public void setUp() {
        networkManager = createManager();
    }

    @After
    public void tearDown() {
        if (networkManager != null) {
            networkManager.disconnect();
        }
    }

    @Test
    public void hostMatch_transitionsToHostRole() {
        networkManager.hostMatch(guests -> {});
        assertEquals(Role.HOST, networkManager.getRole());
    }

    @Test
    public void joinMatch_transitionsToGuestRole() {
        networkManager.joinMatch(rooms -> {});
        assertEquals(Role.GUEST, networkManager.getRole());
    }

    @Test
    public void disconnect_resetsRoleToNotConnected() {
        networkManager.hostMatch(guests -> {});
        networkManager.disconnect();
        assertEquals(Role.NOT_CONNECTED, networkManager.getRole());
    }

    @Test
    public void sendGameChange_throwsIllegalStateException_whenNotConnected() {
        GameAction action = new GameAction(GameAction.ActionType.UNSELECT_CARD);
        assertThrows(IllegalStateException.class, () -> networkManager.sendGameChange(action));
    }

    @Test
    public void closeRoom_resetsRoleToNotConnected() {
        networkManager.hostMatch(guests -> {});
        networkManager.closeRoom();
        assertEquals(Role.NOT_CONNECTED, networkManager.getRole());
    }

    @Test
    public void stopRoomDiscovery_isIgnored_whenNotHost() {
        networkManager.joinMatch(rooms -> {});
        networkManager.stopRoomDiscovery();
        assertEquals(Role.GUEST, networkManager.getRole());
    }
}
