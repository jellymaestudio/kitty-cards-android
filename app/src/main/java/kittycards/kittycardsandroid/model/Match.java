package kittycards.kittycardsandroid.model;

import java.util.List;

/**
 * Represents a complete match between two players.
 * <p>
 * A match consists of multiple rounds and stores the current game state,
 * the players, the match state and the current game status.
 *
 * @author JellyMae
 */
public class Match {

    private final Player playerOne;
    private final Player playerTwo;
    private final MatchState matchState;
    private MatchStatus matchStatus;
    private GameState gameState;


    // --- Constructor ---

    /**
     * Creates a new match between two players.
     * <p>
     * A new match initializes both players, creates a new match state,
     * starts in paused mode, and prepares the first round.
     * </p>
     *
     * @param playerOne the first player
     * @param playerTwo the second player
     * @throws NullPointerException if one or both players are {@code null}
     */
    public Match(Player playerOne, Player playerTwo) {
        if (playerOne == null || playerTwo == null) {
            throw new NullPointerException("players cannot be null");
        }

        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.matchState = new MatchState();
        this.matchStatus = MatchStatus.PAUSED;
        prepareNewRound();
    }


    // --- Getters and Setters ---

    /**
     * Returns the first player of the match.
     *
     * @return the first player
     */
    public Player getPlayerOne() {
        return playerOne;
    }

    /**
     * Returns the second player of the match.
     *
     * @return the second player
     */
    public Player getPlayerTwo() {
        return playerTwo;
    }

    /**
     * Returns the opposing player of the given player.
     *
     * @param player the reference player
     * @return the other player in the match
     * @throws NullPointerException     if {@code player} is {@code null}
     * @throws IllegalArgumentException if {@code player} is not part of this match
     */
    public Player getOtherPlayer(Player player) {
        if (player == null) {
            throw new NullPointerException("player cannot be null");
        }
        if (player != playerOne && player != playerTwo) {
            throw new IllegalArgumentException("player is not part of this match");
        }

        if (player == playerOne) {
            return playerTwo;
        } else {
            return playerOne;
        }
    }

    private Player getNextStartingPlayer() {
        if (matchState.getCurrentRound() == 1) {
            return randomStartingPlayer();
        }

        if (playerOne.getScore() < playerTwo.getScore()) {
            return playerOne;           // loser starts
        }

        if (playerTwo.getScore() < playerOne.getScore()) {
            return playerTwo;           // loser starts
        }

        return randomStartingPlayer();  //draw
    }

    /**
     * Returns the current match state.
     *
     * @return the match state
     */
    public MatchState getMatchState() {
        return matchState;
    }

    /**
     * Returns the current status of the match.
     *
     * @return the match status
     */
    public MatchStatus getMatchStatus() {
        return matchStatus;
    }

    /**
     * Sets the current status of the match.
     *
     * @param matchStatus the new match status
     * @throws NullPointerException if {@code matchStatus} is {@code null}
     */
    public void setMatchStatus(MatchStatus matchStatus) {
        if (matchStatus == null) {
            throw new NullPointerException("matchStatus cannot be null");
        }

        this.matchStatus = matchStatus;
    }

    /**
     * Returns the current game state of the active round.
     *
     * @return the current game state
     */
    public GameState getGameState() {
        return gameState;
    }


    // --- Match Management ---

    /**
     * Prepares a new round.
     * <p>
     * Chooses the next starting player, updates the round winner if
     * necessary, resets round-specific player data and creates a new
     * game state.
     * </p>
     */
    private void prepareNewRound() {
        Player startingPlayer = getNextStartingPlayer();
        Player secondPlayer = getOtherPlayer(startingPlayer);

        playerOne.reset();
        playerTwo.reset();

        gameState = new GameState(startingPlayer, secondPlayer);
    }

    /**
     * Prepares a new round using the provided board colors.
     * <p>
     * Chooses the given Player as the next starting player, updates the round
     * winner if necessary, resets round-specific player data and creates a new
     * game state with the given field colors.
     * </p>
     *
     * @param fieldColors    the colors used to initialize the new board
     * @param startingPlayer the player that starts the next round
     */
    private void prepareNewRound(List<GameColor> fieldColors, Player startingPlayer) {
        Player secondPlayer = getOtherPlayer(startingPlayer);

        playerOne.reset();
        playerTwo.reset();

        gameState = new GameState(startingPlayer, secondPlayer, fieldColors);
    }

    /**
     * Finishes the current round and the match.
     */
    public void finishMatch() {
        if (matchStatus == MatchStatus.FINISHED) {
            return;
        }

        finishCurrentRound();
        matchState.isMatchFinished(playerOne, playerTwo);
        matchStatus = MatchStatus.FINISHED;
    }

    /**
     * Randomly selects one of the two players as the starting player.
     *
     * @return the randomly chosen starting player
     */
    private Player randomStartingPlayer() {
        return Math.random() < 0.5
                ? playerOne
                : playerTwo;
    }

    private void finishCurrentRound() {
        RoundResult result;

        if (playerOne.getScore() > playerTwo.getScore()) {
            playerOne.addWin();
            result = RoundResult.PLAYER_ONE_WIN;
        }
        else if (playerTwo.getScore() > playerOne.getScore()) {
            playerTwo.addWin();
            result = RoundResult.PLAYER_TWO_WIN;
        }
        else {
            result = RoundResult.DRAW;
        }

        matchState.addRoundResult(result);
    }

    /**
     * Starts the next round if the match is not finished yet.
     * <p>
     * If the match has already ended, the status is set to {@link MatchStatus#FINISHED}.
     * Otherwise, the round counter increases and a new round is prepared.
     * </p>
     */
    public void startNextRound() {
        finishCurrentRound();

        if (matchState.isMatchFinished(playerOne, playerTwo)) {
            matchStatus = MatchStatus.FINISHED;
            return;
        }

        matchState.nextRound();
        prepareNewRound();
    }

    /**
     * Starts the next round if the match is not finished yet.
     * <p>
     * If the match has already ended, the status is set to {@link MatchStatus#FINISHED}.
     * Otherwise, the round counter increases and a new round is prepared using the
     * provided board colors.
     * </p>
     *
     * @param fieldColors    the colors used to initialize the new board
     * @param startingPlayer the player that starts the next round
     */
    public void startNextRound(List<GameColor> fieldColors, Player startingPlayer) {
        finishCurrentRound();

        if (matchState.isMatchFinished(playerOne, playerTwo)) {
            matchStatus = MatchStatus.FINISHED;
            return;
        }

        matchState.nextRound();
        prepareNewRound(fieldColors, startingPlayer);
    }


    public void initializeCurrentRound(List<GameColor> fieldColors, Player startingPlayer) {
        if (fieldColors == null) {
            throw new NullPointerException("fieldColors cannot be null");
        }
        if (startingPlayer == null) {
            throw new NullPointerException("startingPlayer cannot be null");
        }

        Player secondPlayer = getOtherPlayer(startingPlayer);

        playerOne.reset();
        playerTwo.reset();

        gameState = new GameState(startingPlayer, secondPlayer, fieldColors);
    }
}
