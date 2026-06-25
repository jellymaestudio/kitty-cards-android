package kittycards.kittycardsandroid.logic;

import org.junit.Test;

import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.GameColor;
import kittycards.kittycardsandroid.model.Match;
import kittycards.kittycardsandroid.model.Player;

import static org.junit.Assert.*;

public class MoveValidatorTest {

    @Test
    public void constructorShouldThrowExceptionIfMatchIsNull() {
        assertThrows(NullPointerException.class, () -> new MoveValidator(null));
    }

    @Test
    public void isPlayersTurnShouldReturnTrueForCurrentPlayer() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);

        assertTrue(validator.isPlayersTurn(match.getGameState().getCurrentPlayer()));
    }

    @Test
    public void isPlayersTurnShouldReturnFalseForInactivePlayer() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);

        Player currentPlayer = match.getGameState().getCurrentPlayer();
        Player inactivePlayer = match.getOtherPlayer(currentPlayer);

        assertFalse(validator.isPlayersTurn(inactivePlayer));
    }

    @Test
    public void isPlayersTurnShouldThrowExceptionIfPlayerIsNull() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);

        assertThrows(NullPointerException.class, () -> validator.isPlayersTurn(null));
    }

    @Test
    public void canPlayCardShouldReturnTrueForValidMove() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);
        Player player = match.getGameState().getCurrentPlayer();
        Card card = new Card(GameColor.PURPLE, 3);

        player.addCard(card);
        player.selectCard(card);

        assertTrue(validator.canPlayCard(player, 0, 0));
    }

    @Test
    public void canPlayCardShouldReturnFalseIfPlayerIsNotCurrentPlayer() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);

        Player inactivePlayer = match.getOtherPlayer(match.getGameState().getCurrentPlayer());
        Card card = new Card(GameColor.PURPLE, 3);

        inactivePlayer.addCard(card);
        inactivePlayer.selectCard(card);

        assertFalse(validator.canPlayCard(inactivePlayer, 0, 0));
    }

    @Test
    public void canPlayCardShouldReturnFalseIfPositionIsOutsideBoard() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);
        Player player = match.getGameState().getCurrentPlayer();
        Card card = new Card(GameColor.PURPLE, 3);

        player.addCard(card);
        player.selectCard(card);

        assertFalse(validator.canPlayCard(player, -1, 0));
        assertFalse(validator.canPlayCard(player, 3, 0));
        assertFalse(validator.canPlayCard(player, 0, -1));
        assertFalse(validator.canPlayCard(player, 0, 3));
    }

    @Test
    public void canPlayCardShouldReturnFalseIfPositionIsCenterField() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);
        Player player = match.getGameState().getCurrentPlayer();
        Card card = new Card(GameColor.PURPLE, 3);

        player.addCard(card);
        player.selectCard(card);

        assertFalse(validator.canPlayCard(player, 1, 1));
    }

    @Test
    public void canPlayCardShouldReturnFalseIfFieldIsOccupied() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);
        Player player = match.getGameState().getCurrentPlayer();
        Card selectedCard = new Card(GameColor.PURPLE, 3);
        Card placedCard = new Card(GameColor.CYAN, 2);

        player.addCard(selectedCard);
        player.selectCard(selectedCard);
        match.getGameState().getBoard().getField(0, 0).placeCard(placedCard);

        assertFalse(validator.canPlayCard(player, 0, 0));
    }

    @Test
    public void canPlayCardShouldReturnFalseIfPlayerHasNoSelectedCard() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);
        Player player = match.getGameState().getCurrentPlayer();

        assertFalse(validator.canPlayCard(player, 0, 0));
    }

    @Test
    public void canDrawCardShouldReturnTrueIfPlayerIsCurrentPlayerAndHandIsNotFull() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);
        Player player = match.getGameState().getCurrentPlayer();

        assertTrue(validator.canDrawCard(player));
    }

    @Test
    public void canDrawCardShouldReturnFalseIfPlayerIsNotCurrentPlayer() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);
        Player inactivePlayer = match.getOtherPlayer(match.getGameState().getCurrentPlayer());

        assertFalse(validator.canDrawCard(inactivePlayer));
    }

    @Test
    public void canDrawCardShouldReturnFalseIfHandHasTenCards() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);
        Player player = match.getGameState().getCurrentPlayer();

        for (int i = 0; i < 10; i++) {
            player.addCard(new Card(GameColor.PURPLE, 1));
        }

        assertFalse(validator.canDrawCard(player));
    }

    @Test
    public void canSelectCardShouldReturnTrueIfPlayerOwnsCard() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);
        Player player = match.getPlayerOne();
        Card card = new Card(GameColor.PURPLE, 3);

        player.addCard(card);

        assertTrue(validator.canSelectCard(player, card));
    }

    @Test
    public void canSelectCardShouldReturnFalseIfPlayerIsNotPartOfMatch() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);
        Player otherPlayer = new Player(3, "Other Player");
        Card card = new Card(GameColor.PURPLE, 3);

        otherPlayer.addCard(card);

        assertFalse(validator.canSelectCard(otherPlayer, card));
    }

    @Test
    public void canSelectCardShouldThrowExceptionIfCardIsNull() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);

        assertThrows(NullPointerException.class, () ->
                validator.canSelectCard(match.getPlayerOne(), null)
        );
    }

    @Test
    public void canSelectCardShouldReturnFalseIfPlayerDoesNotOwnCard() {
        Match match = createMatch();
        MoveValidator validator = new MoveValidator(match);
        Player player = match.getPlayerOne();
        Card card = new Card(GameColor.PURPLE, 3);

        assertFalse(validator.canSelectCard(player, card));
    }

    private Match createMatch() {
        Player playerOne = new Player(1, "Player One");
        Player playerTwo = new Player(2, "Player Two");

        return new Match(playerOne, playerTwo);
    }
}