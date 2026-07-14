package kittycards.kittycardsandroid.logic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import kittycards.kittycardsandroid.model.Board;
import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.Field;
import kittycards.kittycardsandroid.model.GameColor;
import kittycards.kittycardsandroid.model.GameState;
import kittycards.kittycardsandroid.model.RoundResult;
import kittycards.kittycardsandroid.network.GameAction;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import kittycards.kittycardsandroid.components.INetworkManager;
import kittycards.kittycardsandroid.model.Player;
import kittycards.kittycardsandroid.network.Role;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;

import kittycards.kittycardsandroid.model.Match;
import kittycards.kittycardsandroid.model.MatchStatus;

class GameControllerTest {

    private GameController controller;

    private Player playerOne;
    private Player playerTwo;

    private INetworkManager networkManager;

    private static final List<GameColor> REMOTE_FIELD_COLORS = List.of(
            GameColor.YELLOW,
            GameColor.GREY,
            GameColor.GREEN,
            GameColor.GREY,
            GameColor.CYAN,
            GameColor.GREY,
            GameColor.PURPLE,
            GameColor.GREY
    );

    private static final List<GameColor> SECOND_REMOTE_FIELD_COLORS = List.of(
            GameColor.GREY,
            GameColor.PURPLE,
            GameColor.GREY,
            GameColor.YELLOW,
            GameColor.GREY,
            GameColor.CYAN,
            GameColor.GREY,
            GameColor.GREEN
    );

    @BeforeEach
    void setUp() {
        networkManager = Mockito.mock(INetworkManager.class);
        controller = new GameController(networkManager);

        playerOne = new Player(1, "Player One");
        playerTwo = new Player(2, "Player Two");
    }

    @AfterEach
    void tearDown() {
        controller.resetSession();
    }


    // -------------------------------------------------------------------------
    // Initial state after reset
    // -------------------------------------------------------------------------

    @Test
    void resetControllerHasNoActiveMatch() {
        assertNull(controller.getMatch());
    }

    @Test
    void resetControllerHasNoLocalPlayer() {
        assertNull(controller.getLocalPlayer());
    }

    @Test
    void resetControllerIsNotListeningForActions() {
        assertFalse(controller.isListeningForActions());
    }

    // -------------------------------------------------------------------------
    // Local player
    // -------------------------------------------------------------------------

    @Test
    void setLocalPlayerStoresProvidedPlayer() {
        controller.setLocalPlayer(playerOne);

        assertSame(
                playerOne,
                controller.getLocalPlayer()
        );
    }

    @Test
    void setLocalPlayerCanReplacePreviousPlayer() {
        controller.setLocalPlayer(playerOne);

        controller.setLocalPlayer(playerTwo);

        assertSame(
                playerTwo,
                controller.getLocalPlayer()
        );
    }

    @Test
    void setLocalPlayerAcceptsNull() {
        controller.setLocalPlayer(playerOne);

        assertDoesNotThrow(
                () -> controller.setLocalPlayer(null)
        );

        assertNull(controller.getLocalPlayer());
    }

    // -------------------------------------------------------------------------
    // Remote player
    // -------------------------------------------------------------------------

    @Test
    void getRemotePlayerReturnsPlayerTwoWhenPlayerOneIsLocal() {
        controller.startMatch(playerOne, playerTwo);
        controller.setLocalPlayer(playerOne);

        assertSame(
                playerTwo,
                controller.getRemotePlayer()
        );
    }

    @Test
    void getRemotePlayerReturnsPlayerOneWhenPlayerTwoIsLocal() {
        controller.startMatch(playerOne, playerTwo);
        controller.setLocalPlayer(playerTwo);

        assertSame(
                playerOne,
                controller.getRemotePlayer()
        );
    }

    @Test
    void getRemotePlayerThrowsExceptionWhenNoMatchExists() {
        controller.setLocalPlayer(playerOne);

        assertThrows(
                NullPointerException.class,
                controller::getRemotePlayer
        );
    }

    @Test
    void getRemotePlayerThrowsExceptionWhenNoLocalPlayerIsSet() {
        controller.startMatch(playerOne, playerTwo);

        assertThrows(
                NullPointerException.class,
                controller::getRemotePlayer
        );
    }

    @Test
    void getRemotePlayerThrowsExceptionWhenLocalPlayerIsNotPartOfMatch() {
        Player foreignPlayer =
                new Player(3, "Foreign Player");

        controller.startMatch(playerOne, playerTwo);
        controller.setLocalPlayer(foreignPlayer);

        assertThrows(
                IllegalArgumentException.class,
                controller::getRemotePlayer
        );
    }

    @Test
    void getRemotePlayerRejectsSeparatePlayerWithSameData() {
        Player separatePlayer =
                new Player(
                        playerOne.getId(),
                        playerOne.getName()
                );

        controller.startMatch(playerOne, playerTwo);
        controller.setLocalPlayer(separatePlayer);

        assertThrows(
                IllegalArgumentException.class,
                controller::getRemotePlayer
        );
    }

    // -------------------------------------------------------------------------
    // Network configuration
    // -------------------------------------------------------------------------

    @Test
    void setNetworkManagerAllowsActionListenerConfiguration() {
        controller.setNetworkManager(networkManager);
        controller.setNetworkRole(Role.HOST);

        assertDoesNotThrow(
                controller::startListeningForActions
        );

        controller.stopListeningForActions();
    }

    @Test
    void replacingNetworkManagerDoesNotThrowException() {
        INetworkManager secondNetworkManager =
                Mockito.mock(INetworkManager.class);

        controller.setNetworkManager(networkManager);

        assertDoesNotThrow(
                () -> controller.setNetworkManager(
                        secondNetworkManager
                )
        );
    }

    @Test
    void setNetworkManagerAcceptsNull() {
        controller.setNetworkManager(networkManager);

        assertDoesNotThrow(
                () -> controller.setNetworkManager(null)
        );
    }

    @Test
    void setNetworkRoleAcceptsEveryRole() {
        assertDoesNotThrow(
                () -> controller.setNetworkRole(Role.HOST)
        );

        assertDoesNotThrow(
                () -> controller.setNetworkRole(Role.GUEST)
        );

        assertDoesNotThrow(
                () -> controller.setNetworkRole(
                        Role.NOT_CONNECTED
                )
        );
    }

    @Test
    void setNetworkRoleRejectsNull() {
        assertThrows(
                NullPointerException.class,
                () -> controller.setNetworkRole(null)
        );
    }

    @Test
    void rejectedNullNetworkRoleDoesNotChangeCurrentRole() {
        controller.setNetworkRole(Role.HOST);

        assertThrows(
                NullPointerException.class,
                () -> controller.setNetworkRole(null)
        );

        controller.setNetworkManager(networkManager);

        assertDoesNotThrow(controller::startListeningForActions);

        controller.stopListeningForActions();
    }

    // -------------------------------------------------------------------------
    // resetSession
    // -------------------------------------------------------------------------

    @Test
    void resetSessionClearsActiveMatch() {
        controller.startMatch(playerOne, playerTwo);

        controller.resetSession();

        assertNull(controller.getMatch());
    }

    @Test
    void resetSessionClearsLocalPlayer() {
        controller.setLocalPlayer(playerOne);

        controller.resetSession();

        assertNull(controller.getLocalPlayer());
    }

    @Test
    void resetSessionStopsActionListener() {
        controller.setNetworkManager(networkManager);
        controller.setNetworkRole(Role.HOST);
        controller.startListeningForActions();

        controller.resetSession();

        assertFalse(controller.isListeningForActions());
    }

    //TODO
    /*
    @Test
    void resetSessionClearsNetworkManager() {
        controller.setNetworkManager(networkManager);
        controller.setNetworkRole(Role.HOST);

        controller.resetSession();

        assertThrows(
                IllegalStateException.class,
                controller::startListeningForActions
        );
    }
    */

    @Test
    void resetSessionRestoresNotConnectedRole() {
        controller.setNetworkManager(networkManager);
        controller.setNetworkRole(Role.HOST);

        controller.resetSession();

        controller.setNetworkManager(networkManager);

        assertThrows(
                IllegalStateException.class,
                controller::startListeningForActions
        );
    }

    @Test
    void resetSessionCanBeCalledWithoutActiveSession() {
        assertDoesNotThrow(controller::resetSession);
    }

    @Test
    void resetSessionCanBeCalledMultipleTimes() {
        controller.resetSession();

        assertDoesNotThrow(controller::resetSession);

        assertNull(controller.getMatch());
        assertNull(controller.getLocalPlayer());
        assertFalse(controller.isListeningForActions());
    }

    @Test
    void controllerCanStartNewMatchAfterResetSession() {
        controller.startMatch(playerOne, playerTwo);

        controller.resetSession();

        Player newPlayerOne =
                new Player(3, "New Player One");
        Player newPlayerTwo =
                new Player(4, "New Player Two");

        controller.startMatch(
                newPlayerOne,
                newPlayerTwo
        );

        assertSame(
                newPlayerOne,
                controller.getMatch().getPlayerOne()
        );

        assertSame(
                newPlayerTwo,
                controller.getMatch().getPlayerTwo()
        );
    }

    @Test
    void resetSessionClearsPreviousLocalPlayerBeforeNewSession() {
        controller.setLocalPlayer(playerOne);
        controller.startMatch(playerOne, playerTwo);

        controller.resetSession();

        Player newPlayerOne =
                new Player(3, "New Player One");
        Player newPlayerTwo =
                new Player(4, "New Player Two");

        controller.startMatch(
                newPlayerOne,
                newPlayerTwo
        );

        assertNull(controller.getLocalPlayer());
    }

    // -------------------------------------------------------------------------
    // startMatch - common and non-host behavior
    // -------------------------------------------------------------------------

    @Test
    void startMatchCreatesNewMatch() {
        controller.startMatch(playerOne, playerTwo);

        assertNotNull(controller.getMatch());
    }

    @Test
    void startMatchStoresPlayerOne() {
        controller.startMatch(playerOne, playerTwo);

        assertSame(
                playerOne,
                controller.getMatch().getPlayerOne()
        );
    }

    @Test
    void startMatchStoresPlayerTwo() {
        controller.startMatch(playerOne, playerTwo);

        assertSame(
                playerTwo,
                controller.getMatch().getPlayerTwo()
        );
    }

    @Test
    void startMatchSetsMatchStatusToRunning() {
        controller.startMatch(playerOne, playerTwo);

        assertEquals(
                MatchStatus.RUNNING,
                controller.getMatch().getMatchStatus()
        );
    }

    @Test
    void startMatchCreatesGameState() {
        controller.startMatch(playerOne, playerTwo);

        assertNotNull(controller.getMatch().getGameState());
    }

    @Test
    void startMatchCreatesMatchState() {
        controller.startMatch(playerOne, playerTwo);

        assertNotNull(controller.getMatch().getMatchState());
    }

    @Test
    void startMatchStartsInRoundOne() {
        controller.startMatch(playerOne, playerTwo);

        assertEquals(
                1,
                controller.getMatch()
                        .getMatchState()
                        .getCurrentRound()
        );
    }

    @Test
    void startMatchUsesOneMatchPlayerAsStartingPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Player startingPlayer =
                controller.getMatch()
                        .getGameState()
                        .getStartingPlayer();

