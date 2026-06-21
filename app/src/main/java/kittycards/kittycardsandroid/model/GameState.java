package kittycards.kittycardsandroid.model;

import java.util.List;

/**
 * Represents the current state of a single round.
 * <p>
 * A game state contains the board, both players of the round,
 * the starting player, and the player whose turn is currently active.
 * </p>
 *
 * @author JellyMae
 */
public class GameState {

    private final Board board;
    private final Player startingPlayer;
    private final Player secondPlayer;
    private Player currentPlayer;


    /**
     * Creates a new game state for a round.
     *
     * @param startingPlayer the player who starts the round
     * @param secondPlayer   the second player
     * @throws NullPointerException if one of the players is {@code null}
     */
    public GameState(Player startingPlayer, Player secondPlayer) {
        if (startingPlayer == null || secondPlayer == null) {
            throw new NullPointerException("players cannot be null");
        }

        this.board = new Board();
        this.startingPlayer = startingPlayer;
        this.secondPlayer = secondPlayer;
        this.currentPlayer = startingPlayer;
    }

    public GameState(Player startingPlayer, Player secondPlayer, List<GameColor> fieldColors) {
        if (startingPlayer == null || secondPlayer == null) {
            throw new NullPointerException("players cannot be null");
        }

        this.board = new Board(fieldColors);
        this.startingPlayer = startingPlayer;
        this.secondPlayer = secondPlayer;
        this.currentPlayer = startingPlayer;
    }


    /**
     * Returns the board of the current round.
     *
     * @return the game board
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Returns the player who starts this round.
     *
     * @return the starting player
     */
    public Player getStartingPlayer() {
        return startingPlayer;
    }

    /**
     * Returns the second player of this round.
     *
     * @return the second player
     */
    public Player getSecondPlayer() {
        return secondPlayer;
    }

    /**
     * Returns the player whose turn is currently active.
     *
     * @return the current player
     */
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Sets the current active player.
     *
     * @param currentPlayer the new current player
     * @throws NullPointerException if {@code currentPlayer} is {@code null}
     * @throws IllegalArgumentException if {@code currentPlayer} is not part of this match
     */
    public void setCurrentPlayer(Player currentPlayer) {
        if (currentPlayer == null) {
            throw new NullPointerException("current player cannot be null");
        }
        if (currentPlayer != startingPlayer && currentPlayer != secondPlayer) {
            throw new IllegalArgumentException("player is not part of this match");
        }

        this.currentPlayer = currentPlayer;
    }

    /**
     * Checks whether the current round is over.
     * <p>
     * A round ends when all playable board fields are occupied.
     * </p>
     *
     * @return {@code true} if the round is over, otherwise {@code false}
     */
    public boolean isGameOver() {
        return board.isFull();
    }
}
