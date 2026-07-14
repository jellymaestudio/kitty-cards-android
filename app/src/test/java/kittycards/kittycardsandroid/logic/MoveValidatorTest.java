package kittycards.kittycardsandroid.logic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.Field;
import kittycards.kittycardsandroid.model.GameColor;
import kittycards.kittycardsandroid.model.Match;
import kittycards.kittycardsandroid.model.MatchStatus;
import kittycards.kittycardsandroid.model.Player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class MoveValidatorTest {

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

    private Player playerOne;
    private Player playerTwo;
    private Player foreignPlayer;

    private Match match;
    private MoveValidator validator;

    private Card yellowCard;
    private Card greenCard;

    @BeforeEach
    void setUp() {
        playerOne = new Player(1, "Player One");
        playerTwo = new Player(2, "Player Two");
        foreignPlayer = new Player(3, "Foreign Player");

        match = new Match(playerOne, playerTwo);
        match.initializeCurrentRound(FIELD_COLORS, playerOne);
        match.setMatchStatus(MatchStatus.RUNNING);

        validator = new MoveValidator(match);

        yellowCard = new Card(GameColor.YELLOW, 3);
        greenCard = new Card(GameColor.GREEN, 5);
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    @Test
    void constructorAcceptsValidMatch() {
        assertDoesNotThrow(() -> new MoveValidator(match));
    }

    @Test
    void constructorRejectsNullMatch() {
        assertThrows(
                NullPointerException.class,
                () -> new MoveValidator(null)
        );
    }

    // -------------------------------------------------------------------------
    // isPlayersTurn
    // -------------------------------------------------------------------------

    @Test
    void isPlayersTurnReturnsTrueForCurrentPlayer() {
        assertTrue(validator.isPlayersTurn(playerOne));
    }

    @Test
    void isPlayersTurnReturnsFalseForOtherMatchPlayer() {
        assertFalse(validator.isPlayersTurn(playerTwo));
    }

    @Test
    void isPlayersTurnReturnsFalseForForeignPlayer() {
        assertFalse(validator.isPlayersTurn(foreignPlayer));
    }

    @Test
    void isPlayersTurnReturnsTrueAfterCurrentPlayerChanges() {
        match.getGameState().setCurrentPlayer(playerTwo);

        assertTrue(validator.isPlayersTurn(playerTwo));
        assertFalse(validator.isPlayersTurn(playerOne));
    }

    @Test
    void isPlayersTurnRejectsNullPlayer() {
        assertThrows(
                NullPointerException.class,
                () -> validator.isPlayersTurn(null)
        );
    }

    // -------------------------------------------------------------------------
    // canPlayCard - valid cases
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Current player should be able to play at ({0}, {1})")
    @CsvSource({
            "0, 0",
            "0, 1",
            "0, 2",
            "1, 0",
            "1, 2",
            "2, 0",
            "2, 1",
            "2, 2"
    })
    void canPlayCardReturnsTrueForValidPlayableField(int row, int column) {
        addAndSelectCard(playerOne, yellowCard);

        assertTrue(validator.canPlayCard(playerOne, row, column));
    }

    @Test
    void canPlayCardReturnsTrueForSecondPlayerWhenItIsTheirTurn() {
        match.getGameState().setCurrentPlayer(playerTwo);
        addAndSelectCard(playerTwo, greenCard);

        assertTrue(validator.canPlayCard(playerTwo, 0, 0));
    }

    // -------------------------------------------------------------------------
    // canPlayCard - invalid match states
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(
            value = MatchStatus.class,
            names = {"PAUSED", "FINISHED", "WAITING_FOR_NETWORK"}
    )
    void canPlayCardReturnsFalseWhenMatchIsNotRunning(
            MatchStatus matchStatus
    ) {
        addAndSelectCard(playerOne, yellowCard);
        match.setMatchStatus(matchStatus);

        assertFalse(validator.canPlayCard(playerOne, 0, 0));
    }

    // -------------------------------------------------------------------------
    // canPlayCard - invalid players
    // -------------------------------------------------------------------------

    @Test
    void canPlayCardReturnsFalseWhenItIsNotPlayersTurn() {
        addAndSelectCard(playerTwo, greenCard);

        assertFalse(validator.canPlayCard(playerTwo, 0, 0));
    }

    @Test
    void canPlayCardReturnsFalseForForeignPlayer() {
        addAndSelectCard(foreignPlayer, yellowCard);

        assertFalse(validator.canPlayCard(foreignPlayer, 0, 0));
    }

    @Test
    void canPlayCardRejectsNullPlayer() {
        assertThrows(
                NullPointerException.class,
                () -> validator.canPlayCard(null, 0, 0)
        );
    }

    // -------------------------------------------------------------------------
    // canPlayCard - invalid positions
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Position ({0}, {1}) should be rejected")
    @CsvSource({
            "-1, 0",
            "0, -1",
            "3, 0",
            "0, 3",
            "-1, -1",
            "-1, 3",
            "3, -1",
            "3, 3",
            "-100, 1",
            "1, 100"
    })
    void canPlayCardReturnsFalseForPositionOutsideBoard(
            int row,
            int column
    ) {
        addAndSelectCard(playerOne, yellowCard);

        assertFalse(validator.canPlayCard(playerOne, row, column));
    }

    @Test
    void canPlayCardReturnsFalseForCenterField() {
        addAndSelectCard(playerOne, yellowCard);

        assertFalse(validator.canPlayCard(playerOne, 1, 1));
    }

    // -------------------------------------------------------------------------
    // canPlayCard - field and selection conditions
    // -------------------------------------------------------------------------

    @Test
    void canPlayCardReturnsFalseForOccupiedField() {
        addAndSelectCard(playerOne, yellowCard);

        Player owner = new Player(5, "Owner");
        Card placedCard = new Card(GameColor.GREEN, 4);

        match.getGameState()
                .getBoard()
                .getField(0, 0)
                .placeCard(placedCard, owner, 4);

        assertFalse(validator.canPlayCard(playerOne, 0, 0));
    }

    @Test
    void canPlayCardReturnsFalseWhenPlayerHasNoSelectedCard() {
        playerOne.addCard(yellowCard);

        assertFalse(validator.canPlayCard(playerOne, 0, 0));
    }

    @Test
    void canPlayCardReturnsFalseWhenPlayersHandIsEmpty() {
        assertFalse(validator.canPlayCard(playerOne, 0, 0));
    }

    @Test
    void canPlayCardUsesSelectedCardState() {
        playerOne.addCard(yellowCard);

        assertFalse(validator.canPlayCard(playerOne, 0, 0));

        playerOne.selectCard(yellowCard);

        assertTrue(validator.canPlayCard(playerOne, 0, 0));

        playerOne.unselectCard();

        assertFalse(validator.canPlayCard(playerOne, 0, 0));
    }

    // -------------------------------------------------------------------------
    // canPlayCard - no side effects
    // -------------------------------------------------------------------------

    @Test
    void canPlayCardDoesNotPlaceSelectedCard() {
        addAndSelectCard(playerOne, yellowCard);

        Field field = match.getGameState()
                .getBoard()
                .getField(0, 0);

        assertTrue(validator.canPlayCard(playerOne, 0, 0));

        assertTrue(field.isEmpty());
    }

    @Test
    void canPlayCardDoesNotRemoveCardFromHand() {
        addAndSelectCard(playerOne, yellowCard);

        validator.canPlayCard(playerOne, 0, 0);

        assertTrue(playerOne.hasCard(yellowCard));
    }

    @Test
    void canPlayCardDoesNotChangeSelectedCard() {
        addAndSelectCard(playerOne, yellowCard);

        validator.canPlayCard(playerOne, 0, 0);

        assertSame(yellowCard, playerOne.getSelectedCard());
    }

    @Test
    void canPlayCardDoesNotChangeCurrentPlayer() {
        addAndSelectCard(playerOne, yellowCard);

        validator.canPlayCard(playerOne, 0, 0);

        assertSame(
                playerOne,
                match.getGameState().getCurrentPlayer()
        );
    }

    // -------------------------------------------------------------------------
    // canDrawCard - valid cases
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Player with {0} cards should be allowed to draw")
    @ValueSource(ints = {0, 1, 5, 8, 9})
    void canDrawCardReturnsTrueWhenHandContainsFewerThanTenCards(
            int cardCount
    ) {
        addCards(playerOne, cardCount);

        assertTrue(validator.canDrawCard(playerOne));
    }

    @Test
    void canDrawCardReturnsTrueForSecondPlayerWhenItIsTheirTurn() {
        match.getGameState().setCurrentPlayer(playerTwo);

        assertTrue(validator.canDrawCard(playerTwo));
    }

    // -------------------------------------------------------------------------
    // canDrawCard - invalid conditions
    // -------------------------------------------------------------------------

    @Test
    void canDrawCardReturnsFalseWhenHandContainsExactlyTenCards() {
        addCards(playerOne, 10);

        assertFalse(validator.canDrawCard(playerOne));
    }

    @Test
    void canDrawCardReturnsFalseWhenHandContainsMoreThanTenCards() {
        addCards(playerOne, 11);

        assertFalse(validator.canDrawCard(playerOne));
    }

    @Test
    void canDrawCardReturnsFalseWhenItIsNotPlayersTurn() {
        assertFalse(validator.canDrawCard(playerTwo));
    }

    @Test
    void canDrawCardReturnsFalseForForeignPlayer() {
        assertFalse(validator.canDrawCard(foreignPlayer));
    }

    @ParameterizedTest
    @EnumSource(
            value = MatchStatus.class,
            names = {"PAUSED", "FINISHED", "WAITING_FOR_NETWORK"}
    )
    void canDrawCardReturnsFalseWhenMatchIsNotRunning(
            MatchStatus matchStatus
    ) {
        match.setMatchStatus(matchStatus);

        assertFalse(validator.canDrawCard(playerOne));
    }

    @Test
    void canDrawCardRejectsNullPlayer() {
        assertThrows(
                NullPointerException.class,
                () -> validator.canDrawCard(null)
        );
    }

    // -------------------------------------------------------------------------
    // canDrawCard - no side effects
    // -------------------------------------------------------------------------

    @Test
    void canDrawCardDoesNotAddCardToHand() {
        int originalCardCount = playerOne.getHandCardCount();

        assertTrue(validator.canDrawCard(playerOne));

        assertTrue(
                playerOne.getHandCardCount() == originalCardCount
        );
    }

    @Test
    void canDrawCardDoesNotChangeCurrentPlayer() {
        validator.canDrawCard(playerOne);

        assertSame(
                playerOne,
                match.getGameState().getCurrentPlayer()
        );
    }

    // -------------------------------------------------------------------------
    // canSelectCard - valid cases
    // -------------------------------------------------------------------------

    @Test
    void canSelectCardReturnsTrueWhenPlayerOwnsCard() {
        playerOne.addCard(yellowCard);

        assertTrue(
                validator.canSelectCard(playerOne, yellowCard)
        );
    }

    @Test
    void canSelectCardReturnsTrueForSecondMatchPlayer() {
        playerTwo.addCard(greenCard);

        assertTrue(
                validator.canSelectCard(playerTwo, greenCard)
        );
    }

    @Test
    void canSelectCardDoesNotRequirePlayerToBeCurrentPlayer() {
        playerTwo.addCard(greenCard);

        assertTrue(
                validator.canSelectCard(playerTwo, greenCard)
        );
    }

    @Test
    void canSelectCardUsesCardEquality() {
        Card storedCard = new Card(GameColor.YELLOW, 3);
        Card equalCard = new Card(GameColor.YELLOW, 3);

        playerOne.addCard(storedCard);

        assertTrue(
                validator.canSelectCard(playerOne, equalCard)
        );
    }

    // -------------------------------------------------------------------------
    // canSelectCard - invalid conditions
    // -------------------------------------------------------------------------

    @Test
    void canSelectCardReturnsFalseWhenPlayerDoesNotOwnCard() {
        assertFalse(
                validator.canSelectCard(playerOne, yellowCard)
        );
    }

    @Test
    void canSelectCardReturnsFalseForDifferentCard() {
        playerOne.addCard(yellowCard);

        assertFalse(
                validator.canSelectCard(playerOne, greenCard)
        );
    }

    @Test
    void canSelectCardReturnsFalseForForeignPlayer() {
        foreignPlayer.addCard(yellowCard);

        assertFalse(
                validator.canSelectCard(foreignPlayer, yellowCard)
        );
    }

    @Test
    void canSelectCardReturnsFalseForNullPlayer() {
        assertFalse(
                validator.canSelectCard(null, yellowCard)
        );
    }

    @Test
    void canSelectCardRejectsNullCard() {
        assertThrows(
                NullPointerException.class,
                () -> validator.canSelectCard(playerOne, null)
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = MatchStatus.class,
            names = {"PAUSED", "FINISHED", "WAITING_FOR_NETWORK"}
    )
    void canSelectCardReturnsFalseWhenMatchIsNotRunning(
            MatchStatus matchStatus
    ) {
        playerOne.addCard(yellowCard);
        match.setMatchStatus(matchStatus);

        assertFalse(
                validator.canSelectCard(playerOne, yellowCard)
        );
    }

    // -------------------------------------------------------------------------
    // canSelectCard - no side effects
    // -------------------------------------------------------------------------

    @Test
    void canSelectCardDoesNotSelectCard() {
        playerOne.addCard(yellowCard);

        assertTrue(
                validator.canSelectCard(playerOne, yellowCard)
        );

        assertFalse(playerOne.hasSelectedCard());
    }

    @Test
    void canSelectCardDoesNotModifyPlayersHand() {
        playerOne.addCard(yellowCard);
        int originalCardCount = playerOne.getHandCardCount();

        validator.canSelectCard(playerOne, yellowCard);

        assertTrue(playerOne.hasCard(yellowCard));
        assertTrue(
                playerOne.getHandCardCount() == originalCardCount
        );
    }

    // -------------------------------------------------------------------------
// canUnselectCard
// -------------------------------------------------------------------------

    @Test
    void canUnselectCardReturnsTrueForPlayerOne() {
        assertTrue(
                validator.canUnselectCard(playerOne)
        );
    }

    @Test
    void canUnselectCardReturnsTrueForPlayerTwo() {
        assertTrue(
                validator.canUnselectCard(playerTwo)
        );
    }

    @Test
    void canUnselectCardDoesNotRequirePlayerToBeCurrentPlayer() {
        assertSame(
                playerOne,
                match.getGameState().getCurrentPlayer()
        );

        assertTrue(
                validator.canUnselectCard(playerTwo)
        );
    }

    @Test
    void canUnselectCardReturnsTrueWhenPlayerHasSelectedCard() {
        playerOne.addCard(yellowCard);
        playerOne.selectCard(yellowCard);

        assertTrue(
                validator.canUnselectCard(playerOne)
        );
    }

    @Test
    void canUnselectCardReturnsTrueWhenPlayerHasNoSelectedCard() {
        assertFalse(playerOne.hasSelectedCard());

        assertTrue(
                validator.canUnselectCard(playerOne)
        );
    }

    @Test
    void canUnselectCardReturnsFalseForForeignPlayer() {
        assertFalse(
                validator.canUnselectCard(foreignPlayer)
        );
    }

    @Test
    void canUnselectCardRejectsNullPlayer() {
        assertThrows(
                NullPointerException.class,
                () -> validator.canUnselectCard(null)
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = MatchStatus.class,
            names = {"PAUSED", "FINISHED", "WAITING_FOR_NETWORK"}
    )
    void canUnselectCardReturnsFalseWhenMatchIsNotRunning(
            MatchStatus matchStatus
    ) {
        playerOne.addCard(yellowCard);
        playerOne.selectCard(yellowCard);

        match.setMatchStatus(matchStatus);

        assertFalse(
                validator.canUnselectCard(playerOne)
        );
    }

    @Test
    void canUnselectCardDoesNotChangeSelectedCard() {
        playerOne.addCard(yellowCard);
        playerOne.selectCard(yellowCard);

        validator.canUnselectCard(playerOne);

        assertSame(
                yellowCard,
                playerOne.getSelectedCard()
        );
    }

    @Test
    void canUnselectCardDoesNotChangeCurrentPlayer() {
        Player currentPlayer =
                match.getGameState().getCurrentPlayer();

        validator.canUnselectCard(playerOne);

        assertSame(
                currentPlayer,
                match.getGameState().getCurrentPlayer()
        );
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void addAndSelectCard(Player player, Card card) {
        player.addCard(card);
        player.selectCard(card);
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
}