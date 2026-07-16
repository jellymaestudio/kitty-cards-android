package kittycards.kittycardsandroid.components;

import android.Manifest;

import androidx.annotation.RequiresPermission;

import kittycards.kittycardsandroid.network.GameAction;
import kittycards.kittycardsandroid.network.NetworkDevice;
import kittycards.kittycardsandroid.network.OnDeviceFoundListener;
import kittycards.kittycardsandroid.network.OnGameConnectionListener;
import kittycards.kittycardsandroid.network.OnGuestConnectedListener;
import kittycards.kittycardsandroid.network.OnRoomConnectionListener;
import kittycards.kittycardsandroid.network.event.NetworkEventListener;

/**
 * Handles all network-related tasks, particularly establishing the connection between Guest and the Host
 * <p>
 * Implementations of this interface are expected to be singletons.
 *
 * @author red_concrete
 */
public interface INetworkManager {

    /**
     * Initializes the device as a Host and starts broadcasting the presence of a new match room.
     * <p>
     * This method transitions the network component into the {@code HOST} role. If the device
     * is currently acting as a {@code GUEST} or has an active connection, it will be
     * disconnected before the host setup begins.
     * <p>
     * The initialization process is asynchronous. Success or failure (e.g., due to missing
     * hardware capabilities) will be reported via the {@link NetworkEventListener}.
     * <p>
     * If the component is already in {@code HOST} mode or initializing, subsequent calls
     * are ignored and a warning is emitted via the {@link NetworkEventListener}.
     *
     * @param listener callback invoked when the list of available guests in the lobby changes
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT})
    void hostMatch(OnGuestConnectedListener listener);

    /**
     * Transitions the device to Guest mode and begins searching for available match rooms.
     * <p>
     * This method sets the local role to {@code GUEST}. If the device is currently
     * acting as a {@code HOST}, any active server or broadcasting will be shut down
     * before discovery begins.
     * <p>
     * The discovery process identifies compatible remote devices and reports them
     * through the provided {@link OnDeviceFoundListener}. Results are delivered
     * asynchronously as devices are detected in the environment.
     * <p>
     * Implementations should verify hardware support and report initialization
     * failures via the {@link NetworkEventListener}.
     *
     * @param listener callback that receives updates about discovered rooms/hosts
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    void joinMatch(OnDeviceFoundListener listener);

    /**
     * Selects a specific host room and initiates the connection process.
     * <p>
     * This method is intended to be called by a Guest after finding a suitable room
     * during the discovery phase (see {@link #joinMatch(OnDeviceFoundListener)}).
     * The {@code room} parameter must be one of the devices previously reported
     * by the {@link OnDeviceFoundListener}.
     * <p>
     * Calling this method will automatically stop the active discovery process.
     * If there is already an active connection or an ongoing connection attempt,
     * it will be terminated before the new connection attempt begins.
     * <p>
     * Connection progress (success, failure, or timeout) is reported via the
     * {@link OnRoomConnectionListener} and the {@link NetworkEventListener}.
     *
     * @param room the remote device representing the room to connect to
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    void confirmRoom(NetworkDevice room);

    /**
     * Confirms a specific guest as the active game partner and closes the lobby.
     * <p>
     * This method is called by the Host to finalize a connection with one of the
     * connected guests. Once a guest is selected, the communication becomes
     * exclusive to that device.
     * <p>
     * Implementation should ensure that all other guests currently in the lobby are
     * disconnected to maintain a stable point-to-point connection for the match.
     * <p>
     * A protocol-level confirmation is sent to the selected guest, and subsequent
     * game actions will only be exchanged with this device.
     *
     * @param guest the remote device to accept as the primary game partner
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    void selectGuest(NetworkDevice guest);

    /**
     * Terminates any active network session and releases all associated resources.
     * <p>
     * This method resets the network component to an idle state ({@code NOT_CONNECTED}).
     * It handles the cleanup for both Host and Guest roles, including:
     * <ul>
     *     <li>Closing all active transport connections.</li>
     *     <li>Stopping ongoing discovery or broadcasting.</li>
     *     <li>Clearing internal message queues to ensure a clean state for future sessions.</li>
     * </ul>
     * <p>
     * This should be called whenever a match ends, a player leaves, or the application
     * is paused/stopped to ensure proper resource management.
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE})
    void disconnect();

    /**
     * Gracefully shuts down the match room from the Host's perspective.
     * <p>
     * This method is only valid when the device is in {@code HOST} mode. It initiates
     * a protocol-level shutdown by notifying the connected partner (if one exists)
     * that the room is being closed.
     * <p>
     * Unlike a hard {@link #disconnect()}, this method attempts to ensure that the
     * shutdown signal is successfully transmitted before the physical connection
     * is severed. If no partner is connected, it performs an immediate disconnect.
     */
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
    })
    void closeRoom();

    /**
     * Stops broadcasting the presence of the match room while maintaining active connections.
     * <p>
     * This method is used by the Host to make the room private (e.g., once the match
     * starts). It stops the network discovery signals so that no new
     * potential guests can find or attempt to connect to the room.
     * <p>
     * Active connections and the underlying communication server remain operational
     * to allow for continued gameplay.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    void stopRoomDiscovery();

    /**
     * Transmits a game-related action to the connected remote peer.
     * <p>
     * This is the primary method for synchronizing game state between Host and Guest.
     * The action is queued for transmission and sent asynchronously.
     *
     * @param action the game action to be sent
     * @throws IllegalStateException if the component is currently in the
     *                               {@code NOT_CONNECTED} state
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    void sendGameChange(GameAction action);

    /**
     * Retrieves the next received game action from the incoming message queue.
     * <p>
     * This method follows a blocking consumer pattern. If the queue is empty, the
     * calling thread will be suspended until a new action is received from the
     * remote device and successfully decoded.
     * <p>
     * It is designed to be used within a dedicated game loop or worker thread to
     * process incoming network traffic in a synchronized, sequential manner.
     *
     * @return the next {@link GameAction} in the FIFO queue
     * @throws InterruptedException if the thread is interrupted while waiting for
     *                              an action to arrive
     */
    GameAction fetchNextAction() throws InterruptedException;

    /**
     * Registers a listener to receive diagnostic events and status updates.
     * <p>
     * The listener is notified of high-level network occurrences, such as successful
     * initialization, connection errors, hardware warnings, and diagnostic logs.
     * Only one global event listener is supported; calling this replaces any
     * previously registered listener.
     *
     * @param listener the listener to receive network events
     */
    void setNetworkEventListener(NetworkEventListener listener);

    /**
     * Registers a listener for events related to room entry and connection attempts.
     * <p>
     * This listener is primarily used by a Guest to monitor the progress of a
     * connection to a Host's room after calling {@link #confirmRoom(NetworkDevice)}.
     * It provides callbacks for successful connection, failed attempts, and
     * service discovery status.
     *
     * @param listener the listener for room-level connection events
     */
    void setRoomConnectionListener(OnRoomConnectionListener listener);

    /**
     * Registers a listener for events during an active game session.
     * <p>
     * This listener focuses on the state of the established point-to-point connection
     * during the match. It notifies the application if the game partner disconnects
     * or if the connection quality changes, allowing the game logic to react
     * (e.g., by ending the match).
     *
     * @param listener the listener for active game session events
     */
    void setGameConnectionListener(OnGameConnectionListener listener);

}