        assertTrue(
                startingPlayer == playerOne
                        || startingPlayer == playerTwo
        );
    }

    @Test
    void startMatchUsesOtherMatchPlayerAsSecondPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Player startingPlayer =
                controller.getMatch()
                        .getGameState()
                        .getStartingPlayer();

        Player secondPlayer =
                controller.getMatch()
                        .getGameState()
                        .getSecondPlayer();

        assertTrue(
                startingPlayer != secondPlayer
        );

        assertTrue(
                startingPlayer == playerOne
                        || startingPlayer == playerTwo
        );

        assertTrue(
                secondPlayer == playerOne
                        || secondPlayer == playerTwo
        );
    }

    @Test
    void startMatchSetsStartingPlayerAsCurrentPlayer() {
        controller.startMatch(playerOne, playerTwo);

        assertSame(
                controller.getMatch()
                        .getGameState()
                        .getStartingPlayer(),
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer()
        );
    }

    @Test
    void startMatchRejectsNullPlayerOne() {
        assertThrows(
                NullPointerException.class,
                () -> controller.startMatch(null, playerTwo)
        );
    }

    @Test
    void startMatchRejectsNullPlayerTwo() {
        assertThrows(
                NullPointerException.class,
                () -> controller.startMatch(playerOne, null)
        );
    }

    @Test
    void startMatchRejectsBothPlayersBeingNull() {
        assertThrows(
                NullPointerException.class,
                () -> controller.startMatch(null, null)
        );
    }

    @Test
    void failedStartMatchDoesNotCreateMatch() {
        assertThrows(
                NullPointerException.class,
                () -> controller.startMatch(null, playerTwo)
        );

        assertNull(controller.getMatch());
    }

    @Test
    void startMatchReplacesExistingMatch() {
        controller.startMatch(playerOne, playerTwo);
        Match firstMatch = controller.getMatch();

        Player newPlayerOne =
                new Player(3, "New Player One");
        Player newPlayerTwo =
                new Player(4, "New Player Two");

        controller.startMatch(newPlayerOne, newPlayerTwo);

        assertNotSame(firstMatch, controller.getMatch());
        assertSame(
                newPlayerOne,
                controller.getMatch().getPlayerOne()
        );
        assertSame(
                newPlayerTwo,
                controller.getMatch().getPlayerTwo()
        );
    }

    @Test
    void startMatchDoesNotAutomaticallySetLocalPlayer() {
        controller.startMatch(playerOne, playerTwo);

        assertNull(controller.getLocalPlayer());
    }

    @Test
    void startMatchPreservesPreviouslySetLocalPlayer() {
        controller.setLocalPlayer(playerTwo);

        controller.startMatch(playerOne, playerTwo);

        assertSame(playerTwo, controller.getLocalPlayer());
    }

    // -------------------------------------------------------------------------
    // startMatch - listener
    // -------------------------------------------------------------------------

    @Test
    void startMatchDoesNotNotifyReplacedStateListener() {
        AtomicInteger firstListenerCount =
                new AtomicInteger();

        AtomicInteger secondListenerCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                firstListenerCount::incrementAndGet
        );

        controller.setOnStateChangedListener(
                secondListenerCount::incrementAndGet
        );

        controller.startMatch(playerOne, playerTwo);

        assertEquals(0, firstListenerCount.get());
        assertEquals(1, secondListenerCount.get());
    }

    @Test
    void startMatchDoesNotNotifyRemovedStateListener() {
        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.setOnStateChangedListener(null);

        controller.startMatch(playerOne, playerTwo);

        assertEquals(0, notificationCount.get());
    }

    @Test
    void failedStartMatchDoesNotNotifyStateChangedListener() {
        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        assertThrows(
                NullPointerException.class,
                () -> controller.startMatch(null, playerTwo)
        );

        assertEquals(0, notificationCount.get());
    }

    // -------------------------------------------------------------------------
    // startMatch - guest behavior
    // -------------------------------------------------------------------------

    @Test
    void guestStartMatchCreatesRunningMatch() {
        controller.setNetworkRole(Role.GUEST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        assertNotNull(controller.getMatch());
        assertEquals(
                MatchStatus.RUNNING,
                controller.getMatch().getMatchStatus()
        );
    }

    @Test
    void guestStartMatchDoesNotSendGameActions() {
        controller.setNetworkRole(Role.GUEST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any());
    }

    @Test
    void guestStartMatchDoesNotDealInitialCardsLocally() {
        controller.setNetworkRole(Role.GUEST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        assertEquals(0, playerOne.getHandCardCount());
        assertEquals(0, playerTwo.getHandCardCount());
    }

    @Test
    void guestStartMatchWaitsForRemoteBoardSetup() {
        controller.setNetworkRole(Role.GUEST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        assertEquals(
                8,
                controller.getMatch()
                        .getGameState()
                        .getBoard()
                        .getFieldColors()
                        .size()
        );

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any());
    }

    // -------------------------------------------------------------------------
    // startMatch - not connected behavior
    // -------------------------------------------------------------------------

    @Test
    void notConnectedStartMatchCreatesRunningMatch() {
        controller.setNetworkRole(Role.NOT_CONNECTED);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        assertNotNull(controller.getMatch());
        assertEquals(
                MatchStatus.RUNNING,
                controller.getMatch().getMatchStatus()
        );
    }

    @Test
    void notConnectedStartMatchDoesNotSendGameActions() {
        controller.setNetworkRole(Role.NOT_CONNECTED);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any());
    }

    @Test
    void notConnectedStartMatchDoesNotDealInitialCards() {
        controller.setNetworkRole(Role.NOT_CONNECTED);

        controller.startMatch(playerOne, playerTwo);

        assertEquals(0, playerOne.getHandCardCount());
        assertEquals(0, playerTwo.getHandCardCount());
    }

    //TODO
    @Test
    void startMatchWithoutNetworkManagerDoesNotThrow() {
        controller.setNetworkRole(Role.GUEST);

        assertDoesNotThrow(
                () -> controller.startMatch(
                        playerOne,
                        playerTwo
                )
        );
    }

    //TODO
    /*
    @Test
    void startMatchWithoutNetworkConfigurationDoesNotRequireNetworkInteraction() {
        controller.startMatch(playerOne, playerTwo);

        verifyNoInteractions(networkManager);

        assertEquals(
                MatchStatus.RUNNING,
                controller.getMatch().getMatchStatus()
        );
    }
    */

    // -------------------------------------------------------------------------
    // startMatch - host behavior
    // -------------------------------------------------------------------------

    @Test
    void hostStartMatchCreatesRunningMatch() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        assertNotNull(controller.getMatch());
        assertEquals(
                MatchStatus.RUNNING,
                controller.getMatch().getMatchStatus()
        );
    }

    @Test
    void hostStartMatchDealsTwoCardsToStartingPlayer() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        Player startingPlayer =
                controller.getMatch()
                        .getGameState()
                        .getStartingPlayer();

        assertEquals(
                2,
                startingPlayer.getHandCardCount()
        );
    }

    @Test
    void hostStartMatchDealsThreeCardsToSecondPlayer() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        Player secondPlayer =
                controller.getMatch()
                        .getGameState()
                        .getSecondPlayer();

        assertEquals(
                3,
                secondPlayer.getHandCardCount()
        );
    }

    @Test
    void hostStartMatchDealsFiveCardsInTotal() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        int totalCards =
                playerOne.getHandCardCount()
                        + playerTwo.getHandCardCount();

        assertEquals(5, totalCards);
    }

    @Test
    void hostStartMatchDealsOnlyValidCards() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        List<Card> allCards = new ArrayList<>();
        allCards.addAll(playerOne.getHandCards());
        allCards.addAll(playerTwo.getHandCards());

        assertEquals(5, allCards.size());

        for (Card card : allCards) {
            assertNotNull(card);
            assertTrue(card.getColor().isCardColor());
            assertTrue(card.getValue() >= 1);
            assertTrue(card.getValue() <= 6);
        }
    }

    @Test
    void hostStartMatchSendsExactlyFourteenGameActions() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        verify(
                networkManager,
                times(14)
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    @Test
    void hostStartMatchSendsStartingPlayerActionOnce() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        List<GameAction> actions = captureSentActions();

        long count = actions.stream()
                .filter(action ->
                        action.type()
                                == GameAction.ActionType.SET_STARTING_PLAYER
                )
                .count();

        assertEquals(1, count);
    }

    @Test
    void hostStartMatchSendsEightBoardColorActions() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        List<GameAction> actions = captureSentActions();

        long count = actions.stream()
                .filter(action ->
                        action.type()
                                == GameAction.ActionType.SET_BOARD_COLOR
                )
                .count();

        assertEquals(8, count);
    }

    @Test
    void hostStartMatchSendsFiveDealCardActions() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        List<GameAction> actions = captureSentActions();

        long count = actions.stream()
                .filter(action ->
                        action.type()
                                == GameAction.ActionType.DEAL_CARD
                )
                .count();

        assertEquals(5, count);
    }

    @Test
    void hostStartMatchSendsActionsInExpectedGeneralOrder() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        List<GameAction> actions = captureSentActions();

        assertEquals(
                GameAction.ActionType.SET_STARTING_PLAYER,
                actions.get(0).type()
        );

        for (int index = 1; index <= 8; index++) {
            assertEquals(
                    GameAction.ActionType.SET_BOARD_COLOR,
                    actions.get(index).type()
            );
        }

        for (int index = 9; index <= 13; index++) {
            assertEquals(
                    GameAction.ActionType.DEAL_CARD,
                    actions.get(index).type()
            );
        }
    }

    @Test
    void sentStartingPlayerMatchesLocalStartingPlayer() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        GameAction startingPlayerAction =
                captureSentActions()
                        .stream()
                        .filter(action ->
                                action.type()
                                        == GameAction.ActionType.SET_STARTING_PLAYER
                        )
                        .findFirst()
                        .orElseThrow();

        Player startingPlayer =
                controller.getMatch()
                        .getGameState()
                        .getStartingPlayer();

        int expectedIndex =
                startingPlayer == playerOne ? 0 : 1;

        assertEquals(
                expectedIndex,
                startingPlayerAction.contextSensitiveInt()
        );
    }

    @Test
    void sentBoardColorsMatchLocalBoard() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        List<GameAction> boardActions =
                captureSentActions()
                        .stream()
                        .filter(action ->
                                action.type()
                                        == GameAction.ActionType.SET_BOARD_COLOR
                        )
                        .toList();

        assertEquals(8, boardActions.size());

        for (GameAction action : boardActions) {
            int row = action.boardPositionRow();
            int column = action.boardPositionColumn();

            assertFalse(
                    controller.getMatch()
                            .getGameState()
                            .getBoard()
                            .isCenterField(row, column)
            );

            assertEquals(
                    controller.getMatch()
                            .getGameState()
                            .getBoard()
                            .getField(row, column)
                            .getColor(),
                    action.boardColor()
            );
        }
    }

    @Test
    void hostDoesNotSendCenterFieldAsBoardColor() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        List<GameAction> boardActions =
                captureSentActions()
                        .stream()
                        .filter(action ->
                                action.type()
                                        == GameAction.ActionType.SET_BOARD_COLOR
                        )
                        .toList();

        boolean containsCenter =
                boardActions.stream()
                        .anyMatch(action ->
                                action.boardPositionRow() == 1
                                        && action.boardPositionColumn() == 1
                        );

        assertFalse(containsCenter);
    }

    @Test
    void everySentBoardColorActionHasValidPosition() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        List<GameAction> boardActions =
                captureSentActions()
                        .stream()
                        .filter(action ->
                                action.type()
                                        == GameAction.ActionType.SET_BOARD_COLOR
                        )
                        .toList();

        for (GameAction action : boardActions) {
            assertTrue(action.boardPositionRow() >= 0);
            assertTrue(action.boardPositionRow() <= 2);
            assertTrue(action.boardPositionColumn() >= 0);
            assertTrue(action.boardPositionColumn() <= 2);
            assertNotNull(action.boardColor());
        }
    }

    @Test
    void sentDealCardsMatchLocallyStoredCards() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        List<GameAction> dealActions =
                captureSentActions()
                        .stream()
                        .filter(action ->
                                action.type()
                                        == GameAction.ActionType.DEAL_CARD
                        )
                        .toList();

        List<Card> sentCards =
                dealActions.stream()
                        .map(GameAction::card)
                        .toList();

        List<Card> localCards = new ArrayList<>();
        localCards.addAll(playerOne.getHandCards());
        localCards.addAll(playerTwo.getHandCards());

        assertEquals(5, sentCards.size());
        assertEquals(5, localCards.size());

        for (Card localCard : localCards) {
            assertTrue(sentCards.contains(localCard));
        }
    }

    @Test
    void dealCardActionsTargetCorrectPlayers() {
        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);

        controller.startMatch(playerOne, playerTwo);

        List<GameAction> dealActions =
                captureSentActions()
                        .stream()
                        .filter(action ->
                                action.type()
                                        == GameAction.ActionType.DEAL_CARD
                        )
                        .toList();

        long playerOneDeals =
                dealActions.stream()
                        .filter(action ->
                                action.contextSensitiveInt() == 0
                        )
                        .count();

        long playerTwoDeals =
                dealActions.stream()
                        .filter(action ->
                                action.contextSensitiveInt() == 1
                        )
                        .count();

        if (
                controller.getMatch()
                        .getGameState()
                        .getStartingPlayer() == playerOne
        ) {
            assertEquals(2, playerOneDeals);
            assertEquals(3, playerTwoDeals);
        } else {
            assertEquals(3, playerOneDeals);
            assertEquals(2, playerTwoDeals);
        }
    }

    @Test
    void hostStartMatchNotifiesStateChangedListenerExactlyOnce() {
        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setNetworkRole(Role.HOST);
        controller.setNetworkManager(networkManager);
        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.startMatch(playerOne, playerTwo);

        assertEquals(1, notificationCount.get());
    }

    //TODO
    @Test
    void hostStartMatchWorksWithoutNetworkManager() {
        controller.setNetworkRole(Role.HOST);

        assertDoesNotThrow(
                () -> controller.startMatch(
                        playerOne,
                        playerTwo
                )
        );

        assertEquals(5,
                playerOne.getHandCardCount()
                        + playerTwo.getHandCardCount()
        );
    }

    //TODO
    /*
    @Test
    void hostStartMatchWithoutNetworkManagerStillCreatesValidLocalSetup() {
        controller.setNetworkRole(Role.HOST);

        controller.startMatch(playerOne, playerTwo);

        assertEquals(
                MatchStatus.RUNNING,
                controller.getMatch().getMatchStatus()
        );

        assertEquals(
                5,
                playerOne.getHandCardCount()
                        + playerTwo.getHandCardCount()
        );

        assertEquals(
                8,
                controller.getMatch()
                        .getGameState()
                        .getBoard()
                        .getFieldColors()
                        .size()
        );
    }
    */

    // -------------------------------------------------------------------------
    // State changed listener
    // -------------------------------------------------------------------------

    @Test
    void setOnStateChangedListenerRegistersListener() {
        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.startMatch(playerOne, playerTwo);

        assertEquals(1, notificationCount.get());
    }

    @Test
    void setOnStateChangedListenerReplacesPreviousListener() {
        AtomicInteger firstListenerCount =
                new AtomicInteger();

        AtomicInteger secondListenerCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                firstListenerCount::incrementAndGet
        );

        controller.setOnStateChangedListener(
                secondListenerCount::incrementAndGet
        );

        controller.startMatch(playerOne, playerTwo);

        assertEquals(0, firstListenerCount.get());
        assertEquals(1, secondListenerCount.get());
    }

    @Test
    void setOnStateChangedListenerAcceptsNullToUnregister() {
        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.setOnStateChangedListener(null);

        controller.startMatch(playerOne, playerTwo);

        assertEquals(0, notificationCount.get());
    }

    @Test
    void startMatchNotifiesStateChangedListenerOnce() {
        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.startMatch(playerOne, playerTwo);

        assertEquals(1, notificationCount.get());
    }

    @Test
    void successfulSelectCardNotifiesStateChangedListenerOnce() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.selectCard(playerOne, card);

        assertEquals(1, notificationCount.get());
    }

    @Test
    void rejectedSelectCardDoesNotNotifyStateChangedListener() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.selectCard(playerOne, card);

        assertEquals(0, notificationCount.get());
    }

    @Test
    void unselectCardNotifiesStateChangedListenerOnce() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);
        playerOne.selectCard(card);

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.unselectCard(playerOne);

        assertEquals(1, notificationCount.get());
    }

    @Test
    void unselectCardWithoutSelectionStillNotifiesStateChangedListener() {
        controller.startMatch(playerOne, playerTwo);

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.unselectCard(playerOne);

        assertEquals(1, notificationCount.get());
    }

    @Test
    void successfulDrawCardNotifiesStateChangedListenerOnce() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer =
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer();

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.drawCard(currentPlayer);

        assertEquals(1, notificationCount.get());
    }

    @Test
    void successfulPlayCardNotifiesStateChangedListenerOnce() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer =
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer();

        Card card = new Card(GameColor.YELLOW, 3);

        currentPlayer.addCard(card);
        currentPlayer.selectCard(card);

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.playCard(currentPlayer, 0, 0);

        assertEquals(1, notificationCount.get());
    }

    // -------------------------------------------------------------------------
    // Match aborted listener
    // -------------------------------------------------------------------------

    @Test
    void setOnMatchAbortedListenerRegistersListener() {
        controller.startMatch(playerOne, playerTwo);
        controller.setLocalPlayer(playerOne);

        AtomicInteger abortCount =
                new AtomicInteger();

        controller.setOnMatchAbortedListener(
                abortCount::incrementAndGet
        );

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.MATCH_ABORTED
                )
        );

        assertEquals(1, abortCount.get());
    }

    @Test
    void setOnMatchAbortedListenerReplacesPreviousListener() {
        controller.startMatch(playerOne, playerTwo);
        controller.setLocalPlayer(playerOne);

        AtomicInteger firstListenerCount =
                new AtomicInteger();

        AtomicInteger secondListenerCount =
                new AtomicInteger();

        controller.setOnMatchAbortedListener(
                firstListenerCount::incrementAndGet
        );

        controller.setOnMatchAbortedListener(
                secondListenerCount::incrementAndGet
        );

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.MATCH_ABORTED
                )
        );

        assertEquals(0, firstListenerCount.get());
        assertEquals(1, secondListenerCount.get());
    }

    @Test
    void setOnMatchAbortedListenerAcceptsNullToUnregister() {
        controller.startMatch(playerOne, playerTwo);
        controller.setLocalPlayer(playerOne);

        AtomicInteger abortCount =
                new AtomicInteger();

        controller.setOnMatchAbortedListener(
                abortCount::incrementAndGet
        );

        controller.setOnMatchAbortedListener(null);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.MATCH_ABORTED
                )
        );

        assertEquals(0, abortCount.get());
    }

    @Test
    void matchAbortedActionAlsoNotifiesStateChangedListener() {
        controller.startMatch(playerOne, playerTwo);
        controller.setLocalPlayer(playerOne);

        AtomicInteger stateNotificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                stateNotificationCount::incrementAndGet
        );

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.MATCH_ABORTED
                )
        );

        assertEquals(1, stateNotificationCount.get());
    }

    @Test
    void matchAbortedActionInvokesBothListenersExactlyOnce() {
        controller.startMatch(playerOne, playerTwo);
        controller.setLocalPlayer(playerOne);

        AtomicInteger stateNotificationCount =
                new AtomicInteger();

        AtomicInteger abortNotificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                stateNotificationCount::incrementAndGet
        );

        controller.setOnMatchAbortedListener(
                abortNotificationCount::incrementAndGet
        );

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.MATCH_ABORTED
                )
        );

        assertEquals(1, stateNotificationCount.get());
        assertEquals(1, abortNotificationCount.get());
    }

    // -------------------------------------------------------------------------
    // Listener reset behavior
    // -------------------------------------------------------------------------

    @Test
    void resetSessionRemovesStateChangedListener() {
        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.resetSession();

        controller.startMatch(playerOne, playerTwo);

        assertEquals(0, notificationCount.get());
    }

    @Test
    void resetSessionRemovesMatchAbortedListener() {
        AtomicInteger abortCount =
                new AtomicInteger();

        controller.setOnMatchAbortedListener(
                abortCount::incrementAndGet
        );

        controller.resetSession();

        controller.startMatch(playerOne, playerTwo);
        controller.setLocalPlayer(playerOne);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.MATCH_ABORTED
                )
        );

        assertEquals(0, abortCount.get());
    }

    // -------------------------------------------------------------------------
    // selectCard - successful actions
    // -------------------------------------------------------------------------

    @Test
    void selectCardSelectsOwnedCard() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);

        controller.selectCard(playerOne, card);

        assertSame(card, playerOne.getSelectedCard());
        assertTrue(playerOne.hasSelectedCard());
    }

    @Test
    void selectCardCanSelectCardForEitherMatchPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Card firstCard = new Card(GameColor.YELLOW, 3);
        Card secondCard = new Card(GameColor.GREEN, 5);

        playerOne.addCard(firstCard);
        playerTwo.addCard(secondCard);

        controller.selectCard(playerOne, firstCard);
        assertSame(firstCard, playerOne.getSelectedCard());

        controller.selectCard(playerTwo, secondCard);
        assertSame(secondCard, playerTwo.getSelectedCard());
    }

    @Test
    void selectCardDoesNotRequirePlayerToBeCurrentPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer =
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer();

        Player otherPlayer =
                controller.getMatch()
                        .getOtherPlayer(currentPlayer);

        Card card = new Card(GameColor.CYAN, 4);
        otherPlayer.addCard(card);

        controller.selectCard(otherPlayer, card);

        assertSame(card, otherPlayer.getSelectedCard());
    }

    @Test
    void selectCardAcceptsEqualCardWhenPlayerOwnsEquivalentCard() {
        controller.startMatch(playerOne, playerTwo);

        Card storedCard =
                new Card(GameColor.PURPLE, 2);

        Card equalCard =
                new Card(GameColor.PURPLE, 2);

        playerOne.addCard(storedCard);

        controller.selectCard(playerOne, equalCard);

        assertEquals(equalCard, playerOne.getSelectedCard());
    }

    @Test
    void selectCardCanReplacePreviousSelection() {
        controller.startMatch(playerOne, playerTwo);

        Card firstCard =
                new Card(GameColor.YELLOW, 2);

        Card secondCard =
                new Card(GameColor.GREEN, 4);

        playerOne.addCard(firstCard);
        playerOne.addCard(secondCard);

        controller.selectCard(playerOne, firstCard);
        controller.selectCard(playerOne, secondCard);

        assertSame(secondCard, playerOne.getSelectedCard());
    }

    @Test
    void selectCardDoesNotRemoveCardFromHand() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);

        controller.selectCard(playerOne, card);

        assertTrue(playerOne.hasCard(card));
        assertEquals(1, playerOne.getHandCardCount());
    }

    @Test
    void selectCardDoesNotChangeCurrentPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer =
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer();

        Card card = new Card(GameColor.YELLOW, 3);
        currentPlayer.addCard(card);

        controller.selectCard(currentPlayer, card);

        assertSame(
                currentPlayer,
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer()
        );
    }

    // -------------------------------------------------------------------------
    // selectCard - rejected actions
    // -------------------------------------------------------------------------

    @Test
    void selectCardDoesNothingWhenPlayerDoesNotOwnCard() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);

        controller.selectCard(playerOne, card);

        assertFalse(playerOne.hasSelectedCard());
        assertNull(playerOne.getSelectedCard());
    }

    @Test
    void selectCardDoesNothingForForeignPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Player foreignPlayer =
                new Player(3, "Foreign Player");

        Card card = new Card(GameColor.YELLOW, 3);
        foreignPlayer.addCard(card);

        controller.selectCard(foreignPlayer, card);

        assertFalse(foreignPlayer.hasSelectedCard());
    }

    @Test
    void selectCardDoesNothingForNullPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);

        assertDoesNotThrow(
                () -> controller.selectCard(null, card)
        );
    }

    @Test
    void selectCardRejectsNullCard() {
        controller.startMatch(playerOne, playerTwo);

        assertThrows(
                NullPointerException.class,
                () -> controller.selectCard(playerOne, null)
        );
    }

    @Test
    void selectCardDoesNothingWhenMatchIsPaused() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);

        controller.getMatch()
                .setMatchStatus(MatchStatus.PAUSED);

        controller.selectCard(playerOne, card);

        assertFalse(playerOne.hasSelectedCard());
    }

    @Test
    void selectCardDoesNothingWhenMatchIsFinished() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);

        controller.getMatch()
                .setMatchStatus(MatchStatus.FINISHED);

        controller.selectCard(playerOne, card);

        assertFalse(playerOne.hasSelectedCard());
    }

    @Test
    void rejectedSelectionDoesNotReplacePreviousSelection() {
        controller.startMatch(playerOne, playerTwo);

        Card ownedCard =
                new Card(GameColor.YELLOW, 3);

        Card unownedCard =
                new Card(GameColor.GREEN, 5);

        playerOne.addCard(ownedCard);
        controller.selectCard(playerOne, ownedCard);

        controller.selectCard(playerOne, unownedCard);

        assertSame(ownedCard, playerOne.getSelectedCard());
    }

    // -------------------------------------------------------------------------
    // selectCard - network communication
    // -------------------------------------------------------------------------

    @Test
    void successfulSelectCardSendsSelectCardAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);

        controller.selectCard(playerOne, card);

        ArgumentCaptor<GameAction> captor =
                ArgumentCaptor.forClass(GameAction.class);

        verify(networkManager)
                .sendGameChange(captor.capture());

        GameAction sentAction = captor.getValue();

        assertEquals(
                GameAction.ActionType.SELECT_CARD,
                sentAction.type()
        );

        assertSame(card, sentAction.card());
    }

    @Test
    void successfulSelectCardSendsExactlyOneAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);

        controller.selectCard(playerOne, card);

        verify(
                networkManager,
                times(1)
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    @Test
    void rejectedSelectCardDoesNotSendAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Card unownedCard =
                new Card(GameColor.YELLOW, 3);

        controller.selectCard(playerOne, unownedCard);

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    @Test
    void selectCardWithNullPlayerDoesNotSendAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);

        controller.selectCard(null, card);

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    @Test
    void selectCardWithNullCardDoesNotSendAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        assertThrows(
                NullPointerException.class,
                () -> controller.selectCard(playerOne, null)
        );

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    // -------------------------------------------------------------------------
    // unselectCard - successful actions
    // -------------------------------------------------------------------------

    @Test
    void unselectCardClearsSelectedCard() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);
        playerOne.selectCard(card);

        controller.unselectCard(playerOne);

        assertNull(playerOne.getSelectedCard());
        assertFalse(playerOne.hasSelectedCard());
    }

    @Test
    void unselectCardDoesNotRemoveCardFromHand() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);
        playerOne.selectCard(card);

        controller.unselectCard(playerOne);

        assertTrue(playerOne.hasCard(card));
        assertEquals(1, playerOne.getHandCardCount());
    }

    @Test
    void unselectCardDoesNotChangeCurrentPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer =
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer();

        Card card = new Card(GameColor.YELLOW, 3);
        currentPlayer.addCard(card);
        currentPlayer.selectCard(card);

        controller.unselectCard(currentPlayer);

        assertSame(
                currentPlayer,
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer()
        );
    }

    @Test
    void unselectCardDoesNothingWhenNoCardIsSelected() {
        controller.startMatch(playerOne, playerTwo);

        assertDoesNotThrow(
                () -> controller.unselectCard(playerOne)
        );

        assertFalse(playerOne.hasSelectedCard());
    }

    @Test
    void unselectCardCanBeCalledMultipleTimes() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);
        playerOne.selectCard(card);

        controller.unselectCard(playerOne);

        assertDoesNotThrow(
                () -> controller.unselectCard(playerOne)
        );

        assertFalse(playerOne.hasSelectedCard());
    }

    @Test
    void unselectCardDoesNothingWhenMatchIsPaused() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);
        playerOne.selectCard(card);

        controller.getMatch()
                .setMatchStatus(MatchStatus.PAUSED);

        controller.unselectCard(playerOne);

        assertSame(card, playerOne.getSelectedCard());
        assertTrue(playerOne.hasSelectedCard());
    }

    @Test
    void unselectCardDoesNothingWhenMatchIsFinished() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);
        playerOne.selectCard(card);

        controller.getMatch()
                .setMatchStatus(MatchStatus.FINISHED);

        controller.unselectCard(playerOne);

        assertSame(card, playerOne.getSelectedCard());
        assertTrue(playerOne.hasSelectedCard());
    }

    // -------------------------------------------------------------------------
    // unselectCard - network communication
    // -------------------------------------------------------------------------

    @Test
    void unselectCardSendsUnselectCardAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);
        playerOne.selectCard(card);

        controller.unselectCard(playerOne);

        ArgumentCaptor<GameAction> captor =
                ArgumentCaptor.forClass(GameAction.class);

        verify(networkManager)
                .sendGameChange(captor.capture());

        GameAction sentAction = captor.getValue();

        assertEquals(
                GameAction.ActionType.UNSELECT_CARD,
                sentAction.type()
        );

        assertNull(sentAction.card());
    }

    @Test
    void unselectCardSendsActionEvenWithoutExistingSelection() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        controller.unselectCard(playerOne);

        verify(
                networkManager,
                times(1)
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    @Test
    void unselectCardWithoutNetworkManagerStillClearsSelection() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);
        playerOne.selectCard(card);

        controller.unselectCard(playerOne);

        assertFalse(playerOne.hasSelectedCard());
    }

    // -------------------------------------------------------------------------
    // unselectCard - invalid player behavior
    // -------------------------------------------------------------------------

    @Test
    void unselectCardWithNullPlayerThrowsNullPointerException() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        assertThrows(
                NullPointerException.class,
                () -> controller.unselectCard(null)
        );
    }

    @Test
    void unselectCardWithNullPlayerDoesNotSendAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        assertThrows(
                NullPointerException.class,
                () -> controller.unselectCard(null)
        );

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    @Test
    void unselectCardDoesNothingForForeignPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Player foreignPlayer =
                new Player(3, "Foreign Player");

        Card card = new Card(GameColor.CYAN, 4);
        foreignPlayer.addCard(card);
        foreignPlayer.selectCard(card);

        controller.unselectCard(foreignPlayer);

        assertSame(card, foreignPlayer.getSelectedCard());
        assertTrue(foreignPlayer.hasSelectedCard());
    }

    @Test
    void rejectedUnselectCardForForeignPlayerDoesNotSendAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Player foreignPlayer =
                new Player(3, "Foreign Player");

        Card card = new Card(GameColor.CYAN, 4);
        foreignPlayer.addCard(card);
        foreignPlayer.selectCard(card);

        controller.unselectCard(foreignPlayer);

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    @Test
    void rejectedUnselectCardWhenMatchIsPausedDoesNotSendAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);
        playerOne.selectCard(card);

        controller.getMatch()
                .setMatchStatus(MatchStatus.PAUSED);

        controller.unselectCard(playerOne);

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    @Test
    void rejectedUnselectCardDoesNotNotifyStateChangedListener() {
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 3);
        playerOne.addCard(card);
        playerOne.selectCard(card);

        controller.getMatch()
                .setMatchStatus(MatchStatus.FINISHED);

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.unselectCard(playerOne);

        assertEquals(0, notificationCount.get());
    }

    // -------------------------------------------------------------------------
