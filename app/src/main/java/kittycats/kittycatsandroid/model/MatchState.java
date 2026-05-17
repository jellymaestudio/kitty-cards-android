package kittycats.kittycatsandroid.model;

public class MatchState {

    private int currentRound;
    private static final int MAX_ROUNDS = 3;
    private static final int WINS_NEEDED = 2;
    private Player matchWinner;



    // --- Constructors ---

    public MatchState() {
        this.currentRound = 1;
        this.matchWinner = null;
    }



    // --- Getters and Setters ---

    public int getCurrentRound() {
        return currentRound;
    }

    public int getMaxRounds() {
        return MAX_ROUNDS;
    }

    public int getWinsNeeded() {
        return WINS_NEEDED;
    }

    public Player getMatchWinner() {
        return matchWinner;
    }



    // --- Operations ---

    public void nextRound() {
        if(currentRound < MAX_ROUNDS) {
            currentRound++;
        }
    }

    public boolean isMatchFinished(Player playerOne, Player playerTwo) {
        if(playerOne.getWins() >= WINS_NEEDED) {
            matchWinner = playerOne;
            return true;
        }
        if(playerTwo.getWins() >= WINS_NEEDED) {
            matchWinner = playerTwo;
            return true;
        }

        return currentRound >= MAX_ROUNDS;
    }
}
