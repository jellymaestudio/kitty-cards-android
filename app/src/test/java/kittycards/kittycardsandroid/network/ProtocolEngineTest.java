package kittycards.kittycardsandroid.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import kittycards.kittycardsandroid.model.Card;
import kittycards.kittycardsandroid.model.GameColor;

/**
 * Technical verification of the network protocol encoding and decoding.
 * Ensures all possible game actions are correctly serialized for BLE transmission.
 */
class ProtocolEngineTest {

    private ProtocolEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ProtocolEngine();
    }

    @Test
    void encode_nullAction_throwsNPE() {
        assertThrows(NullPointerException.class, () -> engine.encodeGameAction(null));
    }

    @Test
    void decode_nullBytes_throwsNPE() {
        assertThrows(NullPointerException.class, () -> engine.decodeGameAction(null));
    }

    @Test
    void decode_invalidLength_throwsIAE() {
        assertThrows(IllegalArgumentException.class, () -> engine.decodeGameAction(new byte[6]));
        assertThrows(IllegalArgumentException.class, () -> engine.decodeGameAction(new byte[8]));
    }

    @ParameterizedTest
    @EnumSource(GameAction.ActionType.class)
    void roundtrip_allActionTypes(GameAction.ActionType type) {
        GameAction original = createActionForType(type);
        byte[] encoded = engine.encodeGameAction(original);
        GameAction decoded = engine.decodeGameAction(encoded);

        assertEquals(original.type(), decoded.type(), "ActionType mismatch for " + type);
        assertEquals(original.boardPositionColumn(), decoded.boardPositionColumn(), "Column mismatch for " + type);
        assertEquals(original.boardPositionRow(), decoded.boardPositionRow(), "Row mismatch for " + type);
        assertEquals(original.contextSensitiveInt(), decoded.contextSensitiveInt(), "ContextInt mismatch for " + type);
        
        if (original.card() != null) {
            assertNotNull(decoded.card(), "Card should not be null for " + type);
            assertEquals(original.card().getValue(), decoded.card().getValue());
            assertEquals(original.card().getColor(), decoded.card().getColor());
        } else {
            assertNull(decoded.card(), "Card should be null for " + type);
        }

        if (original.boardColor() != null) {
            assertEquals(original.boardColor(), decoded.boardColor(), "BoardColor mismatch for " + type);
        } else {
            assertNull(decoded.boardColor(), "BoardColor should be null for " + type);
        }
    }

    private GameAction createActionForType(GameAction.ActionType type) {
        Card card = new Card(GameColor.CYAN, 5);
        return switch (type) {
            case DRAW_CARD, SELECT_CARD, DEAL_CARD -> new GameAction(type, card, 0);
            case PLAY_CARD -> new GameAction(type, card, 1, 2);
            case SET_BOARD_COLOR -> new GameAction(type, GameColor.PURPLE, 1, 2);
            case SET_STARTING_PLAYER -> new GameAction(type, 1);
            default -> new GameAction(type);
        };
    }

    @Test
    void decode_invalidActionType_throwsIAE() {
        byte[] bytes = new byte[7];
        bytes[0] = (byte) GameAction.ActionType.values().length; // Out of bounds
        assertThrows(IllegalArgumentException.class, () -> engine.decodeGameAction(bytes));
    }

    @Test
    void decode_invalidCardColor_throwsIAE() {
        byte[] bytes = new byte[7];
        bytes[0] = (byte) GameAction.ActionType.DRAW_CARD.ordinal();
        bytes[1] = 5;
        bytes[2] = (byte) GameColor.values().length; // Out of bounds
        assertThrows(IllegalArgumentException.class, () -> engine.decodeGameAction(bytes));
    }

    @Test
    void decode_nonCardColorForCard_throwsIAE() {
        byte[] bytes = new byte[7];
        bytes[0] = (byte) GameAction.ActionType.DRAW_CARD.ordinal();
        bytes[1] = 5;
        bytes[2] = (byte) GameColor.GREY.ordinal(); // GREY is not a card color
        assertThrows(IllegalArgumentException.class, () -> engine.decodeGameAction(bytes));
    }
}
