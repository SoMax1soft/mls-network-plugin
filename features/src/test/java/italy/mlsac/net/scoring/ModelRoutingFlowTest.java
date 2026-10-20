// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.api.ModelChoice;
import italy.mlsac.net.api.bukkit.ModelCheckEvent;
import italy.mlsac.net.api.bukkit.UltraFlagEvent;
import italy.mlsac.net.bus.SynchronousEventBus;
import italy.mlsac.net.detection.choice.LegacyCheckObserved;
import italy.mlsac.net.detection.choice.ModelRoute;
import italy.mlsac.net.detection.choice.PlayerModelRoutes;
import italy.mlsac.net.detection.choice.UltraFlagObserved;
import italy.mlsac.net.detection.choice.bukkit.ApiEventRelay;
import italy.mlsac.net.features.live.ModelCheckRelay;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.FixedModels;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.RecordingLog;
import italy.mlsac.net.testkit.TestLogs;
import italy.mlsac.net.testkit.TickQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;

class ModelRoutingFlowTest {

    private final RecordingLog log = new RecordingLog();
    private final TickQueue ticks = new TickQueue();
    private final Map<UUID, ModelRoute> routes = new HashMap<>();
    private final List<CheckOutcome> checks = new ArrayList<>();
    private final List<FlagContext> flags = new ArrayList<>();
    private final SuspectRecord suspect = new SuspectRecord(UUID.nameUUIDFromBytes("Lab".getBytes()), "Lab",
            SuspectKind.JAVA);
    private FixedModels models = FixedModels.ultraOff();
    private CheckObserver observer = checks::add;

    @Test
    void theServerDefaultScreensExactlyAsBefore() {
        DetectionFlow ultraServer = flow(FixedModels.ultraOn());
        ultraServer.process(suspect, new Verdict("pro", 0.9));
        ultraServer.process(suspect, new Verdict("ultra", 0.9));
        DetectionFlow legacyServer = flow(FixedModels.ultraOff());
        legacyServer.process(suspect, new Verdict("ultra", 0.9));
        legacyServer.process(suspect, new Verdict("fast", 0.9));

        assertEquals(List.of("ultra", "fast"), checkedModels());
    }

    @Test
    void aProChoiceCountsOnlyProAnswersEvenOnAnUltraServer() {
        routes.put(suspect.id(), ModelRoute.PRO);
        DetectionFlow flow = flow(FixedModels.ultraOn());
        for (String model : new String[] {"ultra", "fast", "mini", "experimental", "pro"}) {
            flow.process(suspect, new Verdict(model, 0.9));
        }
        assertEquals(List.of("pro"), checkedModels());
    }

    @Test
    void aMiniChoiceCountsFastAndMiniAnswers() {
        routes.put(suspect.id(), ModelRoute.MINI);
        DetectionFlow flow = flow(FixedModels.ultraOff());
        for (String model : new String[] {"pro", "fast", "mini", "ultra"}) {
            flow.process(suspect, new Verdict(model, 0.9));
        }
        assertEquals(List.of("fast", "mini"), checkedModels());
    }

    @Test
    void anUltraChoiceCountsUltraAnswersOnALegacyServerWhoseRosterHasUltraOff() {
        routes.put(suspect.id(), ModelRoute.ULTRA);
        FixedModels legacy = FixedModels.ultraOff();
        legacy.disabled.add("ultra");
        DetectionFlow flow = flow(legacy);
        flow.process(suspect, new Verdict("pro", 0.9));
        flow.process(suspect, new Verdict("ultra", 0.9));
        flow.process(suspect, new Verdict(IntegrityVerdict.MODEL, 1.0));
        assertEquals(List.of("ultra", IntegrityVerdict.MODEL), checkedModels());
    }

    @Test
    void aProChoiceRespectsAModelTheSwitchboardTurnedOff() {
        routes.put(suspect.id(), ModelRoute.PRO);
        FixedModels switchedOff = FixedModels.ultraOff();
        switchedOff.disabled.add("pro");
        flow(switchedOff).process(suspect, new Verdict("pro", 0.9));
        assertTrue(checks.isEmpty());
    }

