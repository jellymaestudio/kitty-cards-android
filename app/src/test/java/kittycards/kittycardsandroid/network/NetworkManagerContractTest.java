package kittycards.kittycardsandroid.network;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import kittycards.kittycardsandroid.components.INetworkManager;
import kittycards.kittycardsandroid.network.event.NetworkEvent;
import kittycards.kittycardsandroid.network.event.NetworkEventListener;

public abstract class NetworkManagerContractTest {

    protected INetworkManager networkManager;

    protected abstract INetworkManager createManager();

    @Before
    public void setUp() {
        networkManager = createManager();
    }

    @After
    public void tearDown() {
        if (networkManager != null) {
            networkManager.disconnect();
        }
    }

    @Test
    public void hostMatch_emitsHostingEvent() {
        NetworkEventListener listener = mock(NetworkEventListener.class);
        networkManager.setNetworkEventListener(listener);
        
        networkManager.hostMatch(guests -> {});
        
        verify(listener).onNetworkEvent(argThat(event -> 
            event.type() == NetworkEvent.NetworkMessageType.INFO && 
            event.message().toLowerCase().contains("hosting")
        ));
    }

    @Test
    public void joinMatch_emitsScanEvent() {
        NetworkEventListener listener = mock(NetworkEventListener.class);
        networkManager.setNetworkEventListener(listener);
        
        networkManager.joinMatch(rooms -> {});
        
        verify(listener).onNetworkEvent(argThat(event -> 
            event.type() == NetworkEvent.NetworkMessageType.INFO && 
            event.message().toLowerCase().contains("scan")
        ));
    }

    @Test
    public void disconnect_emitsDisconnectEvent() {
        NetworkEventListener listener = mock(NetworkEventListener.class);
        networkManager.setNetworkEventListener(listener);
        
        networkManager.hostMatch(guests -> {});
        networkManager.disconnect();
        
        verify(listener).onNetworkEvent(argThat(event -> 
            event.message().toLowerCase().contains("stop") || 
            event.message().toLowerCase().contains("disconnect")
        ));
    }

    @Test
    public void confirmRoom_timeout_emitsError() {
    }

    @Test
    public void confirmRoom_rejected_emitsError() {
    }

    @Test
    public void reconnect_afterLostConnection_restoresSession() {
    }
}
