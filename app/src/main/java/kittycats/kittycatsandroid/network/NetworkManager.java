package kittycats.kittycatsandroid.network;

import java.util.ArrayList;

import kittycats.kittycatsandroid.components.INetworkManager;

/**
 * calls the listener methods
 *
 * @author red_concrete
 */
public class NetworkManager implements INetworkManager {

    private static NetworkManager INSTANCE;


    private NetworkManager() {}

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


    public static NetworkManager getInstance() {
        if (INSTANCE == null) INSTANCE = new NetworkManager();
        return INSTANCE;
    }
}
