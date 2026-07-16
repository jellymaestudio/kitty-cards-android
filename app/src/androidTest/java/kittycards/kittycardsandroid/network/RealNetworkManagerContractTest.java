package kittycards.kittycardsandroid.network;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.runner.RunWith;

import kittycards.kittycardsandroid.components.INetworkManager;
import kittycards.kittycardsandroid.network.ProtocolEngine;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class RealNetworkManagerContractTest extends NetworkManagerContractTest {

    @Override
    protected INetworkManager createManager() {
        // Direct construction for the contract test to avoid Hilt complexity if possible,
        // or we could use Hilt if it's already set up for androidTest.
        return new NetworkManager(
            InstrumentationRegistry.getInstrumentation().getTargetContext(),
            new ProtocolEngine()
        );
    }
    
    // Some tests might require BLE and thus won't work on all devices.
    // We override them here to add @Ignore for the real implementation
    // until we have a way to simulate GATT errors on real hardware.

    @org.junit.Ignore("GATT timeout simulation not yet implemented for real BLE")
    @Override
    public void confirmRoom_timeout_emitsError() {
        super.confirmRoom_timeout_emitsError();
    }

    @org.junit.Ignore("GATT rejection simulation not yet implemented for real BLE")
    @Override
    public void confirmRoom_rejected_emitsError() {
        super.confirmRoom_rejected_emitsError();
    }

    @org.junit.Ignore("GATT connection loss simulation not yet implemented for real BLE")
    @Override
    public void reconnect_afterLostConnection_restoresSession() {
        super.reconnect_afterLostConnection_restoresSession();
    }
}
