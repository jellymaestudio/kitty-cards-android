package kittycats.kittycatsandroid.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player in a match.
 * <p>
 * A player has a unique id, a name, a hand of cards, an optional selected card,
 * a score for the current round, and a number of wins for the current match.
 * </p>
 *
 * @author JellyMae
 */
public class Player {

    private final int id;
    private final String name;
    private final List<Card> handCards;
    private Card selectedCard;
    private int score;
    private int wins;


    // --- Constructor ---

    /**
     * Creates a new player with the given id and name.
     *
     * @param id   the player's id
     * @param name the player's name
     * @throws IllegalArgumentException if {@code name} is {@code null}, blank, or empty
     */
    public Player(int id, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be empty");
        }

        this.id = id;
        this.name = name;
        this.handCards = new ArrayList<>(); //Maybe a different list?
        this.selectedCard = null;
        this.score = 0;
        this.wins = 0;
    }


    // --- Getters and Setters ---

    /**
     * Returns all cards currently in the player's hand.
     *
     * @return the player's hand cards
     */
    public List<Card> getHandCards() {
        return handCards;
    }

    /**
     * Returns the currently selected card.
     *
     * @return the selected card, or {@code null} if no card is selected
     */
    public Card getSelectedCard() {
        return selectedCard;
    }

    /**
     * Sets the selected card of the player.
     * <p>
     * A {@code null} value unselects the current card.
     *
     * @param selectedCard the selected card
     */
    public void setSelectedCard(Card selectedCard) {
        this.selectedCard = selectedCard;
    }

    /**
     * Returns the current score of the player.
     *
     * @return the player's current score
     */
    public int getScore() {
        return score;
    }

    /**
     * Returns the number of match wins of this player.
     *
     * @return the player's total wins
     */
    public int getWins() {
        return wins;
    }

    /**
     * Checks whether the player currently owns the given card.
     *
     * @param card the card to check
     * @return {@code true} if the player has the card, otherwise {@code false}
     */
    public boolean hasCard(Card card) {
        return handCards.contains(card);
    }

    /**
     * Checks whether the player currently has a selected card.
     *
     * @return {@code true} if a card is selected, otherwise {@code false}
     */
    public boolean hasSelectedCard() {
        return selectedCard != null;
    }


    // --- Player Management ---

    /**
     * Adds a card to the player's hand.
     *
     * @param card the card to add
     * @throws NullPointerException if {@code card} is {@code null}
     */
    public void addCard(Card card) {
        if (card == null) {
            throw new NullPointerException("card cannot be null");
        }

        handCards.add(card);
    }

    /**
     * Removes a card from the player's hand.
     * <p>
     * If the removed card is currently selected,
     * it is automatically unselected.
     * </p>
     *
     * @param card the card to remove
     * @throws NullPointerException if {@code card} is {@code null}
     */
    public void removeCard(Card card) {
        if (card == null) {
            throw new NullPointerException("card cannot be null");
        }

        handCards.remove(card);

        if (card.equals(selectedCard)) {
            unselectCard();
        }
    }

    /**
     * Selects a card from the player's hand.
     *
     * @param card the card to select
     * @throws NullPointerException     if {@code card} is {@code null}
     * @throws IllegalArgumentException if the player does not own the card
     */
    public void selectCard(Card card) {
        if (card == null) {
            throw new NullPointerException("card cannot be null");
        }
        if (!handCards.contains(card)) {
            throw new IllegalArgumentException("player does not have this card");
        }

        this.selectedCard = card;
    }

    /**
     * Unselects the currently selected card.
     */
    public void unselectCard() {
        this.selectedCard = null;
    }

    /**
     * Removes all cards from the player's hand.
     */
    public void clearHandCards() {
        handCards.clear();
    }

    /**
     * Adds points to the player's current score.
     *
     * @param points the points to add
     * @throws IllegalArgumentException if {@code points} is negative
     */
    public void addScore(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("points must be positive");
        }

        this.score += points;
    }

    /**
     * Resets the player's round score to zero.
     */
    public void resetScore() {
        this.score = 0;
    }

    /**
     * Increases the player's match wins by one.
     */
    public void addWin() {
        this.wins++;
    }

    /**
     * Resets all round-specific player data.
     * <p>
     * This includes score, hand cards and selected card.
     * Match-specific data such as id, name and wins remain unchanged.
     */
    public void reset() {
        resetScore();
        clearHandCards();
        unselectCard();
    }
}
