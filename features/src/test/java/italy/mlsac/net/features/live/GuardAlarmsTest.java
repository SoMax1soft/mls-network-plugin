// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.guard.GuardMode;
import italy.mlsac.net.guard.CommandOrigin;
import italy.mlsac.net.guard.ConsoleOrder;
import italy.mlsac.net.guard.GuardReason;
import italy.mlsac.net.guard.GuardRefusal;
import italy.mlsac.net.guard.OriginKind;
import italy.mlsac.net.guard.RepeatGate;
import italy.mlsac.net.testkit.BundledResources;
import italy.mlsac.net.testkit.RecordingLog;
import italy.mlsac.net.text.LanguageId;
import italy.mlsac.net.text.MessageDefaults;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuardAlarmsTest {

    private final RecordingLog log = new RecordingLog();
    private final List<String> staff = new ArrayList<>();
    private long now = 1_000_000L;
    private final GuardAlarms alarms = new GuardAlarms(BundledResources.english(), GuardMessages.defaults(),
            staff::add, log, () -> now);

    @Test
    void aBlockedCommandWarnsOnceAndAlertsStaff() {
        ConsoleOrder order = ConsoleOrder.untargeted("op &cSteve", CommandOrigin.ladderStep(3));
        alarms.refused(order, new GuardRefusal(GuardReason.DENIED_ROOT, "op"), GuardMode.BLOCK);
        alarms.refused(order, new GuardRefusal(GuardReason.DENIED_ROOT, "op"), GuardMode.BLOCK);
        assertEquals(List.of("[CommandGuard] Blocked console command from ladder step VL 3: op cSteve"
                + " - 'op' is never allowed"), log.warnings);
        assertEquals(1, staff.size());
        assertTrue(staff.get(0).contains("Blocked a site command"));
        assertTrue(staff.get(0).contains("ladder step VL 3"));
        now += RepeatGate.QUIET_MILLIS;
        alarms.refused(order, new GuardRefusal(GuardReason.DENIED_ROOT, "op"), GuardMode.BLOCK);
        assertTrue(log.warnings.get(1).endsWith("(+1 similar in the last 5 min)"));
        assertEquals(2, staff.size());
    }

    @Test
    void warnModeSaysTheCommandRan() {
        alarms.refused(ConsoleOrder.untargeted("op Steve", CommandOrigin.menuButton()),
                new GuardRefusal(GuardReason.DENIED_ROOT, "op"), GuardMode.WARN);
        assertTrue(log.warnings.get(0).startsWith("[CommandGuard] Would block (mode warn, ran anyway)"));
        assertTrue(staff.get(0).contains("ran anyway"));
    }

    @Test
    void unknownCommandsOnlyGoToTheConsole() {
        alarms.refused(ConsoleOrder.untargeted("broadcast hi", CommandOrigin.ladderStep(2)),
                new GuardRefusal(GuardReason.UNKNOWN_COMMAND, "broadcast"), GuardMode.BLOCK);
        assertEquals(1, log.warnings.size());
        assertTrue(staff.isEmpty());
    }

    @Test
    void aBurstAlwaysReachesStaff() {
        alarms.burst("ban", 16, 10, GuardMode.BLOCK);
        assertTrue(log.warnings.get(0).contains("16 different players got 'ban'"));
        assertTrue(staff.get(0).contains("paused for"));
    }

    @Test
    void everyTextExistsInEnglishAndRussian() {
        MessageDefaults texts = GuardMessages.defaults();
        List<String> keys = new ArrayList<>(List.of(GuardMessages.BLOCKED, GuardMessages.WARNED,
                GuardMessages.PAUSED, GuardMessages.PAUSE_WARNED));
        for (GuardReason reason : GuardReason.values()) {
            keys.add(GuardMessages.reasonKey(reason));
        }
        for (OriginKind kind : OriginKind.values()) {
            keys.add(GuardMessages.originKey(kind));
        }
        for (String key : keys) {
            assertFalse(texts.text(key, LanguageId.EN).orElse("").isBlank(), key);
            assertFalse(texts.text(key, LanguageId.RU).orElse("").equals(texts.text(key, LanguageId.EN).orElse(""))
                    && !key.endsWith("-punish"), key);
        }
    }
}
