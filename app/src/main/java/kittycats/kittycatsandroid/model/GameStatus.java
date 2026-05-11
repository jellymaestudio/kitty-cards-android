package kittycats.kittycatsandroid.model;

/**
 * Enum for the current status of the game.
 *
 * @author JellyMae
 */
public enum GameStatus {

    /**
     * Says the game is currently running.
     */
    RUNNING,

    /**
     * Says the game is currently paused.
     */
    PAUSED,

    /**
     * Says the game is currently finished.
     */
    FINISHED,

    /**
     * Says the game is currently waiting for a reaction from network.
     */
    WAITING_FOR_NETWORK;


    /**
     *
     */
    GameStatus () {

    }
}
