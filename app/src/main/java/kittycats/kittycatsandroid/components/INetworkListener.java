package kittycats.kittycatsandroid.components;

import kittycats.kittycatsandroid.network.NetworkDevice;

/**
 * Listener interface for asynchronous network events.
 */
public interface INetworkListener {

    /**
     * Called when a new device advertising a match has been discovered.
     * Triggered during a guest's scan after calling {@link INetworkManager#joinMatch()}.
     * or when a guest requests to join the host's room after {@link INetworkManager#hostMatch()}.
     *
     * @param device The discovered device.
     */
    void onDeviceFound(NetworkDevice device);

    /**
     * Called when a connection between host and guest has been established.
     * Triggered on both sides once the connection process is complete.
     *
     * @param device The device this client has connected to.
     */
    void onConnected(NetworkDevice device);

    /**
     * Called when the connection to the remote device has been lost or intentionally closed.
     */
    void onDisconnected();

    /**
     * Called when raw data has been received from the remote device.
     * The received data should be forwarded to the {@link IProtocolEngine} for further processing.
     *
     * @param data The raw byte array received over the network.
     */
    void onDataReceived(byte[] data);
}