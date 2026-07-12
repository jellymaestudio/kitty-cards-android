package kittycards.kittycardsandroid.logic;

import kittycards.kittycardsandroid.model.Board;
import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.Match;
import kittycards.kittycardsandroid.model.MatchStatus;
import kittycards.kittycardsandroid.model.Player;

/**
 * Validates whether players are allowed to perform specific actions during a match.
 *
 * @author JellyMae
 */
public class MoveValidator {

    private final Match match;


    /**
     * Creates a new MoveValidator for the given match.
     *
     * @param match the match whose current state should be validated
     * @throws NullPointerException if the match is null
     */
    public MoveValidator(Match match) {
        if(match == null) {
            throw new NullPointerException("match cannot be null");
        }

        this.match = match;
    }


    /**
     * Checks whether the given player is the current active player.
     *
     * @param player the player to check
     * @return true if it is the player's turn, false otherwise
     * @throws NullPointerException if the player is null
     */
    public boolean isPlayersTurn(Player player) {
        if(player == null) {
            throw new NullPointerException("player cannot be null");
        }

        return player == match.getGameState().getCurrentPlayer();
    }

    /**
     * Checks whether the given player can play a selected card on the given board position.
     * <p>
     * A card can only be played if it is the player's turn, the position is on the board,
     * the position is not the center field, the field is empty, and the player has selected a card.
     * </p>
     *
     * @param player the player who wants to play a card
     * @param row the row of the target field
     * @param column the column of the target field
     * @return true if the player can play a card on the given field, false otherwise
     * @throws NullPointerException if the player is null
     */
    public boolean canPlayCard(Player player, int row, int column) {
        if (match.getMatchStatus() != MatchStatus.RUNNING) {
            return false;
        }

        Board board = match.getGameState().getBoard();

        if(!isPlayersTurn(player)) {
            return false;
        }
        if(!board.isOnBoard(row, column)) {
            return false;
        }
        if(board.isCenterField(row, column)) {
            return false;
        }
        if(!board.getField(row, column).isEmpty()) {
            return false;
        }

        return player.hasSelectedCard();
    }

    /**
     * Checks whether the given player can draw a card.
     * <p>
     * A player can only draw a card if it is their turn and their hand contains fewer than ten cards.
     * </p>
     *
     * @param player the player who wants to draw a card
     * @return true if the player can draw a card, false otherwise
     * @throws NullPointerException if the player is null
     */
    public boolean canDrawCard(Player player) {
        if (match.getMatchStatus() != MatchStatus.RUNNING) {
            return false;
        }

        if (!isPlayersTurn(player)) {
            return false;
        }

        return player.getHandCardCount() < 10;
    }

    /**
     * Checks whether the given player can select the given card.
     * <p>
     * A card can only be selected if the player belongs to the match and owns the card.
     * </p>
     *
     * @param player the player who wants to select a card
     * @param card the card to select
     * @return true if the player can select the card, false otherwise
     * @throws NullPointerException if the card is null
     */
    public boolean canSelectCard(Player player, Card card) {
        if (match.getMatchStatus() != MatchStatus.RUNNING) {
            return false;
        }

        if (player != match.getPlayerOne() && player != match.getPlayerTwo()) {
            return false;
        }

        if (card == null) {
            throw new NullPointerException("card cannot be null");
        }

        return player.hasCard(card);
    }
}