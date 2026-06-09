package kittycards.kittycardsandroid.network;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

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

    private static final long SCAN_PERIOD = 10000;

    private static volatile NetworkManager instance;

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final IProtocolEngine protocolEngine;
    private final LinkedBlockingQueue<GameAction> actionQueue = new LinkedBlockingQueue<>();

    private final ArrayList<NetworkDevice> foundRooms = new ArrayList<>();
    private boolean scanning = false;

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

    @SuppressLint("MissingPermission") // Permission wird in der Activity geprüft
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

    @SuppressLint("MissingPermission") // Permission is checked in the activity
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
    private void onBytesReceived(byte[] bytes) throws InterruptedException {
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

    @Override
    public ArrayList<NetworkDevice> joinMatch() {
        startScan();
        return foundRooms;
    }

    @Override
    public void confirmRoom(NetworkDevice room) {
        // TODO: Connect to the selected host (connectGatt)
    }

    @Override
    public void selectGuest(NetworkDevice guest) {
        // TODO: Accept the selected guest, (disconnect the others?)
    }

    @Override
    public void disconnect() {
        stopScan();
        handler.removeCallbacksAndMessages(null);
        // TODO: Disconnect active GATT connection
    }

    @Override
    public void sendGameChange(GameAction action) {
        // TODO: Encode the action using ProtocolEngine and send it via GATT
    }

    @Override
    public GameAction fetchNextAction() throws InterruptedException {
        return actionQueue.take(); // Blocks the calling thread until something is in the queue
    }
}