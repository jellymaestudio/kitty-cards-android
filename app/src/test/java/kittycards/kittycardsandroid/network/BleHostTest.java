package kittycards.kittycardsandroid.network;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import kittycards.kittycardsandroid.components.IProtocolEngine;
import kittycards.kittycardsandroid.network.event.NetworkEventListener;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33}) // ggf. zusätzlich 31/32 für die API-Fallback-Pfade parametrisieren
public class BleHostTest {

    private static final String ADDR_A = "AA:AA:AA:AA:AA:AA";
    private static final String ADDR_B = "BB:BB:BB:BB:BB:BB";

    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer gattServer;
    private BluetoothLeAdvertiser advertiser;

    private NetworkManager networkManager;
    private BleHost bleHost;
    private BluetoothGattServerCallback callback;
    private IProtocolEngine protocolEngine;

    private BluetoothDevice deviceA;
    private BluetoothDevice deviceB;

    // Erfasst Listener-Aufrufe für Assertions
    private final AtomicInteger listenerCallCount = new AtomicInteger(0);
    private List<NetworkDevice> lastGuestList;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        bluetoothManager = mock(BluetoothManager.class);
        bluetoothAdapter = mock(BluetoothAdapter.class);
        gattServer = mock(BluetoothGattServer.class);
        BluetoothLeAdvertiser advertiser = mock(BluetoothLeAdvertiser.class);
        protocolEngine = mock(IProtocolEngine.class);
        when(protocolEngine.encodeGameAction(any())).thenReturn(new byte[]{0, 0, 0, 0, 0}); // 5 Byte, wie im Protokoll definiert

        deviceA = mock(BluetoothDevice.class);
        when(deviceA.getAddress()).thenReturn(ADDR_A);
        deviceB = mock(BluetoothDevice.class);
        when(deviceB.getAddress()).thenReturn(ADDR_B);

        when(bluetoothManager.getAdapter()).thenReturn(bluetoothAdapter);
        when(bluetoothAdapter.getBluetoothLeAdvertiser()).thenReturn(advertiser);
        when(bluetoothManager.openGattServer(eq(context), any())).thenReturn(gattServer);
        when(gattServer.addService(any())).thenReturn(true);
        when(bluetoothAdapter.getRemoteDevice(ADDR_A)).thenReturn(deviceA);
        when(bluetoothAdapter.getRemoteDevice(ADDR_B)).thenReturn(deviceB);

        networkManager = new NetworkManager(context, bluetoothManager, protocolEngine);
        bleHost = networkManager.bleHost;

        bleHost.hostMatch(guests -> {
            listenerCallCount.incrementAndGet();
            lastGuestList = guests;
        });
        shadowOf(Looper.getMainLooper()).idle();

        ArgumentCaptor<BluetoothGattServerCallback> captor =
                ArgumentCaptor.forClass(BluetoothGattServerCallback.class);
        verify(bluetoothManager).openGattServer(eq(context), captor.capture());
        callback = captor.getValue();
    }


    @After
    public void resetSingleton() throws Exception {
        var f = NetworkManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    @Test
    public void onConnect_newGuest_isAddedAndListenerNotified() {
        callback.onConnectionStateChange(deviceA, 0, BluetoothProfile.STATE_CONNECTED);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, listenerCallCount.get());
        assertEquals(1, lastGuestList.size());
        assertEquals(NetworkDevice.from(deviceA), lastGuestList.getFirst());
    }

    @Test
    public void onConnect_sameGuestTwice_noDuplicateAndListenerNotCalledAgain() {
        callback.onConnectionStateChange(deviceA, 0, BluetoothProfile.STATE_CONNECTED);
        shadowOf(Looper.getMainLooper()).idle();
        listenerCallCount.set(0); // Reset nach erstem Connect

        callback.onConnectionStateChange(deviceA, 0, BluetoothProfile.STATE_CONNECTED);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(0, listenerCallCount.get());
        assertEquals(1, lastGuestList.size()); // unverändert aus erstem Aufruf
    }

    @Test
    public void onDisconnect_selectedGuest_clearsSelectionAndQueue() {
        // Setup: A verbindet sich und wird als aktiver Partner ausgewählt
        callback.onConnectionStateChange(deviceA, 0, BluetoothProfile.STATE_CONNECTED);
        shadowOf(Looper.getMainLooper()).idle();
        bleHost.selectGuest(NetworkDevice.from(deviceA));
        shadowOf(Looper.getMainLooper()).idle();

        listenerCallCount.set(0);
        callback.onConnectionStateChange(deviceA, 0, BluetoothProfile.STATE_DISCONNECTED);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, listenerCallCount.get());
        assertTrue(lastGuestList.isEmpty());
        // sendGameChange darf nach Reset nicht mehr funktionieren (kein aktiver Partner)
        bleHost.sendGameChange(mock(GameAction.class));
        // kein Crash, kein NPE -> selectedGuestDevice wurde korrekt zurückgesetzt
}

    @Test
    public void onDisconnect_nonSelectedGuest_selectionUnaffected() {
        callback.onConnectionStateChange(deviceA, 0, BluetoothProfile.STATE_CONNECTED);
        callback.onConnectionStateChange(deviceB, 0, BluetoothProfile.STATE_CONNECTED);
        shadowOf(Looper.getMainLooper()).idle();
        bleHost.selectGuest(NetworkDevice.from(deviceA));
        shadowOf(Looper.getMainLooper()).idle();

        listenerCallCount.set(0);
        callback.onConnectionStateChange(deviceB, 0, BluetoothProfile.STATE_DISCONNECTED);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, listenerCallCount.get());
        assertEquals(1, lastGuestList.size());
        assertEquals(NetworkDevice.from(deviceA), lastGuestList.getFirst());

        // Indirekter Beweis, dass selectedGuestDevice weiterhin A ist:
        // sendGameChange darf NICHT den "Sending not possible"-Fehlerpfad nehmen.
        NetworkEventListener eventListener = mock(NetworkEventListener.class);
        networkManager.setNetworkEventListener(eventListener);

        bleHost.sendGameChange(mock(GameAction.class));
        shadowOf(Looper.getMainLooper()).idle();

        verify(eventListener, never()).onNetworkEvent(argThat(e ->
                e.message().contains("Sending not possible")));
    }
}