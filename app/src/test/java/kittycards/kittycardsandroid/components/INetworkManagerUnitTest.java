package kittycards.kittycardsandroid.components;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import kittycards.kittycardsandroid.network.NetworkDevice;

public class INetworkManagerUnitTest {

    private FakeNetworkManager fake;

    @BeforeEach
    public void setUp() {
        fake = new FakeNetworkManager();
    }


    //HOST MATCH TEST
//    @Test
//    public void hostMatch_discoversDevicesAfterShortAmountOfTime() throws InterruptedException {
//        fake.expectDevices(2);
//
//        fake.hostMatch((updatedRooms) -> {
//            // Hier können Sie die Logik hinzufügen, um die gefundenen Geräte zu verarbeiten
//            // Zum Beispiel könnten Sie die Geräte in einer Liste speichern oder direkt überprüfen
//        });
//        assertTrue(result.isEmpty(), "Expected: No devices discovered initially");
//
//        fake.simulateDeviceFoundAfter(new NetworkDevice("Pixel7", "a"), 1000);
//        fake.simulateDeviceFoundAfter(new NetworkDevice("Galaxy S24", "b"), 3000);
//
//        boolean alleGefunden = fake.awaitDiscovery(5, TimeUnit.SECONDS);
//
//        assertTrue(alleGefunden, "Timeout: Nicht alle Geräte rechtzeitig entdeckt");
//        assertEquals(2, result.size());
//
//    }

    @AfterEach
    public void tearDown() {
        fake.shutdown();
    }
}
