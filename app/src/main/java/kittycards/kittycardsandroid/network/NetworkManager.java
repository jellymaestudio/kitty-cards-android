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
    //(Client Characteristic Configuration Descriptor) default in every BluetoothGattCharacteristic, used to enable notifications on the client side
    static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
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

    private volatile Role role = Role.NOT_CONNECTED;
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

        this.bleGuest = new BleGuest(this, this.context, this.bluetoothManager);
        this.bleHost = new BleHost(this, this.context, this.bluetoothManager);
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
    void decodeAndQueueDataSafe(byte[] bytes) {
        GameAction action = protocolEngine.decodeGameAction(bytes);
        try {
            actionQueue.put(action);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    // -------------------------------------------------------------------------
    // INetworkManager
    // -------------------------------------------------------------------------

    @Override
    public void hostMatch(OnGuestConnectedListener listener) {
        role = Role.HOST;
        bleHost.hostMatch(listener);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    public void joinMatch(OnDeviceFoundListener listener) {
        role = Role.GUEST;
        bleGuest.joinMatch(listener);
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    public void confirmRoom(NetworkDevice room) {
        bleGuest.confirmRoom(room);
    }

    @Override
    public void selectGuest(NetworkDevice guest) {
        bleHost.selectGuest(guest);
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    @Override
    public void disconnect() {
        switch (role) {
            case GUEST -> bleGuest.disconnect();
            case HOST -> bleHost.disconnect();
            case NOT_CONNECTED -> {
            }
        }
        role = Role.NOT_CONNECTED;
        actionQueue.clear();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void sendGameChange(GameAction action) {
        switch (role) {
            case GUEST -> bleGuest.sendGameChange(action);
            case HOST -> bleHost.sendGameChange(action);
            case NOT_CONNECTED ->
                    throw new IllegalStateException("Not connected - cannot send an action.");
        }
    }

    @Override
    public GameAction fetchNextAction() throws InterruptedException {
        return actionQueue.take(); // Blocks the calling thread until something is in the queue
    }

    // -------------------------------------------------------------------------
    // Getter/Setter
    // -------------------------------------------------------------------------


    public Role getRole() {
        return role;
    }
}