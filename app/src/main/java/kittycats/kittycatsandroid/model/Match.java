package kittycats.kittycatsandroid.model;

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



    // --- Constructors ---

    public Match(Player playerOne, Player playerTwo) {
        if(playerOne == null || playerTwo == null) {
            throw new NullPointerException("players cannot be null");
        }

        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.matchState = new MatchState();
        this.matchStatus = MatchStatus.PAUSED;
        prepareNewRound();
    }



    // --- Getters and Setters ---

    public Player getPlayerOne() {
        return playerOne;
    }

    public Player getPlayerTwo() {
        return playerTwo;
    }

    public Player getOtherPlayer(Player player) {
        if(player == playerOne) {
            return playerTwo;
        }
        else {
            return playerOne;
        }
    }

    private Player getNextStartingPlayer() {
        if(matchState.getCurrentRound() == 1) {
            return chooseStartingPlayer();
        }

        if(playerOne.getScore() < playerTwo.getScore()) {
            return playerOne;           // loser starts
        }

        if(playerTwo.getScore() < playerOne.getScore()) {
            return playerTwo;           // loser starts
        }

        return chooseStartingPlayer();  //draw
    }


    public MatchState getMatchState() {
        return matchState;
    }


    public MatchStatus getGameStatus() {
        return matchStatus;
    }

    public void setGameStatus(MatchStatus matchStatus) {
        this.matchStatus = matchStatus;
    }



    // --- Operations ---

    /**
     * Prepares a new round by choosing the next starting player,
     * updating the round winner if necessary, resetting round-specific
     * player data and creating a new game state.
     */
    private void prepareNewRound() {
        Player startingPlayer = getNextStartingPlayer();

        if(matchState.getCurrentRound() > 1) {
            addWinToRoundWinner();
        }

        Player secondPlayer = getOtherPlayer(startingPlayer);

        playerOne.reset();
        playerTwo.reset();

        gameState = new GameState(startingPlayer, secondPlayer);
    }

    private Player chooseStartingPlayer() {
        return Math.random() < 0.5
                ? playerOne
                : playerTwo;
    }


    private void addWinToRoundWinner() {
        if(playerOne.getScore() > playerTwo.getScore()) {
            playerOne.addWin();
        }
        else if(playerTwo.getScore() > playerOne.getScore()) {
            playerTwo.addWin();
        }
    }


    /**
     * Starts the next round if the match is not finished yet.
     * <p>
     * If the match is finished, the game status is set to {@link MatchStatus#FINISHED}.
     */
    public void startNextRound() {
        if(matchState.isMatchFinished(playerOne, playerTwo)) {
            matchStatus = MatchStatus.FINISHED;
            return;
        }

        matchState.nextRound();
        prepareNewRound();
    }
}
