package kittycats.kittycatsandroid.network;

/**
 *
 * @author red_concrete
 */
public class NetworkDevice {

    private final String deviceName;
    private final String deviceAddress; // MAC-Adresse

    public NetworkDevice(String deviceName, String deviceAddress) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }
}
