package kittycats.kittycatsandroid.network;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import kittycats.kittycatsandroid.components.INetworkManager;
import kittycats.kittycatsandroid.components.IProtocolEngine;

/**
 * calls the listener methods
 *
 * @author red_concrete
 */
public class NetworkManager implements INetworkManager {

    private static NetworkManager INSTANCE;

    private final IProtocolEngine protocolEngine;
    private final LinkedBlockingQueue<GameAction> actionQueue = new LinkedBlockingQueue<>();


    private NetworkManager() {
        protocolEngine = new ProtocolEngine();
    }

    //BLE background thread calls this, when data arrives:
    private void onBytesReceived(byte[] bytes) throws InterruptedException {
        GameAction action = protocolEngine.decode(bytes);
        actionQueue.put(action);
    }

    @Override
    public ArrayList<NetworkDevice> hostMatch() {
        //TODO
        return null;
    }

    @Override
    public ArrayList<NetworkDevice> joinMatch() {
        //TODO
        return null;
    }

    @Override
    public void confirmRoom(NetworkDevice room) {
        //TODO
    }

    @Override
    public void selectGuest(NetworkDevice guest) {
        //TODO
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void sendGameChange(GameAction action) {

    }

    @Override
    public GameAction fetchNextAction() throws InterruptedException {
        return actionQueue.take(); //Blocks the calling thread until something is in the queue
    }


    public static NetworkManager getInstance() {
        if (INSTANCE == null) INSTANCE = new NetworkManager();
        return INSTANCE;
    }
}
