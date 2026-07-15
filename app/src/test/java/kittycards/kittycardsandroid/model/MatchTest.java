package kittycards.kittycardsandroid.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MatchTest {

    private static final List<GameColor> FIELD_COLORS = List.of(
            GameColor.YELLOW,
            GameColor.GREY,
            GameColor.GREEN,
            GameColor.CYAN,
            GameColor.GREY,
            GameColor.PURPLE,
            GameColor.GREY,
            GameColor.GREY
    );

    private static final List<GameColor> SECOND_FIELD_COLORS = List.of(
            GameColor.PURPLE,
            GameColor.GREY,
            GameColor.CYAN,
            GameColor.GREY,
            GameColor.YELLOW,
            GameColor.GREY,
            GameColor.GREEN,
            GameColor.GREY
    );

    private Player playerOne;
    private Player playerTwo;
    private Match match;

    @BeforeEach
    void setUp() {
        playerOne = new Player(1, "Player One");
        playerTwo = new Player(2, "Player Two");
        match = new Match(playerOne, playerTwo);
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    @Test
    void constructorAcceptsValidPlayers() {
        assertDoesNotThrow(
                () -> new Match(playerOne, playerTwo)
        );
    }

    @Test
    void constructorRejectsNullPlayerOne() {
        assertThrows(
                NullPointerException.class,
                () -> new Match(null, playerTwo)
        );
    }

    @Test
    void constructorRejectsNullPlayerTwo() {
        assertThrows(
                NullPointerException.class,
                () -> new Match(playerOne, null)
        );
    }

    @Test
    void constructorRejectsBothPlayersBeingNull() {
        assertThrows(
                NullPointerException.class,
                () -> new Match(null, null)
        );
    }

    @Test
    void constructorStoresPlayerOne() {
        assertSame(playerOne, match.getPlayerOne());
    }

    @Test
    void constructorStoresPlayerTwo() {
        assertSame(playerTwo, match.getPlayerTwo());
    }

    @Test
    void constructorCreatesMatchState() {
        assertNotNull(match.getMatchState());
    }

    @Test
    void constructorCreatesGameState() {
        assertNotNull(match.getGameState());
    }

    @Test
    void newMatchStartsPaused() {
        assertEquals(
                MatchStatus.PAUSED,
                match.getMatchStatus()
        );
    }

    @Test
    void newMatchStartsInRoundOne() {
        assertEquals(
                1,
                match.getMatchState().getCurrentRound()
        );
    }

    @Test
    void newMatchIsNotFinished() {
        assertFalse(match.getMatchState().isFinished());
    }

    @Test
    void newMatchHasNoWinner() {
        assertEquals(
                null,
                match.getMatchState().getMatchWinner()
        );
    }

    @Test
    void constructorUsesMatchPlayersInInitialGameState() {
        GameState gameState = match.getGameState();

        boolean containsBothPlayers =
                gameState.getStartingPlayer() == playerOne
                        && gameState.getSecondPlayer() == playerTwo
                        || gameState.getStartingPlayer() == playerTwo
                        && gameState.getSecondPlayer() == playerOne;

        assertTrue(containsBothPlayers);
    }

    @Test
    void initialCurrentPlayerIsStartingPlayer() {
        assertSame(
                match.getGameState().getStartingPlayer(),
                match.getGameState().getCurrentPlayer()
        );
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    @Test
    void getPlayerOneReturnsSameInstanceOnRepeatedAccess() {
        assertSame(match.getPlayerOne(), match.getPlayerOne());
    }

    @Test
    void getPlayerTwoReturnsSameInstanceOnRepeatedAccess() {
        assertSame(match.getPlayerTwo(), match.getPlayerTwo());
    }

    @Test
    void getMatchStateReturnsSameInstanceOnRepeatedAccess() {
        assertSame(
                match.getMatchState(),
                match.getMatchState()
        );
    }

    @Test
    void getGameStateReturnsSameInstanceUntilRoundChanges() {
        assertSame(
                match.getGameState(),
                match.getGameState()
        );
    }

    // -------------------------------------------------------------------------
    // getOtherPlayer
    // -------------------------------------------------------------------------

    @Test
    void getOtherPlayerReturnsPlayerTwoForPlayerOne() {
        assertSame(
                playerTwo,
                match.getOtherPlayer(playerOne)
        );
    }

    @Test
    void getOtherPlayerReturnsPlayerOneForPlayerTwo() {
        assertSame(
                playerOne,
                match.getOtherPlayer(playerTwo)
        );
    }

    @Test
    void getOtherPlayerRejectsNull() {
        assertThrows(
                NullPointerException.class,
                () -> match.getOtherPlayer(null)
        );
    }

    @Test
    void getOtherPlayerRejectsForeignPlayer() {
        Player foreignPlayer = new Player(3, "Foreign Player");

        assertThrows(
                IllegalArgumentException.class,
                () -> match.getOtherPlayer(foreignPlayer)
        );
    }

    @Test
    void getOtherPlayerRejectsSeparatePlayerWithSameIdAndName() {
        Player equalDataPlayer =
                new Player(playerOne.getId(), playerOne.getName());

        assertThrows(
                IllegalArgumentException.class,
                () -> match.getOtherPlayer(equalDataPlayer)
        );
    }

    @Test
    void rejectedForeignPlayerDoesNotModifyMatchPlayers() {
        Player foreignPlayer = new Player(3, "Foreign Player");

        assertThrows(
                IllegalArgumentException.class,
                () -> match.getOtherPlayer(foreignPlayer)
        );

        assertSame(playerOne, match.getPlayerOne());
        assertSame(playerTwo, match.getPlayerTwo());
    }

    // -------------------------------------------------------------------------
    // Match status
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(MatchStatus.class)
    void setMatchStatusAcceptsEveryStatus(MatchStatus status) {
        match.setMatchStatus(status);

        assertEquals(status, match.getMatchStatus());
    }

    @Test
    void setMatchStatusRejectsNull() {
        assertThrows(
                NullPointerException.class,
                () -> match.setMatchStatus(null)
        );
    }

    @Test
    void rejectedNullStatusDoesNotChangeCurrentStatus() {
        MatchStatus previousStatus = match.getMatchStatus();

        assertThrows(
                NullPointerException.class,
                () -> match.setMatchStatus(null)
        );

        assertEquals(previousStatus, match.getMatchStatus());
    }

    @Test
    void matchStatusCanChangeMultipleTimes() {
        match.setMatchStatus(MatchStatus.RUNNING);
        assertEquals(MatchStatus.RUNNING, match.getMatchStatus());

        match.setMatchStatus(MatchStatus.WAITING_FOR_NETWORK);
        assertEquals(
                MatchStatus.WAITING_FOR_NETWORK,
                match.getMatchStatus()
        );

        match.setMatchStatus(MatchStatus.PAUSED);
        assertEquals(MatchStatus.PAUSED, match.getMatchStatus());
    }

    // -------------------------------------------------------------------------
    // finishMatch
    // -------------------------------------------------------------------------

    @Test
    void finishMatchSetsStatusToFinished() {
        match.finishMatch();

        assertEquals(
                MatchStatus.FINISHED,
                match.getMatchStatus()
        );
    }

    @Test
    void finishMatchCanBeCalledWhenMatchIsRunning() {
        match.setMatchStatus(MatchStatus.RUNNING);

        match.finishMatch();

        assertEquals(
                MatchStatus.FINISHED,
                match.getMatchStatus()
        );
    }

    @Test
    void finishMatchCanBeCalledMultipleTimes() {
        match.finishMatch();

        assertDoesNotThrow(match::finishMatch);

        assertEquals(
                MatchStatus.FINISHED,
                match.getMatchStatus()
        );
    }

    @Test
    void finishMatchDoesNotReplaceGameState() {
        GameState previousGameState = match.getGameState();

        match.finishMatch();

        assertSame(previousGameState, match.getGameState());
    }

    @Test
    void finishMatchDoesNotReplaceMatchState() {
        MatchState previousMatchState = match.getMatchState();

        match.finishMatch();

        assertSame(previousMatchState, match.getMatchState());
    }

    @Test
    void finishMatchDoesNotResetPlayers() {
        addRoundDataToPlayers();

        match.finishMatch();

        assertEquals(5, playerOne.getScore());
        assertEquals(7, playerTwo.getScore());
        assertEquals(1, playerOne.getHandCardCount());
        assertEquals(1, playerTwo.getHandCardCount());
    }

    // -------------------------------------------------------------------------
    // initializeCurrentRound
    // -------------------------------------------------------------------------

    @Test
    void initializeCurrentRoundAcceptsValidArguments() {
        assertDoesNotThrow(
                () -> match.initializeCurrentRound(
                        FIELD_COLORS,
                        playerOne
                )
        );
    }

    @Test
    void initializeCurrentRoundRejectsNullFieldColors() {
        assertThrows(
                NullPointerException.class,
                () -> match.initializeCurrentRound(
                        null,
                        playerOne
                )
        );
    }

    @Test
    void initializeCurrentRoundRejectsNullStartingPlayer() {
        assertThrows(
                NullPointerException.class,
                () -> match.initializeCurrentRound(
                        FIELD_COLORS,
                        null
                )
        );
    }

    @Test
    void initializeCurrentRoundRejectsBothArgumentsBeingNull() {
        assertThrows(
                NullPointerException.class,
                () -> match.initializeCurrentRound(null, null)
        );
    }

    @Test
    void initializeCurrentRoundRejectsForeignStartingPlayer() {
        Player foreignPlayer = new Player(3, "Foreign Player");

        assertThrows(
                IllegalArgumentException.class,
                () -> match.initializeCurrentRound(
                        FIELD_COLORS,
                        foreignPlayer
                )
        );
    }

    @Test
    void initializeCurrentRoundCreatesNewGameState() {
        GameState previousGameState = match.getGameState();

        match.initializeCurrentRound(
                FIELD_COLORS,
                playerOne
        );

        assertNotSame(previousGameState, match.getGameState());
    }

    @Test
    void initializeCurrentRoundUsesProvidedBoardColors() {
        match.initializeCurrentRound(
                FIELD_COLORS,
                playerOne
        );

        assertEquals(
                FIELD_COLORS,
                match.getGameState()
                        .getBoard()
                        .getFieldColors()
        );
    }

    @Test
    void initializeCurrentRoundUsesProvidedStartingPlayer() {
        match.initializeCurrentRound(
                FIELD_COLORS,
                playerTwo
        );

        assertSame(
                playerTwo,
                match.getGameState().getStartingPlayer()
        );
    }

    @Test
    void initializeCurrentRoundUsesOtherPlayerAsSecondPlayer() {
        match.initializeCurrentRound(
                FIELD_COLORS,
                playerTwo
        );

        assertSame(
                playerOne,
                match.getGameState().getSecondPlayer()
        );
    }

    @Test
    void initializeCurrentRoundSetsStartingPlayerAsCurrentPlayer() {
        match.initializeCurrentRound(
                FIELD_COLORS,
                playerTwo
        );

        assertSame(
                playerTwo,
                match.getGameState().getCurrentPlayer()
        );
    }

    @Test
    void initializeCurrentRoundResetsPlayerScores() {
        playerOne.addScore(5);
        playerTwo.addScore(7);

        match.initializeCurrentRound(
                FIELD_COLORS,
                playerOne
        );

        assertEquals(0, playerOne.getScore());
        assertEquals(0, playerTwo.getScore());
    }

    @Test
    void initializeCurrentRoundClearsPlayerHands() {
        playerOne.addCard(new Card(GameColor.YELLOW, 3));
        playerTwo.addCard(new Card(GameColor.GREEN, 5));

        match.initializeCurrentRound(
                FIELD_COLORS,
                playerOne
        );

        assertTrue(playerOne.getHandCards().isEmpty());
        assertTrue(playerTwo.getHandCards().isEmpty());
    }

    @Test
    void initializeCurrentRoundClearsSelectedCards() {
        Card firstCard = new Card(GameColor.YELLOW, 3);
        Card secondCard = new Card(GameColor.GREEN, 5);

        playerOne.addCard(firstCard);
        playerTwo.addCard(secondCard);
        playerOne.selectCard(firstCard);
        playerTwo.selectCard(secondCard);

        match.initializeCurrentRound(
                FIELD_COLORS,
                playerOne
        );

        assertFalse(playerOne.hasSelectedCard());
        assertFalse(playerTwo.hasSelectedCard());
    }

    @Test
    void initializeCurrentRoundPreservesPlayerWins() {
        playerOne.addWin();
        playerOne.addWin();
        playerTwo.addWin();

        match.initializeCurrentRound(
                FIELD_COLORS,
                playerOne
        );

        assertEquals(2, playerOne.getWins());
        assertEquals(1, playerTwo.getWins());
    }

    @Test
    void initializeCurrentRoundPreservesPlayerIdentityAndNames() {
        match.initializeCurrentRound(
                FIELD_COLORS,
                playerTwo
        );

        assertSame(playerOne, match.getPlayerOne());
        assertSame(playerTwo, match.getPlayerTwo());
        assertEquals("Player One", playerOne.getName());
        assertEquals("Player Two", playerTwo.getName());
    }

    @Test
    void initializeCurrentRoundDoesNotAdvanceRoundCounter() {
        int previousRound =
                match.getMatchState().getCurrentRound();

        match.initializeCurrentRound(
                FIELD_COLORS,
                playerOne
        );

        assertEquals(
                previousRound,
                match.getMatchState().getCurrentRound()
        );
    }

    @Test
    void failedRoundInitializationDoesNotReplaceGameState() {
        GameState previousGameState = match.getGameState();

        assertThrows(
                NullPointerException.class,
                () -> match.initializeCurrentRound(
                        null,
                        playerOne
                )
        );

        assertSame(previousGameState, match.getGameState());
    }

    // -------------------------------------------------------------------------
    // startNextRound with predefined colors
    // -------------------------------------------------------------------------

    @Test
    void startNextRoundWithColorsAdvancesRoundCounter() {
        match.startNextRound(
                FIELD_COLORS,
                playerTwo
        );

        assertEquals(
                2,
                match.getMatchState().getCurrentRound()
        );
    }

    @Test
    void startNextRoundWithColorsCreatesNewGameState() {
        GameState previousGameState = match.getGameState();

        match.startNextRound(
                FIELD_COLORS,
                playerTwo
        );

        assertNotSame(previousGameState, match.getGameState());
    }

    @Test
    void startNextRoundWithColorsUsesProvidedFieldColors() {
        match.startNextRound(
                SECOND_FIELD_COLORS,
                playerTwo
        );

        assertEquals(
                SECOND_FIELD_COLORS,
                match.getGameState()
                        .getBoard()
                        .getFieldColors()
        );
    }

    @Test
    void startNextRoundWithColorsUsesProvidedStartingPlayer() {
        match.startNextRound(
                FIELD_COLORS,
                playerTwo
        );

        assertSame(
                playerTwo,
                match.getGameState().getStartingPlayer()
        );
        assertSame(
                playerTwo,
                match.getGameState().getCurrentPlayer()
        );
    }

    @Test
    void startNextRoundWithColorsResetsRoundSpecificPlayerData() {
        addRoundDataToPlayers();

        match.startNextRound(
                FIELD_COLORS,
                playerTwo
        );

        assertRoundSpecificDataIsReset(playerOne);
        assertRoundSpecificDataIsReset(playerTwo);
    }

    @Test
    void startNextRoundWithColorsPreservesWins() {
        playerOne.addWin();
        playerTwo.addWin();

        match.startNextRound(
                FIELD_COLORS,
                playerTwo
        );

        assertEquals(1, playerOne.getWins());
        assertEquals(1, playerTwo.getWins());
    }

    @Test
    void startNextRoundWithColorsCanAdvanceToRoundThree() {
        match.startNextRound(
                FIELD_COLORS,
                playerTwo
        );

        match.startNextRound(
                SECOND_FIELD_COLORS,
                playerOne
        );

        assertEquals(
                3,
                match.getMatchState().getCurrentRound()
        );
    }

    @Test
    void startNextRoundWithColorsRejectsNullFieldColors() {
        assertThrows(
                NullPointerException.class,
                () -> match.startNextRound(
                        null,
                        playerTwo
                )
        );
    }

    @Test
    void startNextRoundWithColorsRejectsNullStartingPlayer() {
        assertThrows(
                NullPointerException.class,
                () -> match.startNextRound(
                        FIELD_COLORS,
                        null
                )
        );
    }

    @Test
    void startNextRoundWithColorsRejectsForeignStartingPlayer() {
        Player foreignPlayer = new Player(3, "Foreign Player");

        assertThrows(
                IllegalArgumentException.class,
                () -> match.startNextRound(
                        FIELD_COLORS,
                        foreignPlayer
                )
        );
    }

    // -------------------------------------------------------------------------
    // startNextRound without predefined colors
    // -------------------------------------------------------------------------

    @Test
    void startNextRoundAdvancesRoundCounter() {
        match.startNextRound();

        assertEquals(
                2,
                match.getMatchState().getCurrentRound()
        );
    }

    @Test
    void startNextRoundCreatesNewGameState() {
        GameState previousGameState = match.getGameState();

        match.startNextRound();

        assertNotSame(previousGameState, match.getGameState());
    }

    @Test
    void startNextRoundCreatesBoardWithEightPlayableColors() {
        match.startNextRound();

        assertEquals(
                8,
                match.getGameState()
                        .getBoard()
                        .getFieldColors()
                        .size()
        );
    }

    @Test
    void startNextRoundCreatesGreyCenterField() {
        match.startNextRound();

        assertEquals(
                GameColor.GREY,
                match.getGameState()
                        .getBoard()
                        .getField(1, 1)
                        .getColor()
        );
    }

    @Test
    void startNextRoundCreatesValidCurrentPlayer() {
        match.startNextRound();

        Player currentPlayer =
                match.getGameState().getCurrentPlayer();

        assertTrue(
                currentPlayer == playerOne
                        || currentPlayer == playerTwo
        );
    }

    @Test
    void startNextRoundResetsRoundSpecificPlayerData() {
        addRoundDataToPlayers();

        match.startNextRound();

        assertRoundSpecificDataIsReset(playerOne);
        assertRoundSpecificDataIsReset(playerTwo);
    }

    @Test
    void startNextRoundPreservesWins() {
        playerOne.addWin();
        playerTwo.addWin();

        match.startNextRound();

        assertEquals(1, playerOne.getWins());
        assertEquals(1, playerTwo.getWins());
    }

    // -------------------------------------------------------------------------
    // Starting a round after match completion
    // -------------------------------------------------------------------------

    @Test
    void startNextRoundSetsStatusToFinishedWhenPlayerOneHasWonMatch() {
        playerOne.addWin();
        playerOne.addWin();

        match.startNextRound();

        assertEquals(
                MatchStatus.FINISHED,
                match.getMatchStatus()
        );
    }

    @Test
    void startNextRoundSetsStatusToFinishedWhenPlayerTwoHasWonMatch() {
        playerTwo.addWin();
        playerTwo.addWin();

        match.startNextRound();

        assertEquals(
                MatchStatus.FINISHED,
                match.getMatchStatus()
        );
    }

    @Test
    void startNextRoundDoesNotAdvanceRoundAfterPlayerHasWonMatch() {
        playerOne.addWin();
        playerOne.addWin();

        int previousRound =
                match.getMatchState().getCurrentRound();

        match.startNextRound();

        assertEquals(
                previousRound,
                match.getMatchState().getCurrentRound()
        );
    }

    @Test
    void startNextRoundDoesNotReplaceGameStateAfterMatchHasFinished() {
        playerOne.addWin();
        playerOne.addWin();

        GameState previousGameState = match.getGameState();

        match.startNextRound();

        assertSame(previousGameState, match.getGameState());
    }

    @Test
    void overloadedStartNextRoundSetsStatusToFinishedWhenMatchHasEnded() {
        playerTwo.addWin();
        playerTwo.addWin();

        match.startNextRound(
                FIELD_COLORS,
                playerOne
        );

        assertEquals(
                MatchStatus.FINISHED,
                match.getMatchStatus()
        );
    }

    @Test
    void overloadedStartNextRoundDoesNotReplaceGameStateAfterMatchHasEnded() {
        playerTwo.addWin();
        playerTwo.addWin();

        GameState previousGameState = match.getGameState();

        match.startNextRound(
                FIELD_COLORS,
                playerOne
        );

        assertSame(previousGameState, match.getGameState());
    }

    @Test
    void startNextRoundDoesNotAdvancePastThirdRound() {
        match.startNextRound(
                FIELD_COLORS,
                playerTwo
        );

        match.startNextRound(
                SECOND_FIELD_COLORS,
                playerOne
        );

        int roundAtMaximum =
                match.getMatchState().getCurrentRound();

        match.startNextRound();

        assertEquals(
                roundAtMaximum,
                match.getMatchState().getCurrentRound()
        );

        assertEquals(
                MatchStatus.FINISHED,
                match.getMatchStatus()
        );
    }

    // -------------------------------------------------------------------------
    // Interaction between methods
    // -------------------------------------------------------------------------

    @Test
    void initializedRoundCanBeFollowedByNextRound() {
        match.initializeCurrentRound(
                FIELD_COLORS,
                playerOne
        );

        GameState initializedState = match.getGameState();

        match.startNextRound(
                SECOND_FIELD_COLORS,
                playerTwo
        );

        assertNotSame(initializedState, match.getGameState());
        assertEquals(
                SECOND_FIELD_COLORS,
                match.getGameState()
                        .getBoard()
                        .getFieldColors()
        );
        assertEquals(
                2,
                match.getMatchState().getCurrentRound()
        );
    }

    @Test
    void matchKeepsSameMatchStateAcrossRounds() {
        MatchState originalMatchState = match.getMatchState();

        match.startNextRound(
                FIELD_COLORS,
                playerTwo
        );

        assertSame(
                originalMatchState,
                match.getMatchState()
        );
    }

    @Test
    void playersRemainSameInstancesAcrossRounds() {
        match.startNextRound(
                FIELD_COLORS,
                playerTwo
        );

        assertSame(playerOne, match.getPlayerOne());
        assertSame(playerTwo, match.getPlayerTwo());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void addRoundDataToPlayers() {
        Card firstCard = new Card(GameColor.YELLOW, 3);
        Card secondCard = new Card(GameColor.GREEN, 5);

        playerOne.addCard(firstCard);
        playerTwo.addCard(secondCard);

        playerOne.selectCard(firstCard);
        playerTwo.selectCard(secondCard);

        playerOne.addScore(5);
        playerTwo.addScore(7);
    }

    private void assertRoundSpecificDataIsReset(Player player) {
        assertEquals(0, player.getScore());
        assertEquals(0, player.getHandCardCount());
        assertTrue(player.getHandCards().isEmpty());
        assertFalse(player.hasSelectedCard());
    }
}