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
import kittycards.kittycardsandroid.network.event.NetworkEvent;
import kittycards.kittycardsandroid.network.event.NetworkEventListener;

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

    final BleHost bleHost;
    final BleGuest bleGuest;
    final IProtocolEngine protocolEngine;
    final Handler handler = new Handler(Looper.getMainLooper());

    private volatile Role role = Role.NOT_CONNECTED;

    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothManager bluetoothManager;
    private final Context context;

    private final LinkedBlockingQueue<GameAction> actionQueue = new LinkedBlockingQueue<>();

    private NetworkEventListener eventListener;


    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private NetworkManager(Context context, BleHost bleHost, BleGuest bleGuest, boolean isTestInstance) {
        this.context = context.getApplicationContext();
        this.protocolEngine = new ProtocolEngine();
        this.bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        this.bleGuest = bleGuest != null ? bleGuest : new BleGuest(this, this.context, this.bluetoothManager);
        this.bleHost = bleHost != null ? bleHost : new BleHost(this, this.context, this.bluetoothManager);
    }

    private NetworkManager(Context context) {
        this(context, null, null, false);
    }

    // For testing purposes only
    NetworkManager(Context context, BleHost bleHost, BleGuest bleGuest) {
        this(context, bleHost, bleGuest, true);
        instance = this;
    }

    // For testing purposes only
    NetworkManager(Context context, BluetoothManager bluetoothManager, IProtocolEngine protocolEngine) {
        this.context = context.getApplicationContext();
        this.protocolEngine = protocolEngine;
        this.bluetoothManager = bluetoothManager;
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        this.bleGuest = new BleGuest(this, this.context, this.bluetoothManager);
        this.bleHost = new BleHost(this, this.context, this.bluetoothManager);
        instance = this;
    }

    /**
     * Returns the singleton instance of NetworkManager.
     * Must be initialized with a Context first.
     *
     * @param context application context used for BLE setup
     * @return singleton instance
     */
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

    /**
     * Returns the already initialized singleton instance.
     *
     * @return NetworkManager instance
     * @throws IllegalStateException if not initialized via getInstance(Context)
     */
    public static NetworkManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NetworkManager not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // BLE data receiving (Shared for Host and Guest)
    // -------------------------------------------------------------------------

    /**
     * Decodes raw BLE byte payload into a GameAction and enqueues it for consumption.
     * Thread-safe and intended to be called from BLE callback threads.
     *
     * @param bytes raw BLE payload
     */
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

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    public void hostMatch(OnGuestConnectedListener listener) {
        if (role == Role.GUEST) disconnect();
        role = Role.HOST;
        bleHost.hostMatch(listener);
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    @Override
    public void joinMatch(OnDeviceFoundListener listener) {
        if (role == Role.HOST) disconnect();
        role = Role.GUEST;
        bleGuest.joinMatch(listener);
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    public void confirmRoom(NetworkDevice room) {
        bleGuest.confirmRoom(room);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void selectGuest(NetworkDevice guest) {
        bleHost.selectGuest(guest);
    }


    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT})
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

    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
    })
    public void closeHostedRoom() {
        if (role != Role.HOST) {
            return;
        }

        bleHost.closeHostedRoom();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    public void stopRoomDiscovery() {
        if (role != Role.HOST) {
            return;
        }

        bleHost.stopRoomDiscovery();
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

    @Override
    public void setNetworkEventListener(NetworkEventListener listener) {
        this.eventListener = listener;
    }

    public void setRoomConnectionListener(OnRoomConnectionListener listener) {
        bleGuest.setRoomConnectionListener(listener);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Emits a network event to the registered listener on the main thread.
     * Used by BLE components to forward status, warnings and errors to UI layer.
     *
     * @param type    event severity/type
     * @param source  origin of the event (e.g. BleHost, BleGuest)
     * @param message human-readable message
     */
    protected void emitEvent(NetworkEvent.NetworkMessageType type, String source, String message) {
        handler.post(() -> {
            if (eventListener != null) {
                eventListener.onNetworkEvent(new NetworkEvent(type, message, source));
            }
        });
    }

    // -------------------------------------------------------------------------
    // Getter/Setter
    // -------------------------------------------------------------------------

    /**
     * Returns the player's current role (HOST, GUEST, NOT_CONNECTED)
     *
     * @return the role
     */
    public Role getRole() {
        return role;
    }
}