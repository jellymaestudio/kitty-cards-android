package kittycards.kittycardsandroid.components;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import kittycards.kittycardsandroid.network.GameAction;
import kittycards.kittycardsandroid.network.NetworkDevice;
import kittycards.kittycardsandroid.network.OnDeviceFoundListener;
import kittycards.kittycardsandroid.network.OnGameConnectionListener;
import kittycards.kittycardsandroid.network.OnGuestConnectedListener;
import kittycards.kittycardsandroid.network.OnRoomConnectionListener;
import kittycards.kittycardsandroid.network.Role;
import kittycards.kittycardsandroid.network.event.NetworkEventListener;

/*only for testing*/
public class FakeNetworkManager implements INetworkManager {

    private final ArrayList<NetworkDevice> discoveredDevices = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private CountDownLatch latch;

    public void expectDevices(int count) {
        latch = new CountDownLatch(count);
    }

    public void simulateDeviceFoundAfter(NetworkDevice device, long delayMs) {
        scheduler.schedule(() -> {
            discoveredDevices.add(device);
            if (latch != null) latch.countDown();
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    public boolean awaitDiscovery(long timeout, TimeUnit unit) throws InterruptedException {
        return latch != null && latch.await(timeout, unit);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }


    @Override
    public void hostMatch(OnGuestConnectedListener listener) {

    }

    @Override
    public void joinMatch(OnDeviceFoundListener listener) {
        listener.onDeviceFound(discoveredDevices);
    }

    @Override
    public void confirmRoom(NetworkDevice room) {

    }

    @Override
    public void selectGuest(NetworkDevice guest) {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void sendGameChange(GameAction action) {

    }

    @Override
    public GameAction fetchNextAction() throws InterruptedException {
        return null;
    }

    @Override
    public void setNetworkEventListener(NetworkEventListener listener) {

    }

    @Override
    public void setRoomConnectionListener(OnRoomConnectionListener listener) {

    }

    @Override
    public void setGameConnectionListener(OnGameConnectionListener listener) {

    }

    @Override
    public void closeRoom() {

    }

    @Override
    public void stopRoomDiscovery() {

    }

    @Override
    public Role getRole() {
        return null;
    }
}
