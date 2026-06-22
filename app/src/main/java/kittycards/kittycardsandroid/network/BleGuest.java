package kittycards.kittycardsandroid.network;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.List;

/**
 * Acts as the BLE Central/Client in the Bluetooth Low Energy communication, meaning it scans for hosts and connects to them as a guest.
 *
 * @author red_concrete
 */
public class BleGuest {
    private final NetworkManager networkManager;
    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;

    private boolean scanning = false;
    private final ArrayList<NetworkDevice> foundRooms = new ArrayList<>();
    private OnDeviceFoundListener deviceListener;

    private BluetoothGatt activeGattConnection;
    private BluetoothGattCharacteristic gattCharacteristic;

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            NetworkDevice device = NetworkDevice.from(result.getDevice());

            if (!foundRooms.contains(device)) {
                foundRooms.add(device);

                // Notify the UI thread if a listener is present
                if (deviceListener != null) {
                    networkManager.handler.post(() -> deviceListener.onDeviceFound(new ArrayList<>(foundRooms))); // Provide a COPY of the list
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            // TODO: Dealing with scan errors
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();    //"Discover" the services offered by the remote device (calls onServicesDiscovered() when done)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close();
                // TODO: Dealing with a loss of connection
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(NetworkManager.KITTY_CARDS_SERVICE_UUID);
            if (service == null) return; // TODO: Handle Error (connected to a device that doesn't offer the expected service)
            gattCharacteristic = service.getCharacteristic(NetworkManager.KITTY_CARDS_CHARACTERISTIC_UUID);
            if (gattCharacteristic == null) return; // TODO: Handle Error (connected to a device that doesn't offer the expected characteristic)

            gatt.setCharacteristicNotification(gattCharacteristic, true);//local setting: report changes to onCharacteristicChanged()

            BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(NetworkManager.CCCD_UUID);
            if (descriptor == null) return; // TODO: (can this even happen?) Handle Error (missing CCCD descriptor, can't enable notifications on the client side)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }

        // API 33+
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            handleIncomingData(value);
        }

        // API 31–32
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            handleIncomingData(characteristic.getValue());
        }

        private void handleIncomingData(byte[] value) {
            networkManager.decodeAndQueueDataSafe(value);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // TODO: Confirmation that sendGameChange() has been received?
        }
    };

    public BleGuest(NetworkManager networkManager, Context context, BluetoothManager bluetoothManager) {
        this.networkManager = networkManager;
        this.context = context;
        this.bluetoothManager = bluetoothManager;
        this.bluetoothAdapter = bluetoothManager.getAdapter();
    }

    // -------------------------------------------------------------------------
    // Scan
    // -------------------------------------------------------------------------

    /**
     * @see NetworkManager#joinMatch(OnDeviceFoundListener)
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void joinMatch(OnDeviceFoundListener listener) {
        this.deviceListener = listener;
        startScan();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScan() {
        if (bluetoothAdapter == null) return;

        if (!scanning) {
            scanning = true;
            foundRooms.clear();

            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(NetworkManager.KITTY_CARDS_SERVICE_UUID)).build();

            bluetoothAdapter.getBluetoothLeScanner().startScan(List.of(filter), new ScanSettings.Builder().build(), leScanCallback);

            networkManager.handler.postDelayed(this::stopScan, NetworkManager.SCAN_PERIOD);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void stopScan() {
        if (bluetoothAdapter == null) return;

        if (bluetoothAdapter.getBluetoothLeScanner() != null) {
            bluetoothAdapter.getBluetoothLeScanner().stopScan(leScanCallback);
        } else {
            // TODO: The scanner could not be stopped (resource leak)
        }
        scanning = false;
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    public void confirmRoom(NetworkDevice room) {
        if (bluetoothAdapter == null) return;
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(room.deviceAddress());
        stopScan();
        activeGattConnection = device.connectGatt(context, false, gattCallback);
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    public void disconnect() {
        stopScan();
        networkManager.handler.removeCallbacksAndMessages(null);

        if (activeGattConnection != null) {
            activeGattConnection.disconnect();
            activeGattConnection.close();
            activeGattConnection = null;
        }
        gattCharacteristic = null;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void sendGameChange(GameAction action) {
        if (activeGattConnection == null || gattCharacteristic == null) {
            //TODO: Exception/return ?
            //throw new IllegalStateException("No active connection to send data.");
            return;
        }
        //TODO: Fehlende Outgoing-Queue (Sende-Warteschlange)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {    //Android SDK 33+ meaning Android 13+
            activeGattConnection.writeCharacteristic(gattCharacteristic, networkManager.protocolEngine.encodeGameAction(action), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        } else {                                                        //Android SDK < 33 meaning Android 12 and below
            gattCharacteristic.setValue(networkManager.protocolEngine.encodeGameAction(action));
            activeGattConnection.writeCharacteristic(gattCharacteristic);
        }
    }
}