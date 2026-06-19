package kittycards.kittycardsandroid.logic;

import kittycards.kittycardsandroid.components.IGameController;
import kittycards.kittycardsandroid.components.INetworkManager;
import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.Field;
import kittycards.kittycardsandroid.model.GameColor;
import kittycards.kittycardsandroid.model.GameState;
import kittycards.kittycardsandroid.model.Match;
import kittycards.kittycardsandroid.model.MatchStatus;
import kittycards.kittycardsandroid.model.Player;
import kittycards.kittycardsandroid.network.GameAction;

/**
 *
 *
 * @author JellyMae
 * @author red_concrete
 */
public class GameController implements IGameController {

    /**
     * Speichert die eine gemeinsame Instanz von GameController (?)
     */
    private static GameController INSTANCE;
    private Match match;
    private MoveValidator moveValidator;
    private INetworkManager networkManager;
    private Runnable onStateChangedListener;


    // --- Constructor ---

    /**
     * Constructor
     */
    private GameController() {

    }


    // --- Getters and Setters ---

    /**
     * Bedeutet: Wenn es noch keinen GameController gibt: Erstelle einen.
     * Sonst: Gib mir den vorhandenen zurück.
     *
     * @return die GameController Instanz
     */
    public static GameController getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GameController();
        }

        return INSTANCE;
    }

    public Match getMatch() {
        return match;
    }

    @Override
    public void setOnStateChangedListener(Runnable listener) {
        this.onStateChangedListener = listener;
    }

    public void setNetworkManager(INetworkManager networkManager) {
        this.networkManager = networkManager;
    }


    // --- Assistance Methods ---

    private Card generateCard() {
        int value = (int) (Math.random() * 6) + 1;
        int colorIndex = (int) (Math.random() * (GameColor.values().length - 1));
        GameColor color = GameColor.values()[colorIndex];

        return new Card(color, value);
    }

    private int calculateScore(Card card, Field field) {
        GameColor cardColor = card.getColor();
        GameColor fieldColor = field.getColor();

        if(fieldColor == GameColor.WHITE) {
            return card.getValue();
        }
        else if(cardColor == fieldColor) {
            return card.getValue() * 2;
        }
        else {
            return 0;
        }
    }

    private void switchTurn() {
        GameState gameState = match.getGameState();

        gameState.setCurrentPlayer(
                match.getOtherPlayer(gameState.getCurrentPlayer())
        );
    }

    private void notifyStateChanged() {
        if(onStateChangedListener != null) {
            onStateChangedListener.run();
        }
    }

    private void sendGameAction(GameAction action) {
        if(networkManager != null) {
            networkManager.sendGameChange(action);
        }
    }


    // --- GameController Management ---

    @Override
    public void startMatch(Player playerOne, Player playerTwo) {
        //Darf nur Host ausführen!! -> ich geh davon aus, dass die Bedingung erfüllt ist
        //Hat genau 2 Spieler im Raum -> -"-

        this.match = new Match(playerOne, playerTwo);
        this.moveValidator = new MoveValidator(getMatch());
        this.match.setMatchStatus(MatchStatus.RUNNING);

        // TODO : Send match start action

        /*sendGameAction(new GameAction(GameAction.ActionType.START_MATCH));
        notifyStateChanged();*/
    }

    @Override
    public void selectCard(Player player, Card card) {
        if(!moveValidator.canSelectCard(player, card)) {
            return;
        }

        player.selectCard(card);

        /*sendGameAction(new GameAction(GameAction.ActionType.SELECT_CARD, card));
        notifyStateChanged();*/
    }

    @Override
    public void unselectCard(Player player) {
        player.unselectCard();

        /*sendGameAction(new GameAction(GameAction.ActionType.UNSELECT_CARD));
        notifyStateChanged();*/
    }

    @Override
    public void playCard(Player player, int row, int column) {
        if(!moveValidator.canPlayCard(player, row, column)) {
            return;
        }

        GameState gameState = match.getGameState();
        Field choosenField = gameState.getBoard().getField(row, column);
        Card selectedCard = player.getSelectedCard();

        choosenField.placeCard(selectedCard);
        gameState.getCurrentPlayer().addScore(calculateScore(selectedCard, choosenField));
        player.unselectCard();
        player.removeCard(selectedCard);

        if(gameState.isGameOver()) {
            match.startNextRound();

            if(match.getMatchStatus() == MatchStatus.FINISHED) {
                // TODO : Send match end action
            }
            else {
                // TODO: Send next round action
            }
        }
        else {
            switchTurn();
        }

        sendGameAction(new GameAction(GameAction.ActionType.PLAY_CARD, selectedCard, column, row));
        notifyStateChanged();
    }

    @Override
    public void drawCard(Player player) {
        if(!moveValidator.canDrawCard(player)) {
            return;
        }

        Card newCard = generateCard();
        player.addCard(newCard);

        switchTurn();

        sendGameAction(new GameAction(GameAction.ActionType.DRAW_CARD, newCard));
        notifyStateChanged();


        /*
        remote:
            - keine neue Karte beim anderen Gerät generieren
            - empfangene Karte hinzufügen
         */
    }
}


    /*
    Brainstorm:
    (Das soll alles in diese Klasse, die Namen "ScoreCalculator", etc dienen gerade nur als Überschrift, um die Abschnitte besser zu unterteilen)

    ---
    Legende:
    (?) = Unsicher
    (!) = Problem entdeckt, nicht so übernehmen!
    (/) = Idee verworfen
    ---

    - calculateScore(Card playedCard, Field playedField)
    -> bekommt Card + Field; berechnet, wieviele Punkte ein Spieler beim Legen der Karte erhält
        -> Weißes Feld                              -> normale Punkte
        -> Farbiges Feld mit gleichfarbiger Karte   -> doppelte Punkte
        -> Farbiges Feld mit andersfarbiger Farbe   -> keine Punkte


    - switchTurn() -> über GameState: setCurrentPlayer() und getOtherPlayer() (?)


    - generateCard()
    -> generiert zufällige Karte mit Farbe (kein Weiß) und Wert (1-6)
    -> Player darf nur max. 10 Karten auf der Hand haben (MoveValidator)


    - GameMessageHandler ->
    -> sendet/erhält Daten an/von Network; pausiert Game bis Daten gesendet/erhalten wurden (mit MatchState aktualisierung)
    -> übersetzt Spielaktionen in GameMessages, verarbeitet empfangene GameMessages (?)
    -> Interface: setOnStateChangedListener(Runnable listener) (?)

    - GameController (GC) (Hauptfunktionen und nicht Methode):
    -> kümmert sich um: Game starten, Karte aus-/abwählen, Karte ziehen, Karte legen, CurrentPlayer ändern, Game beenden
    -> Muss mit Network und UI kommunizieren
    -> z.B: Spieler will Karte legen:
        -> GC fragt MoveValidator nach Erlaubnis, GC legt Karte, GC fragt ScoreCalculator nach Punkten, GC erhöht Score des Spielers, GC fragt ob Board voll ist, GameState bzw. Match wird aktualisier, Network kriegt Info (am besten nur das, was sich explizit geändert hat)
    -> Interface:
        -> startMatch(Player playerOne, Player playerTwo), drawCard(Player player), selectCard(Player player, Card card), unselectCard(Player player), playCard()
           (/) -> playCard(int row, in column) im Interface würde ich vllt zu playCard(Card selectedCard, Field field) ändern (falls das geht)



    - playCard(row, column)
    -> validatePlayCard(...)
    -> Karte aufs Feld legen
    -> Punkte berechnen
    -> Spielerpunkte erhöhen
    -> Karte aus der Hand entfernen
    -> Prüfen, ob Runde vorbei ist
    -> ggf. nächste Runde starten
    -> sonst Spieler wechseln
    -> Network informieren
    -> Listener informieren


    - startMatch()
    -> nur Host/UI darf es auslösen
    -> prüft: genau 2 Spieler im Raum
    -> erstellt Match
    -> setzt Status auf RUNNING
    -> sendet Start-Action ans Network
    -> Informiert UI


    - Wenn Player nicht dran ist, hör auf:
    if (!moveValidator.isPlayersTurn(player, match)) {
        return;
    }


    Für Leonard:

    - handleStartMatchAction() --> für den Gast, der nicht hostet
    -> empfängt Matchdaten
    -> erstellt lokalen Match-Zustand passend zum Host
    -> setzt Status auf RUNNING
    -> informiert UI


     */