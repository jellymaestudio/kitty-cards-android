package kittycards.kittycardsandroid.network;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothManager;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import dagger.hilt.android.testing.HiltTestApplication;
import kittycards.kittycardsandroid.components.IProtocolEngine;

@RunWith(RobolectricTestRunner.class)
@Config(application = HiltTestApplication.class)
public class NetworkManagerOrchestrationTest {

    private NetworkManager networkManager;
    private BleHost mockBleHost;
    private BleGuest mockBleGuest;
    private BluetoothManager mockBluetoothManager;
    private IProtocolEngine mockProtocolEngine;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        mockBluetoothManager = mock(BluetoothManager.class);
        mockProtocolEngine = mock(IProtocolEngine.class);
        mockBleHost = mock(BleHost.class);
        mockBleGuest = mock(BleGuest.class);

        networkManager = new NetworkManager(
            context,
            mockBluetoothManager,
            mockProtocolEngine,
            mockBleHost,
            mockBleGuest
        );
    }

    @Test
    public void networkManager_delegatesHostMatchToBleHost() {
        OnGuestConnectedListener listener = guests -> {};
        networkManager.hostMatch(listener);
        verify(mockBleHost).hostMatch(listener);
    }

    @Test
    public void networkManager_delegatesJoinMatchToBleGuest() {
        OnDeviceFoundListener listener = devices -> {};
        networkManager.joinMatch(listener);
        verify(mockBleGuest).joinMatch(listener);
    }

    @Test
    public void networkManager_disconnectsHost_whenDisconnectCalled() {
        networkManager.hostMatch(guests -> {});
        networkManager.disconnect();
        verify(mockBleHost).disconnect();
    }

    @Test
    public void networkManager_disconnectsGuest_whenDisconnectCalled() {
        networkManager.joinMatch(devices -> {});
        networkManager.disconnect();
        verify(mockBleGuest).disconnect();
    }

    @Test
    public void networkManager_switchesFromHostToGuest_disconnectsHostFirst() {
        networkManager.hostMatch(guests -> {});
        networkManager.joinMatch(devices -> {});
        
        verify(mockBleHost).disconnect();
        verify(mockBleGuest).joinMatch(any());
    }

    @Test
    public void networkManager_switchesFromGuestToHost_disconnectsGuestFirst() {
        networkManager.joinMatch(devices -> {});
        networkManager.hostMatch(guests -> {});
        
        verify(mockBleGuest).disconnect();
        verify(mockBleHost).hostMatch(any());
    }
}