// playCard - score calculation
// -------------------------------------------------------------------------

    @Test
    void playCardOnGreyFieldAwardsCardValue() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 4);

        prepareSelectedCard(currentPlayer, card);

        Field greyField = findEmptyFieldWithColor(GameColor.GREY);

        controller.playCard(
                currentPlayer,
                greyField.getRow(),
                greyField.getColumn()
        );

        assertEquals(4, currentPlayer.getScore());
        assertEquals(4, greyField.getDisplayedScore());
    }

    @Test
    void playCardOnMatchingColoredFieldAwardsDoubleCardValue() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        Field coloredField = findEmptyCardColoredField();

        Card card = new Card(
                coloredField.getColor(),
                5
        );

        prepareSelectedCard(currentPlayer, card);

        controller.playCard(
                currentPlayer,
                coloredField.getRow(),
                coloredField.getColumn()
        );

        assertEquals(10, currentPlayer.getScore());
        assertEquals(10, coloredField.getDisplayedScore());
    }

    @Test
    void playCardOnDifferentColoredFieldAwardsZeroPoints() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        Field coloredField = findEmptyCardColoredField();

        GameColor differentColor =
                findDifferentCardColor(coloredField.getColor());

        Card card = new Card(differentColor, 6);

        prepareSelectedCard(currentPlayer, card);

        controller.playCard(
                currentPlayer,
                coloredField.getRow(),
                coloredField.getColumn()
        );

        assertEquals(0, currentPlayer.getScore());
        assertEquals(0, coloredField.getDisplayedScore());
    }

    @Test
    void playCardAccumulatesScoreWithExistingScore() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        currentPlayer.addScore(7);

        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(currentPlayer, card);

        Field greyField = findEmptyFieldWithColor(GameColor.GREY);

        controller.playCard(
                currentPlayer,
                greyField.getRow(),
                greyField.getColumn()
        );

        assertEquals(10, currentPlayer.getScore());
    }

