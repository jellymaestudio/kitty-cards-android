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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PlayerTest {

    private Player player;
    private Card yellowCard;
    private Card greenCard;

    @BeforeEach
    void setUp() {
        player = new Player(1, "Player One");
        yellowCard = new Card(GameColor.YELLOW, 3);
        greenCard = new Card(GameColor.GREEN, 5);
    }

    @Test
    void constructorAcceptsValidIdAndName() {
        assertDoesNotThrow(() -> new Player(1, "Player One"));
    }

    @ParameterizedTest
    @ValueSource(ints = {
            Integer.MIN_VALUE,
            -100,
            -1,
            0,
            1,
            42,
            Integer.MAX_VALUE
    })
    void constructorAcceptsAnyIntegerId(int id) {
        Player createdPlayer = new Player(id, "Player");

        assertEquals(id, createdPlayer.getId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ",
            "   ",
            "\t",
            "\n",
            "\r\n"
    })
    void constructorRejectsNullEmptyOrBlankName(String invalidName) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Player(1, invalidName)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "A",
            "Player One",
            "Leonard",
            "Player 2",
            " Kitty ",
            "123"
    })
    void constructorAcceptsNonBlankName(String name) {
        Player createdPlayer = new Player(1, name);

        assertEquals(name, createdPlayer.getName());
    }

    @Test
    void getIdReturnsConstructorId() {
        Player createdPlayer = new Player(42, "Player");

        assertEquals(42, createdPlayer.getId());
    }

    @Test
    void getNameReturnsConstructorName() {
        Player createdPlayer = new Player(1, "Kitty Player");

        assertEquals("Kitty Player", createdPlayer.getName());
    }

    @Test
    void newPlayerHasEmptyHand() {
        assertTrue(player.getHandCards().isEmpty());
    }

    @Test
    void newPlayerHasHandCardCountOfZero() {
        assertEquals(0, player.getHandCardCount());
    }

    @Test
    void newPlayerHasNoSelectedCard() {
        assertNull(player.getSelectedCard());
    }

    @Test
    void newPlayerHasNoSelectedCardAccordingToBooleanCheck() {
        assertFalse(player.hasSelectedCard());
    }

    @Test
    void newPlayerHasScoreOfZero() {
        assertEquals(0, player.getScore());
    }

    @Test
    void newPlayerHasZeroWins() {
        assertEquals(0, player.getWins());
    }

    @Test
    void getHandCardsReturnsPlayersHandList() {
        List<Card> handCards = player.getHandCards();

        handCards.add(yellowCard);

        assertTrue(player.hasCard(yellowCard));
        assertEquals(1, player.getHandCardCount());
    }

    @Test
    void addCardAddsCardToHand() {
        player.addCard(yellowCard);

        assertTrue(player.hasCard(yellowCard));
    }

    @Test
    void addCardStoresProvidedCardReference() {
        player.addCard(yellowCard);

        assertSame(yellowCard, player.getHandCards().get(0));
    }

    @Test
    void addCardIncreasesHandCardCount() {
        player.addCard(yellowCard);

        assertEquals(1, player.getHandCardCount());
    }

    @Test
    void addCardRejectsNull() {
        assertThrows(
                NullPointerException.class,
                () -> player.addCard(null)
        );
    }

    @Test
    void rejectedNullCardDoesNotChangeHand() {
        assertThrows(
                NullPointerException.class,
                () -> player.addCard(null)
        );

        assertTrue(player.getHandCards().isEmpty());
        assertEquals(0, player.getHandCardCount());
    }

    @Test
    void addCardCanAddMultipleCards() {
        player.addCard(yellowCard);
        player.addCard(greenCard);

        assertEquals(2, player.getHandCardCount());
        assertTrue(player.hasCard(yellowCard));
        assertTrue(player.hasCard(greenCard));
    }

    @Test
    void addCardPreservesInsertionOrder() {
        Card purpleCard = new Card(GameColor.PURPLE, 2);

        player.addCard(yellowCard);
        player.addCard(greenCard);
        player.addCard(purpleCard);

        assertEquals(
                List.of(yellowCard, greenCard, purpleCard),
                player.getHandCards()
        );
    }

    @Test
    void addCardAllowsDuplicateEqualCards() {
        Card firstCard = new Card(GameColor.CYAN, 4);
        Card secondCard = new Card(GameColor.CYAN, 4);

        player.addCard(firstCard);
        player.addCard(secondCard);

        assertEquals(2, player.getHandCardCount());
        assertSame(firstCard, player.getHandCards().get(0));
        assertSame(secondCard, player.getHandCards().get(1));
    }

    @Test
    void hasCardReturnsTrueForOwnedCard() {
        player.addCard(yellowCard);

        assertTrue(player.hasCard(yellowCard));
    }

    @Test
    void hasCardUsesCardEquality() {
        Card storedCard = new Card(GameColor.YELLOW, 3);
        Card equalCard = new Card(GameColor.YELLOW, 3);

        player.addCard(storedCard);

        assertTrue(player.hasCard(equalCard));
    }

    @Test
    void hasCardReturnsFalseForCardNotInHand() {
        assertFalse(player.hasCard(yellowCard));
    }

    @Test
    void hasCardReturnsFalseForDifferentCard() {
        player.addCard(yellowCard);

        assertFalse(player.hasCard(greenCard));
    }

    @Test
    void hasCardReturnsFalseForNull() {
        assertFalse(player.hasCard(null));
    }

    @Test
    void removeCardRemovesOwnedCard() {
        player.addCard(yellowCard);

        player.removeCard(yellowCard);

        assertFalse(player.hasCard(yellowCard));
        assertEquals(0, player.getHandCardCount());
    }

    @Test
    void removeCardUsesCardEquality() {
        Card storedCard = new Card(GameColor.YELLOW, 3);
        Card equalCard = new Card(GameColor.YELLOW, 3);

        player.addCard(storedCard);
        player.removeCard(equalCard);

        assertTrue(player.getHandCards().isEmpty());
    }

    @Test
    void removeCardRemovesOnlyOneEqualCard() {
        Card firstCard = new Card(GameColor.CYAN, 4);
        Card secondCard = new Card(GameColor.CYAN, 4);

        player.addCard(firstCard);
        player.addCard(secondCard);

        player.removeCard(new Card(GameColor.CYAN, 4));

        assertEquals(1, player.getHandCardCount());
        assertTrue(player.hasCard(new Card(GameColor.CYAN, 4)));
    }

    @Test
    void removeCardDoesNothingWhenCardIsNotOwned() {
        player.addCard(yellowCard);

        player.removeCard(greenCard);

        assertEquals(1, player.getHandCardCount());
        assertTrue(player.hasCard(yellowCard));
    }

    @Test
    void removeCardRejectsNull() {
        assertThrows(
                NullPointerException.class,
                () -> player.removeCard(null)
        );
    }

    @Test
    void removingSelectedCardUnselectsIt() {
        player.addCard(yellowCard);
        player.selectCard(yellowCard);

        player.removeCard(yellowCard);

        assertNull(player.getSelectedCard());
        assertFalse(player.hasSelectedCard());
    }

    @Test
    void removingEqualSelectedCardUnselectsIt() {
        Card storedCard = new Card(GameColor.YELLOW, 3);
        Card equalCard = new Card(GameColor.YELLOW, 3);

        player.addCard(storedCard);
        player.selectCard(storedCard);

        player.removeCard(equalCard);

        assertNull(player.getSelectedCard());
        assertFalse(player.hasSelectedCard());
    }

    @Test
    void removingDifferentCardKeepsSelection() {
        player.addCard(yellowCard);
        player.addCard(greenCard);
        player.selectCard(yellowCard);

        player.removeCard(greenCard);

        assertSame(yellowCard, player.getSelectedCard());
        assertTrue(player.hasSelectedCard());
    }

    @Test
    void selectCardSelectsOwnedCard() {
        player.addCard(yellowCard);

        player.selectCard(yellowCard);

        assertSame(yellowCard, player.getSelectedCard());
    }

    @Test
    void selectCardMakesHasSelectedCardReturnTrue() {
        player.addCard(yellowCard);

        player.selectCard(yellowCard);

        assertTrue(player.hasSelectedCard());
    }

    @Test
    void selectCardAcceptsEqualCardWhenEqualCardIsOwned() {
        Card storedCard = new Card(GameColor.YELLOW, 3);
        Card equalCard = new Card(GameColor.YELLOW, 3);

        player.addCard(storedCard);
        player.selectCard(equalCard);

        assertEquals(equalCard, player.getSelectedCard());
    }

    @Test
    void selectCardRejectsNull() {
        assertThrows(
                NullPointerException.class,
                () -> player.selectCard(null)
        );
    }

    @Test
    void selectCardRejectsCardNotOwnedByPlayer() {
        assertThrows(
                IllegalArgumentException.class,
                () -> player.selectCard(yellowCard)
        );
    }

    @Test
    void failedSelectionDoesNotChangeCurrentSelection() {
        player.addCard(yellowCard);
        player.selectCard(yellowCard);

        assertThrows(
                IllegalArgumentException.class,
                () -> player.selectCard(greenCard)
        );

        assertSame(yellowCard, player.getSelectedCard());
    }

    @Test
    void selectCardCanReplaceCurrentSelection() {
        player.addCard(yellowCard);
        player.addCard(greenCard);
        player.selectCard(yellowCard);

        player.selectCard(greenCard);

        assertSame(greenCard, player.getSelectedCard());
    }

    @Test
    void setSelectedCardSetsProvidedCard() {
        player.setSelectedCard(yellowCard);

        assertSame(yellowCard, player.getSelectedCard());
        assertTrue(player.hasSelectedCard());
    }

    @Test
    void setSelectedCardAllowsCardNotInHand() {
        player.setSelectedCard(yellowCard);

        assertSame(yellowCard, player.getSelectedCard());
        assertFalse(player.hasCard(yellowCard));
    }

    @Test
    void setSelectedCardWithNullUnselectsCurrentCard() {
        player.setSelectedCard(yellowCard);

        player.setSelectedCard(null);

        assertNull(player.getSelectedCard());
        assertFalse(player.hasSelectedCard());
    }

    @Test
    void setSelectedCardCanReplaceCurrentSelection() {
        player.setSelectedCard(yellowCard);

        player.setSelectedCard(greenCard);

        assertSame(greenCard, player.getSelectedCard());
    }

    @Test
    void unselectCardClearsSelectedCard() {
        player.addCard(yellowCard);
        player.selectCard(yellowCard);

        player.unselectCard();

        assertNull(player.getSelectedCard());
        assertFalse(player.hasSelectedCard());
    }

    @Test
    void unselectCardDoesNotRemoveCardFromHand() {
        player.addCard(yellowCard);
        player.selectCard(yellowCard);

        player.unselectCard();

        assertTrue(player.hasCard(yellowCard));
        assertEquals(1, player.getHandCardCount());
    }

    @Test
    void unselectCardDoesNothingWhenNoCardIsSelected() {
        assertDoesNotThrow(player::unselectCard);

        assertNull(player.getSelectedCard());
        assertFalse(player.hasSelectedCard());
    }

    @Test
    void clearHandCardsRemovesAllCards() {
        player.addCard(yellowCard);
        player.addCard(greenCard);

        player.clearHandCards();

        assertTrue(player.getHandCards().isEmpty());
        assertEquals(0, player.getHandCardCount());
    }

    @Test
    void clearHandCardsDoesNothingWhenHandIsAlreadyEmpty() {
        assertDoesNotThrow(player::clearHandCards);

        assertTrue(player.getHandCards().isEmpty());
        assertEquals(0, player.getHandCardCount());
    }

    @Test
    void addScoreAddsPointsToCurrentScore() {
        player.addScore(5);

        assertEquals(5, player.getScore());
    }

    @Test
    void addScoreAccumulatesPoints() {
        player.addScore(3);
        player.addScore(5);
        player.addScore(2);

        assertEquals(10, player.getScore());
    }

    @Test
    void addScoreAcceptsZero() {
        player.addScore(0);

        assertEquals(0, player.getScore());
    }

    @ParameterizedTest
    @ValueSource(ints = {
            Integer.MIN_VALUE,
            -100,
            -2,
            -1
    })
    void addScoreRejectsNegativePoints(int negativePoints) {
        assertThrows(
                IllegalArgumentException.class,
                () -> player.addScore(negativePoints)
        );
    }

    @Test
    void rejectedNegativeScoreDoesNotChangeCurrentScore() {
        player.addScore(5);

        assertThrows(
                IllegalArgumentException.class,
                () -> player.addScore(-1)
        );

        assertEquals(5, player.getScore());
    }

    @Test
    void resetScoreSetsScoreToZero() {
        player.addScore(12);

        player.resetScore();

        assertEquals(0, player.getScore());
    }

    @Test
    void resetScoreDoesNothingWhenScoreIsAlreadyZero() {
        assertDoesNotThrow(player::resetScore);

        assertEquals(0, player.getScore());
    }

    @Test
    void addWinIncreasesWinsByOne() {
        player.addWin();

        assertEquals(1, player.getWins());
    }

    @Test
    void addWinAccumulatesWins() {
        player.addWin();
        player.addWin();
        player.addWin();

        assertEquals(3, player.getWins());
    }

    @Test
    void resetClearsScore() {
        player.addScore(10);

        player.reset();

        assertEquals(0, player.getScore());
    }

    @Test
    void resetClearsHandCards() {
        player.addCard(yellowCard);
        player.addCard(greenCard);

        player.reset();

        assertTrue(player.getHandCards().isEmpty());
        assertEquals(0, player.getHandCardCount());
    }

    @Test
    void resetClearsSelectedCard() {
        player.addCard(yellowCard);
        player.selectCard(yellowCard);

        player.reset();

        assertNull(player.getSelectedCard());
        assertFalse(player.hasSelectedCard());
    }

    @Test
    void resetPreservesPlayerId() {
        Player createdPlayer = new Player(42, "Player");
        createdPlayer.addScore(5);

        createdPlayer.reset();

        assertEquals(42, createdPlayer.getId());
    }

    @Test
    void resetPreservesPlayerName() {
        Player createdPlayer = new Player(1, "Kitty Player");
        createdPlayer.addScore(5);

        createdPlayer.reset();

        assertEquals("Kitty Player", createdPlayer.getName());
    }

    @Test
    void resetPreservesWins() {
        player.addWin();
        player.addWin();
        player.addScore(10);

        player.reset();

        assertEquals(2, player.getWins());
    }

    @Test
    void resetRestoresCompleteRoundState() {
        int expectedId = player.getId();
        String expectedName = player.getName();

        player.addCard(yellowCard);
        player.addCard(greenCard);
        player.selectCard(yellowCard);
        player.addScore(15);
        player.addWin();
        player.addWin();

        player.reset();

        assertEquals(expectedId, player.getId());
        assertEquals(expectedName, player.getName());
        assertEquals(0, player.getScore());
        assertEquals(2, player.getWins());
        assertTrue(player.getHandCards().isEmpty());
        assertEquals(0, player.getHandCardCount());
        assertNull(player.getSelectedCard());
        assertFalse(player.hasSelectedCard());
    }
}