package kittycards.kittycardsandroid.network;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeAdvertiser;

import androidx.annotation.RequiresPermission;

import java.util.ArrayList;

public class BleHost {
    private final NetworkManager networkManager;
    private final BluetoothAdapter bluetoothAdapter;

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
            @SuppressLint("MissingPermission")
            NetworkDevice networkDevice = new NetworkDevice(device.getName(), device.getAddress());

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
                try {
                    // Daten decodieren und in dieselbe actionQueue packen
                    networkManager.decodeAndQueueData(value);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Ein Server MUSS dem Client antworten, dass die Daten angekommen sind
                if (responseNeeded) {
                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            }
        }
    };

    public BleHost(NetworkManager networkManager, BluetoothAdapter bluetoothAdapter) {
        this.networkManager = networkManager;
        this.bluetoothAdapter = bluetoothAdapter;
    }

    public void hostMatch(OnGuestConnectedListener listener) {
        this.guestListener = listener;
        //TODO: Implement hosting functionality (BLE advertising, accepting connections, etc.)
    }

    public void selectGuest(NetworkDevice guest) {
        // TODO: Accept the selected guest, (disconnect the others?)
    }
}