// -------------------------------------------------------------------------
// playCard - successful state changes
// -------------------------------------------------------------------------

    @Test
    void playCardPlacesSelectedCardOnTargetField() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);

        prepareSelectedCard(currentPlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertSame(card, targetField.getCard());
    }

    @Test
    void playCardStoresPlayerIdAsCardOwner() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.GREEN, 2);

        prepareSelectedCard(currentPlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertEquals(
                currentPlayer.getId(),
                targetField.getCardOwnerId()
        );
    }

    @Test
    void playCardRemovesCardFromPlayersHand() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.CYAN, 4);

        prepareSelectedCard(currentPlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertFalse(currentPlayer.hasCard(card));
        assertEquals(0, currentPlayer.getHandCardCount());
    }

    @Test
    void playCardRemovesOnlyPlayedCardFromHand() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        Card playedCard = new Card(GameColor.YELLOW, 3);
        Card remainingCard = new Card(GameColor.GREEN, 5);

        currentPlayer.addCard(playedCard);
        currentPlayer.addCard(remainingCard);
        currentPlayer.selectCard(playedCard);

        Field targetField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertFalse(currentPlayer.hasCard(playedCard));
        assertTrue(currentPlayer.hasCard(remainingCard));
        assertEquals(1, currentPlayer.getHandCardCount());
    }

    @Test
    void playCardRemovesOnlyOneOfTwoEqualCards() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        Card firstCard = new Card(GameColor.PURPLE, 4);
        Card secondCard = new Card(GameColor.PURPLE, 4);

        currentPlayer.addCard(firstCard);
        currentPlayer.addCard(secondCard);
        currentPlayer.selectCard(firstCard);

        Field targetField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertEquals(1, currentPlayer.getHandCardCount());
        assertTrue(
                currentPlayer.hasCard(
                        new Card(GameColor.PURPLE, 4)
                )
        );
    }

    @Test
    void playCardClearsSelectedCard() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);

        prepareSelectedCard(currentPlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertNull(currentPlayer.getSelectedCard());
        assertFalse(currentPlayer.hasSelectedCard());
    }

    @Test
    void playCardSwitchesTurnToOtherPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Player otherPlayer =
                controller.getMatch()
                        .getOtherPlayer(currentPlayer);

        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(currentPlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertSame(
                otherPlayer,
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer()
        );
    }

    @Test
    void playCardNotifiesStateChangedListenerOnce() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);

        prepareSelectedCard(currentPlayer, card);

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        Field targetField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertEquals(1, notificationCount.get());
    }

// -------------------------------------------------------------------------
// playCard - network action
// -------------------------------------------------------------------------

    @Test
    void successfulPlayCardSendsPlayCardAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);

        prepareSelectedCard(currentPlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        ArgumentCaptor<GameAction> captor =
                ArgumentCaptor.forClass(GameAction.class);

        verify(networkManager)
                .sendGameChange(captor.capture());

        GameAction action = captor.getValue();

        assertEquals(
                GameAction.ActionType.PLAY_CARD,
                action.type()
        );

        assertSame(card, action.card());

        assertEquals(
                targetField.getRow(),
                action.boardPositionRow()
        );

        assertEquals(
                targetField.getColumn(),
                action.boardPositionColumn()
        );
    }

    @Test
    void successfulPlayCardSendsExactlyOneAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.GREEN, 2);

        prepareSelectedCard(currentPlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        verify(
                networkManager,
                times(1)
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    //TODO
    /*
    @Test
    void playCardWithoutNetworkManagerStillAppliesLocally() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.CYAN, 5);

        prepareSelectedCard(currentPlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertSame(card, targetField.getCard());
    }
   */

// -------------------------------------------------------------------------
// playCard - rejected actions
// -------------------------------------------------------------------------

    @Test
    void playCardDoesNothingWhenPlayerHasNoSelectedCard() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Field targetField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertTrue(targetField.isEmpty());
        assertEquals(0, currentPlayer.getScore());
    }

    @Test
    void playCardDoesNothingWhenItIsNotPlayersTurn() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Player otherPlayer =
                controller.getMatch()
                        .getOtherPlayer(currentPlayer);

        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(otherPlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.playCard(
                otherPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertTrue(targetField.isEmpty());
        assertTrue(otherPlayer.hasCard(card));
        assertSame(card, otherPlayer.getSelectedCard());
    }

    @Test
    void playCardDoesNothingForForeignPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Player foreignPlayer =
                new Player(3, "Foreign Player");

        Card card = new Card(GameColor.GREEN, 3);
        prepareSelectedCard(foreignPlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.playCard(
                foreignPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertTrue(targetField.isEmpty());
        assertTrue(foreignPlayer.hasCard(card));
    }

    @Test
    void playCardRejectsNullPlayer() {
        controller.startMatch(playerOne, playerTwo);

        assertThrows(
                NullPointerException.class,
                () -> controller.playCard(null, 0, 0)
        );
    }

    @Test
    void playCardDoesNothingOnCenterField() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);

        prepareSelectedCard(currentPlayer, card);

        controller.playCard(currentPlayer, 1, 1);

        Field centerField =
                controller.getMatch()
                        .getGameState()
                        .getBoard()
                        .getField(1, 1);

        assertTrue(centerField.isEmpty());
        assertTrue(currentPlayer.hasCard(card));
        assertSame(card, currentPlayer.getSelectedCard());
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 0",
            "0, -1",
            "3, 0",
            "0, 3",
            "-1, -1",
            "3, 3"
    })
    void playCardDoesNothingForPositionOutsideBoard(
            int row,
            int column
    ) {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);

        prepareSelectedCard(currentPlayer, card);

        assertDoesNotThrow(
                () -> controller.playCard(
                        currentPlayer,
                        row,
                        column
                )
        );

        assertTrue(currentPlayer.hasCard(card));
        assertSame(card, currentPlayer.getSelectedCard());
        assertEquals(0, currentPlayer.getScore());
    }

    @Test
    void playCardDoesNothingOnOccupiedField() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Player otherPlayer =
                controller.getMatch()
                        .getOtherPlayer(currentPlayer);

        Field targetField = findEmptyPlayableField();

        Card existingCard =
                new Card(GameColor.GREEN, 2);

        targetField.placeCard(
                existingCard,
                otherPlayer,
                2
        );

        Card selectedCard =
                new Card(GameColor.YELLOW, 3);

        prepareSelectedCard(currentPlayer, selectedCard);

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertSame(existingCard, targetField.getCard());
        assertTrue(currentPlayer.hasCard(selectedCard));
        assertSame(
                selectedCard,
                currentPlayer.getSelectedCard()
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = MatchStatus.class,
            names = {"PAUSED", "FINISHED", "WAITING_FOR_NETWORK"}
    )
    void playCardDoesNothingWhenMatchIsNotRunning(
            MatchStatus status
    ) {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);

        prepareSelectedCard(currentPlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.getMatch().setMatchStatus(status);

        controller.playCard(
                currentPlayer,
                targetField.getRow(),
                targetField.getColumn()
        );

        assertTrue(targetField.isEmpty());
        assertTrue(currentPlayer.hasCard(card));
        assertSame(card, currentPlayer.getSelectedCard());
    }

// -------------------------------------------------------------------------
// playCard - rejected action side effects
// -------------------------------------------------------------------------

    @Test
    void rejectedPlayCardDoesNotSendNetworkAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        controller.playCard(currentPlayer, 0, 0);

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    @Test
    void rejectedPlayCardDoesNotNotifyStateChangedListener() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.playCard(currentPlayer, 0, 0);

        assertEquals(0, notificationCount.get());
    }

    @Test
    void rejectedPlayCardDoesNotChangeCurrentPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        controller.playCard(currentPlayer, 0, 0);

        assertSame(
                currentPlayer,
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer()
        );
    }

    @Test
    void rejectedPlayCardDoesNotChangeScore() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        currentPlayer.addScore(5);

        controller.playCard(currentPlayer, 0, 0);

        assertEquals(5, currentPlayer.getScore());
    }

    @Test
    void rejectedPlayCardDoesNotChangeHandOrSelection() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);

        prepareSelectedCard(currentPlayer, card);

        Field occupiedField = findEmptyPlayableField();

        occupiedField.placeCard(
                new Card(GameColor.GREEN, 2),
                playerTwo,
                2
        );

        controller.playCard(
                currentPlayer,
                occupiedField.getRow(),
                occupiedField.getColumn()
        );

        assertTrue(currentPlayer.hasCard(card));
        assertSame(card, currentPlayer.getSelectedCard());
        assertEquals(1, currentPlayer.getHandCardCount());
    }

    // -------------------------------------------------------------------------
