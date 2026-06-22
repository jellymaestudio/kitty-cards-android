package kittycards.kittycardsandroid.network;

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
            // Host is now discoverable by other devices during scanning
        }

        @Override
        public void onStartFailure(int errorCode) {
            // TODO: Error handling, e.g., advertising could not be started
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

                        // Notify the UI thread if a listener is present
                        if (guestListener != null) {
                            guestListener.onGuestListUpdated(new ArrayList<>(connectedGuests));
                        }
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // Guest lost or closed the connection -> remove from the list
                    if (connectedGuests.remove(networkDevice)) {

                        // Inform UI: A guest left the lobby
                        if (guestListener != null) {
                            guestListener.onGuestListUpdated(new ArrayList<>(connectedGuests));
                        }
                    }
                    if (selectedGuestDevice != null && selectedGuestDevice.getAddress().equals(device.getAddress())) {
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
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {

            if (NetworkManager.KITTY_CARDS_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
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
            // TODO Error handling: BLE advertising not supported on this device
            return;
        }
        this.guestListener = listener;
        networkManager.handler.post(() -> {
            startAdvertising();
            startGattServer();
        });
    }

    private void startAdvertising() {
        if (bluetoothAdapter == null) return;
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) return;

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(new ParcelUuid(NetworkManager.KITTY_CARDS_SERVICE_UUID))
                .build();
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(120000)
                .build();

        advertiser.startAdvertising(settings, data, advertiseCallback);
        // TODO: Call disconnect in GameController/UI if game is paused or left, to save battery
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void startGattServer() {
        bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback);
        if (bluetoothGattServer == null) return; // TODO: Error handling (e.g. Bluetooth not available, disabled or permission missing)

        int writeProperty = BluetoothGattCharacteristic.PROPERTY_WRITE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            writeProperty |= BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
        }
        serverCharacteristic = new BluetoothGattCharacteristic(
                NetworkManager.KITTY_CARDS_CHARACTERISTIC_UUID,
                writeProperty | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );

        BluetoothGattService service = new BluetoothGattService(
                NetworkManager.KITTY_CARDS_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
        );

        service.addCharacteristic(serverCharacteristic);
        bluetoothGattServer.addService(service);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void selectGuest(NetworkDevice guest) {
        networkManager.handler.post(() -> {
            if (!connectedGuests.contains(guest) || bluetoothAdapter == null || bluetoothGattServer == null) {
                return; // TODO: Exception/Error handling: The selected guest is not in the list of connected guests
            }

            selectedGuestDevice = bluetoothAdapter.getRemoteDevice(guest.deviceAddress());

            for (NetworkDevice other : new ArrayList<>(connectedGuests)) {
                if (!other.equals(guest)) {
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
            return; // TODO: Exception/return?
        }
        byte[] data = networkManager.protocolEngine.encodeGameAction(action);

        networkManager.handler.post(() -> {
            outgoingQueue.add(data);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bluetoothGattServer.notifyCharacteristicChanged(selectedGuestDevice, serverCharacteristic, false, data);
        } else {
            serverCharacteristic.setValue(data);
            bluetoothGattServer.notifyCharacteristicChanged(selectedGuestDevice, serverCharacteristic, false);
        }
    }
}