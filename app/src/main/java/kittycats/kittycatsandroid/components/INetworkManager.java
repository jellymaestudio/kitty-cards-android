package kittycats.kittycatsandroid.components;

public interface INetworkManager {
    void hostMatch();

    void selectPlayer();

    void startMatch();

    void joinMatch();

    void selectRoom();

    /**
     * Closes the active Network connection to the remote device.
     * Should be called when the match ends or a player disconnects.
     */
    void disconnect();
}
