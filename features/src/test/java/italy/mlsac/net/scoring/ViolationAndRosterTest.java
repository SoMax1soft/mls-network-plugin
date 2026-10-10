// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.testkit.RecordingLog;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ViolationAndRosterTest {

    private final UUID player = UUID.randomUUID();

    @Test
    void vlDecaysEachRunAndIsRemovedAtZero() {
        ViolationLedger ledger = new ViolationLedger();
        ledger.raise(player);
        ledger.raise(player);
        ledger.raise(player);
        AtomicReference<ViolationDecayRules> rules = new AtomicReference<>(new ViolationDecayRules(true, 1, 1));
        ViolationDecayRun run = new ViolationDecayRun(ledger, rules::get, CombatProbe.NEVER);
        run.run();
        assertEquals(2, ledger.level(player));
        run.run();
        assertEquals(1, ledger.level(player));
        run.run();
        assertEquals(0, ledger.level(player));
        assertEquals(20L, rules.get().everyTicks());
    }

    @Test
    void vlDoesNotDecayInLegacyCombat() {
        ViolationLedger ledger = new ViolationLedger();
        ledger.raise(player);
        ledger.raise(player);
        new ViolationDecayRun(ledger, () -> new ViolationDecayRules(true, 1, 1), id -> true).run();
        assertEquals(2, ledger.level(player));
    }

    @Test
    void disabledDecayDoesNothing() {
        ViolationLedger ledger = new ViolationLedger();
        ledger.raise(player);
        new ViolationDecayRun(ledger, ViolationDecayRules::pluginDefaults, CombatProbe.NEVER).run();
        assertEquals(1, ledger.level(player));
        assertEquals(30_000L, ViolationDecayRules.pluginDefaults().everyTicks());
    }

    @Test
    void rejoinWithinFifteenMinutesKeepsBufferAndVl() {
        SuspectRoster roster = new SuspectRoster(SuspectRoster.REJOIN_RETENTION);
        ViolationLedger ledger = new ViolationLedger();
        SuspectRecord record = roster.admit(player, "Steve", SuspectKind.JAVA, 0);
        record.count("ultra", 1.0, before -> 33.0);
        ledger.raise(player);
        roster.depart(player, 1_000);
        assertTrue(roster.active(player).isEmpty());
        SuspectRecord back = roster.admit(player, "Steve2", SuspectKind.JAVA, 1_000 + Duration.ofMinutes(14).toMillis());
        assertSame(record, back);
        assertEquals(33.0, back.buffer());
        assertEquals("Steve2", back.name());
        assertEquals(1, ledger.level(player));
    }

    @Test
    void rejoinAfterRetentionStartsFresh() {
        SuspectRoster roster = new SuspectRoster(SuspectRoster.REJOIN_RETENTION);
        SuspectRecord record = roster.admit(player, "Steve", SuspectKind.JAVA, 0);
        record.count("ultra", 1.0, before -> 33.0);
        roster.depart(player, 0);
        SuspectRecord back = roster.admit(player, "Steve", SuspectKind.JAVA, Duration.ofMinutes(16).toMillis());
        assertNotSame(record, back);
        assertEquals(0.0, back.buffer());
    }

    @Test
    void purgeDropsExpiredDepartures() {
        SuspectRoster roster = new SuspectRoster(Duration.ofMinutes(15));
        roster.admit(player, "Steve", SuspectKind.JAVA, 0);
        roster.depart(player, 0);
        assertEquals(0, roster.purgeExpired(Duration.ofMinutes(10).toMillis()));
        assertEquals(1, roster.purgeExpired(Duration.ofMinutes(16).toMillis()));
    }

    @Test
    void bypassPermissionIsCachedForFiveSeconds() {
        AtomicInteger checks = new AtomicInteger();
        BypassVerdictMemo cache = new BypassVerdictMemo(id -> checks.incrementAndGet() > 0);
        assertTrue(cache.bypasses(player, 0));
        assertTrue(cache.bypasses(player, 4_999));
        assertEquals(1, checks.get());
        cache.bypasses(player, 5_000);
        assertEquals(2, checks.get());
        cache.forget(player);
        cache.bypasses(player, 5_001);
        assertEquals(3, checks.get());
    }

    @Test
    void collectionVerdicts() {
        assertEquals(CollectionVerdict.DETECTION_OFF,
                CollectionVerdict.of(new CollectionFacts(false, false, SuspectKind.JAVA, false)));
        assertEquals(CollectionVerdict.BYPASS,
                CollectionVerdict.of(new CollectionFacts(true, true, SuspectKind.JAVA, false)));
        assertEquals(CollectionVerdict.BEDROCK,
                CollectionVerdict.of(new CollectionFacts(true, false, SuspectKind.BEDROCK, false)));
        assertEquals(CollectionVerdict.REGION_EXEMPT,
                CollectionVerdict.of(new CollectionFacts(true, false, SuspectKind.JAVA, true)));
        assertTrue(CollectionVerdict.of(new CollectionFacts(true, false, SuspectKind.JAVA, false)).collects());
        assertFalse(CollectionVerdict.BYPASS.collects());
    }

    @Test
    void ultraIntakeDropsErrorsAndNonFinite() {
        UltraIntake intake = new UltraIntake();
        assertTrue(intake.admit(new ModelAnswer(0.5, "x", java.util.Optional.of("boom"))).isEmpty());
        assertTrue(intake.admit(ModelAnswer.of(Double.POSITIVE_INFINITY, "x")).isEmpty());
        assertEquals(new Verdict("ultra", 0.5), intake.admit(ModelAnswer.of(0.5, "anything")).orElseThrow());
    }

    @Test
    void legacySequenceUpdates() {
        RecordingLog log = new RecordingLog();
        AtomicInteger length = new AtomicInteger(40);
        LegacyWindowLength window = new LegacyWindowLength() {
            @Override
            public int current() {
                return length.get();
            }

            @Override
            public void change(int newLength) {
                length.set(newLength);
            }
        };
        LegacyIntake intake = new LegacyIntake(new LegacySequenceReply(window, log));
        assertTrue(intake.admit(new ModelAnswer(0, "pro", java.util.Optional.of("INVALID_SEQUENCE: 60"))).isEmpty());
        assertEquals(60, length.get());
        intake.admit(new ModelAnswer(0, "pro", java.util.Optional.of("INVALID_SEQUENCE:60")));
        intake.admit(new ModelAnswer(0, "pro", java.util.Optional.of("INVALID_SEQUENCE:500")));
        intake.admit(new ModelAnswer(0, "pro", java.util.Optional.of("INVALID_SEQUENCE:abc")));
        assertEquals(60, length.get());
        assertEquals(List.of("[Detection] Backend asked for window length 500, kept the old one (5..200 allowed)",
                "[Detection] Window length in backend error is not a number: INVALID_SEQUENCE:abc"), log.warnings);
        assertEquals(1, log.infos.size());
        assertEquals(new Verdict("pro", 0.7), intake.admit(new ModelAnswer(0.7, "pro",
                java.util.Optional.of("other error"))).orElseThrow());
    }
}
