package kittycats.kittycatsandroid.components;

import kittycats.kittycatsandroid.network.NetworkDevice;

/**
 * Handles all network-related tasks, particularly establishing the connection between Guest and the Host
 * <p>
 * All methods trigger asynchronous operations. Results and state changes
 * are communicated back via the registered {@link INetworkListener}.
 * <p>
 * Implementations of this interface are expected to be singletons.
 */
public interface INetworkManager {
    /**
     * Registers a listener to receive asynchronous network events.
     * Should be called before any other method.
     *
     * @param listener The listener to be notified of network events.
     */
    void setListener(INetworkListener listener);

    /**
     * To be called when the host wishes to open a Room for a new match.
     * Handles Network permissions and the advertisement to other mobile phones
     * <p>
     * Results are reported via {@link INetworkListener#onDeviceFound(NetworkDevice)}.
     */
    void hostMatch();

    /**
     * To be called when the guest wishes to join a match.
     * Handles Network permissions and searches for hosted matches advertised by other phones
     * <p>
     * Discovered rooms are reported via {@link INetworkListener#onDeviceFound(NetworkDevice)}.
     */
    void joinMatch();

    /**
     * To be called when the guest wishes to select an advertised match he wants to join.
     * <p>
     * A successful connection is reported via {@link INetworkListener#onConnected(NetworkDevice)}.
     */
    void selectRoom(NetworkDevice room);

    /**
     * Should be called to confirm that the guest wishes to start the match with the selected player.
     * Establishes a connection between this device and the Hostmatch mobile phone.
     * <p>
     * Requires a prior call to {@link #selectRoom(NetworkDevice)}.
     */
    void confirmRoom();

    /**
     * To be called when the host wishes to select a guest (that has asked to join the Room) he wants to play with.
     *
     * @param guest The guest device the host wishes to accept.
     */
    void selectGuest(NetworkDevice guest);

    /**
     * Once a match has been hosted, another device has joined and successfully connected,
     * this method should be called when the host wishes to confirm that they want to start the game with the connected player.
     * Initiates the creation of the match board.
     * <p>
     * Requires an active connection established via {@link INetworkListener#onConnected(NetworkDevice)}.
     */
    void startMatch();

    void joinMatch();


    void getInputStream();

    void getOutputStream();
}
