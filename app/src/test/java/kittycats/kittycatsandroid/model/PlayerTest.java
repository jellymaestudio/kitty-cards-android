package kittycats.kittycatsandroid.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class PlayerTest {

    @Test
    public void constructorShouldSetId() {
        Player player = new Player(1, "Player One");

        assertEquals(1, player.getId());
    }

    @Test
    public void constructorShouldSetName() {
        Player player = new Player(1, "Player One");

        assertEquals("Player One", player.getName());
    }

    @Test
    public void constructorShouldInitializeHandCardsAsEmpty() {
        Player player = new Player(1, "Player One");

        assertTrue(player.getHandCards().isEmpty());
    }

    @Test
    public void constructorShouldInitializeSelectedCardAsNull() {
        Player player = new Player(1, "Player One");

        assertNull(player.getSelectedCard());
        assertFalse(player.hasSelectedCard());
    }

    @Test
    public void constructorShouldInitializeScoreWithZero() {
        Player player = new Player(1, "Player One");

        assertEquals(0, player.getScore());
    }

    @Test
    public void constructorShouldInitializeWinsWithZero() {
        Player player = new Player(1, "Player One");

        assertEquals(0, player.getWins());
    }

    @Test
    public void constructorShouldThrowExceptionIfNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Player(1, null));
    }

    @Test
    public void constructorShouldThrowExceptionIfNameIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new Player(1, ""));
    }

    @Test
    public void constructorShouldThrowExceptionIfNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Player(1, "   "));
    }

    @Test
    public void addCardShouldAddCardToHandCards() {
        Player player = new Player(1, "Player One");
        Card card = new Card(GameColor.RED, 3);

        player.addCard(card);

        assertTrue(player.getHandCards().contains(card));
        assertTrue(player.hasCard(card));
    }

    @Test
    public void addCardShouldThrowExceptionIfCardIsNull() {
        Player player = new Player(1, "Player One");

        assertThrows(NullPointerException.class, () -> player.addCard(null));
    }

    @Test
    public void removeCardShouldRemoveCardFromHandCards() {
        Player player = new Player(1, "Player One");
        Card card = new Card(GameColor.BLUE, 4);

        player.addCard(card);
        player.removeCard(card);

        assertFalse(player.getHandCards().contains(card));
        assertFalse(player.hasCard(card));
    }

    @Test
    public void removeCardShouldThrowExceptionIfCardIsNull() {
        Player player = new Player(1, "Player One");

        assertThrows(NullPointerException.class, () -> player.removeCard(null));
    }

    @Test
    public void removeCardShouldUnselectRemovedCard() {
        Player player = new Player(1, "Player One");
        Card card = new Card(GameColor.GREEN, 5);

        player.addCard(card);
        player.selectCard(card);
        player.removeCard(card);

        assertNull(player.getSelectedCard());
        assertFalse(player.hasSelectedCard());
    }

    @Test
    public void selectCardShouldSetSelectedCard() {
        Player player = new Player(1, "Player One");
        Card card = new Card(GameColor.YELLOW, 2);

        player.addCard(card);
        player.selectCard(card);

        assertEquals(card, player.getSelectedCard());
        assertTrue(player.hasSelectedCard());
    }

    @Test
    public void selectCardShouldThrowExceptionIfCardIsNull() {
        Player player = new Player(1, "Player One");

        assertThrows(NullPointerException.class, () -> player.selectCard(null));
    }

    @Test
    public void selectCardShouldThrowExceptionIfPlayerDoesNotHaveCard() {
        Player player = new Player(1, "Player One");
        Card card = new Card(GameColor.RED, 1);

        assertThrows(IllegalArgumentException.class, () -> player.selectCard(card));
    }

    @Test
    public void unselectCardShouldClearSelectedCard() {
        Player player = new Player(1, "Player One");
        Card card = new Card(GameColor.BLUE, 6);

        player.addCard(card);
        player.selectCard(card);
        player.unselectCard();

        assertNull(player.getSelectedCard());
        assertFalse(player.hasSelectedCard());
    }

    @Test
    public void clearHandCardsShouldRemoveAllCards() {
        Player player = new Player(1, "Player One");

        player.addCard(new Card(GameColor.RED, 1));
        player.addCard(new Card(GameColor.BLUE, 2));
        player.clearHandCards();

        assertTrue(player.getHandCards().isEmpty());
    }

    @Test
    public void addScoreShouldIncreaseScore() {
        Player player = new Player(1, "Player One");

        player.addScore(5);
        player.addScore(3);

        assertEquals(8, player.getScore());
    }

    @Test
    public void addScoreShouldAllowZeroPoints() {
        Player player = new Player(1, "Player One");

        player.addScore(0);

        assertEquals(0, player.getScore());
    }

    @Test
    public void addScoreShouldThrowExceptionIfPointsAreNegative() {
        Player player = new Player(1, "Player One");

        assertThrows(IllegalArgumentException.class, () -> player.addScore(-1));
    }

    @Test
    public void resetScoreShouldSetScoreToZero() {
        Player player = new Player(1, "Player One");

        player.addScore(10);
        player.resetScore();

        assertEquals(0, player.getScore());
    }

    @Test
    public void addWinShouldIncreaseWins() {
        Player player = new Player(1, "Player One");

        player.addWin();
        player.addWin();

        assertEquals(2, player.getWins());
    }

    @Test
    public void resetShouldClearRoundSpecificData() {
        Player player = new Player(1, "Player One");
        Card card = new Card(GameColor.RED, 4);

        player.addCard(card);
        player.selectCard(card);
        player.addScore(10);
        player.reset();

        assertTrue(player.getHandCards().isEmpty());
        assertNull(player.getSelectedCard());
        assertFalse(player.hasSelectedCard());
        assertEquals(0, player.getScore());
    }

    @Test
    public void resetShouldKeepWins() {
        Player player = new Player(1, "Player One");

        player.addWin();
        player.reset();

        assertEquals(1, player.getWins());
    }
}