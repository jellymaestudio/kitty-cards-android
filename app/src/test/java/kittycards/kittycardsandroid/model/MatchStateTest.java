package kittycards.kittycardsandroid.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MatchStateTest {

    private MatchState matchState;
    private Player playerOne;
    private Player playerTwo;

    @BeforeEach
    void setUp() {
        matchState = new MatchState();
        playerOne = new Player(1, "Player One");
        playerTwo = new Player(2, "Player Two");
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    void constructorCreatesMatchStateWithoutThrowing() {
        assertDoesNotThrow(MatchState::new);
    }

    @Test
    void newMatchStartsInRoundOne() {
        assertEquals(1, matchState.getCurrentRound());
    }

    @Test
    void maximumNumberOfRoundsIsThree() {
        assertEquals(3, matchState.getMaxRounds());
    }

    @Test
    void twoWinsAreRequiredToWinMatch() {
        assertEquals(2, matchState.getWinsNeeded());
    }

    @Test
    void newMatchIsNotFinished() {
        assertFalse(matchState.isFinished());
    }

    @Test
    void newMatchHasNoWinner() {
        assertNull(matchState.getMatchWinner());
    }

    @Test
    void newMatchIsNotDraw() {
        assertFalse(matchState.isDraw());
    }

    @Test
    void newMatchHasNoRoundResults() {
        assertTrue(matchState.getRoundResults().isEmpty());
    }

    // -------------------------------------------------------------------------
    // getRoundResults
    // -------------------------------------------------------------------------

    @Test
    void getRoundResultsReturnsStoredResultsInInsertionOrder() {
        matchState.addRoundResult(RoundResult.PLAYER_ONE_WIN);
        matchState.addRoundResult(RoundResult.DRAW);
        matchState.addRoundResult(RoundResult.PLAYER_TWO_WIN);

        assertEquals(
                List.of(
                        RoundResult.PLAYER_ONE_WIN,
                        RoundResult.DRAW,
                        RoundResult.PLAYER_TWO_WIN
                ),
                matchState.getRoundResults()
        );
    }

    @Test
    void getRoundResultsReturnsUnmodifiableList() {
        matchState.addRoundResult(RoundResult.PLAYER_ONE_WIN);

        List<RoundResult> results = matchState.getRoundResults();

        assertThrows(
                UnsupportedOperationException.class,
                () -> results.add(RoundResult.DRAW)
        );
    }

    @Test
    void modifyingReturnedRoundResultsListDoesNotChangeState() {
        matchState.addRoundResult(RoundResult.PLAYER_ONE_WIN);

        List<RoundResult> results = matchState.getRoundResults();

        assertThrows(
                UnsupportedOperationException.class,
                () -> results.clear()
        );

        assertEquals(
                List.of(RoundResult.PLAYER_ONE_WIN),
                matchState.getRoundResults()
        );
    }

    // -------------------------------------------------------------------------
    // addRoundResult
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(RoundResult.class)
    void addRoundResultAcceptsEveryRoundResult(RoundResult result) {
        matchState.addRoundResult(result);

        assertEquals(
                List.of(result),
                matchState.getRoundResults()
        );
    }

    @Test
    void addRoundResultAllowsThreeResults() {
        matchState.addRoundResult(RoundResult.PLAYER_ONE_WIN);
        matchState.addRoundResult(RoundResult.PLAYER_TWO_WIN);
        matchState.addRoundResult(RoundResult.DRAW);

        assertEquals(3, matchState.getRoundResults().size());
    }

    @Test
    void addRoundResultRejectsNull() {
        assertThrows(
                NullPointerException.class,
                () -> matchState.addRoundResult(null)
        );
    }

    @Test
    void rejectedNullResultDoesNotChangeStoredResults() {
        matchState.addRoundResult(RoundResult.PLAYER_ONE_WIN);

        assertThrows(
                NullPointerException.class,
                () -> matchState.addRoundResult(null)
        );

        assertEquals(
                List.of(RoundResult.PLAYER_ONE_WIN),
                matchState.getRoundResults()
        );
    }

    @Test
    void addRoundResultRejectsFourthResult() {
        matchState.addRoundResult(RoundResult.PLAYER_ONE_WIN);
        matchState.addRoundResult(RoundResult.PLAYER_TWO_WIN);
        matchState.addRoundResult(RoundResult.DRAW);

        assertThrows(
                IllegalStateException.class,
                () -> matchState.addRoundResult(
                        RoundResult.PLAYER_ONE_WIN
                )
        );
    }

    @Test
    void rejectedFourthResultDoesNotChangeStoredResults() {
        List<RoundResult> expectedResults = List.of(
                RoundResult.PLAYER_ONE_WIN,
                RoundResult.PLAYER_TWO_WIN,
                RoundResult.DRAW
        );

        expectedResults.forEach(matchState::addRoundResult);

        assertThrows(
                IllegalStateException.class,
                () -> matchState.addRoundResult(
                        RoundResult.PLAYER_ONE_WIN
                )
        );

        assertEquals(
                expectedResults,
                matchState.getRoundResults()
        );
    }

    // -------------------------------------------------------------------------
    // nextRound
    // -------------------------------------------------------------------------

    @Test
    void nextRoundAdvancesFromRoundOneToRoundTwo() {
        matchState.nextRound();

        assertEquals(2, matchState.getCurrentRound());
    }

    @Test
    void nextRoundAdvancesFromRoundTwoToRoundThree() {
        matchState.nextRound();
        matchState.nextRound();

        assertEquals(3, matchState.getCurrentRound());
    }

    @Test
    void nextRoundDoesNotAdvancePastMaximumRound() {
        matchState.nextRound();
        matchState.nextRound();
        matchState.nextRound();

        assertEquals(3, matchState.getCurrentRound());
    }

    @Test
    void repeatedNextRoundCallsDoNotExceedMaximumRound() {
        for (int count = 0; count < 100; count++) {
            matchState.nextRound();
        }

        assertEquals(
                matchState.getMaxRounds(),
                matchState.getCurrentRound()
        );
    }

    @Test
    void nextRoundDoesNotAddRoundResult() {
        matchState.nextRound();

        assertTrue(matchState.getRoundResults().isEmpty());
    }

    @Test
    void nextRoundDoesNotFinishMatchByItself() {
        matchState.nextRound();
        matchState.nextRound();

        assertFalse(matchState.isFinished());
        assertNull(matchState.getMatchWinner());
    }

    // -------------------------------------------------------------------------
    // isMatchFinished - validation
    // -------------------------------------------------------------------------

    @Test
    void isMatchFinishedRejectsNullPlayerOne() {
        assertThrows(
                NullPointerException.class,
                () -> matchState.isMatchFinished(null, playerTwo)
        );
    }

    @Test
    void isMatchFinishedRejectsNullPlayerTwo() {
        assertThrows(
                NullPointerException.class,
                () -> matchState.isMatchFinished(playerOne, null)
        );
    }

    @Test
    void isMatchFinishedRejectsBothPlayersBeingNull() {
        assertThrows(
                NullPointerException.class,
                () -> matchState.isMatchFinished(null, null)
        );
    }

    // -------------------------------------------------------------------------
    // isMatchFinished - unfinished match
    // -------------------------------------------------------------------------

    @Test
    void isMatchFinishedReturnsFalseAtBeginning() {
        assertFalse(
                matchState.isMatchFinished(playerOne, playerTwo)
        );
    }

    @Test
    void unfinishedMatchHasNoWinner() {
        matchState.isMatchFinished(playerOne, playerTwo);

        assertNull(matchState.getMatchWinner());
    }

    @Test
    void unfinishedMatchIsNotDraw() {
        matchState.isMatchFinished(playerOne, playerTwo);

        assertFalse(matchState.isDraw());
    }

    @Test
    void oneWinIsNotEnoughToFinishMatch() {
        playerOne.addWin();

        assertFalse(
                matchState.isMatchFinished(playerOne, playerTwo)
        );
    }

    @Test
    void oneWinEachIsNotEnoughBeforeThirdRoundEnds() {
        playerOne.addWin();
        playerTwo.addWin();

        assertFalse(
                matchState.isMatchFinished(playerOne, playerTwo)
        );
    }

    @Test
    void currentRoundThreeFinishesMatchEvenWithoutStoredThirdRoundResult() {
        matchState.nextRound();
        matchState.nextRound();

        assertTrue(
                matchState.isMatchFinished(playerOne, playerTwo)
        );

        assertTrue(matchState.isFinished());
        assertNull(matchState.getMatchWinner());
        assertTrue(matchState.isDraw());
    }

    // -------------------------------------------------------------------------
    // isMatchFinished - player one wins
    // -------------------------------------------------------------------------

    @Test
    void playerOneWinsMatchWithTwoWins() {
        playerOne.addWin();
        playerOne.addWin();

        assertTrue(
                matchState.isMatchFinished(playerOne, playerTwo)
        );
    }

    @Test
    void playerOneIsStoredAsWinnerWithTwoWins() {
        playerOne.addWin();
        playerOne.addWin();

        matchState.isMatchFinished(playerOne, playerTwo);

        assertSame(playerOne, matchState.getMatchWinner());
    }

    @Test
    void matchIsMarkedFinishedWhenPlayerOneHasTwoWins() {
        playerOne.addWin();
        playerOne.addWin();

        matchState.isMatchFinished(playerOne, playerTwo);

        assertTrue(matchState.isFinished());
    }

    @Test
    void playerOneVictoryIsNotDraw() {
        playerOne.addWin();
        playerOne.addWin();

        matchState.isMatchFinished(playerOne, playerTwo);

        assertFalse(matchState.isDraw());
    }

    @Test
    void moreThanTwoWinsAlsoFinishesMatchForPlayerOne() {
        playerOne.addWin();
        playerOne.addWin();
        playerOne.addWin();

        assertTrue(
                matchState.isMatchFinished(playerOne, playerTwo)
        );

        assertSame(playerOne, matchState.getMatchWinner());
    }

    // -------------------------------------------------------------------------
    // isMatchFinished - player two wins
    // -------------------------------------------------------------------------

    @Test
    void playerTwoWinsMatchWithTwoWins() {
        playerTwo.addWin();
        playerTwo.addWin();

        assertTrue(
                matchState.isMatchFinished(playerOne, playerTwo)
        );
    }

    @Test
    void playerTwoIsStoredAsWinnerWithTwoWins() {
        playerTwo.addWin();
        playerTwo.addWin();

        matchState.isMatchFinished(playerOne, playerTwo);

        assertSame(playerTwo, matchState.getMatchWinner());
    }

    @Test
    void matchIsMarkedFinishedWhenPlayerTwoHasTwoWins() {
        playerTwo.addWin();
        playerTwo.addWin();

        matchState.isMatchFinished(playerOne, playerTwo);

        assertTrue(matchState.isFinished());
    }

    @Test
    void playerTwoVictoryIsNotDraw() {
        playerTwo.addWin();
        playerTwo.addWin();

        matchState.isMatchFinished(playerOne, playerTwo);

        assertFalse(matchState.isDraw());
    }

    // -------------------------------------------------------------------------
    // isMatchFinished - draw after maximum rounds
    // -------------------------------------------------------------------------

    @Test
    void matchFinishesAfterThreeCompletedRoundsWithoutTwoWins() {
        completeThreeRounds();

        assertTrue(
                matchState.isMatchFinished(playerOne, playerTwo)
        );
    }

    @Test
    void matchWinnerIsNullForDrawAfterThreeRounds() {
        completeThreeRounds();

        matchState.isMatchFinished(playerOne, playerTwo);

        assertNull(matchState.getMatchWinner());
    }

    @Test
    void isDrawReturnsTrueForFinishedMatchWithoutWinner() {
        completeThreeRounds();

        matchState.isMatchFinished(playerOne, playerTwo);

        assertTrue(matchState.isDraw());
    }

    @Test
    void matchIsMarkedFinishedForDrawAfterThreeRounds() {
        completeThreeRounds();

        matchState.isMatchFinished(playerOne, playerTwo);

        assertTrue(matchState.isFinished());
    }

    @Test
    void thirdRoundDrawCanFinishMatchWithOneWinEach() {
        playerOne.addWin();
        playerTwo.addWin();

        matchState.addRoundResult(RoundResult.PLAYER_ONE_WIN);
        matchState.nextRound();

        matchState.addRoundResult(RoundResult.PLAYER_TWO_WIN);
        matchState.nextRound();

        matchState.addRoundResult(RoundResult.DRAW);

        assertTrue(
                matchState.isMatchFinished(playerOne, playerTwo)
        );
        assertNull(matchState.getMatchWinner());
        assertTrue(matchState.isDraw());
    }

    @Test
    void threeDrawnRoundsFinishMatchAsDraw() {
        matchState.addRoundResult(RoundResult.DRAW);
        matchState.nextRound();

        matchState.addRoundResult(RoundResult.DRAW);
        matchState.nextRound();

        matchState.addRoundResult(RoundResult.DRAW);

        assertTrue(
                matchState.isMatchFinished(playerOne, playerTwo)
        );
        assertTrue(matchState.isDraw());
    }

    // -------------------------------------------------------------------------
    // Re-evaluation behavior
    // -------------------------------------------------------------------------

    @Test
    void reevaluationOfFinishedMatchReturnsTrueAndKeepsFinishedState() {
        playerOne.addWin();
        playerOne.addWin();

        assertTrue(
                matchState.isMatchFinished(playerOne, playerTwo)
        );
        assertTrue(matchState.isFinished());

        Player replacementPlayerOne =
                new Player(3, "Replacement One");
        Player replacementPlayerTwo =
                new Player(4, "Replacement Two");

        boolean result = matchState.isMatchFinished(
                replacementPlayerOne,
                replacementPlayerTwo
        );

        assertTrue(result);
        assertTrue(matchState.isFinished());
        assertSame(playerOne, matchState.getMatchWinner());
    }

    @Test
    void previousWinnerRemainsStoredAfterReevaluationWithoutNewWinner() {
        playerOne.addWin();
        playerOne.addWin();

        matchState.isMatchFinished(playerOne, playerTwo);

        assertSame(playerOne, matchState.getMatchWinner());

        Player replacementPlayerOne =
                new Player(3, "Replacement One");
        Player replacementPlayerTwo =
                new Player(4, "Replacement Two");

        matchState.isMatchFinished(
                replacementPlayerOne,
                replacementPlayerTwo
        );

        assertSame(playerOne, matchState.getMatchWinner());
        assertTrue(matchState.isFinished());
        assertFalse(matchState.isDraw());
    }

    @Test
    void finishedMatchDoesNotReplaceStoredWinner() {
        playerOne.addWin();
        playerOne.addWin();

        matchState.isMatchFinished(playerOne, playerTwo);

        Player replacementPlayerOne =
                new Player(3, "Replacement One");
        Player replacementPlayerTwo =
                new Player(4, "Replacement Two");

        replacementPlayerTwo.addWin();
        replacementPlayerTwo.addWin();

        assertTrue(
                matchState.isMatchFinished(
                        replacementPlayerOne,
                        replacementPlayerTwo
                )
        );

        assertSame(playerOne, matchState.getMatchWinner());
        assertTrue(matchState.isFinished());
    }

    @Test
    void finishedMatchRemainsFinishedDuringLaterChecks() {
        playerOne.addWin();
        playerOne.addWin();

        assertTrue(
                matchState.isMatchFinished(playerOne, playerTwo)
        );

        Player replacementPlayerOne =
                new Player(3, "Replacement One");
        Player replacementPlayerTwo =
                new Player(4, "Replacement Two");

        assertTrue(
                matchState.isMatchFinished(
                        replacementPlayerOne,
                        replacementPlayerTwo
                )
        );

        assertTrue(matchState.isFinished());
        assertSame(playerOne, matchState.getMatchWinner());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void completeThreeRounds() {
        matchState.addRoundResult(RoundResult.DRAW);
        matchState.nextRound();

        matchState.addRoundResult(RoundResult.DRAW);
        matchState.nextRound();

        matchState.addRoundResult(RoundResult.DRAW);
    }
}