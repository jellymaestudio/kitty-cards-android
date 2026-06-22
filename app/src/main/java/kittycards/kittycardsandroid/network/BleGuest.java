package kittycards.kittycardsandroid.network;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Acts as the BLE Central/Client in the Bluetooth Low Energy communication, meaning it scans for hosts and connects to them as a guest.
 *
 * @author red_concrete
 */
public class BleGuest {
    private static final int MAX_QUEUE_SIZE = 100;
    private static final long WRITE_TIMEOUT_MS = 5000;
    private final NetworkManager networkManager;
    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;

    private boolean scanning = false;
    private final ArrayList<NetworkDevice> foundRooms = new ArrayList<>();
    private OnDeviceFoundListener deviceListener;

    private BluetoothGatt activeGattConnection;
    private BluetoothGattCharacteristic gattCharacteristic;

    private final Queue<byte[]> outgoingQueue = new LinkedList<>();

    private boolean writeInProgress = false;
    private boolean connected = false;

    private final Runnable stopScanRunnable = this::stopScan;

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private final Runnable writeTimeoutRunnable = () -> {
        writeInProgress = false;
        processNextWrite();
    };

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            networkManager.handler.post(() -> {
                NetworkDevice device = NetworkDevice.from(result.getDevice());
                if (!foundRooms.contains(device)) {
                    foundRooms.add(device);
                    // Notify the UI thread if a listener is present
                    if (deviceListener != null) {
                        deviceListener.onDeviceFound(new ArrayList<>(foundRooms));
                    }
                }
            });
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
            networkManager.handler.post(() -> {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    connected = false;

                    if (activeGattConnection != null) {
                        activeGattConnection.close();
                    }
                    activeGattConnection = null;
                    gattCharacteristic = null;
                    outgoingQueue.clear();
                    writeInProgress = false;
                    return;
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connected = true;
                    gatt.discoverServices();//"Discover" the services offered by the remote device (calls onServicesDiscovered() when done)


                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // TODO: Dealing with a loss of connection (e.g. inform GameController/UI)
                    connected = false;
                    networkManager.handler.removeCallbacks(writeTimeoutRunnable);
                    outgoingQueue.clear();
                    writeInProgress = false;
                    gattCharacteristic = null;
                    if (activeGattConnection != null) {
                        activeGattConnection.close();
                    }
                    activeGattConnection = null;
                }
            });
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            networkManager.handler.post(() -> {
                if (status != BluetoothGatt.GATT_SUCCESS) return;

                BluetoothGattService service = gatt.getService(NetworkManager.KITTY_CARDS_SERVICE_UUID);
                if (service == null)
                    return; // TODO: Handle Error (connected to a device that doesn't offer the expected service)

                BluetoothGattCharacteristic characteristic = service.getCharacteristic(NetworkManager.KITTY_CARDS_CHARACTERISTIC_UUID);
                if (characteristic == null)
                    return; // TODO: Handle Error (connected to a device that doesn't offer the expected characteristic)

                gattCharacteristic = characteristic;
                gatt.setCharacteristicNotification(gattCharacteristic, true);

                BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(NetworkManager.CCCD_UUID);
                if (descriptor == null) return;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
            });
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

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // TODO: Confirmation that sendGameChange() has been received by host?
            networkManager.handler.post(() -> {
                networkManager.handler.removeCallbacks(writeTimeoutRunnable);
                writeInProgress = false;
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w("BleGuest", "Characteristic write failed with status: " + status);
                    // TODO: Fehlgeschlagenen Schreibvorgang wiederholen (Retry) oder UI über Übertragungsfehler informieren
                }

                processNextWrite();
            });
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
        if (bluetoothAdapter == null || bluetoothAdapter.getBluetoothLeScanner() == null) {
            return;
        }
        this.deviceListener = listener;
        networkManager.handler.post(this::startScan);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScan() {
        if (bluetoothAdapter == null || bluetoothAdapter.getBluetoothLeScanner() == null) return;

        if (!scanning) {
            scanning = true;
            foundRooms.clear();

            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(NetworkManager.KITTY_CARDS_SERVICE_UUID)).build();

            bluetoothAdapter.getBluetoothLeScanner().startScan(List.of(filter), new ScanSettings.Builder().build(), leScanCallback);
            networkManager.handler.postDelayed(stopScanRunnable, NetworkManager.SCAN_PERIOD);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void stopScan() {
        if (bluetoothAdapter == null || bluetoothAdapter.getBluetoothLeScanner() == null) return;

        if (scanning) {
            bluetoothAdapter.getBluetoothLeScanner().stopScan(leScanCallback);
            scanning = false;
        } else {
            // TODO: The scanner could not be stopped (resource leak)
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    public void confirmRoom(NetworkDevice room) {
        if (bluetoothAdapter == null) return;
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(room.deviceAddress());
        networkManager.handler.post(() -> {
            stopScan();
            activeGattConnection = device.connectGatt(context, false, gattCallback);
        });
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    public void disconnect() {
        networkManager.handler.post(() -> {
            connected = false;
            stopScan();
            networkManager.handler.removeCallbacks(stopScanRunnable);

            networkManager.handler.removeCallbacks(writeTimeoutRunnable);
            outgoingQueue.clear();
            writeInProgress = false;

            if (activeGattConnection != null) {
                activeGattConnection.disconnect();
                activeGattConnection.close();
                activeGattConnection = null;
            }
            gattCharacteristic = null;
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void sendGameChange(GameAction action) {
        // TODO: Exception/return if not connected? (e.g. throw new IllegalStateException("No active connection to send data."))
        byte[] data = networkManager.protocolEngine.encodeGameAction(action);
        networkManager.handler.post(() -> {
            if (!connected || activeGattConnection == null || gattCharacteristic == null) return;

            if (outgoingQueue.size() >= MAX_QUEUE_SIZE) outgoingQueue.poll();
            outgoingQueue.add(data);
            processNextWrite();
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void processNextWrite() {
        if (writeInProgress || !connected || activeGattConnection == null || gattCharacteristic == null)
            return;

        byte[] data = outgoingQueue.poll();
        if (data == null) return;

        writeInProgress = true;
        boolean success;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {    //Android SDK 33+ meaning Android 13+
            int result = activeGattConnection.writeCharacteristic(gattCharacteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            success = result == BluetoothStatusCodes.SUCCESS;

        } else {                                                        //Android SDK < 33 meaning Android 12 and below
            gattCharacteristic.setValue(data);
            success = activeGattConnection.writeCharacteristic(gattCharacteristic);
        }

        if (!success) {
            writeInProgress = false;
            processNextWrite();
            return;
        }

        networkManager.handler.removeCallbacks(writeTimeoutRunnable);
        networkManager.handler.postDelayed(writeTimeoutRunnable, WRITE_TIMEOUT_MS);
    }
}