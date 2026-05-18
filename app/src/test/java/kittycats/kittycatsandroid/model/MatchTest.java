package kittycats.kittycatsandroid.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class MatchTest {

    @Test
    public void constructorShouldSetPlayerOne() {
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        Match match = new Match(playerOne, playerTwo);

        assertEquals(playerOne, match.getPlayerOne());
    }

    @Test
    public void constructorShouldSetPlayerTwo() {
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        Match match = new Match(playerOne, playerTwo);

        assertEquals(playerTwo, match.getPlayerTwo());
    }

    @Test
    public void constructorShouldCreateMatchState() {
        Match match = createMatch();

        assertNotNull(match.getMatchState());
    }

    @Test
    public void constructorShouldCreateGameState() {
        Match match = createMatch();

        assertNotNull(match.getGameState());
    }

    @Test
    public void constructorShouldSetMatchStatusToPaused() {
        Match match = createMatch();

        assertEquals(MatchStatus.PAUSED, match.getMatchStatus());
    }

    @Test
    public void constructorShouldThrowExceptionIfPlayerOneIsNull() {
        Player playerTwo = new Player(2, "Player Two");

        assertThrows(NullPointerException.class, () -> new Match(null, playerTwo));
    }

    @Test
    public void constructorShouldThrowExceptionIfPlayerTwoIsNull() {
        Player playerOne = new Player(1, "Player One");

        assertThrows(NullPointerException.class, () -> new Match(playerOne, null));
    }

    @Test
    public void getOtherPlayerShouldReturnPlayerTwoIfPlayerOneIsGiven() {
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");
        Match match = new Match(playerOne, playerTwo);

        assertEquals(playerTwo, match.getOtherPlayer(playerOne));
    }

    @Test
    public void getOtherPlayerShouldReturnPlayerOneIfPlayerTwoIsGiven() {
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");
        Match match = new Match(playerOne, playerTwo);

        assertEquals(playerOne, match.getOtherPlayer(playerTwo));
    }

    @Test
    public void getOtherPlayerShouldThrowExceptionIfPlayerIsNull() {
        Match match = createMatch();

        assertThrows(NullPointerException.class, () -> match.getOtherPlayer(null));
    }

    @Test
    public void getOtherPlayerShouldThrowExceptionIfPlayerIsNotPartOfMatch() {
        Match match = createMatch();
        Player otherPlayer = new Player(3, "Other Player");

        assertThrows(IllegalArgumentException.class, () -> match.getOtherPlayer(otherPlayer));
    }

    @Test
    public void setMatchStatusShouldChangeMatchStatus() {
        Match match = createMatch();

        match.setMatchStatus(MatchStatus.RUNNING);

        assertEquals(MatchStatus.RUNNING, match.getMatchStatus());
    }

    @Test
    public void setMatchStatusShouldThrowExceptionIfMatchStatusIsNull() {
        Match match = createMatch();

        assertThrows(NullPointerException.class, () -> match.setMatchStatus(null));
    }

    @Test
    public void startNextRoundShouldIncreaseCurrentRoundIfMatchIsNotFinished() {
        Match match = createMatch();

        match.startNextRound();

        assertEquals(2, match.getMatchState().getCurrentRound());
    }

    @Test
    public void startNextRoundShouldCreateNewGameStateIfMatchIsNotFinished() {
        Match match = createMatch();
        GameState oldGameState = match.getGameState();

        match.startNextRound();

        assertNotSame(oldGameState, match.getGameState());
    }

    @Test
    public void startNextRoundShouldResetPlayerRoundData() {
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");
        Match match = new Match(playerOne, playerTwo);

        Card card = new Card(GameColor.RED, 3);
        playerOne.addCard(card);
        playerOne.selectCard(card);
        playerOne.addScore(5);

        match.startNextRound();

        assertTrue(playerOne.getHandCards().isEmpty());
        assertNull(playerOne.getSelectedCard());
        assertEquals(0, playerOne.getScore());
    }

    @Test
    public void startNextRoundShouldAddWinToPlayerOneIfPlayerOneHasHigherScore() {
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");
        Match match = new Match(playerOne, playerTwo);

        playerOne.addScore(10);
        playerTwo.addScore(5);

        match.startNextRound();

        assertEquals(1, playerOne.getWins());
        assertEquals(0, playerTwo.getWins());
    }

    @Test
    public void startNextRoundShouldAddWinToPlayerTwoIfPlayerTwoHasHigherScore() {
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");
        Match match = new Match(playerOne, playerTwo);

        playerOne.addScore(5);
        playerTwo.addScore(10);

        match.startNextRound();

        assertEquals(0, playerOne.getWins());
        assertEquals(1, playerTwo.getWins());
    }

    @Test
    public void startNextRoundShouldNotAddWinIfRoundEndsInDraw() {
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");
        Match match = new Match(playerOne, playerTwo);

        playerOne.addScore(5);
        playerTwo.addScore(5);

        match.startNextRound();

        assertEquals(0, playerOne.getWins());
        assertEquals(0, playerTwo.getWins());
    }

    @Test
    public void startNextRoundShouldSetMatchStatusToFinishedIfMatchIsFinished() {
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");
        Match match = new Match(playerOne, playerTwo);

        playerOne.addWin();
        playerOne.addWin();

        match.startNextRound();

        assertEquals(MatchStatus.FINISHED, match.getMatchStatus());
    }

    private Match createMatch() {
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        return new Match(playerOne, playerTwo);
    }
}