package kittycards.kittycardsandroid.network;

/**
 * Notifies the UI about changes to the guest's room connection.
 *
 * @author JellyMae
 */
public interface OnRoomConnectionListener {

    /**
     * Called when the guest successfully connects to a host.
     *
     * @param room the connected host room
     */
    void onRoomConnected(NetworkDevice room);

    /**
     * Called when the connection to the host is closed or lost.
     */
    void onRoomDisconnected();
}