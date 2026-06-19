package kittycards.kittycardsandroid.network;

import java.util.ArrayList;

public interface OnDeviceFoundListener {
    void onDeviceFound(ArrayList<NetworkDevice> updatedRooms);
}