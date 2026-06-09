package kittycards.kittycardsandroid.components;

import java.util.ArrayList;

import kittycards.kittycardsandroid.network.GameAction;
import kittycards.kittycardsandroid.network.NetworkDevice;

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
     * Starts BLE advertising to make this device discoverable to potential guests.
     * <p>
     * Requires {@link android.Manifest.permission#BLUETOOTH_ADVERTISE} to be granted
     * before calling this method.
     *
     * @return a list of network devices that are ready to join the match; will be updated for 10 seconds after being called.
     */
    ArrayList<NetworkDevice> hostMatch();

    /**
     * To be called when the guest wishes to join a match.
     * Scans for hosted matches advertised by other devices.
     * <p>
     * Requires {@link android.Manifest.permission#BLUETOOTH_SCAN} to be granted
     * before calling this method.
     *
     * @return a list of network devices offering a game to join; will be updated for 10 seconds after being called.
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
