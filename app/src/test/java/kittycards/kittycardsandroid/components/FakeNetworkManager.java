package kittycards.kittycardsandroid.components;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import kittycards.kittycardsandroid.network.GameAction;
import kittycards.kittycardsandroid.network.NetworkDevice;
import kittycards.kittycardsandroid.network.OnDeviceFoundListener;
import kittycards.kittycardsandroid.network.OnGameConnectionListener;
import kittycards.kittycardsandroid.network.OnGuestConnectedListener;
import kittycards.kittycardsandroid.network.OnRoomConnectionListener;
import kittycards.kittycardsandroid.network.Role;
import kittycards.kittycardsandroid.network.event.NetworkEvent;
import kittycards.kittycardsandroid.network.event.NetworkEventListener;

/**
 * A robust fake implementation of INetworkManager for testing purposes.
 * It allows simulating network events and verifying outgoing actions.
 */
public class FakeNetworkManager implements INetworkManager {

    private final LinkedBlockingQueue<GameAction> actionQueue = new LinkedBlockingQueue<>();
    private final List<GameAction> sentActions = new ArrayList<>();

    private Role role = Role.NOT_CONNECTED;
    private NetworkEventListener eventListener;
    private OnRoomConnectionListener roomConnectionListener;
    private OnGameConnectionListener gameConnectionListener;
    private OnDeviceFoundListener deviceFoundListener;
    private OnGuestConnectedListener guestConnectedListener;

    private final ArrayList<NetworkDevice> connectedGuests = new ArrayList<>();
    private final ArrayList<NetworkDevice> discoveredDevices = new ArrayList<>();

    // --- Simulation API (Methods to control the fake from tests) ---

    public void simulateIncomingAction(GameAction action) {
        actionQueue.add(action);
    }

    public void simulateDeviceFound(NetworkDevice device) {
        discoveredDevices.add(device);
        if (deviceFoundListener != null) {
            deviceFoundListener.onDeviceFound(new ArrayList<>(discoveredDevices));
        }
    }

    public void simulateGuestConnected(NetworkDevice guest) {
        connectedGuests.add(guest);
        if (guestConnectedListener != null) {
            guestConnectedListener.onGuestListUpdated(new ArrayList<>(connectedGuests));
        }
    }

    public void simulatePartnerDisconnected() {
        if (gameConnectionListener != null) {
            gameConnectionListener.onGamePartnerDisconnected();
        }
    }

    public void simulateNetworkEvent(NetworkEvent.NetworkMessageType type, String message) {
        if (eventListener != null) {
            eventListener.onNetworkEvent(new NetworkEvent(type, message, "FakeNetworkManager"));
        }
    }

    public List<GameAction> getSentActions() {
        return new ArrayList<>(sentActions);
    }

    // --- INetworkManager Implementation ---

    @Override
    public void hostMatch(OnGuestConnectedListener listener) {
        this.role = Role.HOST;
        this.guestConnectedListener = listener;
    }

    @Override
    public void joinMatch(OnDeviceFoundListener listener) {
        this.role = Role.GUEST;
        this.deviceFoundListener = listener;
    }

    @Override
    public void confirmRoom(NetworkDevice room) {
        // Simulation: could trigger a connection success event
    }

    @Override
    public void selectGuest(NetworkDevice guest) {
        // Host selects guest
    }

    @Override
    public void disconnect() {
        this.role = Role.NOT_CONNECTED;
        actionQueue.clear();
        connectedGuests.clear();
        discoveredDevices.clear();
    }

    @Override
    public void sendGameChange(GameAction action) {
        sentActions.add(action);
    }

    @Override
    public GameAction fetchNextAction() throws InterruptedException {
        return actionQueue.take(); // Correctly blocks like the real implementation
    }

    @Override
    public void setNetworkEventListener(NetworkEventListener listener) {
        this.eventListener = listener;
    }

    @Override
    public void setRoomConnectionListener(OnRoomConnectionListener listener) {
        this.roomConnectionListener = listener;
    }

    @Override
    public void setGameConnectionListener(OnGameConnectionListener listener) {
        this.gameConnectionListener = listener;
    }

    @Override
    public void closeRoom() {
        this.role = Role.NOT_CONNECTED;
    }

    @Override
    public void stopRoomDiscovery() {

    }
}
