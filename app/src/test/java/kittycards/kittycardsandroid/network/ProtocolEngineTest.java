package kittycards.kittycardsandroid.network;

import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.GameColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolEngineTest {

    private ProtocolEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ProtocolEngine();
    }

    /** Liefert die erste GameColor, die als Kartenfarbe gilt. */
    private static GameColor firstCardColor() {
        return Arrays.stream(GameColor.values())
                .filter(GameColor::isCardColor)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Keine Kartenfarbe in GameColor gefunden"));
    }

    // -------------------------------------------------------------------------
    // encode
    // -------------------------------------------------------------------------

    @Test
    void encode_nullAction_throwsNPE() {
        assertThrows(NullPointerException.class, () -> engine.encodeGameAction(null));
    }

    @Test
    void encode_producesArrayOfLength6() {
        GameAction action = new GameAction(GameAction.ActionType.UNSELECT_CARD);
        assertEquals(6, engine.encodeGameAction(action).length);
    }

    @Test
    void encode_unselectCard_correctBytes() {
        GameAction action = new GameAction(GameAction.ActionType.UNSELECT_CARD);
        byte[] bytes = engine.encodeGameAction(action);

        assertEquals((byte) GameAction.ActionType.UNSELECT_CARD.ordinal(), bytes[0]);
        assertEquals(-1, bytes[1]); // kein Kartenwert
        assertEquals(-1, bytes[2]); // keine Kartenfarbe
        assertEquals(-1, bytes[3]); // keine Boardfarbe
        assertEquals(-1, bytes[4]); // keine Spalte
        assertEquals(-1, bytes[5]); // keine Zeile
    }

    @Test
    void encode_drawCard_cardBytesAreSet() {
        GameColor cardColor = firstCardColor();
        Card card = new Card(cardColor, 7);
        GameAction action = new GameAction(GameAction.ActionType.DRAW_CARD, card);
        byte[] bytes = engine.encodeGameAction(action);

        assertEquals((byte) GameAction.ActionType.DRAW_CARD.ordinal(), bytes[0]);
        assertEquals(7, bytes[1]);
        assertEquals((byte) cardColor.ordinal(), bytes[2]);
        assertEquals(-1, bytes[3]); // keine Boardfarbe
        assertEquals(-1, bytes[4]);
        assertEquals(-1, bytes[5]);
    }

    @Test
    void encode_selectCard_cardBytesAreSet() {
        GameColor cardColor = firstCardColor();
        Card card = new Card(cardColor, 3);
        GameAction action = new GameAction(GameAction.ActionType.SELECT_CARD, card);
        byte[] bytes = engine.encodeGameAction(action);

        assertEquals((byte) GameAction.ActionType.SELECT_CARD.ordinal(), bytes[0]);
        assertEquals(3, bytes[1]);
        assertEquals((byte) cardColor.ordinal(), bytes[2]);
        assertEquals(-1, bytes[3]);
    }

    @Test
    void encode_playCard_allBytesAreSet() {
        GameColor cardColor = firstCardColor();
        Card card = new Card(cardColor, 5);
        GameAction action = new GameAction(GameAction.ActionType.PLAY_CARD, card, 1, 2);
        byte[] bytes = engine.encodeGameAction(action);

        assertEquals((byte) GameAction.ActionType.PLAY_CARD.ordinal(), bytes[0]);
        assertEquals(5, bytes[1]);
        assertEquals((byte) cardColor.ordinal(), bytes[2]);
        assertEquals(-1, bytes[3]); // keine Boardfarbe
        assertEquals(1, bytes[4]);
        assertEquals(2, bytes[5]);
    }

    @Test
    void encode_setBoardColor_boardColorByteIsSet() {
        GameColor boardColor = GameColor.values()[0];
        GameAction action = new GameAction(GameAction.ActionType.SET_BOARD_COLOR, boardColor, 0, 1);
        byte[] bytes = engine.encodeGameAction(action);

        assertEquals((byte) GameAction.ActionType.SET_BOARD_COLOR.ordinal(), bytes[0]);
        assertEquals(-1, bytes[1]); // kein Kartenwert
        assertEquals(-1, bytes[2]); // keine Kartenfarbe
        assertEquals((byte) boardColor.ordinal(), bytes[3]);
        assertEquals(0, bytes[4]);
        assertEquals(1, bytes[5]);
    }

    // -------------------------------------------------------------------------
    // decode
    // -------------------------------------------------------------------------

    @Test
    void decode_nullBytes_throwsNPE() {
        assertThrows(NullPointerException.class, () -> engine.decodeGameAction(null));
    }

    @Test
    void decode_wrongLength_throwsIAE() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.decodeGameAction(new byte[3]));
    }

    @Test
    void decode_emptyArray_throwsIAE() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.decodeGameAction(new byte[0]));
    }

    @Test
    void decode_hugeArray_throwsIAE() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.decodeGameAction(new byte[1000]));
    }

    @Test
    void decode_invalidActionTypeOrdinal_throwsIAE() {
        // bytes[0] = 99: weit außerhalb jedes realistischen ActionType-Bereichs
        byte[] bytes = {99, -1, -1, -1, -1, -1};
        assertThrows(IllegalArgumentException.class, () -> engine.decodeGameAction(bytes));
    }

    @Test
    void decode_allNegativeOnes_throwsIAE() {
        // Alle -1: ActionType-Ordinal -1 ist ungültig
        byte[] bytes = {-1, -1, -1, -1, -1, -1};
        assertThrows(IllegalArgumentException.class, () -> engine.decodeGameAction(bytes));
    }

    @Test
    void decode_allMaxValue_throwsIAE() {
        // Byte.MAX_VALUE (127) für alles: Ordinals weit außerhalb gültiger Bereiche
        byte[] bytes = {Byte.MAX_VALUE, Byte.MAX_VALUE, Byte.MAX_VALUE,
                Byte.MAX_VALUE, Byte.MAX_VALUE, Byte.MAX_VALUE};
        assertThrows(IllegalArgumentException.class, () -> engine.decodeGameAction(bytes));
    }

    @Test
    void decode_invalidCardColorOrdinal_throwsIAE() {
        byte[] bytes = {
                (byte) GameAction.ActionType.DRAW_CARD.ordinal(),
                5,
                99,  // out of range für GameColor
                -1, -1, -1
        };
        assertThrows(IllegalArgumentException.class, () -> engine.decodeGameAction(bytes));
    }

    @Test
    void decode_unselectCard_returnsCorrectAction() {
        byte[] bytes = {
                (byte) GameAction.ActionType.UNSELECT_CARD.ordinal(),
                -1, -1, -1, -1, -1
        };
        GameAction action = engine.decodeGameAction(bytes);

        assertEquals(GameAction.ActionType.UNSELECT_CARD, action.type());
        assertNull(action.card());
        assertNull(action.boardColor());
        assertEquals(-1, action.boardPositionColumn());
        assertEquals(-1, action.boardPositionRow());
    }

    @Test
    void decode_drawCard_returnsCorrectCard() {
        GameColor cardColor = firstCardColor();
        byte[] bytes = {
                (byte) GameAction.ActionType.DRAW_CARD.ordinal(),
                7,
                (byte) cardColor.ordinal(),
                -1, -1, -1
        };
        GameAction action = engine.decodeGameAction(bytes);

        assertEquals(GameAction.ActionType.DRAW_CARD, action.type());
        assertNotNull(action.card());
        assertEquals(7, action.card().getValue());
        assertEquals(cardColor, action.card().getColor());
        assertNull(action.boardColor());
    }

    @Test
    void decode_playCard_returnsCorrectAction() {
        GameColor cardColor = firstCardColor();
        byte[] bytes = {
                (byte) GameAction.ActionType.PLAY_CARD.ordinal(),
                3,
                (byte) cardColor.ordinal(),
                -1,
                0,
                2
        };
        GameAction action = engine.decodeGameAction(bytes);

        assertEquals(GameAction.ActionType.PLAY_CARD, action.type());
        assertNotNull(action.card());
        assertEquals(3, action.card().getValue());
        assertEquals(cardColor, action.card().getColor());
        assertEquals(0, action.boardPositionColumn());
        assertEquals(2, action.boardPositionRow());
    }

    @Test
    void decode_setBoardColor_returnsCorrectAction() {
        GameColor boardColor = GameColor.values()[0];
        byte[] bytes = {
                (byte) GameAction.ActionType.SET_BOARD_COLOR.ordinal(),
                -1,
                -1,
                (byte) boardColor.ordinal(),
                1,
                2
        };
        GameAction action = engine.decodeGameAction(bytes);

        assertEquals(GameAction.ActionType.SET_BOARD_COLOR, action.type());
        assertNull(action.card());
        assertEquals(boardColor, action.boardColor());
        assertEquals(1, action.boardPositionColumn());
        assertEquals(2, action.boardPositionRow());
    }

    // -------------------------------------------------------------------------
    // Roundtrip
    // -------------------------------------------------------------------------

    @Test
    void roundtrip_unselectCard() {
        GameAction original = new GameAction(GameAction.ActionType.UNSELECT_CARD);
        GameAction decoded = engine.decodeGameAction(engine.encodeGameAction(original));

        assertEquals(original.type(), decoded.type());
        assertNull(decoded.card());
        assertNull(decoded.boardColor());
        assertEquals(-1, decoded.boardPositionColumn());
        assertEquals(-1, decoded.boardPositionRow());
    }

    @Test
    void roundtrip_setBoardColor() {
        GameColor color = GameColor.values()[0]; // ggf. anpassen
        GameAction original = new GameAction(GameAction.ActionType.SET_BOARD_COLOR, color, 1, 2);
        GameAction decoded = engine.decodeGameAction(engine.encodeGameAction(original));

        assertEquals(original.type(), decoded.type());
        assertNull(decoded.card());
        assertEquals(color, decoded.boardColor());
        assertEquals(1, decoded.boardPositionColumn());
        assertEquals(2, decoded.boardPositionRow());
    }
}