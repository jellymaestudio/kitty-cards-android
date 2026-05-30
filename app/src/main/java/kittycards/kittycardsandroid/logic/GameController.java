package kittycards.kittycardsandroid.logic;

import kittycards.kittycardsandroid.components.IGameController;
import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.Player;

/**
 * @author red_concrete
 */
public class GameController implements IGameController {
    private static GameController INSTANCE;
    private GameController() {}

    //GameController
    @Override
    public void startMatch() {

    }

    @Override
    public void drawCard() {

    }

    @Override
    public void selectCard(Player player, Card card) {

    }

    @Override
    public void unselectCard(Player player) {

    }

    @Override
    public void playCard(int row, int column) {

    }

    @Override
    public void setOnStateChangedListener(Runnable listener) {
        //Das ist basically ein setter
    }

    public static GameController getInstance() {
        if (INSTANCE == null) INSTANCE = new GameController();
        return INSTANCE;
    }
}
