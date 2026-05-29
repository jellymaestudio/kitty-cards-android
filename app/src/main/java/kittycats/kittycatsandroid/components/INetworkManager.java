package kittycats.kittycatsandroid.components;

import java.util.ArrayList;

import kittycats.kittycatsandroid.network.GameAction;
import kittycats.kittycatsandroid.network.NetworkDevice;

/**
 * Handles all network-related tasks, particularly establishing the connection between Guest and the Host
 * <p>
 * Implementations of this interface are expected to be singletons.
 *
 * @author red_concrete
 */
public interface INetworkManager {

    /**
     * To be called when the host wishes to open a Room for a new match.
     * Handles Network permissions and the advertisement to other mobile phones
     *
     * @return a continuously updating list of found NetworkDevices willing to join the match
     */
    ArrayList<NetworkDevice> hostMatch();

    /**
     * To be called when the guest wishes to join a match.
     * Handles Network permissions and searches for hosted matches advertised by other phones
     *
     * @return a continuously updating list of found NetworkDevices offering a game to join
     */
    ArrayList<NetworkDevice> joinMatch();

    /**
     * Should be called when the guest wishes to confirm and connect to the selected player.
     * Establishes a connection between this device and the Hostmatch mobile phone.
     *
     * @param room the host device the player wants to join to
     */
    void confirmRoom(NetworkDevice room);

    /**
     * To be called when the host wishes to select a guest (that has asked to join the Room) he wants to play with.
     *
     * @param guest The guest device the host wishes to accept.
     */
    void selectGuest(NetworkDevice guest);

    /**
     * Closes the active Network connection to the remote device.
     * Should be called when the match ends or a player disconnects.
     */
    void disconnect();

    /**
     * Sends raw data to the connected remote device.
     *
     * @param action The raw byte array to be transmitted.
     */
    void sendGameChange(GameAction action);

    /**
     * Retrieves the next game action received from the remote device.
     * Pauses until an action becomes available.
     *
     * @return the retrieved GameAction object
     * @throws InterruptedException If the blocked thread is interrupted from outside
     *                              (e.g. due to the app being closed or a loss of connection),
     *                              in order to end the wait prematurely.
     */
    GameAction fetchNextAction() throws InterruptedException;

}
