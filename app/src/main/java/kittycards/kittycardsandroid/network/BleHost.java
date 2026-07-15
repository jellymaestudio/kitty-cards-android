package kittycards.kittycardsandroid.network;

import static kittycards.kittycardsandroid.network.event.NetworkEvent.NetworkMessageType.ERROR;
import static kittycards.kittycardsandroid.network.event.NetworkEvent.NetworkMessageType.INFO;
import static kittycards.kittycardsandroid.network.event.NetworkEvent.NetworkMessageType.WARNING;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import kittycards.kittycardsandroid.network.event.NetworkEvent;

/**
 * Acts as the BLE Peripheral/Server in the Bluetooth Low Energy communication, meaning it
 * advertises itself as a host and accepts connections from guests.
 *
 * @author red_concrete
 */
public class BleHost {

    private final NetworkManager networkManager;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;

    private BluetoothGattServer bluetoothGattServer;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothDevice selectedGuestDevice;
    private BluetoothGattCharacteristic serverCharacteristic;

    private final ArrayList<NetworkDevice> connectedGuests = new ArrayList<>();
    private OnGuestConnectedListener guestListener;

    private final Queue<byte[]> outgoingQueue = new LinkedList<>();
    private boolean notificationInProgress = false;
    private boolean disconnectAfterQueueIsEmpty = false;

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {

        /**
         * Called by the Android BLE advertising subsystem after advertising has been
         * started successfully.
         *
         * <p>This callback indicates that the host is now discoverable by nearby BLE
         * guest devices and can accept incoming connection attempts.</p>
         *
         * <p>No additional setup is performed here because the GATT server has already
         * been initialized before advertising started.</p>
         *
         * <p>Typical test cases:</p>
         * <ul>
         *     <li>Verify that hosting remains active after the callback.</li>
         * </ul>
         * @param settingsInEffect The advertising settings that were applied by the
         *                         Android BLE stack.
         */
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            emitEvent(INFO, "Advertising started successfully. Host is discoverable.");
        }

