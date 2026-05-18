package kittycats.kittycatsandroid.model;

/**
 * Represents the current status of a match.
 * <p>
 * RUNNING: The match is currently active. <br>
 * PAUSED: The match is temporarily paused. <br>
 * FINISHED: The match has ended. <br>
 * WAITING_FOR_NETWORK: The match is waiting for network communication.
 * </p>
 *
 * @author JellyMae
 */
public enum MatchStatus {
    RUNNING,
    PAUSED,
    FINISHED,
    WAITING_FOR_NETWORK
}
