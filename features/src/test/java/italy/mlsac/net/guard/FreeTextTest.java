// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FreeTextTest {

    @Test
    void controlCharactersColoursAndChainingAreRemoved() {
        assertEquals("Aim assist op Steve", FreeText.argumentTail("&cAim\nassist; op Steve"));
        assertEquals("Fly hack", FreeText.argumentTail("§4Fly &#FF0000hack"));
        assertEquals("a b a", FreeText.argumentTail("a | b && @a"));
    }

    @Test
    void wordsCannotBecomeFlagsOrCommands() {
        assertEquals("s silent sender=Console", FreeText.argumentTail("-s --silent --sender=Console"));
        assertEquals("op x", FreeText.argumentTail("/op x"));
        assertEquals("", FreeText.argumentTail(null));
    }

    @Test
    void lengthIsCapped() {
        String reason = FreeText.argumentTail("x".repeat(500));
        assertEquals(FreeText.MAX_LENGTH, reason.length());
        assertTrue(CommandShape.refusal("kick Steve " + reason).isEmpty());
    }
}