        /**
         * Called by the Android BLE advertising subsystem when advertising could not
         * be started or was rejected by the operating system.
         *
         * <p>The host enters a shutdown sequence because advertising is required for
         * guests to discover and connect to this device.</p>
         *
         * <p>The actual cleanup is delegated to {@link BleHost#disconnect()} and
         * executed on the network thread.</p>
         *
         * <p>Typical causes include unsupported hardware, resource exhaustion,
         * oversized advertising payloads or Bluetooth stack failures.</p>
         * <p>Typical test cases:</p>
         *
         * <ul>
         *     <li>Verify that disconnect() is scheduled.</li>
         *     <li>Verify that advertising resources are released.</li>
         * </ul>
         *
         * @param errorCode Android advertising error code reported by the BLE stack.
         */
        @Override
        public void onStartFailure(int errorCode) {
            emitEvent(ERROR, "Advertising failed or timed out: " + advertiseErrorText(errorCode));
            networkManager.handler.post(BleHost.this::disconnect);
        }
    };

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        /**
         * Called by the Android BLE stack whenever a guest device connects to or
         * disconnects from this host.
         *
         * <p>This callback is the primary source of truth for maintaining the lobby's
         * guest list.</p>
         *
         * <p>When a device connects:</p>
         * <ul>
         *     <li>The guest is added to {@code connectedGuests}.</li>
         *     <li>The UI listener is notified.</li>
         *     <li>An INFO event is emitted.</li>
         * </ul>
         *
         * <p>When a device disconnects:</p>
         * <ul>
         *     <li>The guest is removed from {@code connectedGuests}.</li>
         *     <li>The UI listener is notified.</li>
         *     <li>If the guest was the currently selected game partner, the active
         *     connection state and outgoing message queue are reset.</li>
         * </ul>
         *
         * <p>This callback is triggered by the Android Bluetooth framework and must
         * not be called directly by application code.</p>
         *
         * <p>Typical test cases:</p>
         * <ul>
         *     <li>Connected guest is added exactly once.</li>
         *     <li>Disconnected guest is removed.</li>
         *     <li>Listener receives updated guest lists.</li>
         *     <li>Disconnecting the selected guest clears active session state.</li>
         * </ul>
         *
         * @param device The BLE device whose connection state changed.
         * @param status Status code reported by Android.
         * @param newState The new Bluetooth connection state.
         */
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            NetworkDevice networkDevice = NetworkDevice.from(device);

            networkManager.handler.post(() -> {
                if (newState == BluetoothProfile.STATE_CONNECTED) {

                    if (!connectedGuests.contains(networkDevice)) {
                        connectedGuests.add(networkDevice);

                        emitEvent(INFO, "Guest connected: " + device.getAddress());
                        // Notify the UI thread if a listener is present
                        if (guestListener != null) {
                            guestListener.onGuestListUpdated(new ArrayList<>(connectedGuests));
                        }
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // Guest lost or closed the connection -> remove from the list
                    if (connectedGuests.remove(networkDevice)) {

                        emitEvent(WARNING, "Guest disconnected: " + device.getAddress());
                        // Inform UI: A guest left the lobby
                        if (guestListener != null) {
                            guestListener.onGuestListUpdated(new ArrayList<>(connectedGuests));
                        }
                    }
                    if (
                            selectedGuestDevice != null
                                    && selectedGuestDevice
                                    .getAddress()
                                    .equals(device.getAddress())
                    ) {
                        emitEvent(
                                WARNING,
                                "Active game partner disconnected: "
                                        + device.getAddress()
                        );

                        selectedGuestDevice = null;
                        outgoingQueue.clear();
                        notificationInProgress = false;
                        disconnectAfterQueueIsEmpty = false;

                        networkManager.notifyGamePartnerDisconnected();
                    }
                }
            });
        }

        /**
         * Called by the Android BLE stack when a guest writes data to the host's
         * game characteristic.
         *
         * <p>This is the host-side entry point for all incoming game messages.
         * It is the counterpart to the guest's
         * {@code BluetoothGattCallback.onCharacteristicChanged(...)} flow.</p>
         *
         * <p>Only the currently selected guest is allowed to submit game actions.
         * Messages from other connected guests are rejected.</p>
         *
         * <p>Valid payloads are forwarded to the {@link NetworkManager} for decoding
         * and later processing by the game layer.</p>
         *
         * <p>Typical test cases:</p>
         * <ul>
         *     <li>Selected guest messages are accepted.</li>
         *     <li>Unselected guest messages are rejected.</li>
         *     <li>Successful writes trigger decodeAndQueueDataSafe().</li>
         *     <li>Response packets are returned when requested.</li>
         * </ul>
         *
         * @param device Guest device that submitted the write request.
         * @param requestId Android request identifier used for write responses.
         * @param characteristic Target characteristic being written.
         * @param preparedWrite Whether this write is part of a prepared write sequence.
         * @param responseNeeded Whether the client expects a write response.
         * @param offset Offset inside the characteristic value.
         * @param value Raw encoded game payload.
         */
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (bluetoothGattServer == null) {
                return;
            }
            if (NetworkManager.KITTY_CARDS_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {

                if (selectedGuestDevice == null || !selectedGuestDevice.getAddress().equals(device.getAddress())) {
                    emitEvent(WARNING, "Ignored write request from unselected guest: " + device.getAddress());

                    if (responseNeeded) {
                        bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_WRITE_NOT_PERMITTED, offset, null);
                    }
                    return;
                }
                if (value == null) {
                    emitEvent(ERROR, "Received empty write payload");
                    return;
                }

                networkManager.decodeAndQueueDataSafe(value);

                if (responseNeeded) {
                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            }
        }

        /**
         * Called when a guest writes to one of the host's GATT descriptors.
         *
         * <p>In practice this is primarily used for BLE notification subscription
         * management through the Client Characteristic Configuration Descriptor
         * (CCCD).</p>
         *
         * <p>The implementation currently acknowledges all descriptor writes without
         * performing additional validation.</p>
         *
         * <p>Typical test cases:</p>
         * <ul>
         *     <li>Response is sent when requested.</li>
         *     <li>No exception occurs for valid CCCD writes.</li>
         * </ul>
         *
         * @param device Device performing the descriptor write.
         * @param requestId Android request identifier.
         * @param descriptor Descriptor being modified.
         * @param preparedWrite Whether the operation is a prepared write.
         * @param responseNeeded Whether a response is expected.
         * @param offset Write offset.
         * @param value New descriptor value.
         */
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {


            if (responseNeeded && bluetoothGattServer != null) {
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
        }

        /**
         * Called by the Android BLE stack after a notification transmission attempt
         * has completed.
         *
         * <p>This callback drives the host's outgoing transmission queue. Once a
         * notification finishes, the next queued game message can be sent.</p>
         *
         * <p>The callback therefore acts as a flow-control mechanism preventing
         * multiple BLE notifications from being transmitted concurrently.</p>
         *
         * <p>Typical test cases:</p>
         * <ul>
         *     <li>notificationInProgress is cleared.</li>
         *     <li>The next queued notification is processed.</li>
         * </ul>
         *
         * @param device Target guest device.
         * @param status BLE transmission result status.
         */
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                emitEvent(ERROR, "Notification failed: " + status);
            }
            networkManager.handler.post(() -> {
                notificationInProgress = false;

                /*
                 * The transmitted message was the final queued message.
                 * ROOM_CLOSED has therefore been sent and the connection can
                 * now be closed safely.
                 */
                if (
                        disconnectAfterQueueIsEmpty
                                && outgoingQueue.isEmpty()
                ) {
                    disconnectAfterQueueIsEmpty = false;
                    disconnect();
                    return;
                }

                processNextNotification();
            });
        }
    };

    public BleHost(NetworkManager networkManager, Context context, BluetoothManager bluetoothManager) {
        this.networkManager = networkManager;
        this.context = context;
        this.bluetoothManager = bluetoothManager;
        this.bluetoothAdapter = bluetoothManager.getAdapter();
    }

    /**
     * Starts BLE host mode and begins accepting guest connections.
     *
     * <p>This method is typically invoked by the UI when a player creates a new
     * match room</p>
     *
     * <p>The method performs capability checks, stores the lobby listener and
     * schedules initialization of both the GATT server and BLE advertising.</p>
     *
     * <p>If hosting is already active the request is ignored.</p>
     *
     * <p>Successful execution eventually leads to:</p>
     * <ol>
     *     <li>GATT server creation.</li>
     *     <li>BLE advertising startup.</li>
     *     <li>Guest discovery and connection attempts.</li>
     * </ol>
     *
     * <p>Typical test cases:</p>
     * <ul>
     *     <li>Hosting starts on supported devices.</li>
     *     <li>Duplicate start requests are ignored.</li>
     *     <li>Missing advertiser support emits an error.</li>
     * </ul>
     *
     * @param listener Listener receiving guest lobby updates.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void hostMatch(OnGuestConnectedListener listener) {
        if (bluetoothAdapter == null || bluetoothAdapter.getBluetoothLeAdvertiser() == null) {
            emitEvent(ERROR, "BLE Advertising not supported");
            return;
        }
        this.guestListener = listener;
        if (advertiser != null || bluetoothGattServer != null) {
            emitEvent(WARNING, "Hosting läuft bereits, Anfrage ignoriert");
            return;
        }
        emitEvent(INFO, "Starting match hosting...");
        networkManager.handler.post(() -> {
            startGattServer();
            startAdvertising();
        });
    }

    /**
     * Creates and configures the BLE GATT server used by all guests.
     *
     * <p>The server exposes the KittyCards service and characteristic required
     * for game communication.</p>
     *
     * <p>This method is executed during host startup before advertising begins.</p>
     *
     * <p>Configured capabilities:</p>
     * <ul>
     *     <li>Receiving game actions from guests via writes.</li>
     *     <li>Sending game updates to guests via notifications.</li>
     *     <li>Notification subscription through CCCD.</li>
     * </ul>
     *
     * <p>Typical test cases:</p>
     * <ul>
     *     <li>GATT server is opened successfully.</li>
     *     <li>Service and characteristic are registered.</li>
     *     <li>Failure conditions emit error events.</li>
     * </ul>
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void startGattServer() {
        bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback);
        if (bluetoothGattServer == null) {
            emitEvent(ERROR, "GattServer could not be opened");
            return;
        }

        int writeProperty = BluetoothGattCharacteristic.PROPERTY_WRITE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            writeProperty |= BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
        }
        serverCharacteristic = new BluetoothGattCharacteristic(NetworkManager.KITTY_CARDS_CHARACTERISTIC_UUID, writeProperty | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattService service = new BluetoothGattService(NetworkManager.KITTY_CARDS_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattDescriptor cccd = new BluetoothGattDescriptor(
                NetworkManager.CCCD_UUID,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE
        );
        serverCharacteristic.addDescriptor(cccd);

        service.addCharacteristic(serverCharacteristic);
        boolean success = bluetoothGattServer.addService(service);

        if (!success) emitEvent(ERROR, "Failed to add BLE service");
    }

    /**
     * Starts BLE advertising so guest devices can discover this host.
     *
     * <p>Advertising publishes the KittyCards service UUID and the device name,
     * allowing guests to identify compatible hosts.</p>
     *
     * <p>This method is executed after the GATT server has been created.</p>
     *
     * <p>The host remains discoverable until advertising is stopped manually,
     * times out or an error occurs.</p>
     *
     * <p>Typical test cases:</p>
     * <ul>
     *     <li>Advertising starts with the expected service UUID.</li>
     *     <li>Device name is included in scan responses.</li>
     *     <li>AdvertiseCallback receives success or failure events.</li>
     * </ul>
     */
    private void startAdvertising() {
        if (bluetoothAdapter == null) return;
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) return;


        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(NetworkManager.KITTY_CARDS_SERVICE_UUID))
                .build();

        AdvertiseData scanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(120000)
                .build();

        advertiser.startAdvertising(
                settings,
                advertiseData,
                scanResponse,
                advertiseCallback
        );
        // TODO: Call disconnect in GameController/UI if game is paused or left, to save battery

    }

    /**
     * Selects a connected guest as the active game communication partner.
     *
     * <p>Only one guest may participate in an active game session at a time.
     * Once selected, incoming and outgoing game traffic is restricted to the
     * chosen guest.</p>
     *
     * <p>All remaining connected guests are actively disconnected to ensure a
     * single peer-to-peer game session.</p>
     *
     * <p>This method is typically invoked after the host player chooses a guest
     * from the lobby screen.</p>
     *
     * <p>Typical test cases:</p>
     * <ul>
     *     <li>Selected guest becomes the active device.</li>
     *     <li>Other guests are disconnected.</li>
     *     <li>Selecting a non-connected guest fails.</li>
     * </ul>
     *
     * @param guest Guest chosen for the game session
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void selectGuest(NetworkDevice guest) {
        networkManager.handler.post(() -> {
            if (guest == null) {
                emitEvent(ERROR, "Guest selection failed: guest is null");
                return;
            }

            if (
                    !connectedGuests.contains(guest)
                            || bluetoothAdapter == null
                            || bluetoothGattServer == null
                            || serverCharacteristic == null
            ) {
                emitEvent(ERROR, "Guest selection failed: guest is not connected");
                return;
            }

            emitEvent(
                    INFO,
                    "Selecting guest as active partner: " + guest.deviceAddress()
            );

            selectedGuestDevice = bluetoothAdapter.getRemoteDevice(guest.deviceAddress());

            /*
             * Inform the selected guest before disconnecting the remaining connected devices.
             */
            sendGameChange(new GameAction(GameAction.ActionType.GUEST_ACCEPTED));

            for (NetworkDevice other : new ArrayList<>(connectedGuests)) {
                if (!other.equals(guest)) {
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(other.deviceAddress());
                    bluetoothGattServer.cancelConnection(device);
                }
            }
        });
    }

    /**
     * Stops advertising the hosted room while keeping the active guest
     * connection and GATT server alive.
     *
     * <p>This is used when the match starts. The room should no longer
     * appear in guest scan results, but the established Bluetooth
     * connection must remain available for gameplay.</p>
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    public void stopRoomDiscovery() {
        networkManager.handler.post(() -> {
            if (advertiser == null) {
                emitEvent(
                        WARNING,
                        "Room discovery is already stopped"
                );
                return;
            }

            try {
                advertiser.stopAdvertising(advertiseCallback);
                emitEvent(
                        INFO,
                        "Room discovery stopped"
                );
            } catch (IllegalStateException exception) {
                emitEvent(
                        WARNING,
                        "Room discovery could not be stopped cleanly"
                );
            } finally {
                advertiser = null;
            }
        });
    }

    /**
     * Closes the hosted room.
     *
     * <p>If an active guest is selected, the guest is informed through a
     * {@link GameAction.ActionType#ROOM_CLOSED} action before the BLE connection
     * is closed. If no guest is selected, the host disconnects immediately.</p>
     */
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
    })
    public void closeHostedRoom() {
        networkManager.handler.post(() -> {
            if (
                    selectedGuestDevice == null
                            || bluetoothGattServer == null
                            || serverCharacteristic == null
            ) {
                disconnect();
                return;
            }

            disconnectAfterQueueIsEmpty = true;

            byte[] data = networkManager.protocolEngine.encodeGameAction(
                    new GameAction(GameAction.ActionType.ROOM_CLOSED)
            );

            /*
             * ROOM_CLOSED is placed behind any notification that may already
             * be waiting, such as GUEST_ACCEPTED.
             */
            outgoingQueue.add(data);
            processNextNotification();
        });
    }

    /**
     * Stops hosting and releases all BLE resources owned by the host.
     *
     * <p>This method terminates advertising, disconnects all guests, closes the
     * GATT server and clears all runtime state associated with the current
     * session.</p>
     *
     * <p>It may be triggered by:</p>
     * <ul>
     *     <li>User leaving the lobby.</li>
     *     <li>Application shutdown.</li>
     *     <li>Advertising startup failure.</li>
     *     <li>Game lifecycle events requiring cleanup.</li>
     * </ul>
     *
     * <p>After completion the host returns to an idle state and can be started
     * again using {@link #hostMatch(OnGuestConnectedListener)}.</p>
     *
     * <p>Typical test cases:</p>
     * <ul>
     *     <li>Advertising is stopped.</li>
     *     <li>All guests are disconnected.</li>
     *     <li>GATT server is closed.</li>
     *     <li>Internal state is reset.</li>
     * </ul>
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT})
    public void disconnect() {
        networkManager.handler.post(() -> {
            emitEvent(INFO, "Host is stopping advertising and closing GATT server");

            disconnectAfterQueueIsEmpty = false;
            outgoingQueue.clear();
            notificationInProgress = false;

            if (advertiser != null) {
                try {
                    advertiser.stopAdvertising(advertiseCallback);
                } catch (IllegalStateException e) {
                    // Falls das OS intern schon dichtgemacht hat
                }
                advertiser = null;
            }

            if (bluetoothGattServer != null && bluetoothAdapter != null) {
                for (NetworkDevice guest : connectedGuests) {
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(guest.deviceAddress());
                    bluetoothGattServer.cancelConnection(device);
                }
            }

            networkManager.handler.postDelayed(() -> {
                if (bluetoothGattServer != null) {
                    bluetoothGattServer.close();
                    bluetoothGattServer = null;
                }
                connectedGuests.clear();
                selectedGuestDevice = null;
                serverCharacteristic = null;
                emitEvent(INFO, "GATT server closed and resources cleared successfully");
            }, 200);
        });
    }

    /**
     * Queues a game action for transmission to the currently selected guest.
     *
     * <p>The action is encoded into the network protocol format and added to the
     * outgoing notification queue.</p>
     *
     * <p>Actual BLE transmission may occur immediately or later depending on
     * whether another notification is currently being transmitted.</p>
     *
     * <p>This method is typically called by the game logic whenever a local game
     * state change must be synchronized with the remote player.</p>
     *
     * <p>Typical test cases:</p>
     * <ul>
     *     <li>Game actions are encoded correctly.</li>
     *     <li>Payloads are added to the outgoing queue.</li>
     *     <li>Sending without an active guest fails.</li>
     * </ul>
     *
     * @param action Game action that should be sent to the active guest.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void sendGameChange(GameAction action) {
        if (bluetoothGattServer == null || serverCharacteristic == null || selectedGuestDevice == null) {
            emitEvent(ERROR, "Sending not possible: no active guest");
            return;
        }
        byte[] data = networkManager.protocolEngine.encodeGameAction(action);

        networkManager.handler.post(() -> {
            outgoingQueue.add(data);
            processNextNotification();
        });
    }

    /**
     * Processes the next pending BLE notification in the outgoing queue.
     *
     * <p>This method implements sequential transmission of game messages. Only
     * one notification may be in flight at a time because completion is tracked
     * through {@link BluetoothGattServerCallback#onNotificationSent(BluetoothDevice, int)}.</p>
     *
     * <p>If transmission prerequisites are not met, the method exits without
     * performing any action.</p>
     *
     * <p>This method is an internal flow-control component and should never be
     * called directly from outside the host implementation.</p>
     *
     * <p>Typical test cases:</p>
     * <ul>
     *     <li>Messages are sent in FIFO order.</li>
     *     <li>Concurrent sends are prevented.</li>
     *     <li>Queue processing continues after completion callbacks.</li>
     * </ul>
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void processNextNotification() {
        if (notificationInProgress || outgoingQueue.isEmpty() || bluetoothGattServer == null || serverCharacteristic == null || selectedGuestDevice == null) {
            return;
        }

        notificationInProgress = true;
        byte[] data = outgoingQueue.poll();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bluetoothGattServer.notifyCharacteristicChanged(selectedGuestDevice, serverCharacteristic, false, data);
        } else {
            serverCharacteristic.setValue(data);
            bluetoothGattServer.notifyCharacteristicChanged(selectedGuestDevice, serverCharacteristic, false);
        }
    }

    /**
     * Emits a network event originating from the BLE host component.
     *
     * <p>This method centralizes event reporting and automatically assigns the
     * source component name "BleHost".</p>
     *
     * @param type Event severity or category.
     * @param msg  Human-readable event message.
     */
    private void emitEvent(NetworkEvent.NetworkMessageType type, String msg) {
        networkManager.emitEvent(type, "BleHost", msg);
    }

    /**
     * Converts Android BLE advertising error codes into human-readable messages.
     *
     * <p>The returned text is intended for logging, diagnostics and UI-facing
     * network events.</p>
     *
     * @param code Android advertising error code.
     * @return Descriptive text representing the supplied error code.
     */
    private String advertiseErrorText(int code) {
        return switch (code) {
            case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising already active";

            case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE ->
                    "Advertise data too large (UUID / Name / Payload)";

            case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
                    "Advertising not supported";

            case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal Bluetooth error";

            case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
                    "Too many active advertisers";

            default -> "Unknown advertising error (" + code + ")";
        };
    }
//TODO
    /*
    Was ich konkret bauen würde

        Neue Klasse:

        private static class PendingMessage {
            final byte[] payload;
            final int sequenceNumber;
            int retryCount;
        }

        Host:

        private PendingMessage inFlightMessage;

        Senden:

        Queue
         ↓
        wenn kein inFlightMessage
         ↓
        send
         ↓
        warte ACK

        Guest:

        Empfängt:

        MOVE #17

        Antwortet sofort:
        
        ACK #17

        Host:

        Empfängt:

        ACK #17

        Dann:

        inFlightMessage = null;
        processNextNotification();

        Timeout:

        handler.postDelayed(...)

        z.B.

        2 Sekunden

        Wenn ACK fehlt:

        retry

        Nach

        MAX_RETRIES = 3

        dann:

        disconnect()

        oder

        emitEvent(ERROR, ...)
     */
}