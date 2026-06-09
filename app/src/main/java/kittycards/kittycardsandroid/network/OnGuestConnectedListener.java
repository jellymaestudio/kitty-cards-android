package kittycards.kittycardsandroid.network;

import java.util.ArrayList;

public interface OnGuestConnectedListener {
    void onGuestListUpdated(ArrayList<NetworkDevice> connectedGuests);
}