// drawCard - successful actions
// -------------------------------------------------------------------------

    @Test
    void drawCardAddsOneCardToCurrentPlayersHand() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        int previousCardCount = currentPlayer.getHandCardCount();

        controller.drawCard(currentPlayer);

        assertEquals(
                previousCardCount + 1,
                currentPlayer.getHandCardCount()
        );
    }

    @Test
    void drawCardAddsValidCard() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        controller.drawCard(currentPlayer);

        Card drawnCard = currentPlayer.getHandCards().get(0);

        assertNotNull(drawnCard);
        assertTrue(drawnCard.getColor().isCardColor());
        assertTrue(drawnCard.getValue() >= 1);
        assertTrue(drawnCard.getValue() <= 6);
    }

    @Test
    void drawCardPreservesExistingCards() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Card existingCard = new Card(GameColor.YELLOW, 3);

        currentPlayer.addCard(existingCard);

        controller.drawCard(currentPlayer);

        assertTrue(currentPlayer.hasCard(existingCard));
        assertEquals(2, currentPlayer.getHandCardCount());
    }

    @Test
    void drawCardSwitchesTurnToOtherPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Player otherPlayer =
                controller.getMatch().getOtherPlayer(currentPlayer);

        controller.drawCard(currentPlayer);

        assertSame(
                otherPlayer,
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer()
        );
    }

    @Test
    void drawCardDoesNotChangePlayerScore() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        currentPlayer.addScore(8);

        controller.drawCard(currentPlayer);

        assertEquals(8, currentPlayer.getScore());
    }

    @Test
    void drawCardDoesNotModifyBoard() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Board board =
                controller.getMatch()
                        .getGameState()
                        .getBoard();

        controller.drawCard(currentPlayer);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                assertTrue(board.getField(row, column).isEmpty());
            }
        }
    }

    @Test
    void drawCardNotifiesStateChangedListenerOnce() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.drawCard(currentPlayer);

        assertEquals(1, notificationCount.get());
    }

// -------------------------------------------------------------------------
// drawCard - hand limit
// -------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 8, 9})
    void drawCardSucceedsWhenPlayerHasFewerThanTenCards(
            int initialCardCount
    ) {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        addCards(currentPlayer, initialCardCount);

        controller.drawCard(currentPlayer);

        assertEquals(
                initialCardCount + 1,
                currentPlayer.getHandCardCount()
        );
    }

    @Test
    void drawCardAtNineCardsFillsHandToTen() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        addCards(currentPlayer, 9);

        controller.drawCard(currentPlayer);

        assertEquals(10, currentPlayer.getHandCardCount());
    }

    @Test
    void drawCardDoesNothingWhenPlayerHasTenCards() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        addCards(currentPlayer, 10);

        controller.drawCard(currentPlayer);

        assertEquals(10, currentPlayer.getHandCardCount());
    }

    @Test
    void drawCardDoesNothingWhenPlayerHasMoreThanTenCards() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        addCards(currentPlayer, 11);

        controller.drawCard(currentPlayer);

        assertEquals(11, currentPlayer.getHandCardCount());
    }

    @Test
    void rejectedDrawAtHandLimitDoesNotSwitchTurn() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        addCards(currentPlayer, 10);

        controller.drawCard(currentPlayer);

        assertSame(
                currentPlayer,
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer()
        );
    }

// -------------------------------------------------------------------------
// drawCard - invalid players and states
// -------------------------------------------------------------------------

    @Test
    void drawCardDoesNothingWhenItIsNotPlayersTurn() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Player otherPlayer =
                controller.getMatch().getOtherPlayer(currentPlayer);

        controller.drawCard(otherPlayer);

        assertEquals(0, otherPlayer.getHandCardCount());
        assertSame(
                currentPlayer,
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer()
        );
    }

    @Test
    void drawCardDoesNothingForForeignPlayer() {
        controller.startMatch(playerOne, playerTwo);

        Player foreignPlayer =
                new Player(3, "Foreign Player");

        controller.drawCard(foreignPlayer);

        assertEquals(0, foreignPlayer.getHandCardCount());
    }

    @Test
    void drawCardRejectsNullPlayer() {
        controller.startMatch(playerOne, playerTwo);

        assertThrows(
                NullPointerException.class,
                () -> controller.drawCard(null)
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = MatchStatus.class,
            names = {"PAUSED", "FINISHED", "WAITING_FOR_NETWORK"}
    )
    void drawCardDoesNothingWhenMatchIsNotRunning(
            MatchStatus matchStatus
    ) {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        controller.getMatch().setMatchStatus(matchStatus);

        controller.drawCard(currentPlayer);

        assertEquals(0, currentPlayer.getHandCardCount());
        assertSame(
                currentPlayer,
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer()
        );
    }

// -------------------------------------------------------------------------
// drawCard - network action
// -------------------------------------------------------------------------

    @Test
    void successfulDrawCardSendsDrawCardAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        controller.drawCard(currentPlayer);

        ArgumentCaptor<GameAction> captor =
                ArgumentCaptor.forClass(GameAction.class);

        verify(networkManager)
                .sendGameChange(captor.capture());

        GameAction action = captor.getValue();

        assertEquals(
                GameAction.ActionType.DRAW_CARD,
                action.type()
        );

        assertNotNull(action.card());
        assertTrue(action.card().getColor().isCardColor());
        assertTrue(action.card().getValue() >= 1);
        assertTrue(action.card().getValue() <= 6);
    }

    @Test
    void successfulDrawCardSendsExactlyOneAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        controller.drawCard(currentPlayer);

        verify(
                networkManager,
                times(1)
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    @Test
    void sentDrawnCardMatchesLocallyAddedCard() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        controller.drawCard(currentPlayer);

        ArgumentCaptor<GameAction> captor =
                ArgumentCaptor.forClass(GameAction.class);

        verify(networkManager)
                .sendGameChange(captor.capture());

        GameAction sentAction = captor.getValue();

        assertEquals(1, currentPlayer.getHandCardCount());
        assertSame(
                sentAction.card(),
                currentPlayer.getHandCards().get(0)
        );
    }

    //TODO
    @Test
    void drawCardWithoutNetworkManagerStillAppliesLocally() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();

        controller.drawCard(currentPlayer);

        assertEquals(1, currentPlayer.getHandCardCount());
    }

    @Test
    void rejectedDrawCardDoesNotSendAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Player otherPlayer =
                controller.getMatch().getOtherPlayer(currentPlayer);

        controller.drawCard(otherPlayer);

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    @Test
    void drawCardAtHandLimitDoesNotSendAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        addCards(currentPlayer, 10);

        controller.drawCard(currentPlayer);

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    @Test
    void drawCardWithNullPlayerDoesNotSendAction() {
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        assertThrows(
                NullPointerException.class,
                () -> controller.drawCard(null)
        );

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

// -------------------------------------------------------------------------
// drawCard - listener behavior on rejection
// -------------------------------------------------------------------------

    @Test
    void rejectedDrawCardDoesNotNotifyStateChangedListener() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        Player otherPlayer =
                controller.getMatch().getOtherPlayer(currentPlayer);

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.drawCard(otherPlayer);

        assertEquals(0, notificationCount.get());
    }

    @Test
    void drawCardAtHandLimitDoesNotNotifyStateChangedListener() {
        controller.startMatch(playerOne, playerTwo);

        Player currentPlayer = getCurrentPlayer();
        addCards(currentPlayer, 10);

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.drawCard(currentPlayer);

        assertEquals(0, notificationCount.get());
    }

    // -------------------------------------------------------------------------
// handleRemoteAction - SELECT_CARD
// -------------------------------------------------------------------------

    @Test
    void remoteSelectCardSelectsCardOfRemotePlayer() {
        prepareGuestMatch();

        Player remotePlayer = controller.getRemotePlayer();
        Card card = new Card(GameColor.YELLOW, 3);
        remotePlayer.addCard(card);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SELECT_CARD,
                        card
                )
        );

        assertSame(card, remotePlayer.getSelectedCard());
        assertTrue(remotePlayer.hasSelectedCard());
    }

    @Test
    void remoteSelectCardCanReplacePreviousSelection() {
        prepareGuestMatch();

        Player remotePlayer = controller.getRemotePlayer();

        Card firstCard = new Card(GameColor.YELLOW, 2);
        Card secondCard = new Card(GameColor.GREEN, 4);

        remotePlayer.addCard(firstCard);
        remotePlayer.addCard(secondCard);
        remotePlayer.selectCard(firstCard);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SELECT_CARD,
                        secondCard
                )
        );

        assertSame(secondCard, remotePlayer.getSelectedCard());
    }

    @Test
    void remoteSelectCardUsesCardEquality() {
        prepareGuestMatch();

        Player remotePlayer = controller.getRemotePlayer();

        Card storedCard = new Card(GameColor.CYAN, 5);
        Card equalCard = new Card(GameColor.CYAN, 5);

        remotePlayer.addCard(storedCard);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SELECT_CARD,
                        equalCard
                )
        );

        assertEquals(equalCard, remotePlayer.getSelectedCard());
    }

    @Test
    void remoteSelectCardDoesNotRemoveCardFromHand() {
        prepareGuestMatch();

        Player remotePlayer = controller.getRemotePlayer();
        Card card = new Card(GameColor.PURPLE, 3);

        remotePlayer.addCard(card);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SELECT_CARD,
                        card
                )
        );

        assertTrue(remotePlayer.hasCard(card));
        assertEquals(1, remotePlayer.getHandCardCount());
    }

    @Test
    void remoteSelectCardDoesNotSendNetworkAction() {
        prepareGuestMatchWithNetworkManager();

        Player remotePlayer = controller.getRemotePlayer();
        Card card = new Card(GameColor.YELLOW, 3);
        remotePlayer.addCard(card);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SELECT_CARD,
                        card
                )
        );

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

// -------------------------------------------------------------------------
// handleRemoteAction - UNSELECT_CARD
// -------------------------------------------------------------------------

    @Test
    void remoteUnselectCardClearsRemotePlayersSelection() {
        prepareGuestMatch();

        Player remotePlayer = controller.getRemotePlayer();
        Card card = new Card(GameColor.YELLOW, 3);

        remotePlayer.addCard(card);
        remotePlayer.selectCard(card);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.UNSELECT_CARD
                )
        );

        assertNull(remotePlayer.getSelectedCard());
        assertFalse(remotePlayer.hasSelectedCard());
    }

    @Test
    void remoteUnselectCardDoesNotRemoveCardFromHand() {
        prepareGuestMatch();

        Player remotePlayer = controller.getRemotePlayer();
        Card card = new Card(GameColor.YELLOW, 3);

        remotePlayer.addCard(card);
        remotePlayer.selectCard(card);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.UNSELECT_CARD
                )
        );

        assertTrue(remotePlayer.hasCard(card));
        assertEquals(1, remotePlayer.getHandCardCount());
    }

    @Test
    void remoteUnselectCardDoesNotThrowWithoutSelection() {
        prepareGuestMatch();

        assertDoesNotThrow(
                () -> controller.handleRemoteAction(
                        new GameAction(
                                GameAction.ActionType.UNSELECT_CARD
                        )
                )
        );

        assertFalse(
                controller.getRemotePlayer().hasSelectedCard()
        );
    }

    @Test
    void remoteUnselectCardDoesNotSendNetworkAction() {
        prepareGuestMatchWithNetworkManager();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.UNSELECT_CARD
                )
        );

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

// -------------------------------------------------------------------------
// handleRemoteAction - DRAW_CARD
// -------------------------------------------------------------------------

    @Test
    void remoteDrawCardAddsReceivedCardToRemotePlayersHand() {
        prepareGuestMatch();

        Player remotePlayer = controller.getRemotePlayer();
        Card card = new Card(GameColor.GREEN, 5);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.DRAW_CARD,
                        card
                )
        );

        assertTrue(remotePlayer.hasCard(card));
        assertEquals(1, remotePlayer.getHandCardCount());
    }

    @Test
    void remoteDrawCardStoresExactReceivedCardInstance() {
        prepareGuestMatch();

        Player remotePlayer = controller.getRemotePlayer();
        Card card = new Card(GameColor.CYAN, 4);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.DRAW_CARD,
                        card
                )
        );

        assertSame(
                card,
                remotePlayer.getHandCards().get(0)
        );
    }

    @Test
    void remoteDrawCardPreservesExistingCards() {
        prepareGuestMatch();

        Player remotePlayer = controller.getRemotePlayer();

        Card existingCard = new Card(GameColor.YELLOW, 2);
        Card drawnCard = new Card(GameColor.PURPLE, 6);

        remotePlayer.addCard(existingCard);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.DRAW_CARD,
                        drawnCard
                )
        );

        assertTrue(remotePlayer.hasCard(existingCard));
        assertTrue(remotePlayer.hasCard(drawnCard));
        assertEquals(2, remotePlayer.getHandCardCount());
    }

    @Test
    void remoteDrawCardSwitchesTurn() {
        prepareGuestMatch();

        Player remotePlayer = controller.getRemotePlayer();

        controller.getMatch()
                .getGameState()
                .setCurrentPlayer(remotePlayer);

        Player expectedNextPlayer =
                controller.getMatch().getOtherPlayer(remotePlayer);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.DRAW_CARD,
                        new Card(GameColor.GREEN, 3)
                )
        );

        assertSame(
                expectedNextPlayer,
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer()
        );
    }

    @Test
    void remoteDrawCardDoesNotSendNetworkAction() {
        prepareGuestMatchWithNetworkManager();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.DRAW_CARD,
                        new Card(GameColor.GREEN, 3)
                )
        );

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

