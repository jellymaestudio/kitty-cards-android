package kittycards.kittycardsandroid.network;

/**
 * Notifies the active game screen when the remote player disconnects.
 *
 * @author JellyMae
 */
public interface OnGameConnectionListener {

    /**
     * Called when the active remote game partner disconnects.
     */
    void onGamePartnerDisconnected();
}