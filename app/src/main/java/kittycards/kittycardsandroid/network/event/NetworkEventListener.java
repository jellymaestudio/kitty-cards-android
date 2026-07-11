package kittycards.kittycardsandroid.network.event;

/**
 * Interface for listening to network events, such as messages or status updates related to network operations.
 * Implementations of this interface should define the behavior when a network event occurs.
 * <p>
 * Network events are only intended for displaying information; they should not be used for the implementation of actions.
 */
public interface NetworkEventListener {
    /**
     * Called when a network event occurs.
     *
     * @param event the network event that occurred
     */
    void onNetworkEvent(NetworkEvent event);
}