package kittycards.kittycardsandroid.logic;

import kittycards.kittycardsandroid.model.Board;
import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.Match;
import kittycards.kittycardsandroid.model.Player;

/**
 *
 * @author JellyMae
 */
public class MoveValidator {

    private final Match match;


    public MoveValidator(Match match) {
        if(match == null) {
            throw new NullPointerException("match cannot be null");
        }

        this.match = match;
    }


    public boolean isPlayersTurn(Player player) {
        if(player == null) {
            throw new NullPointerException("player cannot be null");
        }

        return player == match.getGameState().getCurrentPlayer();
    }

    public boolean canPlayCard(Player player, int row, int column) {
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

    public boolean canDrawCard(Player player) {
        if(!isPlayersTurn(player)) {
            return false;
        }

        return player.getHandCardCount() < 10;
    }

    public boolean canSelectCard(Player player, Card card) {
        if(player != match.getPlayerOne() && player != match.getPlayerTwo()) {
            return false;
        }
        if(card == null) {
            throw new NullPointerException("card cannot be null");
        }
        if(!player.hasCard(card)) {
            return false;
        }

        return true;
    }
}