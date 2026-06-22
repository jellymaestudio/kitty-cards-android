package kittycards.kittycardsandroid.network;

import java.util.ArrayList;
/**
 * @author red_concrete
 */
public interface OnGuestConnectedListener {
    void onGuestListUpdated(ArrayList<NetworkDevice> connectedGuests);
}
