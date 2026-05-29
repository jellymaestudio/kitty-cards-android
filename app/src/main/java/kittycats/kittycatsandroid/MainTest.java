package kittycats.kittycatsandroid;

import kittycats.kittycatsandroid.logic.GameController;
import kittycats.kittycatsandroid.model.MatchStatus;

/**
 * This is our first testing GUI...
 * This is where
 */
public class MainTest {

    public static void main(String[] args) {

        GameController.getInstance().setOnStateChangedListener(() -> {

            MatchStatus status = GameController.getInstance().getMatch().getMatchStatus(); //

            switch (status) {
                case RUNNING:
                    System.out.println("Du bist dran! Eingaben sind freigeschaltet.");
                    break;

                case WAITING_FOR_NETWORK:
                    System.out.println("Warte auf den Zug des Gegners...");
                    break;

                case PAUSED:
                    System.out.println("Das Spiel ist pausiert.");
                    break;

                case FINISHED:
                    System.out.println("Das Spiel ist vorbei! Gewinner: " +
                            GameController.getInstance().getMatch().getMatchState().getMatchWinner()); //
                    break;
            }
        });


    }
}
