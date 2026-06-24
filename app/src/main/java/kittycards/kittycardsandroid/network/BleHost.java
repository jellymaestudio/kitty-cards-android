package kittycards.kittycardsandroid.network;

import static kittycards.kittycardsandroid.network.event.NetworkEvent.NetworkMessageType.ERROR;
import static kittycards.kittycardsandroid.network.event.NetworkEvent.NetworkMessageType.INFO;
import static kittycards.kittycardsandroid.network.event.NetworkEvent.NetworkMessageType.WARNING;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
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
 * Acts as the BLE Peripheral/Server in the Bluetooth Low Energy communication, meaning it advertises itself as a host and accepts connections from guests.
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

    private final android.bluetooth.le.AdvertiseCallback advertiseCallback = new android.bluetooth.le.AdvertiseCallback() {
        @Override
        public void onStartSuccess(android.bluetooth.le.AdvertiseSettings settingsInEffect) {
            emitEvent(INFO, "Advertising started successfully. Host is discoverable."); // <-- NEU: Erfolg beim Advertising loggen
        }

        @Override
        public void onStartFailure(int errorCode) {
            emitEvent(ERROR, "Advertising failed: " + advertiseErrorText(errorCode));
        }
    };

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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
                    if (selectedGuestDevice != null && selectedGuestDevice.getAddress().equals(device.getAddress())) {
                        emitEvent(WARNING, "Active game partner disconnected: " + device.getAddress()); // <-- NEU: Aktiver Partner weg
                        selectedGuestDevice = null;
                        outgoingQueue.clear();
                        notificationInProgress = false;
                        // TODO networkManager.handleRemoteDisconnect(); ?
                        // TODO: Inform GameController/UI that the active game partner lost connection
                    }
                }
            });
        }

        // IMPORTANT: The counterpart to onCharacteristicChanged on the client side.
        // This is where game moves sent by the GUEST arrive at the HOST!
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

            if (NetworkManager.KITTY_CARDS_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                emitEvent(INFO, "Received write request from guest: " + device.getAddress()); // <-- NEU: Eingehender Request loggen
                networkManager.decodeAndQueueDataSafe(value);

                // A server MUST acknowledge to the client that the data was received
                if (responseNeeded && bluetoothGattServer != null) {
                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                emitEvent(ERROR, "Notification failed: " + status);
            } else {
                emitEvent(INFO, "Notification successfully sent to " + device.getAddress()); // <-- NEU: Notification gesendet
            }
            networkManager.handler.post(() -> {
                notificationInProgress = false;
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void hostMatch(OnGuestConnectedListener listener) {
        if (bluetoothAdapter == null || bluetoothAdapter.getBluetoothLeAdvertiser() == null) {
            emitEvent(ERROR, "BLE Advertising not supported");
            return;
        }
        this.guestListener = listener;
        emitEvent(INFO, "Starting match hosting..."); // <-- NEU: Startvorgang eingeleitet
        networkManager.handler.post(() -> {
            startGattServer();
            startAdvertising();
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void startGattServer() {
        bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback);
        if (bluetoothGattServer == null) {
            emitEvent(ERROR, "GattServer could not be opened");
            return;
        }
        emitEvent(INFO, "GattServer started");
        int writeProperty = BluetoothGattCharacteristic.PROPERTY_WRITE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            writeProperty |= BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
        }
        serverCharacteristic = new BluetoothGattCharacteristic(NetworkManager.KITTY_CARDS_CHARACTERISTIC_UUID, writeProperty | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattService service = new BluetoothGattService(NetworkManager.KITTY_CARDS_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
//      TODO is this reasonable?
//        BluetoothGattDescriptor cccd = new BluetoothGattDescriptor(
//                NetworkManager.CCCD_UUID,
//                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE
//        );
//        serverCharacteristic.addDescriptor(cccd);

        service.addCharacteristic(serverCharacteristic);
        bluetoothGattServer.addService(service);
        emitEvent(INFO, "Custom BLE Service and Characteristic added to GATT Server"); // <-- NEU: Service-Struktur steht
    }

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

        emitEvent(INFO, "Starting BLE Advertisement configuration..."); // <-- NEU: Initiiere Advertising
        advertiser.startAdvertising(
                settings,
                advertiseData,
                scanResponse,
                advertiseCallback
        );
        // TODO: Call disconnect in GameController/UI if game is paused or left, to save battery

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void selectGuest(NetworkDevice guest) {
        networkManager.handler.post(() -> {
            if (!connectedGuests.contains(guest) || bluetoothAdapter == null || bluetoothGattServer == null) {
                emitEvent(ERROR, "Guest selection failed (not connected)");
                return;
            }

            emitEvent(INFO, "Selecting guest as active partner: " + guest.deviceAddress()); // <-- NEU: Partner ausgewählt
            selectedGuestDevice = bluetoothAdapter.getRemoteDevice(guest.deviceAddress());

            for (NetworkDevice other : new ArrayList<>(connectedGuests)) {
                if (!other.equals(guest)) {
                    emitEvent(INFO, "Disconnecting non-selected guest: " + other.deviceAddress()); // <-- NEU: Andere Clients kicken
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(other.deviceAddress());
                    bluetoothGattServer.cancelConnection(device);
                }
            }
        });
        // TODO: Validate that responseNeeded handling and cancelConnection() don't race —
        //  multiple onConnectionStateChange(DISCONNECTED) callbacks will fire here,
        //  each triggering guestListener.onGuestListUpdated(). Debounce if this causes
        //  unwanted UI flicker.
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT})
    public void disconnect() {
        networkManager.handler.post(() -> {
            emitEvent(INFO, "Host is stopping advertising and closing GATT server"); // <-- NEU: Manueller Host-Shutdown
            outgoingQueue.clear();
            notificationInProgress = false;

            if (advertiser != null) {
                advertiser.stopAdvertising(advertiseCallback);
                advertiser = null;
            }
            if (bluetoothGattServer != null) {
                bluetoothGattServer.close();
                bluetoothGattServer = null;
            }
            connectedGuests.clear();
            selectedGuestDevice = null;
            serverCharacteristic = null;
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void sendGameChange(GameAction action) {
        if (bluetoothGattServer == null || serverCharacteristic == null || selectedGuestDevice == null) {
            emitEvent(ERROR, "Sending not possible: no active guest");
            return;
        }
        byte[] data = networkManager.protocolEngine.encodeGameAction(action);

        networkManager.handler.post(() -> {
            outgoingQueue.add(data);
            emitEvent(INFO, "Notification added to queue. Queue size: " + outgoingQueue.size()); // <-- NEU: Queue-Status beim Host
            processNextNotification();
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void processNextNotification() {
        if (notificationInProgress || outgoingQueue.isEmpty() || bluetoothGattServer == null || serverCharacteristic == null || selectedGuestDevice == null) {
            return;
        }

        notificationInProgress = true;
        byte[] data = outgoingQueue.poll();

        emitEvent(INFO, "Sending notification to guest..."); // <-- NEU: Sendevorgang startet

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bluetoothGattServer.notifyCharacteristicChanged(selectedGuestDevice, serverCharacteristic, false, data);
        } else {
            serverCharacteristic.setValue(data);
            bluetoothGattServer.notifyCharacteristicChanged(selectedGuestDevice, serverCharacteristic, false);
        }
    }

    private void emitEvent(NetworkEvent.NetworkMessageType type, String msg) {
        networkManager.emitEvent(type, "BleHost", msg);
    }

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

}