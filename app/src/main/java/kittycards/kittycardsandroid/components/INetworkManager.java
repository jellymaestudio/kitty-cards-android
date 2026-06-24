package kittycards.kittycardsandroid.components;

import android.Manifest;

import androidx.annotation.RequiresPermission;

import kittycards.kittycardsandroid.network.GameAction;
import kittycards.kittycardsandroid.network.NetworkDevice;
import kittycards.kittycardsandroid.network.OnDeviceFoundListener;
import kittycards.kittycardsandroid.network.OnGuestConnectedListener;
import kittycards.kittycardsandroid.network.Role;
import kittycards.kittycardsandroid.network.event.NetworkEventListener;

/**
 * Handles all network-related tasks, particularly establishing the connection between Guest and the Host
 * <p>
 * Implementations of this interface are expected to be singletons.
 *
 * @author red_concrete
 */
public interface INetworkManager {

    //TODO dafür sorgen, dass empfangene Actions erst über fetchAction auslesbar sind, wenn wir eine Antwort vom senden haben.
    /**
     * To be called when the host wishes to open a Room for a new match.
     * Starts BLE advertising to make this device discoverable to potential guests.
     * <p>
     * Requires {@link android.Manifest.permission#BLUETOOTH_ADVERTISE} to be granted
     * before calling this method.
     *
     * @param listener The callback to be invoked when a guest successfully connects to this host.
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT})
    void hostMatch(OnGuestConnectedListener listener);

    /**
     * To be called when the guest wishes to join a match.
     * Scans for hosted matches advertised by other devices.
     * <p>
     * Requires {@link android.Manifest.permission#BLUETOOTH_SCAN} to be granted
     * before calling this method.
     *
     * @param listener The callback to be invoked when network devices offering a game are discovered.
     *                 The listener will be called multiple times as devices are found, for up to 10 seconds
     *                 after this method is called.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    void joinMatch(OnDeviceFoundListener listener);

    /**
     * Should be called when the guest wishes to confirm and connect to the selected player.
     * Establishes a connection between this device and the Hostmatch mobile phone.
     *
     * @param room the host device the player wants to join to
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    void confirmRoom(NetworkDevice room);

    /**
     * To be called when the host wishes to select a guest (that has asked to join the Room) he wants to play with.
     *
     * @param guest The guest device the host wishes to accept.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    void selectGuest(NetworkDevice guest);

    /**
     * Closes the active Network connection to the remote device.
     * Should be called when the match ends or a player disconnects.
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT})
    void disconnect();

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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

    /**
     * Sets the network event listener, which gets called upon a network event (e.g. a connect or disconnect notification or a bluetooth error.
     * Meant to be set by the UI.
     *
     * @param listener the listener that defines the behavior when a network event occurs
     */
    void setNetworkEventListener(NetworkEventListener listener);

    /**
     * Returns the current role of this device in the network connection (Host, Guest, or Not Connected).
     *
     * @return the role of this device in the network connection
     */
    Role getRole();

}
