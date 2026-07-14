package kittycards.kittycardsandroid.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the overall progress of a match.
 * <p>
 * A match consists of up to three rounds.
 * The match state tracks the current round, the maximum number of rounds,
 * the wins required for victory, and the final match winner.
 * </p>
 *
 * @author JellyMae
 */
public class MatchState {

    private int currentRound;
    private static final int MAX_ROUNDS = 3;
    private static final int WINS_NEEDED = 2;

    private boolean finished;
    private Player matchWinner;
    private final List<RoundResult> roundResults;


    // --- Constructor ---

    /**
     * Creates a new match state.
     * <p>
     * A new match always starts in round one
     * and has no winner initially.
     * </p>
     */
    public MatchState() {
        this.currentRound = 1;
        this.finished = false;
        this.matchWinner = null;
        this.roundResults = new ArrayList<>();
    }


    // --- Getters ---

    /**
     * Returns the current round number.
     *
     * @return the current round
     */
    public int getCurrentRound() {
        return currentRound;
    }

    /**
     * Returns the maximum number of rounds in a match.
     *
     * @return the maximum number of rounds
     */
    public int getMaxRounds() {
        return MAX_ROUNDS;
    }

    /**
     * Returns the number of wins required to win the match.
     *
     * @return the required wins for match victory
     */
    public int getWinsNeeded() {
        return WINS_NEEDED;
    }

    /**
     * Returns whether the match has finished.
     *
     * @return {@code true} if the match has finished
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Returns the winner of the match.
     *
     * @return the match winner, or {@code null} if the match is not decided yet
     */
    public Player getMatchWinner() {
        return matchWinner;
    }

    /**
     * Checks whether the finished match ended in a draw.
     *
     * @return {@code true} if the match is finished without a winner
     */
    public boolean isDraw() {
        return finished && matchWinner == null;
    }

    /**
     * Returns the results of all completed rounds.
     *
     * @return an unmodifiable list of round results
     */
    public List<RoundResult> getRoundResults() {
        return Collections.unmodifiableList(roundResults);
    }


    // --- Operations ---

    /**
     * Advances the match to the next round.
     * <p>
     * The round number only increases if the maximum number of rounds
     * has not been reached yet.
     * </p>
     */
    public void nextRound() {
        if (currentRound < MAX_ROUNDS) {
            currentRound++;
        }
    }

    /**
     * Checks whether the match is finished.
     * <p>
     * A match ends when one player reaches the required number of wins
     * or when the maximum number of rounds has been played.
     * If a player wins by victories, that player is stored as the match winner.
     * </p>
     *
     * @param playerOne the first player
     * @param playerTwo the second player
     * @return {@code true} if the match is finished, otherwise {@code false}
     * @throws NullPointerException if either {@code playerOne} or {@code playerTwo} is {@code null}
     */
    public boolean isMatchFinished(Player playerOne, Player playerTwo) {
        if (playerOne == null || playerTwo == null) {
            throw new NullPointerException("players cannot be null");
        }

        if (finished) {
            return true;
        }

        if (playerOne.getWins() >= WINS_NEEDED) {
            matchWinner = playerOne;
            finished = true;
            return true;
        }

        if (playerTwo.getWins() >= WINS_NEEDED) {
            matchWinner = playerTwo;
            finished = true;
            return true;
        }

        if (currentRound >= MAX_ROUNDS) {
            matchWinner = null;
            finished = true;
            return true;
        }

        return false;
    }

    /**
     * Stores the result of a completed round.
     *
     * @param result the round result
     * @throws NullPointerException     if {@code result} is {@code null}
     * @throws IllegalStateException    if all round results are already stored
     */
    public void addRoundResult(RoundResult result) {
        if (result == null) {
            throw new NullPointerException("round result cannot be null");
        }

        if (roundResults.size() >= MAX_ROUNDS) {
            throw new IllegalStateException("all round results are already stored");
        }

        roundResults.add(result);
    }
}
