package kittycards.kittycardsandroid.network.event;

/**
 * Represents a network event e.g. a message or status update related to network operations.
 * <p>
 * Network events are only intended for displaying information; they should not be used for the implementation of actions.
 * @param type the type of the network event (INFO, WARNING, ERROR)
 * @param message the message associated with the network event
 * @param source the source of the network event
 */
public record NetworkEvent(NetworkMessageType type, String message, String source) {

    public enum NetworkMessageType {
        INFO,
        WARNING,
        ERROR
    }
}
