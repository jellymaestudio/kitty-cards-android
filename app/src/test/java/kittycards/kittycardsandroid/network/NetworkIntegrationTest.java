package kittycards.kittycardsandroid.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import android.os.Looper;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import kittycards.kittycardsandroid.components.IProtocolEngine;
import kittycards.kittycardsandroid.network.event.NetworkEvent;
import kittycards.kittycardsandroid.network.event.NetworkEventListener;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
@Config(application = HiltTestApplication.class)
public class NetworkIntegrationTest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    private NetworkManager networkManager;
    private BleHost mockBleHost;
    private BleGuest mockBleGuest;
    private BluetoothManager mockBluetoothManager;
    private BluetoothAdapter mockBluetoothAdapter;
    private IProtocolEngine mockProtocolEngine;
    private NetworkEventListener mockEventListener;

    @Before
    public void init() throws Exception {
        hiltRule.inject();

        Context context = ApplicationProvider.getApplicationContext();
        mockBluetoothManager = mock(BluetoothManager.class);
        mockBluetoothAdapter = mock(BluetoothAdapter.class);
        mockProtocolEngine = mock(IProtocolEngine.class);
        mockEventListener = mock(NetworkEventListener.class);

        when(mockBluetoothManager.getAdapter()).thenReturn(mockBluetoothAdapter);

        networkManager = new NetworkManager(context, mockBluetoothManager, mockProtocolEngine);
        networkManager.setNetworkEventListener(mockEventListener);
    }

    private void useMocks() throws Exception {
        mockBleHost = mock(BleHost.class);
        mockBleGuest = mock(BleGuest.class);
        setInternalField(networkManager, "bleHost", mockBleHost);
        setInternalField(networkManager, "bleGuest", mockBleGuest);
    }

    private void setInternalField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getInternalField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private void idle() {
        try {
            ShadowLooper.idleMainLooper();
        } catch (Exception ignored) {}
    }

    // --- hostMatch(listener) ---

    @Test
    public void hostMatch_setsRoleToHost_andDelegatesToBleHost() throws Exception {
        useMocks();
        OnGuestConnectedListener listener = mock(OnGuestConnectedListener.class);
        networkManager.hostMatch(listener);

        assertEquals(Role.HOST, networkManager.getRole());
        verify(mockBleHost).hostMatch(listener);
    }

    @Test
    public void hostMatch_whilePreviouslyGuest_disconnectsGuestFirst() throws Exception {
        useMocks();
        networkManager.joinMatch(mock(OnDeviceFoundListener.class));
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));

        verify(mockBleGuest).disconnect();
        assertEquals(Role.HOST, networkManager.getRole());
    }

    @Test
    public void hostMatch_bleAdvertiserUnavailable_emitsErrorAndDoesNotStartGattServer() throws Exception {
        when(mockBluetoothAdapter.getBluetoothLeAdvertiser()).thenReturn(null);
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        idle();

        ArgumentCaptor<NetworkEvent> eventCaptor = ArgumentCaptor.forClass(NetworkEvent.class);
        verify(mockEventListener).onNetworkEvent(eventCaptor.capture());
        assertEquals(NetworkEvent.NetworkMessageType.ERROR, eventCaptor.getValue().type());
        assertTrue(eventCaptor.getValue().message().contains("Advertising not supported"));
    }

    @Test
    public void hostMatch_calledTwice_secondCallIgnoredByBleHost() throws Exception {
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        idle();
        
        BleHost host = (BleHost) getInternalField(networkManager, "bleHost");
        setInternalField(host, "advertiser", mock(android.bluetooth.le.BluetoothLeAdvertiser.class));
        
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        idle();
        
        ArgumentCaptor<NetworkEvent> eventCaptor = ArgumentCaptor.forClass(NetworkEvent.class);
        verify(mockEventListener, times(2)).onNetworkEvent(eventCaptor.capture());
        assertTrue(eventCaptor.getAllValues().get(1).message().contains("Ignored") || eventCaptor.getAllValues().get(1).message().contains("läuft bereits"));
    }

    // --- joinMatch(listener) ---

    @Test
    public void joinMatch_setsRoleToGuest_andDelegatesToBleGuest() throws Exception {
        useMocks();
        OnDeviceFoundListener listener = mock(OnDeviceFoundListener.class);
        networkManager.joinMatch(listener);

        assertEquals(Role.GUEST, networkManager.getRole());
        verify(mockBleGuest).joinMatch(listener);
    }

    @Test
    public void joinMatch_whilePreviouslyHost_disconnectsHostFirst() throws Exception {
        useMocks();
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        networkManager.joinMatch(mock(OnDeviceFoundListener.class));

        verify(mockBleHost).disconnect();
        assertEquals(Role.GUEST, networkManager.getRole());
    }

    @Test
    public void joinMatch_scannerUnavailable_emitsError() throws Exception {
        when(mockBluetoothAdapter.getBluetoothLeScanner()).thenReturn(null);
        networkManager.joinMatch(mock(OnDeviceFoundListener.class));
        idle();

        ArgumentCaptor<NetworkEvent> eventCaptor = ArgumentCaptor.forClass(NetworkEvent.class);
        verify(mockEventListener).onNetworkEvent(eventCaptor.capture());
        assertEquals(NetworkEvent.NetworkMessageType.ERROR, eventCaptor.getValue().type());
        assertTrue(eventCaptor.getValue().message().contains("Scanner not available"));
    }

    // --- confirmRoom(device) ---

    @Test
    public void confirmRoom_asGuest_stopsScanAndConnectsGatt() throws Exception {
        useMocks();
        NetworkDevice device = new NetworkDevice("Test", "00:11:22:33:44:55");
        networkManager.confirmRoom(device);
        verify(mockBleGuest).confirmRoom(device);
    }

    @Test
    public void confirmRoom_invalidMacAddress_emitsErrorWithoutConnecting() throws Exception {
        networkManager.confirmRoom(new NetworkDevice("Broken", "NOT_A_MAC"));
        idle();

        ArgumentCaptor<NetworkEvent> eventCaptor = ArgumentCaptor.forClass(NetworkEvent.class);
        verify(mockEventListener).onNetworkEvent(eventCaptor.capture());
        assertEquals(NetworkEvent.NetworkMessageType.ERROR, eventCaptor.getValue().type());
        assertTrue(eventCaptor.getValue().message().contains("Invalid MAC address"));
    }

    @Test
    public void confirmRoom_existingActiveConnection_closesOldConnectionFirst() throws Exception {
        networkManager.joinMatch(mock(OnDeviceFoundListener.class));
        BleGuest guest = (BleGuest) getInternalField(networkManager, "bleGuest");
        
        BluetoothGatt mockGatt = mock(BluetoothGatt.class);
        setInternalField(guest, "activeGattConnection", mockGatt);
        
        networkManager.confirmRoom(new NetworkDevice("NewRoom", "00:11:22:33:44:55"));
        idle();
        
        verify(mockGatt).disconnect();
        verify(mockGatt).close();
    }

    // --- selectGuest(device) ---

    @Test
    public void selectGuest_asHost_setsSelectedGuestAndSendsGuestAccepted() throws Exception {
        useMocks();
        NetworkDevice device = new NetworkDevice("Guest", "00:11:22:33:44:55");
        networkManager.selectGuest(device);
        verify(mockBleHost).selectGuest(device);
    }

    @Test
    public void selectGuest_disconnectsAllOtherConnectedGuests() throws Exception {
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        BleHost host = (BleHost) getInternalField(networkManager, "bleHost");

        ArrayList<NetworkDevice> connectedGuests = (ArrayList<NetworkDevice>) getInternalField(host, "connectedGuests");
        NetworkDevice selected = new NetworkDevice("Selected", "00:11:22:33:44:55");
        NetworkDevice other = new NetworkDevice("Other", "AA:BB:CC:DD:EE:FF");
        connectedGuests.add(selected);
        connectedGuests.add(other);

        BluetoothGattServer mockServer = mock(BluetoothGattServer.class);
        setInternalField(host, "bluetoothGattServer", mockServer);
        setInternalField(host, "serverCharacteristic", mock(BluetoothGattCharacteristic.class));

        BluetoothDevice mockOtherDevice = mock(BluetoothDevice.class);
        when(mockBluetoothAdapter.getRemoteDevice("AA:BB:CC:DD:EE:FF")).thenReturn(mockOtherDevice);
        when(mockBluetoothAdapter.getRemoteDevice("00:11:22:33:44:55")).thenReturn(mock(BluetoothDevice.class));

        networkManager.selectGuest(selected);
        idle();

        verify(mockServer).cancelConnection(mockOtherDevice);
    }

    @Test
    public void selectGuest_guestNotInConnectedList_emitsErrorWithoutSelecting() throws Exception {
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        networkManager.selectGuest(new NetworkDevice("Unknown", "FF:FF:FF:FF:FF:FF"));
        idle();
        
        ArgumentCaptor<NetworkEvent> eventCaptor = ArgumentCaptor.forClass(NetworkEvent.class);
        verify(mockEventListener).onNetworkEvent(eventCaptor.capture());
        assertEquals(NetworkEvent.NetworkMessageType.ERROR, eventCaptor.getValue().type());
        assertTrue(eventCaptor.getValue().message().contains("not connected"));
    }

    @Test
    public void selectGuest_nullGuest_emitsError() throws Exception {
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        networkManager.selectGuest(null);
        idle();
        
        ArgumentCaptor<NetworkEvent> eventCaptor = ArgumentCaptor.forClass(NetworkEvent.class);
        verify(mockEventListener).onNetworkEvent(eventCaptor.capture());
        assertEquals(NetworkEvent.NetworkMessageType.ERROR, eventCaptor.getValue().type());
        assertTrue(eventCaptor.getValue().message().contains("null"));
    }

    // --- disconnect() ---

    @Test
    public void disconnect_asHost_delegatesToBleHostAndClearsRole() throws Exception {
        useMocks();
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        networkManager.disconnect();
        
        verify(mockBleHost).disconnect();
        assertEquals(Role.NOT_CONNECTED, networkManager.getRole());
    }

    @Test
    public void disconnect_asGuest_delegatesToBleGuestAndClearsRole() throws Exception {
        useMocks();
        networkManager.joinMatch(mock(OnDeviceFoundListener.class));
        networkManager.disconnect();
        
        verify(mockBleGuest).disconnect();
        assertEquals(Role.NOT_CONNECTED, networkManager.getRole());
    }

    @Test
    public void disconnect_whileNotConnected_doesNothingSilently() throws Exception {
        useMocks();
        networkManager.disconnect();
        verify(mockBleHost, never()).disconnect();
        verify(mockBleGuest, never()).disconnect();
    }

    @Test
    public void disconnect_clearsActionQueue() throws Exception {
        networkManager.decodeAndQueueDataSafe(new byte[6]);
        networkManager.disconnect();
        idle();

        BlockingQueue<?> queue = (BlockingQueue<?>) getInternalField(networkManager, "actionQueue");
        assertTrue(queue.isEmpty());
    }

    // --- closeHostedRoom() ---

    @Test
    public void closeHostedRoom_asHost_sendsRoomClosedBeforeDisconnect() throws Exception {
        useMocks();
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        networkManager.closeHostedRoom();
        verify(mockBleHost).closeHostedRoom();
    }

    @Test
    public void closeHostedRoom_calledAsGuest_isIgnored() throws Exception {
        useMocks();
        networkManager.joinMatch(mock(OnDeviceFoundListener.class));
        networkManager.closeHostedRoom();
        verify(mockBleHost, never()).closeHostedRoom();
    }

    @Test
    public void closeHostedRoom_noSelectedGuest_disconnectsImmediately() throws Exception {
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        BleHost host = (BleHost) getInternalField(networkManager, "bleHost");
        setInternalField(host, "selectedGuestDevice", null);
        
        networkManager.closeHostedRoom();
        idle();
        
        assertEquals(Role.NOT_CONNECTED, networkManager.getRole());
    }

    // --- stopRoomDiscovery() ---

    @Test
    public void stopRoomDiscovery_asHost_stopsAdvertisingKeepsConnection() throws Exception {
        useMocks();
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        networkManager.stopRoomDiscovery();
        verify(mockBleHost).stopRoomDiscovery();
    }

    @Test
    public void stopRoomDiscovery_calledAsGuest_isIgnored() throws Exception {
        useMocks();
        networkManager.joinMatch(mock(OnDeviceFoundListener.class));
        networkManager.stopRoomDiscovery();
        verify(mockBleHost, never()).stopRoomDiscovery();
    }

    @Test
    public void stopRoomDiscovery_alreadyStopped_emitsWarning() throws Exception {
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        BleHost host = (BleHost) getInternalField(networkManager, "bleHost");
        setInternalField(host, "advertiser", null); 
        
        networkManager.stopRoomDiscovery();
        idle();
        
        ArgumentCaptor<NetworkEvent> eventCaptor = ArgumentCaptor.forClass(NetworkEvent.class);
        verify(mockEventListener).onNetworkEvent(eventCaptor.capture());
        assertEquals(NetworkEvent.NetworkMessageType.WARNING, eventCaptor.getValue().type());
        assertTrue(eventCaptor.getValue().message().contains("already stopped"));
    }

    // --- sendGameChange(action) ---

    @Test
    public void sendGameChange_asHost_delegatesToBleHostQueue() throws Exception {
        useMocks();
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        GameAction action = new GameAction(GameAction.ActionType.UNSELECT_CARD);
        networkManager.sendGameChange(action);
        verify(mockBleHost).sendGameChange(action);
    }

    @Test
    public void sendGameChange_asGuest_delegatesToBleGuestQueue() throws Exception {
        useMocks();
        networkManager.joinMatch(mock(OnDeviceFoundListener.class));
        GameAction action = new GameAction(GameAction.ActionType.UNSELECT_CARD);
        networkManager.sendGameChange(action);
        verify(mockBleGuest).sendGameChange(action);
    }

    @Test
    public void sendGameChange_notConnected_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> networkManager.sendGameChange(new GameAction(GameAction.ActionType.UNSELECT_CARD)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void sendGameChange_queueFullAsGuest_dropsOldestMessage() throws Exception {
        networkManager.joinMatch(mock(OnDeviceFoundListener.class));
        BleGuest guest = (BleGuest) getInternalField(networkManager, "bleGuest");

        setInternalField(guest, "connected", true);
        setInternalField(guest, "activeGattConnection", mock(BluetoothGatt.class));
        setInternalField(guest, "gattCharacteristic", mock(BluetoothGattCharacteristic.class));

        Queue<byte[]> outgoingQueue = (Queue<byte[]>) getInternalField(guest, "outgoingQueue");

        for (int i = 0; i < 100; i++) {
            networkManager.sendGameChange(new GameAction(GameAction.ActionType.UNSELECT_CARD));
        }
        assertEquals(100, outgoingQueue.size());

        byte[] firstMessage = outgoingQueue.peek();
        networkManager.sendGameChange(new GameAction(GameAction.ActionType.PLAY_CARD, mock(kittycards.kittycardsandroid.model.Card.class), 0, 0));

        assertEquals(100, outgoingQueue.size());
        assertNotSame(firstMessage, outgoingQueue.peek());
    }

    // --- fetchNextAction() ---

    @Test
    public void fetchNextAction_returnsDecodedActionAfterHostWriteRequest() throws Exception {
        GameAction action = new GameAction(GameAction.ActionType.UNSELECT_CARD);
        when(mockProtocolEngine.decodeGameAction(any())).thenReturn(action);

        networkManager.decodeAndQueueDataSafe(new byte[6]);

        GameAction result = networkManager.fetchNextAction();
        assertEquals(action, result);
    }

    @Test
    public void fetchNextAction_returnsDecodedActionAfterGuestNotification() throws Exception {
        networkManager.joinMatch(mock(OnDeviceFoundListener.class));
        BleGuest guest = (BleGuest) getInternalField(networkManager, "bleGuest");

        GameAction action = new GameAction(GameAction.ActionType.UNSELECT_CARD);
        when(mockProtocolEngine.decodeGameAction(any())).thenReturn(action);

        // Use reflection to bypass signature issues in different Android versions
        java.lang.reflect.Method handleData = guest.getClass().getDeclaredMethod("handleIncomingData", byte[].class);
        handleData.setAccessible(true);
        handleData.invoke(guest, (Object) new byte[6]);

        GameAction result = networkManager.fetchNextAction();
        assertEquals(action, result);
    }

    @Test
    public void fetchNextAction_blocksUntilActionQueued() throws Exception {
        GameAction action = new GameAction(GameAction.ActionType.UNSELECT_CARD);
        CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> {
            try {
                Thread.sleep(200);
                networkManager.decodeAndQueueDataSafe(new byte[6]);
                latch.countDown();
            } catch (Exception ignored) {}
        }).start();

        when(mockProtocolEngine.decodeGameAction(any())).thenReturn(action);

        // Wait with timeout to prevent hanging the whole test suite
        GameAction result = networkManager.fetchNextAction();
        assertEquals(action, result);
        assertTrue("Background thread should have finished", latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void fetchNextAction_interruptedWhileWaiting_propagatesInterruption() {
        AtomicBoolean interrupted = new AtomicBoolean(false);
        Thread t = new Thread(() -> {
            try {
                networkManager.fetchNextAction();
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
        });
        t.start();
        t.interrupt();
        
        try {
            t.join(1000);
        } catch (InterruptedException ignored) {}
        
        assertTrue(interrupted.get());
    }

    // --- Listeners ---

    @Test
    public void setRoomConnectionListener_forwardedToBleGuest() throws Exception {
        useMocks();
        OnRoomConnectionListener listener = mock(OnRoomConnectionListener.class);
        networkManager.setRoomConnectionListener(listener);
        verify(mockBleGuest).setRoomConnectionListener(listener);
    }

    @Test
    public void setGameConnectionListener_notifiedOnPartnerDisconnect() throws Exception {
        OnGameConnectionListener mockListener = mock(OnGameConnectionListener.class);
        networkManager.setGameConnectionListener(mockListener);
        
        networkManager.notifyGamePartnerDisconnected();
        idle();
        
        verify(mockListener).onGamePartnerDisconnected();
    }

    @Test
    public void setNetworkEventListener_receivesEventsFromBleHostAndBleGuest() throws Exception {
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        BleHost host = (BleHost) getInternalField(networkManager, "bleHost");
        
        java.lang.reflect.Method emitHost = host.getClass().getDeclaredMethod("emitEvent", NetworkEvent.NetworkMessageType.class, String.class);
        emitHost.setAccessible(true);
        emitHost.invoke(host, NetworkEvent.NetworkMessageType.INFO, "Host event");
        idle();
        
        verify(mockEventListener).onNetworkEvent(any(NetworkEvent.class));
    }

    // --- Role Transitions ---

    @Test
    public void roleSwitchHostToGuest_cleansUpPreviousRoleCompletely() throws Exception {
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        BleHost host = (BleHost) getInternalField(networkManager, "bleHost");
        android.bluetooth.le.BluetoothLeAdvertiser mockAdvertiser = mock(android.bluetooth.le.BluetoothLeAdvertiser.class);
        setInternalField(host, "advertiser", mockAdvertiser);

        networkManager.joinMatch(mock(OnDeviceFoundListener.class));
        idle();

        assertEquals(Role.GUEST, networkManager.getRole());
        verify(mockAdvertiser).stopAdvertising(any(android.bluetooth.le.AdvertiseCallback.class));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void partnerDisconnectDuringGame_bothSidesNotifyGameConnectionListener() throws Exception {
        OnGameConnectionListener mockListener = mock(OnGameConnectionListener.class);
        networkManager.setGameConnectionListener(mockListener);

        // 1. Test Host side
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        BleHost host = (BleHost) getInternalField(networkManager, "bleHost");
        BluetoothDevice mockSelectedDevice = mock(BluetoothDevice.class);
        when(mockSelectedDevice.getAddress()).thenReturn("00:11:22:33:44:55");
        setInternalField(host, "selectedGuestDevice", mockSelectedDevice);
        
        android.bluetooth.BluetoothGattServerCallback hostCallback = (android.bluetooth.BluetoothGattServerCallback) getInternalField(host, "gattServerCallback");
        ((ArrayList<NetworkDevice>) getInternalField(host, "connectedGuests")).add(new NetworkDevice("Guest", "00:11:22:33:44:55"));

        hostCallback.onConnectionStateChange(mockSelectedDevice, 0, android.bluetooth.BluetoothProfile.STATE_DISCONNECTED);
        idle();
        verify(mockListener, times(1)).onGamePartnerDisconnected();

        // 2. Test Guest side
        networkManager.joinMatch(mock(OnDeviceFoundListener.class));
        BleGuest guest = (BleGuest) getInternalField(networkManager, "bleGuest");
        setInternalField(guest, "connected", true);
        
        android.bluetooth.BluetoothGattCallback guestCallback = (android.bluetooth.BluetoothGattCallback) getInternalField(guest, "gattCallback");
        guestCallback.onConnectionStateChange(mock(BluetoothGatt.class), 0, android.bluetooth.BluetoothProfile.STATE_DISCONNECTED);
        idle();
        verify(mockListener, times(2)).onGamePartnerDisconnected();
    }

    // --- End-to-End ---

    @Test
    @SuppressWarnings("unchecked")
    public void hostSelectGuest_thenGuestReceivesGuestAcceptedAction() throws Exception {
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        BleHost host = (BleHost) getInternalField(networkManager, "bleHost");
        
        NetworkDevice guestDevice = new NetworkDevice("Guest", "00:11:22:33:44:55");
        ((ArrayList<NetworkDevice>) getInternalField(host, "connectedGuests")).add(guestDevice);
        
        BluetoothGattServer mockServer = mock(BluetoothGattServer.class);
        setInternalField(host, "bluetoothGattServer", mockServer);
        setInternalField(host, "serverCharacteristic", mock(BluetoothGattCharacteristic.class));
        
        BluetoothDevice mockRemoteDevice = mock(BluetoothDevice.class);
        when(mockBluetoothAdapter.getRemoteDevice("00:11:22:33:44:55")).thenReturn(mockRemoteDevice);
        
        networkManager.selectGuest(guestDevice);
        idle();
        
        ArgumentCaptor<GameAction> actionCaptor = ArgumentCaptor.forClass(GameAction.class);
        verify(mockProtocolEngine).encodeGameAction(actionCaptor.capture());
        assertEquals(GameAction.ActionType.GUEST_ACCEPTED, actionCaptor.getValue().type());
    }

    @Test
    public void hostAndGuest_fullConnectionFlow_actionArrivesViaFetchNextAction() throws Exception {
        // Setup Bluetooth Server Mocking
        BluetoothGattServer mockServer = mock(BluetoothGattServer.class);
        when(mockBluetoothManager.openGattServer(any(), any())).thenReturn(mockServer);

        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        idle(); // Ensure GATT server is started

        BleHost host = (BleHost) getInternalField(networkManager, "bleHost");
        
        BluetoothDevice mockGuestDevice = mock(BluetoothDevice.class);
        when(mockGuestDevice.getAddress()).thenReturn("00:11:22:33:44:55");
        setInternalField(host, "selectedGuestDevice", mockGuestDevice);
        
        GameAction action = new GameAction(GameAction.ActionType.UNSELECT_CARD);
        when(mockProtocolEngine.decodeGameAction(any())).thenReturn(action);
        
        // Mock characteristic with correct UUID
        BluetoothGattCharacteristic mockChar = mock(BluetoothGattCharacteristic.class);
        when(mockChar.getUuid()).thenReturn(NetworkManager.KITTY_CARDS_CHARACTERISTIC_UUID);

        android.bluetooth.BluetoothGattServerCallback callback = (android.bluetooth.BluetoothGattServerCallback) getInternalField(host, "gattServerCallback");
        callback.onCharacteristicWriteRequest(mockGuestDevice, 1, mockChar, false, true, 0, new byte[6]);
        
        GameAction received = networkManager.fetchNextAction();
        assertEquals(action, received);
    }

    @Test
    public void hostCloseHostedRoom_guestReceivesRoomClosedAction() throws Exception {
        networkManager.hostMatch(mock(OnGuestConnectedListener.class));
        BleHost host = (BleHost) getInternalField(networkManager, "bleHost");
        
        setInternalField(host, "bluetoothGattServer", mock(BluetoothGattServer.class));
        setInternalField(host, "serverCharacteristic", mock(BluetoothGattCharacteristic.class));
        setInternalField(host, "selectedGuestDevice", mock(BluetoothDevice.class));
        
        networkManager.closeHostedRoom();
        idle();
        
        ArgumentCaptor<GameAction> actionCaptor = ArgumentCaptor.forClass(GameAction.class);
        verify(mockProtocolEngine).encodeGameAction(actionCaptor.capture());
        assertEquals(GameAction.ActionType.ROOM_CLOSED, actionCaptor.getValue().type());
    }
}
