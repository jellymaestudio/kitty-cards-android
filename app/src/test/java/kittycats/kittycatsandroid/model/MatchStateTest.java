package kittycats.kittycatsandroid.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class MatchStateTest {

    @Test
    public void constructorShouldSetCurrentRoundToOne() {
        MatchState matchState = new MatchState();

        assertEquals(1, matchState.getCurrentRound());
    }

    @Test
    public void constructorShouldSetMatchWinnerToNull() {
        MatchState matchState = new MatchState();

        assertNull(matchState.getMatchWinner());
    }

    @Test
    public void getMaxRoundsShouldReturnThree() {
        MatchState matchState = new MatchState();

        assertEquals(3, matchState.getMaxRounds());
    }

    @Test
    public void getWinsNeededShouldReturnTwo() {
        MatchState matchState = new MatchState();

        assertEquals(2, matchState.getWinsNeeded());
    }

    @Test
    public void nextRoundShouldIncreaseCurrentRound() {
        MatchState matchState = new MatchState();

        matchState.nextRound();

        assertEquals(2, matchState.getCurrentRound());
    }

    @Test
    public void nextRoundShouldNotIncreaseCurrentRoundAboveMaxRounds() {
        MatchState matchState = new MatchState();

        matchState.nextRound();
        matchState.nextRound();
        matchState.nextRound();

        assertEquals(3, matchState.getCurrentRound());
    }

    @Test
    public void isMatchFinishedShouldReturnFalseIfNoPlayerHasEnoughWinsAndMaxRoundsIsNotReached() {
        MatchState matchState = new MatchState();
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        assertFalse(matchState.isMatchFinished(playerOne, playerTwo));
        assertNull(matchState.getMatchWinner());
    }

    @Test
    public void isMatchFinishedShouldReturnTrueIfPlayerOneHasEnoughWins() {
        MatchState matchState = new MatchState();
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        playerOne.addWin();
        playerOne.addWin();

        assertTrue(matchState.isMatchFinished(playerOne, playerTwo));
        assertEquals(playerOne, matchState.getMatchWinner());
    }

    @Test
    public void isMatchFinishedShouldReturnTrueIfPlayerTwoHasEnoughWins() {
        MatchState matchState = new MatchState();
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        playerTwo.addWin();
        playerTwo.addWin();

        assertTrue(matchState.isMatchFinished(playerOne, playerTwo));
        assertEquals(playerTwo, matchState.getMatchWinner());
    }

    @Test
    public void isMatchFinishedShouldReturnTrueIfMaxRoundsIsReached() {
        MatchState matchState = new MatchState();
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        matchState.nextRound();
        matchState.nextRound();

        assertTrue(matchState.isMatchFinished(playerOne, playerTwo));
    }

    @Test
    public void isMatchFinishedShouldNotSetWinnerIfOnlyMaxRoundsIsReached() {
        MatchState matchState = new MatchState();
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        matchState.nextRound();
        matchState.nextRound();

        matchState.isMatchFinished(playerOne, playerTwo);

        assertNull(matchState.getMatchWinner());
    }

    @Test
    public void isMatchFinishedShouldThrowExceptionIfPlayerOneIsNull() {
        MatchState matchState = new MatchState();
        Player playerTwo = new Player(2, "Player Two");

        assertThrows(NullPointerException.class, () -> matchState.isMatchFinished(null, playerTwo));
    }

    @Test
    public void isMatchFinishedShouldThrowExceptionIfPlayerTwoIsNull() {
        MatchState matchState = new MatchState();
        Player playerOne = new Player(1, "Player One");

        assertThrows(NullPointerException.class, () -> matchState.isMatchFinished(playerOne, null));
    }
}