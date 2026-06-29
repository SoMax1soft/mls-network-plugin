package wtf.mlsac.penalty;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PunishmentLadderTest {

    private static Map<Integer, String> rungs(Object... pairs) {
        Map<Integer, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((Integer) pairs[i], (String) pairs[i + 1]);
        }
        return map;
    }

    @Test
    void emptyLadderYieldsNoCommands() {
        PunishmentLadder ladder = new PunishmentLadder(rungs());
        assertTrue(ladder.isEmpty());
        assertFalse(ladder.commandFor(5).isPresent());
        assertFalse(ladder.maxThreshold().isPresent());
        assertFalse(ladder.maxCommand().isPresent());
    }

    @Test
    void nullMapIsTreatedAsEmpty() {
        assertTrue(new PunishmentLadder(null).isEmpty());
    }

    @Test
    void exactThresholdMatchWins() {
        PunishmentLadder ladder = new PunishmentLadder(rungs(3, "kick", 6, "ban"));
        assertEquals("kick", ladder.commandFor(3).orElse(null));
        assertEquals("ban", ladder.commandFor(6).orElse(null));
    }

    @Test
    void usesNearestThresholdBelowWhenNoExactMatch() {
        PunishmentLadder ladder = new PunishmentLadder(rungs(3, "kick", 6, "ban"));
        assertEquals("kick", ladder.commandFor(4).orElse(null), "vl=4 should fall back to rung 3");
        assertEquals("kick", ladder.commandFor(5).orElse(null));
        assertEquals("ban", ladder.commandFor(99).orElse(null), "vl above the top rung keeps the top rung");
    }

    @Test
    void belowLowestThresholdYieldsNothing() {
        PunishmentLadder ladder = new PunishmentLadder(rungs(3, "kick"));
        assertFalse(ladder.commandFor(2).isPresent());
    }

    @Test
    void thresholdZeroIsHonored() {
        // Regression: the old loop returned null for a threshold of 0 (applicableThreshold > 0).
        PunishmentLadder ladder = new PunishmentLadder(rungs(0, "warn", 5, "ban"));
        assertEquals("warn", ladder.commandFor(0).orElse(null));
        assertEquals("warn", ladder.commandFor(3).orElse(null));
        assertEquals("ban", ladder.commandFor(5).orElse(null));
    }

    @Test
    void maxThresholdAndCommandTrackTheTopRung() {
        PunishmentLadder ladder = new PunishmentLadder(rungs(3, "kick", 6, "ban", 10, "ipban"));
        assertEquals(10, ladder.maxThreshold().orElse(-1));
        assertEquals("ipban", ladder.maxCommand().orElse(null));
    }
}
