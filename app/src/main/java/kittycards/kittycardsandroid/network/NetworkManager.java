package kittycards.kittycardsandroid.network;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresPermission;

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
    //TODO: OutgoingQueue für sendGame, wenn mehrere gleichzeitig senden wollen.
    static final UUID KITTY_CARDS_SERVICE_UUID = UUID.fromString("0aac93ed-aff4-4ef0-85ef-019c11b3e434");//from: https://www.uuidgenerator.net/
    static final UUID KITTY_CARDS_CHARACTERISTIC_UUID = UUID.fromString("f4439cae-c811-418e-b314-c7258d85710c");//from: https://www.uuidgenerator.net/
    static final long SCAN_PERIOD = 10000;

    private static volatile NetworkManager instance;

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothManager bluetoothManager;

    final Handler handler = new Handler(Looper.getMainLooper());
    final IProtocolEngine protocolEngine;
    private final LinkedBlockingQueue<GameAction> actionQueue = new LinkedBlockingQueue<>();

    private final BleGuest bleGuest;
    private final BleHost bleHost;

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------
    private NetworkManager(Context context) {
        this.context = context.getApplicationContext();
        this.protocolEngine = new ProtocolEngine();
        this.bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        this.bleGuest = new BleGuest(this, this.context, this.bluetoothAdapter);
        this.bleHost = new BleHost(this, this.bluetoothAdapter);
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

    // -------------------------------------------------------------------------
    // BLE data receiving (Shared for Host and Guest)
    // -------------------------------------------------------------------------

    // BLE background thread calls this when data arrives
    void decodeAndQueueData(byte[] bytes) throws InterruptedException {
        GameAction action = protocolEngine.decodeGameAction(bytes);
        actionQueue.put(action);
    }

    // -------------------------------------------------------------------------
    // INetworkManager
    // -------------------------------------------------------------------------

    @Override
    public void hostMatch(OnGuestConnectedListener listener) {
        bleHost.hostMatch(listener);
    }

    @Override
    public void selectGuest(NetworkDevice guest) {
        bleHost.selectGuest(guest);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    public void joinMatch(OnDeviceFoundListener listener) {
        bleGuest.joinMatch(listener);
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    public void confirmRoom(NetworkDevice room) {
        bleGuest.confirmRoom(room);
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    @Override
    public void disconnect() {
        bleGuest.disconnect();
        // Falls Host-Disconnect Logik dazukommt, hier aufrufen: bleHost.disconnect();
        actionQueue.clear();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void sendGameChange(GameAction action) {
        bleGuest.sendGameChange(action);
    }

    @Override
    public GameAction fetchNextAction() throws InterruptedException {
        return actionQueue.take(); // Blocks the calling thread until something is in the queue
    }
}