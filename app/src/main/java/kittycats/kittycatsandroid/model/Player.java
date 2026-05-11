package kittycats.kittycatsandroid.model;

import java.util.ArrayList;
import java.util.List;

public class Player {

    private final int id;
    private final String name;
    private final List<Card> handCards;
    private int score;
    private int wins;


    // --- Constructors ---

    public Player(int id, String name) {
        if(name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be empty");
        }

        this.id = id;
        this.name = name;
        this.handCards = new ArrayList<>(); //Maybe a different list?
        this.score = 0;
        this.wins = 0;
    }


    // --- Getters and Setters ---

    public List<Card> getHandCards() {
        return handCards;
    }

    public int getWins() {
        return wins;
    }

    public boolean hasCard(Card card) {
        return handCards.contains(card);
    }


    // --- Operations ---

    public void addCard(Card card) {
        if(card == null) {
            throw new NullPointerException("card cannot be null");
        }

        handCards.add(card);
    }

    public void removeCard(Card card) {
        if(card == null) {
            throw new NullPointerException("card cannot be null");
        }

        handCards.remove(card);
    }


    public void addScore(int points) {
        if(points < 0) {
            throw new IllegalArgumentException("points must be positive");
        }

        this.score += points;
    }

    public void resetScore() {
        this.score = 0;
    }


    public void addWin() {
        this.wins++;
    }


    public void clearHandCards() {
        handCards.clear();
    }
}
