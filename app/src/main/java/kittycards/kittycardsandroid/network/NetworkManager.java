package kittycards.kittycardsandroid.network;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import kittycards.kittycardsandroid.components.INetworkManager;
import kittycards.kittycardsandroid.components.IProtocolEngine;

/**
 * calls the listener methods
 *
 * @author red_concrete
 */
public class NetworkManager implements INetworkManager {

    private static final UUID KITTY_CARDS_SERVICE_UUID =
            UUID.fromString("0aac93ed-aff4-4ef0-85ef-019c11b3e434");//from: https://www.uuidgenerator.net/
    private static final UUID KITTY_CARDS_CHARACTERISTIC_UUID =
            UUID.fromString("f4439cae-c811-418e-b314-c7258d85710c");//from: https://www.uuidgenerator.net/

    private static final long SCAN_PERIOD = 10000;

    private static volatile NetworkManager instance;

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final IProtocolEngine protocolEngine;
    private final LinkedBlockingQueue<GameAction> actionQueue = new LinkedBlockingQueue<>();

    private final ArrayList<NetworkDevice> foundRooms = new ArrayList<>();
    private BluetoothGatt activeGattConnection;
    private BluetoothGattCharacteristic gattCharacteristic;
    private boolean scanning = false;

    // -------------------------------------------------------------------------
    // Scan
    // -------------------------------------------------------------------------

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            @SuppressLint("MissingPermission")
            NetworkDevice device = new NetworkDevice(
                    result.getDevice().getName(),
                    result.getDevice().getAddress()
            );
            if (!foundRooms.contains(device)) {
                foundRooms.add(device);
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
                // TODO: Dealing with a loss of connection
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(KITTY_CARDS_SERVICE_UUID);
            if (service == null) return; // TODO: Fehler behandeln
            gattCharacteristic = service.getCharacteristic(KITTY_CARDS_CHARACTERISTIC_UUID);
            // TODO: activate Notifications
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            try {
                decodeAndQueueData(value); //
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            // TODO: Confirmation that sendGameChange() has been received?
        }
    };

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private NetworkManager(Context context) {
        this.context = context.getApplicationContext();
        this.protocolEngine = new ProtocolEngine();
        BluetoothManager bluetoothManager =
                (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public static NetworkManager getInstance(Context context) {
        if (instance == null) {
            synchronized (NetworkManager.class) {
                if (instance == null) {
                    instance = new NetworkManager(context);
                }
            }
        }
        return instance;
    }

    public static NetworkManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NetworkManager not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScan() {
        if (bluetoothAdapter == null) return;

        if (!scanning) {
            scanning = true;
            foundRooms.clear();

            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(KITTY_CARDS_SERVICE_UUID))
                    .build();

            bluetoothAdapter.getBluetoothLeScanner().startScan(
                    List.of(filter),
                    new ScanSettings.Builder().build(),
                    leScanCallback
            );

            handler.postDelayed(this::stopScan, SCAN_PERIOD);
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

    // -------------------------------------------------------------------------
    // BLE data receiving
    // -------------------------------------------------------------------------

    // BLE background thread calls this when data arrives
    private void decodeAndQueueData(byte[] bytes) throws InterruptedException {
        GameAction action = protocolEngine.decode(bytes);
        actionQueue.put(action);
    }

    // -------------------------------------------------------------------------
    // INetworkManager
    // -------------------------------------------------------------------------

    @Override
    public ArrayList<NetworkDevice> hostMatch() {
        // TODO: Start BLE advertising (host announces itself)
        return foundRooms;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    public ArrayList<NetworkDevice> joinMatch() {
        startScan();
        return foundRooms;
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    public void confirmRoom(NetworkDevice room) {
        if (bluetoothAdapter == null) return;
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(room.deviceAddress());
        stopScan();
        activeGattConnection = device.connectGatt(context, false, gattCallback);
        // TODO: Connect to the selected host (connectGatt). Fertig?
    }

    @Override
    public void selectGuest(NetworkDevice guest) {
        // TODO: Accept the selected guest, (disconnect the others?)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    public void disconnect() {
        stopScan();
        handler.removeCallbacksAndMessages(null);
        // TODO: Disconnect active GATT connection
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void sendGameChange(GameAction action) {
        activeGattConnection.writeCharacteristic(
                gattCharacteristic,
                protocolEngine.encode(action),
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        );
    }

    @Override
    public GameAction fetchNextAction() throws InterruptedException {
        return actionQueue.take(); // Blocks the calling thread until something is in the queue
    }
}