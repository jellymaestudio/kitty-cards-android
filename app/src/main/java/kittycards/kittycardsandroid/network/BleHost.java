package kittycards.kittycardsandroid.network;

import android.Manifest;
import android.annotation.SuppressLint;
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

    private final android.bluetooth.le.AdvertiseCallback advertiseCallback = new android.bluetooth.le.AdvertiseCallback() {
        @Override
        public void onStartSuccess(android.bluetooth.le.AdvertiseSettings settingsInEffect) {
            // TODO (?) Host wird jetzt von anderen Geräten beim Scannen gefunden
        }

        @Override
        public void onStartFailure(int errorCode) {
            // TODO: Fehlerbehandlung, z.B. Werbung konnte nicht gestartet werden
        }
    };

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            NetworkDevice networkDevice = NetworkDevice.from(device);

            if (newState == BluetoothProfile.STATE_CONNECTED) {

                if (!connectedGuests.contains(networkDevice)) {
                    connectedGuests.add(networkDevice);

                    // Notify the UI thread if a listener is present
                    if (guestListener != null) {
                        networkManager.handler.post(() -> guestListener.onGuestListUpdated(new ArrayList<>(connectedGuests)));
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Gast hat die Verbindung verloren oder geschlossen -> aus der Liste entfernen
                if (connectedGuests.remove(networkDevice)) {

                    // UI informieren: Ein Gast hat die Lobby verlassen
                    if (guestListener != null) {
                        networkManager.handler.post(() -> guestListener.onGuestListUpdated(new ArrayList<>(connectedGuests)));
                    }
                }
                if (selectedGuestDevice != null && selectedGuestDevice.getAddress().equals(device.getAddress())) {
                    selectedGuestDevice = null;
                    // TODO: UI informieren, dass der aktive Spielpartner die Verbindung verloren hat
                }
            }
        }

        // WICHTIG: Das Gegenstück zu onCharacteristicChanged beim Client.
        // Hier kommen die Spielzüge an, die der GAST an den HOST schickt!
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {

            if (NetworkManager.KITTY_CARDS_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                networkManager.decodeAndQueueDataSafe(value);

                // Ein Server MUSS dem Client antworten, dass die Daten angekommen sind
                if (responseNeeded) {
                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            }
        }
    };

    public BleHost(NetworkManager networkManager, Context context, BluetoothManager bluetoothManager) {
        this.networkManager = networkManager;
        this.context = context;
        this.bluetoothManager = bluetoothManager;
        this.bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public void hostMatch(OnGuestConnectedListener listener) {
        this.guestListener = listener;
        //TODO: Implement hosting functionality (BLE advertising, accepting connections, etc.)
    }

    public void selectGuest(NetworkDevice guest) {
        // TODO: Accept the selected guest, disconnect the others
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT})
    public void disconnect() {
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
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void sendGameChange(GameAction action) {
        if (bluetoothGattServer == null || serverCharacteristic == null || selectedGuestDevice == null) {
            return; //TODO: Exception/return?
        }
        byte[] data = networkManager.protocolEngine.encodeGameAction(action);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bluetoothGattServer.notifyCharacteristicChanged(selectedGuestDevice, serverCharacteristic, false, data);
        } else {
            serverCharacteristic.setValue(data);
            bluetoothGattServer.notifyCharacteristicChanged(selectedGuestDevice, serverCharacteristic, false);
        }
    }
}