// -------------------------------------------------------------------------
// handleRemoteAction - PLAY_CARD
// -------------------------------------------------------------------------

    @Test
    void remotePlayCardPlacesCardOnSpecifiedField() {
        prepareGuestMatch();

        Player remotePlayer = prepareRemotePlayerTurn();
        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(remotePlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.handleRemoteAction(
                createRemotePlayAction(card, targetField)
        );

        assertSame(card, targetField.getCard());
    }

    @Test
    void remotePlayCardStoresRemotePlayerAsOwner() {
        prepareGuestMatch();

        Player remotePlayer = prepareRemotePlayerTurn();
        Card card = new Card(GameColor.GREEN, 4);
        prepareSelectedCard(remotePlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.handleRemoteAction(
                createRemotePlayAction(card, targetField)
        );

        assertEquals(
                remotePlayer.getId(),
                targetField.getCardOwnerId()
        );
    }

    @Test
    void remotePlayCardOnGreyFieldAwardsNormalValue() {
        prepareGuestMatch();

        Player remotePlayer = prepareRemotePlayerTurn();
        Card card = new Card(GameColor.YELLOW, 4);
        prepareSelectedCard(remotePlayer, card);

        Field targetField =
                findEmptyFieldWithColor(GameColor.GREY);

        controller.handleRemoteAction(
                createRemotePlayAction(card, targetField)
        );

        assertEquals(4, targetField.getDisplayedScore());
        assertEquals(4, remotePlayer.getScore());
    }

    @Test
    void remotePlayCardOnMatchingFieldAwardsDoubleValue() {
        prepareGuestMatch();

        Player remotePlayer = prepareRemotePlayerTurn();
        Field targetField = findEmptyCardColoredField();

        Card card = new Card(targetField.getColor(), 5);
        prepareSelectedCard(remotePlayer, card);

        controller.handleRemoteAction(
                createRemotePlayAction(card, targetField)
        );

        assertEquals(10, targetField.getDisplayedScore());
        assertEquals(10, remotePlayer.getScore());
    }

    @Test
    void remotePlayCardOnDifferentColoredFieldAwardsZero() {
        prepareGuestMatch();

        Player remotePlayer = prepareRemotePlayerTurn();
        Field targetField = findEmptyCardColoredField();

        Card card = new Card(
                findDifferentCardColor(targetField.getColor()),
                6
        );

        prepareSelectedCard(remotePlayer, card);

        controller.handleRemoteAction(
                createRemotePlayAction(card, targetField)
        );

        assertEquals(0, targetField.getDisplayedScore());
        assertEquals(0, remotePlayer.getScore());
    }

    @Test
    void remotePlayCardRemovesPlayedCardFromHand() {
        prepareGuestMatch();

        Player remotePlayer = prepareRemotePlayerTurn();
        Card card = new Card(GameColor.CYAN, 3);
        prepareSelectedCard(remotePlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.handleRemoteAction(
                createRemotePlayAction(card, targetField)
        );

        assertFalse(remotePlayer.hasCard(card));
        assertEquals(0, remotePlayer.getHandCardCount());
    }

    @Test
    void remotePlayCardClearsSelectedCard() {
        prepareGuestMatch();

        Player remotePlayer = prepareRemotePlayerTurn();
        Card card = new Card(GameColor.PURPLE, 2);
        prepareSelectedCard(remotePlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.handleRemoteAction(
                createRemotePlayAction(card, targetField)
        );

        assertFalse(remotePlayer.hasSelectedCard());
        assertNull(remotePlayer.getSelectedCard());
    }

    @Test
    void remotePlayCardSwitchesTurnWhenGameContinues() {
        prepareGuestMatch();

        Player remotePlayer = prepareRemotePlayerTurn();
        Player expectedNextPlayer =
                controller.getMatch().getOtherPlayer(remotePlayer);

        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(remotePlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.handleRemoteAction(
                createRemotePlayAction(card, targetField)
        );

        assertSame(
                expectedNextPlayer,
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer()
        );
    }

    @Test
    void remotePlayCardUsesCorrectRowAndColumn() {
        prepareGuestMatch();

        Player remotePlayer = prepareRemotePlayerTurn();
        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(remotePlayer, card);

        Field targetField =
                controller.getMatch()
                        .getGameState()
                        .getBoard()
                        .getField(2, 1);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.PLAY_CARD,
                        card,
                        1,
                        2
                )
        );

        assertSame(card, targetField.getCard());
    }

    @Test
    void remotePlayCardDoesNotSendNetworkAction() {
        prepareGuestMatchWithNetworkManager();

        Player remotePlayer = prepareRemotePlayerTurn();
        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(remotePlayer, card);

        Field targetField = findEmptyPlayableField();

        controller.handleRemoteAction(
                createRemotePlayAction(card, targetField)
        );

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

// -------------------------------------------------------------------------
// handleRemoteAction - SET_STARTING_PLAYER
// -------------------------------------------------------------------------

    @Test
    void receivingStartingPlayerAloneDoesNotReplaceGameState() {
        prepareGuestMatch();

        GameState previousGameState =
                controller.getMatch().getGameState();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        1
                )
        );

        assertSame(
                previousGameState,
                controller.getMatch().getGameState()
        );
    }

    @Test
    void startingPlayerIndexZeroSelectsPlayerOneForSetup() {
        prepareGuestMatch();

        GameState previousGameState =
                controller.getMatch().getGameState();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        0
                )
        );

        sendRemoteBoardColors(REMOTE_FIELD_COLORS);

        assertNotSame(
                previousGameState,
                controller.getMatch().getGameState()
        );

        assertSame(
                playerOne,
                controller.getMatch()
                        .getGameState()
                        .getStartingPlayer()
        );
    }

    @Test
    void startingPlayerIndexOneSelectsPlayerTwoForSetup() {
        prepareGuestMatch();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        1
                )
        );

        sendRemoteBoardColors(REMOTE_FIELD_COLORS);

        assertSame(
                playerTwo,
                controller.getMatch()
                        .getGameState()
                        .getStartingPlayer()
        );
    }

// -------------------------------------------------------------------------
// handleRemoteAction - SET_BOARD_COLOR
// -------------------------------------------------------------------------

    @Test
    void fewerThanEightBoardColorsDoNotReplaceGameState() {
        prepareGuestMatch();

        GameState previousGameState =
                controller.getMatch().getGameState();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        0
                )
        );

        for (int index = 0; index < 7; index++) {
            sendRemoteBoardColor(
                    REMOTE_FIELD_COLORS.get(index),
                    index
            );
        }

        assertSame(
                previousGameState,
                controller.getMatch().getGameState()
        );
    }

    @Test
    void eightBoardColorsWithoutStartingPlayerDoNotReplaceGameState() {
        prepareGuestMatch();

        GameState previousGameState =
                controller.getMatch().getGameState();

        sendRemoteBoardColors(REMOTE_FIELD_COLORS);

        assertSame(
                previousGameState,
                controller.getMatch().getGameState()
        );
    }

    @Test
    void startingPlayerAndEightColorsInitializeGuestRound() {
        prepareGuestMatch();

        GameState previousGameState =
                controller.getMatch().getGameState();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        0
                )
        );

        sendRemoteBoardColors(REMOTE_FIELD_COLORS);

        assertNotSame(
                previousGameState,
                controller.getMatch().getGameState()
        );
    }

    @Test
    void receivedBoardColorsAreAppliedInReceptionOrder() {
        prepareGuestMatch();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        0
                )
        );

        sendRemoteBoardColors(REMOTE_FIELD_COLORS);

        assertEquals(
                REMOTE_FIELD_COLORS,
                controller.getMatch()
                        .getGameState()
                        .getBoard()
                        .getFieldColors()
        );
    }

    @Test
    void receivedSetupSetsStartingPlayerAsCurrentPlayer() {
        prepareGuestMatch();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        1
                )
        );

        sendRemoteBoardColors(REMOTE_FIELD_COLORS);

        assertSame(
                playerTwo,
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer()
        );
    }

    @Test
    void secondCompleteRemoteSetupStartsNextRound() {
        prepareGuestMatch();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        0
                )
        );
        sendRemoteBoardColors(REMOTE_FIELD_COLORS);

        assertEquals(
                1,
                controller.getMatch()
                        .getMatchState()
                        .getCurrentRound()
        );

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        1
                )
        );
        sendRemoteBoardColors(SECOND_REMOTE_FIELD_COLORS);

        assertEquals(
                2,
                controller.getMatch()
                        .getMatchState()
                        .getCurrentRound()
        );

        assertEquals(
                SECOND_REMOTE_FIELD_COLORS,
                controller.getMatch()
                        .getGameState()
                        .getBoard()
                        .getFieldColors()
        );

        assertSame(
                playerTwo,
                controller.getMatch()
                        .getGameState()
                        .getStartingPlayer()
        );
    }

    @Test
    void completeRemoteBoardSetupDoesNotSendNetworkActions() {
        prepareGuestMatchWithNetworkManager();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        0
                )
        );

        sendRemoteBoardColors(REMOTE_FIELD_COLORS);

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

// -------------------------------------------------------------------------
// handleRemoteAction - DEAL_CARD
// -------------------------------------------------------------------------

    @Test
    void remoteDealCardWithIndexZeroAddsCardToPlayerOne() {
        prepareGuestMatch();

        Card card = new Card(GameColor.YELLOW, 3);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.DEAL_CARD,
                        card,
                        0
                )
        );

        assertTrue(playerOne.hasCard(card));
        assertEquals(1, playerOne.getHandCardCount());
        assertEquals(0, playerTwo.getHandCardCount());
    }

    @Test
    void remoteDealCardWithIndexOneAddsCardToPlayerTwo() {
        prepareGuestMatch();

        Card card = new Card(GameColor.GREEN, 5);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.DEAL_CARD,
                        card,
                        1
                )
        );

        assertTrue(playerTwo.hasCard(card));
        assertEquals(1, playerTwo.getHandCardCount());
        assertEquals(0, playerOne.getHandCardCount());
    }

    @Test
    void remoteDealCardStoresExactCardInstance() {
        prepareGuestMatch();

        Card card = new Card(GameColor.CYAN, 4);

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.DEAL_CARD,
                        card,
                        0
                )
        );

        assertSame(card, playerOne.getHandCards().get(0));
    }

    @Test
    void remoteDealCardDoesNotChangeCurrentPlayer() {
        prepareGuestMatch();

        Player currentPlayer =
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.DEAL_CARD,
                        new Card(GameColor.PURPLE, 2),
                        0
                )
        );

        assertSame(
                currentPlayer,
                controller.getMatch()
                        .getGameState()
                        .getCurrentPlayer()
        );
    }

    @Test
    void remoteDealCardDoesNotSendNetworkAction() {
        prepareGuestMatchWithNetworkManager();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.DEAL_CARD,
                        new Card(GameColor.PURPLE, 2),
                        0
                )
        );

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

// -------------------------------------------------------------------------
// handleRemoteAction - MATCH_FINISHED
// -------------------------------------------------------------------------

    @Test
    void remoteMatchFinishedSetsMatchStatusToFinished() {
        prepareGuestMatch();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.MATCH_FINISHED
                )
        );

        assertEquals(
                MatchStatus.FINISHED,
                controller.getMatch().getMatchStatus()
        );
    }

    @Test
    void remoteMatchFinishedDoesNotRemoveMatch() {
        prepareGuestMatch();

        Match activeMatch = controller.getMatch();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.MATCH_FINISHED
                )
        );

        assertSame(activeMatch, controller.getMatch());
    }

    @Test
    void remoteMatchFinishedDoesNotSendNetworkAction() {
        prepareGuestMatchWithNetworkManager();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.MATCH_FINISHED
                )
        );

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

// -------------------------------------------------------------------------
// handleRemoteAction - MATCH_ABORTED
// -------------------------------------------------------------------------

    @Test
    void remoteMatchAbortedInvokesAbortListener() {
        prepareGuestMatch();

        AtomicInteger abortCount = new AtomicInteger();

        controller.setOnMatchAbortedListener(
                abortCount::incrementAndGet
        );

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.MATCH_ABORTED
                )
        );

        assertEquals(1, abortCount.get());
    }

    @Test
    void remoteMatchAbortedDoesNotAutomaticallyRemoveMatch() {
        prepareGuestMatch();

        Match activeMatch = controller.getMatch();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.MATCH_ABORTED
                )
        );

        assertSame(activeMatch, controller.getMatch());
    }

    @Test
    void remoteMatchAbortedDoesNotAutomaticallyFinishMatch() {
        prepareGuestMatch();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.MATCH_ABORTED
                )
        );

        assertEquals(
                MatchStatus.RUNNING,
                controller.getMatch().getMatchStatus()
        );
    }

    @Test
    void remoteMatchAbortedDoesNotSendNetworkAction() {
        prepareGuestMatchWithNetworkManager();

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.MATCH_ABORTED
                )
        );

        verify(
                networkManager,
                never()
        ).sendGameChange(Mockito.any(GameAction.class));
    }

// -------------------------------------------------------------------------
// handleRemoteAction - state listener
// -------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(
            value = GameAction.ActionType.class,
            names = {
                    "UNSELECT_CARD",
                    "MATCH_FINISHED",
                    "MATCH_ABORTED"
            }
    )
    void remoteActionNotifiesStateChangedListenerOnce(
            GameAction.ActionType type
    ) {
        prepareGuestMatch();

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.handleRemoteAction(new GameAction(type));

        assertEquals(1, notificationCount.get());
    }

    @Test
    void partialBoardSetupNotifiesListenerForEachReceivedAction() {
        prepareGuestMatch();

        AtomicInteger notificationCount =
                new AtomicInteger();

        controller.setOnStateChangedListener(
                notificationCount::incrementAndGet
        );

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        0
                )
        );

        for (int index = 0; index < 3; index++) {
            sendRemoteBoardColor(
                    REMOTE_FIELD_COLORS.get(index),
                    index
            );
        }

        assertEquals(4, notificationCount.get());
    }

    // -------------------------------------------------------------------------
