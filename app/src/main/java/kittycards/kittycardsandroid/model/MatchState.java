package kittycards.kittycardsandroid.model;

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
    private Player matchWinner;


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
        this.matchWinner = null;
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
     * Returns the winner of the match.
     *
     * @return the match winner, or {@code null} if the match is not decided yet
     */
    public Player getMatchWinner() {
        return matchWinner;
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
     */
    public boolean isMatchFinished(Player playerOne, Player playerTwo) {
        if (playerOne.getWins() >= WINS_NEEDED) {
            matchWinner = playerOne;
            return true;
        }
        if (playerTwo.getWins() >= WINS_NEEDED) {
            matchWinner = playerTwo;
            return true;
        }

        return currentRound >= MAX_ROUNDS;
    }
}