    @Test
    void checkOutcomesCarryTheBufferAfterTheStepAndTheFlag() {
        routes.put(suspect.id(), ModelRoute.PRO);
        DetectionFlow flow = flow(FixedModels.ultraOff());
        for (int answer = 0; answer < 5; answer++) {
            flow.process(suspect, new Verdict("pro", 0.99));
        }

        CheckOutcome first = checks.get(0);
        assertEquals(BufferFormula.next(0.0, 0.99, ScoringRules.pluginDefaults()), first.buffer());
        assertFalse(first.flagged());
        assertFalse(checks.get(3).flagged());
        CheckOutcome fifth = checks.get(4);
        assertTrue(fifth.flagged());
        assertTrue(fifth.buffer() >= ScoringRules.pluginDefaults().flagThreshold());
        assertEquals(ScoringRules.pluginDefaults().resetBuffer(), suspect.buffer());
        ticks.tick();
        assertEquals(1, flags.size());
    }

    @Test
    void theLabSeesProbabilitiesForProAndMiniButOnlyFlagsForUltra() {
        SynchronousEventBus bus = new SynchronousEventBus(TestLogs.capturing(new CapturingLogSink(),
                new ManualClock(0), false));
        List<Object> observed = new ArrayList<>();
        bus.subscribe(LegacyCheckObserved.class, observed::add);
        bus.subscribe(UltraFlagObserved.class, observed::add);
        List<Event> fired = new ArrayList<>();
        new ApiEventRelay(bus, fired::add).start();
        observer = new ModelCheckRelay(bus);

        routes.put(suspect.id(), ModelRoute.ULTRA);
        DetectionFlow ultra = flow(FixedModels.ultraOn());
        for (int answer = 0; answer < 6; answer++) {
            ultra.process(suspect, new Verdict("ultra", 0.97));
        }
        assertEquals(1, fired.size());
        for (Event event : fired) {
            assertInstanceOf(UltraFlagEvent.class, event);
        }
        for (Object record : observed) {
            assertInstanceOf(UltraFlagObserved.class, record);
        }

        fired.clear();
        routes.put(suspect.id(), ModelRoute.MINI);
        DetectionFlow mini = flow(FixedModels.ultraOff());
        mini.process(suspect, new Verdict("mini", 0.42));
        mini.process(suspect, new Verdict("experimental", 0.99));
        assertEquals(1, fired.size());
        ModelCheckEvent check = assertInstanceOf(ModelCheckEvent.class, fired.get(0));
        assertEquals(ModelChoice.MINI, check.getModel());
        assertEquals(0.42, check.getProbability());
        assertEquals(suspect.buffer(), check.getBuffer());
    }

    private DetectionFlow flow(FixedModels switchboard) {
        this.models = switchboard;
        ticks.online.add(suspect.id());
        PlayerModelRoutes playerRoutes = new PlayerModelRoutes() {
            @Override
            public ModelRoute routeOf(UUID player) {
                return routes.getOrDefault(player, ModelRoute.SERVER_DEFAULT);
            }

            @Override
            public boolean anyPlayerOn(ModelRoute route) {
                return routes.containsValue(route);
            }
        };
        Quiet quiet = new Quiet();
        FlagProcessor flagProcessor = new FlagProcessor(new ViolationLedger(), ticks, quiet, quiet);
        return new DetectionFlow(ScoringRules::pluginDefaults, () -> new ReportingPolicy(false, 0.8),
                new VerdictScreen(() -> models, playerRoutes, log), quiet, event -> { }, () -> BufferReactions.NONE,
                ticks, flagProcessor, outcome -> observer.checked(outcome), log);
    }

    private List<String> checkedModels() {
        return checks.stream().map(check -> check.verdict().model()).toList();
    }

    private final class Quiet implements DetectionAudience, FlagPenalties {

        @Override
        public void answerSeen(SuspectRecord record, ScreenedVerdict verdict) {
        }

        @Override
        public void stepsCrossed(SuspectRecord record, ScreenedVerdict verdict, List<AlertStep> crossed,
                                 double buffer) {
        }

        @Override
        public void flagRaised(FlagContext flag) {
        }

        @Override
        public void onFlag(FlagContext flag) {
            flags.add(flag);
        }
    }
}
