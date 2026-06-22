package kittycards.kittycardsandroid.network;

import java.util.ArrayList;
/**
 * @author red_concrete
 */
public interface OnDeviceFoundListener {
    void onDeviceFound(ArrayList<NetworkDevice> updatedRooms);
}