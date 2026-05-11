package kittycats.kittycatsandroid.model;

public class Game {

    private Board board;
    private Player playerOne;
    private Player playerTwo;
    private Player currentPlayer;
    private Card selectedCard;
    private MatchState matchState;
    private GameStatus gameStatus;


    // --- Constructors ---

    public Game(Player playerOne, Player playerTwo) {
        if(playerOne == null || playerTwo == null) {
            throw new NullPointerException("players cannot be null");
        }

        this.board = new Board();
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        chooseStartingPlayer();
        this.selectedCard = null;
        this.matchState = new MatchState();
        this.gameStatus = GameStatus.PAUSED;
    }


    // --- Getters and Setters ---

    public Board getBoard() {
        return board;
    }


    public Player getPlayerOne() {
        return playerOne;
    }

    public Player getPlayerTwo() {
        return playerTwo;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }


    public Card getSelectedCard() {
        return selectedCard;
    }

    public void setSelectedCard(Card selectedCard) {
        this.selectedCard = selectedCard;
    }


    public MatchState getMatchState() {
        return matchState;
    }


    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }


    // --- Operations ---

    public void chooseStartingPlayer() {
        currentPlayer = Math.random() < 0.5
                ? playerOne
                : playerTwo;
    }

}
