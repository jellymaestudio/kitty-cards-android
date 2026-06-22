package kittycards.kittycardsandroid.network;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

/**
 * Represents a network device with a name and address.
 * @param deviceName Geräte-Name
 * @param deviceAddress Geräte-Adresse
 * @author red_concrete
 */
public record NetworkDevice(String deviceName, String deviceAddress) {

    /**
     * Factory method to create a NetworkDevice from a BluetoothDevice.
     * @param device the BluetoothDevice to convert
     * @return the created NetworkDevice
     */
    @SuppressLint("MissingPermission")
    public static NetworkDevice from(BluetoothDevice device) {
        return new NetworkDevice(device.getName(), device.getAddress());
    }
}
