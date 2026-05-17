package kittycats.kittycatsandroid.model;

public class GameState {

    private final Board board;
    private final Player startingPlayer;
    private final Player secondPlayer;
    private Player currentPlayer;



    // --- Constructors ---

    public GameState(Player startingPlayer, Player secondPlayer) {
        this.board = new Board();
        this.startingPlayer = startingPlayer;
        this.secondPlayer = secondPlayer;
        this.currentPlayer = startingPlayer;
    }



    // --- Getters and Setters ---

    public Board getBoard() {
        return board;
    }


    public Player getStartingPlayer() {
        return startingPlayer;
    }

    public Player getSecondPlayer() {
        return secondPlayer;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }


    public boolean isGameOver() {
        return board.isFull();
    }
}
