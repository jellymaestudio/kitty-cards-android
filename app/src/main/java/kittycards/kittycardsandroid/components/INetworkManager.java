package kittycards.kittycardsandroid.components;

import android.Manifest;

import androidx.annotation.RequiresPermission;

import kittycards.kittycardsandroid.network.GameAction;
import kittycards.kittycardsandroid.network.NetworkDevice;
import kittycards.kittycardsandroid.network.OnDeviceFoundListener;
import kittycards.kittycardsandroid.network.OnGameConnectionListener;
import kittycards.kittycardsandroid.network.OnGuestConnectedListener;
import kittycards.kittycardsandroid.network.OnRoomConnectionListener;
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
    //TODO wenn der Client mehrmals bestätigt/subscribed, bekommt er Nachrichten vom host mehrmals
    //TODO Empfangsbestätigung für nachrichten ausgeben
    //TODO Host Guest setup, Host disconnected, guest sendet, fehler ist write failed mit status 1, soll das so?
    //TODO Host guest setup, Host hat aber guest noch NICHT ausgewählt.
    //  Host disconnected und startet neu -> gast muss nicht neu suchen/bestätigen -> kann direkt annehmen
    /**
     * Opens a discoverable room for a new match.
     *
     * @param listener callback invoked when available guests change
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT})
    void hostMatch(OnGuestConnectedListener listener);

    /**
     * Starts discovering available hosted rooms.
     *
     * @param listener callback receiving the currently available rooms
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

    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
    })
    void closeRoom();

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    void stopRoomDiscovery();

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    void sendGameChange(GameAction action);

    /**
     * Retrieves the next received GameAction from the internal queue.
     * This method blocks until an action becomes available.
     * <p>
     * Intended for game loop consumption.
     *
     * @return next GameAction received from the remote device
     * @throws InterruptedException if thread is interrupted while waiting
     */

    GameAction fetchNextAction() throws InterruptedException;

    /**
     * Registers a listener for network events.
     * Only one listener is active at a time; replacing the previous one.
     *
     * @param listener listener receiving NetworkEvent updates on main thread
     */
    void setNetworkEventListener(NetworkEventListener listener);

    void setRoomConnectionListener(OnRoomConnectionListener listener);

    void setGameConnectionListener(OnGameConnectionListener listener);

    /**
     * Returns the current role of this device in the network connection (Host, Guest, or Not Connected).
     *
     * @return the role of this device in the network connection
     */
    Role getRole();

}