// round end - common behavior
// -------------------------------------------------------------------------

    @Test
    void playingSeventhCardDoesNotEndRound() {
        prepareHostMatchWithoutInitialCards();

        fillPlayableFieldsExcept(2);

        GameState gameStateBeforeMove =
                controller.getMatch().getGameState();

        Player currentPlayer =
                gameStateBeforeMove.getCurrentPlayer();

        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(currentPlayer, card);

        Field emptyField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                emptyField.getRow(),
                emptyField.getColumn()
        );

        assertSame(
                gameStateBeforeMove,
                controller.getMatch().getGameState()
        );

        assertFalse(
                controller.getMatch()
                        .getGameState()
                        .isGameOver()
        );
    }

    @Test
    void playingLastCardEndsCurrentRoundForHost() {
        prepareHostMatchWithoutInitialCards();

        fillPlayableFieldsExcept(1);

        GameState previousGameState =
                controller.getMatch().getGameState();

        Player currentPlayer =
                previousGameState.getCurrentPlayer();

        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(currentPlayer, card);

        Field lastField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                lastField.getRow(),
                lastField.getColumn()
        );

        assertNotSame(
                previousGameState,
                controller.getMatch().getGameState()
        );
    }

    @Test
    void hostAdvancesRoundCounterAfterLastFieldIsPlayed() {
        prepareHostMatchWithoutInitialCards();

        fillPlayableFieldsExcept(1);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(currentPlayer, card);

        Field lastField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                lastField.getRow(),
                lastField.getColumn()
        );

        assertEquals(
                2,
                controller.getMatch()
                        .getMatchState()
                        .getCurrentRound()
        );
    }

    @Test
    void hostCreatesFreshBoardAfterRoundEnds() {
        prepareHostMatchWithoutInitialCards();

        fillPlayableFieldsExcept(1);

        Board previousBoard =
                controller.getMatch()
                        .getGameState()
                        .getBoard();

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(currentPlayer, card);

        Field lastField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                lastField.getRow(),
                lastField.getColumn()
        );

        Board newBoard =
                controller.getMatch()
                        .getGameState()
                        .getBoard();

        assertNotSame(previousBoard, newBoard);
        assertFalse(newBoard.isFull());
    }

    @Test
    void hostDealsFiveNewCardsAfterRoundEnds() {
        prepareHostMatchWithoutInitialCards();

        fillPlayableFieldsExcept(1);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(currentPlayer, card);

        Field lastField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                lastField.getRow(),
                lastField.getColumn()
        );

        int totalCards =
                playerOne.getHandCardCount()
                        + playerTwo.getHandCardCount();

        assertEquals(5, totalCards);
    }

    @Test
    void roundTransitionResetsRoundScores() {
        prepareHostMatchWithoutInitialCards();

        playerOne.addScore(10);
        playerTwo.addScore(7);

        fillPlayableFieldsExcept(1);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(currentPlayer, card);

        Field lastField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                lastField.getRow(),
                lastField.getColumn()
        );

        assertEquals(0, playerOne.getScore());
        assertEquals(0, playerTwo.getScore());
    }

    @Test
    void roundTransitionClearsPreviousHandsAndSelections() {
        prepareHostMatchWithoutInitialCards();

        Card firstExtraCard =
                new Card(GameColor.GREEN, 4);

        Card secondExtraCard =
                new Card(GameColor.CYAN, 5);

        playerOne.addCard(firstExtraCard);
        playerTwo.addCard(secondExtraCard);
        playerOne.selectCard(firstExtraCard);
        playerTwo.selectCard(secondExtraCard);

        fillPlayableFieldsExcept(1);

        Player currentPlayer = getCurrentPlayer();
        Card finalCard = new Card(GameColor.YELLOW, 3);

        currentPlayer.addCard(finalCard);
        currentPlayer.selectCard(finalCard);

        Field lastField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                lastField.getRow(),
                lastField.getColumn()
        );

        assertFalse(playerOne.hasSelectedCard());
        assertFalse(playerTwo.hasSelectedCard());

        assertEquals(
                5,
                playerOne.getHandCardCount()
                        + playerTwo.getHandCardCount()
        );
    }

    // -------------------------------------------------------------------------
// round result and wins
// -------------------------------------------------------------------------

    @Test
    void playerOneReceivesWinWhenPlayerOneHasHigherRoundScore() {
        prepareHostMatchWithoutInitialCards();

        playerOne.addScore(10);
        playerTwo.addScore(4);

        finishDrawnRound();

        assertEquals(1, playerOne.getWins());
        assertEquals(0, playerTwo.getWins());
    }

    @Test
    void playerTwoReceivesWinWhenPlayerTwoHasHigherRoundScore() {
        prepareHostMatchWithoutInitialCards();

        playerOne.addScore(3);
        playerTwo.addScore(9);

        finishDrawnRound();

        assertEquals(0, playerOne.getWins());
        assertEquals(1, playerTwo.getWins());
    }

    @Test
    void drawnRoundDoesNotAddWinToEitherPlayer() {
        prepareHostMatchWithoutInitialCards();

        playerOne.addScore(6);
        playerTwo.addScore(6);

        finishDrawnRound();

        assertEquals(0, playerOne.getWins());
        assertEquals(0, playerTwo.getWins());
    }

    @Test
    void playerOneWinStoresPlayerOneRoundResult() {
        prepareHostMatchWithoutInitialCards();

        playerOne.addScore(12);
        playerTwo.addScore(2);

        finishDrawnRound();

        assertEquals(
                List.of(RoundResult.PLAYER_ONE_WIN),
                controller.getMatch()
                        .getMatchState()
                        .getRoundResults()
        );
    }

    @Test
    void playerTwoWinStoresPlayerTwoRoundResult() {
        prepareHostMatchWithoutInitialCards();

        playerOne.addScore(1);
        playerTwo.addScore(8);

        finishDrawnRound();

        assertEquals(
                List.of(RoundResult.PLAYER_TWO_WIN),
                controller.getMatch()
                        .getMatchState()
                        .getRoundResults()
        );
    }

    @Test
    void drawStoresDrawRoundResult() {
        prepareHostMatchWithoutInitialCards();

        playerOne.addScore(5);
        playerTwo.addScore(5);

        finishDrawnRound();

        assertEquals(
                List.of(RoundResult.DRAW),
                controller.getMatch()
                        .getMatchState()
                        .getRoundResults()
        );
    }

    // -------------------------------------------------------------------------
// round end - host network communication
// -------------------------------------------------------------------------

    @Test
    void hostSendsNewRoundSetupAfterNonFinalRound() {
        prepareHostMatchWithNetworkManagerWithoutInitialCards();

        fillPlayableFieldsExcept(1);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(currentPlayer, card);

        Field lastField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                lastField.getRow(),
                lastField.getColumn()
        );

        ArgumentCaptor<GameAction> captor =
                ArgumentCaptor.forClass(GameAction.class);

        verify(
                networkManager,
                atLeastOnce()
        ).sendGameChange(captor.capture());

        List<GameAction> actions = captor.getAllValues();

        assertEquals(
                1,
                actions.stream()
                        .filter(action ->
                                action.type()
                                        == GameAction.ActionType.PLAY_CARD
                        )
                        .count()
        );

        assertEquals(
                1,
                actions.stream()
                        .filter(action ->
                                action.type()
                                        == GameAction.ActionType.SET_STARTING_PLAYER
                        )
                        .count()
        );

        assertEquals(
                8,
                actions.stream()
                        .filter(action ->
                                action.type()
                                        == GameAction.ActionType.SET_BOARD_COLOR
                        )
                        .count()
        );

        assertEquals(
                5,
                actions.stream()
                        .filter(action ->
                                action.type()
                                        == GameAction.ActionType.DEAL_CARD
                        )
                        .count()
        );
    }

    @Test
    void hostSendsFourteenSetupActionsAfterCompletedNonFinalRound() {
        prepareHostMatchWithNetworkManagerWithoutInitialCards();

        fillPlayableFieldsExcept(1);

        Player currentPlayer = getCurrentPlayer();
        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(currentPlayer, card);

        Field lastField = findEmptyPlayableField();

        controller.playCard(
                currentPlayer,
                lastField.getRow(),
                lastField.getColumn()
        );

        verify(
                networkManager,
                times(15)
        ).sendGameChange(Mockito.any(GameAction.class));
    }

    // -------------------------------------------------------------------------
// round end - guest behavior
// -------------------------------------------------------------------------

    @Test
    void guestDoesNotStartNextRoundAfterBoardBecomesFull() {
        prepareGuestMatch();

        fillPlayableFieldsExcept(1);

        GameState previousGameState =
                controller.getMatch().getGameState();

        Player remotePlayer = prepareRemotePlayerTurn();

        Card card = new Card(GameColor.YELLOW, 3);
        prepareSelectedCard(remotePlayer, card);

        Field lastField = findEmptyPlayableField();

        controller.handleRemoteAction(
                createRemotePlayAction(card, lastField)
        );

        assertSame(
                previousGameState,
                controller.getMatch().getGameState()
        );

        assertTrue(
                controller.getMatch()
                        .getGameState()
                        .isGameOver()
        );
    }

    @Test
    void guestDoesNotAdvanceRoundUntilSetupIsReceived() {
        prepareGuestMatch();

        fillPlayableFieldsExcept(1);

        Player remotePlayer = prepareRemotePlayerTurn();

        Card card = new Card(GameColor.GREEN, 4);
        prepareSelectedCard(remotePlayer, card);

        Field lastField = findEmptyPlayableField();

        controller.handleRemoteAction(
                createRemotePlayAction(card, lastField)
        );

        assertEquals(
                1,
                controller.getMatch()
                        .getMatchState()
                        .getCurrentRound()
        );
    }

    @Test
    void guestStartsNextRoundAfterReceivingCompleteHostSetup() {
        prepareGuestMatch();

        // Initial setup for round one.
        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        0
                )
        );
        sendRemoteBoardColors(REMOTE_FIELD_COLORS);

        assertEquals(
                1,
                controller.getMatch()
                        .getMatchState()
                        .getCurrentRound()
        );

        fillPlayableFieldsExcept(1);

        Player remotePlayer = prepareRemotePlayerTurn();

        Card card = new Card(GameColor.GREEN, 4);
        prepareSelectedCard(remotePlayer, card);

        Field lastField = findEmptyPlayableField();

        controller.handleRemoteAction(
                createRemotePlayAction(card, lastField)
        );

        // Setup for round two.
        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_STARTING_PLAYER,
                        0
                )
        );
        sendRemoteBoardColors(SECOND_REMOTE_FIELD_COLORS);

        assertEquals(
                2,
                controller.getMatch()
                        .getMatchState()
                        .getCurrentRound()
        );
    }

    // -------------------------------------------------------------------------
// match completion
// -------------------------------------------------------------------------

    @Test
    void hostFinishesMatchAfterPlayerOneReachesTwoWins() {
        prepareHostMatchWithoutInitialCards();

        playerOne.addWin();

        playerOne.addScore(10);
        playerTwo.addScore(1);

        finishDrawnRound();

        assertEquals(2, playerOne.getWins());

        assertEquals(
                MatchStatus.FINISHED,
                controller.getMatch().getMatchStatus()
        );
    }

    @Test
    void hostFinishesMatchAfterPlayerTwoReachesTwoWins() {
        prepareHostMatchWithoutInitialCards();

        playerTwo.addWin();

        playerOne.addScore(1);
        playerTwo.addScore(10);

        finishDrawnRound();

        assertEquals(2, playerTwo.getWins());

        assertEquals(
                MatchStatus.FINISHED,
                controller.getMatch().getMatchStatus()
        );
    }

    @Test
    void hostStoresMatchWinnerAfterSecondWin() {
        prepareHostMatchWithoutInitialCards();

        playerOne.addWin();

        playerOne.addScore(12);
        playerTwo.addScore(2);

        finishDrawnRound();

        assertSame(
                playerOne,
                controller.getMatch()
                        .getMatchState()
                        .getMatchWinner()
        );
    }

    @Test
    void hostSendsMatchFinishedAfterDecidingMatch() {
        prepareHostMatchWithNetworkManagerWithoutInitialCards();

        playerOne.addWin();
        playerOne.addScore(10);
        playerTwo.addScore(0);

        finishDrawnRound();

        ArgumentCaptor<GameAction> captor =
                ArgumentCaptor.forClass(GameAction.class);

        verify(
                networkManager,
                atLeastOnce()
        ).sendGameChange(captor.capture());

        assertTrue(
                captor.getAllValues()
                        .stream()
                        .anyMatch(action ->
                                action.type()
                                        == GameAction.ActionType.MATCH_FINISHED
                        )
        );
    }

    @Test
    void hostDoesNotSendNewRoundSetupAfterMatchIsFinished() {
        prepareHostMatchWithNetworkManagerWithoutInitialCards();

        playerOne.addWin();
        playerOne.addScore(10);
        playerTwo.addScore(0);

        finishDrawnRound();

        ArgumentCaptor<GameAction> captor =
                ArgumentCaptor.forClass(GameAction.class);

        verify(
                networkManager,
                atLeastOnce()
        ).sendGameChange(captor.capture());

        List<GameAction> actions = captor.getAllValues();

        assertEquals(
                0,
                actions.stream()
                        .filter(action ->
                                action.type()
                                        == GameAction.ActionType.SET_BOARD_COLOR
                        )
                        .count()
        );

        assertEquals(
                0,
                actions.stream()
                        .filter(action ->
                                action.type()
                                        == GameAction.ActionType.DEAL_CARD
                        )
                        .count()
        );
    }

    // -------------------------------------------------------------------------
    // action listener thread - configuration
    // -------------------------------------------------------------------------

    //TODO
    /*
    @Test
    void startListeningForActionsRejectsMissingNetworkManager() {
        controller.setNetworkRole(Role.HOST);

        assertThrows(
                IllegalStateException.class,
                controller::startListeningForActions
        );
    }
    */

    @Test
    void startListeningForActionsRejectsNotConnectedRole() {
        controller.setNetworkManager(networkManager);
        controller.setNetworkRole(Role.NOT_CONNECTED);

        assertThrows(
                IllegalStateException.class,
                controller::startListeningForActions
        );
    }

    @Test
    void validConfigurationStartsActionListener() throws Exception {
        CountDownLatch fetchStarted = new CountDownLatch(1);

        Mockito.when(networkManager.fetchNextAction())
                .thenAnswer(invocation -> {
                    fetchStarted.countDown();
                    Thread.sleep(Long.MAX_VALUE);
                    return null;
                });

        controller.setNetworkManager(networkManager);
        controller.setNetworkRole(Role.HOST);

        controller.startListeningForActions();

        assertTrue(
                fetchStarted.await(1, TimeUnit.SECONDS)
        );

        assertTrue(controller.isListeningForActions());
    }

    @Test
    void startListeningForActionsDoesNothingWhenAlreadyRunning()
            throws Exception {
        CountDownLatch fetchStarted = new CountDownLatch(1);

        Mockito.when(networkManager.fetchNextAction())
                .thenAnswer(invocation -> {
                    fetchStarted.countDown();
                    Thread.sleep(Long.MAX_VALUE);
                    return null;
                });

        controller.setNetworkManager(networkManager);
        controller.setNetworkRole(Role.HOST);

        controller.startListeningForActions();

        assertTrue(
                fetchStarted.await(1, TimeUnit.SECONDS)
        );

        controller.startListeningForActions();

        verify(
                networkManager,
                atMostOnce()
        ).fetchNextAction();
    }

    @Test
    void stopListeningForActionsStopsRunningListener()
            throws Exception {
        CountDownLatch fetchStarted = new CountDownLatch(1);

        Mockito.when(networkManager.fetchNextAction())
                .thenAnswer(invocation -> {
                    fetchStarted.countDown();

                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException exception) {
                        throw exception;
                    }

                    return null;
                });

        controller.setNetworkManager(networkManager);
        controller.setNetworkRole(Role.HOST);

        controller.startListeningForActions();

        assertTrue(
                fetchStarted.await(1, TimeUnit.SECONDS)
        );

        controller.stopListeningForActions();

        assertFalse(controller.isListeningForActions());
    }

    @Test
    void stopListeningForActionsCanBeCalledWithoutRunningListener() {
        assertDoesNotThrow(
                controller::stopListeningForActions
        );

        assertFalse(controller.isListeningForActions());
    }

    @Test
    void stopListeningForActionsCanBeCalledMultipleTimes() {
        controller.stopListeningForActions();

        assertDoesNotThrow(
                controller::stopListeningForActions
        );

        assertFalse(controller.isListeningForActions());
    }

    @Test
    void resetSessionStopsRunningActionListener()
            throws Exception {
        CountDownLatch fetchStarted = new CountDownLatch(1);

        Mockito.when(networkManager.fetchNextAction())
                .thenAnswer(invocation -> {
                    fetchStarted.countDown();
                    Thread.sleep(Long.MAX_VALUE);
                    return null;
                });

        controller.setNetworkManager(networkManager);
        controller.setNetworkRole(Role.GUEST);

        controller.startListeningForActions();

        assertTrue(
                fetchStarted.await(1, TimeUnit.SECONDS)
        );

        controller.resetSession();

        assertFalse(controller.isListeningForActions());
        assertNull(controller.getMatch());
        assertNull(controller.getLocalPlayer());
    }

    // -------------------------------------------------------------------------
