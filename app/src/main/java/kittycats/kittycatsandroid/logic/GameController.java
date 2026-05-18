package kittycats.kittycatsandroid.logic;

import kittycats.kittycatsandroid.components.IGameController;
import kittycats.kittycatsandroid.model.Card;
import kittycats.kittycatsandroid.model.Player;

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

    public static GameController getInstance() {
        if (INSTANCE == null) INSTANCE = new GameController();
        return INSTANCE;
    }
}