// action listener thread - remote action processing
// -------------------------------------------------------------------------

    @Test
    void actionListenerProcessesFetchedMatchFinishedAction()
            throws Exception {
        controller.setNetworkRole(Role.GUEST);
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        CountDownLatch actionHandled = new CountDownLatch(1);

        controller.setOnStateChangedListener(
                actionHandled::countDown
        );

        Mockito.when(networkManager.fetchNextAction())
                .thenReturn(
                        new GameAction(
                                GameAction.ActionType.MATCH_FINISHED
                        )
                )
                .thenAnswer(invocation -> {
                    Thread.sleep(Long.MAX_VALUE);
                    return null;
                });

        controller.startListeningForActions();

        assertTrue(
                actionHandled.await(1, TimeUnit.SECONDS)
        );

        assertEquals(
                MatchStatus.FINISHED,
                controller.getMatch().getMatchStatus()
        );
    }

    @Test
    void actionListenerProcessesFetchedDealCardAction()
            throws Exception {
        controller.setNetworkRole(Role.GUEST);
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);

        Card card = new Card(GameColor.YELLOW, 4);
        CountDownLatch actionHandled = new CountDownLatch(1);

        controller.setOnStateChangedListener(
                actionHandled::countDown
        );

        Mockito.when(networkManager.fetchNextAction())
                .thenReturn(
                        new GameAction(
                                GameAction.ActionType.DEAL_CARD,
                                card,
                                0
                        )
                )
                .thenAnswer(invocation -> {
                    Thread.sleep(Long.MAX_VALUE);
                    return null;
                });

        controller.startListeningForActions();

        assertTrue(
                actionHandled.await(1, TimeUnit.SECONDS)
        );

        assertTrue(playerOne.hasCard(card));
    }

    @Test
    void interruptedActionListenerStopsCleanly() {
        assertTimeoutPreemptively(
                Duration.ofSeconds(2),
                () -> {
                    CountDownLatch fetchStarted =
                            new CountDownLatch(1);

                    Mockito.when(networkManager.fetchNextAction())
                            .thenAnswer(invocation -> {
                                fetchStarted.countDown();
                                Thread.sleep(Long.MAX_VALUE);
                                return null;
                            });

                    controller.setNetworkManager(networkManager);
                    controller.setNetworkRole(Role.HOST);

                    controller.startListeningForActions();

                    assertTrue(
                            fetchStarted.await(
                                    1,
                                    TimeUnit.SECONDS
                            )
                    );

                    controller.stopListeningForActions();

                    assertFalse(
                            controller.isListeningForActions()
                    );
                }
        );
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private List<GameAction> captureSentActions() {
        ArgumentCaptor<GameAction> captor =
                ArgumentCaptor.forClass(GameAction.class);

        verify(
                networkManager,
                atLeastOnce()
        ).sendGameChange(captor.capture());

        return captor.getAllValues();
    }

    private Player getCurrentPlayer() {
        return controller.getMatch()
                .getGameState()
                .getCurrentPlayer();
    }

    private void prepareSelectedCard(
            Player player,
            Card card
    ) {
        player.addCard(card);
        player.selectCard(card);
    }

    private Field findEmptyPlayableField() {
        Board board =
                controller.getMatch()
                        .getGameState()
                        .getBoard();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (
                        !board.isCenterField(row, column)
                                && board.getField(row, column).isEmpty()
                ) {
                    return board.getField(row, column);
                }
            }
        }

        throw new IllegalStateException(
                "No empty playable field found"
        );
    }

    private Field findEmptyFieldWithColor(GameColor color) {
        Board board =
                controller.getMatch()
                        .getGameState()
                        .getBoard();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (
                        !board.isCenterField(row, column)
                                && board.getField(row, column).isEmpty()
                                && board.getField(row, column).getColor() == color
                ) {
                    return board.getField(row, column);
                }
            }
        }

        throw new IllegalStateException(
                "No empty field found for color " + color
        );
    }

    private Field findEmptyCardColoredField() {
        Board board =
                controller.getMatch()
                        .getGameState()
                        .getBoard();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                Field field = board.getField(row, column);

                if (
                        !board.isCenterField(row, column)
                                && field.isEmpty()
                                && field.getColor().isCardColor()
                ) {
                    return field;
                }
            }
        }

        throw new IllegalStateException(
                "No card-colored field found"
        );
    }

    private GameColor findDifferentCardColor(
            GameColor excludedColor
    ) {
        for (GameColor color : GameColor.values()) {
            if (
                    color.isCardColor()
                            && color != excludedColor
            ) {
                return color;
            }
        }

        throw new IllegalStateException(
                "No different card color found"
        );
    }

    private void addCards(Player player, int amount) {
        for (int index = 0; index < amount; index++) {
            GameColor color = switch (index % 4) {
                case 0 -> GameColor.YELLOW;
                case 1 -> GameColor.GREEN;
                case 2 -> GameColor.CYAN;
                default -> GameColor.PURPLE;
            };

            int value = index % 6 + 1;

            player.addCard(new Card(color, value));
        }
    }

    private void prepareGuestMatch() {
        controller.setNetworkRole(Role.GUEST);
        controller.startMatch(playerOne, playerTwo);
        controller.setLocalPlayer(playerOne);
    }

    private void prepareGuestMatchWithNetworkManager() {
        controller.setNetworkRole(Role.GUEST);
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);
        controller.setLocalPlayer(playerOne);

        Mockito.clearInvocations(networkManager);
    }

    private Player prepareRemotePlayerTurn() {
        Player remotePlayer = controller.getRemotePlayer();

        controller.getMatch()
                .getGameState()
                .setCurrentPlayer(remotePlayer);

        return remotePlayer;
    }

    private GameAction createRemotePlayAction(
            Card card,
            Field field
    ) {
        return new GameAction(
                GameAction.ActionType.PLAY_CARD,
                card,
                field.getColumn(),
                field.getRow()
        );
    }

    private void sendRemoteBoardColors(
            List<GameColor> colors
    ) {
        for (int index = 0; index < colors.size(); index++) {
            sendRemoteBoardColor(colors.get(index), index);
        }
    }

    private void sendRemoteBoardColor(
            GameColor color,
            int index
    ) {
        int[][] positions = {
                {0, 0},
                {0, 1},
                {0, 2},
                {1, 0},
                {1, 2},
                {2, 0},
                {2, 1},
                {2, 2}
        };

        int row = positions[index][0];
        int column = positions[index][1];

        controller.handleRemoteAction(
                new GameAction(
                        GameAction.ActionType.SET_BOARD_COLOR,
                        color,
                        column,
                        row
                )
        );
    }

    private void prepareHostMatchWithoutInitialCards() {
        controller.setNetworkRole(Role.GUEST);
        controller.startMatch(playerOne, playerTwo);

        /*
         * The match is initially created without host-side card dealing.
         * Changing the role afterwards allows testing host round-end behavior
         * without random initial cards affecting the setup.
         */
        controller.setNetworkRole(Role.HOST);
        controller.setLocalPlayer(playerOne);
    }

    private void prepareHostMatchWithNetworkManagerWithoutInitialCards() {
        controller.setNetworkRole(Role.GUEST);
        controller.setNetworkManager(networkManager);
        controller.startMatch(playerOne, playerTwo);
        controller.setLocalPlayer(playerOne);

        Mockito.clearInvocations(networkManager);

        controller.setNetworkRole(Role.HOST);
    }

    private void fillPlayableFieldsExcept(int emptyFieldCount) {
        Board board =
                controller.getMatch()
                        .getGameState()
                        .getBoard();

        int totalPlayableFields = 8;
        int fieldsToFill =
                totalPlayableFields - emptyFieldCount;

        int filledFields = 0;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (board.isCenterField(row, column)) {
                    continue;
                }

                if (filledFields >= fieldsToFill) {
                    return;
                }

                Field field = board.getField(row, column);

                Player owner =
                        filledFields % 2 == 0
                                ? playerOne
                                : playerTwo;

                Card card = new Card(
                        GameColor.YELLOW,
                        filledFields % 6 + 1
                );

                field.placeCard(
                        card,
                        owner,
                        card.getValue()
                );

                filledFields++;
            }
        }
    }

    private void finishCurrentRoundWithZeroPointCard() {
        fillPlayableFieldsExcept(1);

        Player currentPlayer = getCurrentPlayer();
        Field lastField = findEmptyPlayableField();

        GameColor cardColor;

        if (lastField.getColor() == GameColor.GREY) {
            throw new IllegalStateException(
                    "Cannot guarantee zero points on a grey field"
            );
        }

        cardColor = findDifferentCardColor(lastField.getColor());

        Card finalCard = new Card(cardColor, 1);

        prepareSelectedCard(currentPlayer, finalCard);

        controller.playCard(
                currentPlayer,
                lastField.getRow(),
                lastField.getColumn()
        );
    }

    private Field fillBoardLeavingOneCardColoredFieldEmpty() {
        Board board =
                controller.getMatch()
                        .getGameState()
                        .getBoard();

        Field fieldToLeaveEmpty = null;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (board.isCenterField(row, column)) {
                    continue;
                }

                Field field = board.getField(row, column);

                if (field.getColor().isCardColor()) {
                    fieldToLeaveEmpty = field;
                    break;
                }
            }

            if (fieldToLeaveEmpty != null) {
                break;
            }
        }

        if (fieldToLeaveEmpty == null) {
            throw new IllegalStateException(
                    "Board contains no card-colored field"
            );
        }

        int index = 0;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (board.isCenterField(row, column)) {
                    continue;
                }

                Field field = board.getField(row, column);

                if (field == fieldToLeaveEmpty) {
                    continue;
                }

                Card card = new Card(
                        GameColor.YELLOW,
                        index % 6 + 1
                );

                Player owner =
                        index % 2 == 0
                                ? playerOne
                                : playerTwo;

                field.placeCard(card, owner, card.getValue());
                index++;
            }
        }

        return fieldToLeaveEmpty;
    }

    private void finishDrawnRound() {
        Field lastField =
                fillBoardLeavingOneCardColoredFieldEmpty();

        Player currentPlayer = getCurrentPlayer();

        Card zeroPointCard = new Card(
                findDifferentCardColor(lastField.getColor()),
                1
        );

        prepareSelectedCard(currentPlayer, zeroPointCard);

        controller.playCard(
                currentPlayer,
                lastField.getRow(),
                lastField.getColumn()
        );
    }
}