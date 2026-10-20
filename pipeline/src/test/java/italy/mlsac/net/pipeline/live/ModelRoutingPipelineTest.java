// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.api.ModelChoice;
import italy.mlsac.net.pipeline.engine.DataPipeline;
import italy.mlsac.net.pipeline.engine.PipelinePorts;
import italy.mlsac.net.pipeline.environment.DetectionSwitches;
import italy.mlsac.net.testkit.TestConfigs;
import italy.mlsac.net.testkit.TestCore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class ModelRoutingPipelineTest {

    private static final int FIGHTERS = 4;
    private static final long TICKS = 600;

    @Test
    void onALegacyServerAnUltraChoiceGetsOnlyUltraWindowsAndAProChoiceOnlyLegacyWindows() {
        TestCore core = new TestCore();
        core.apply(TestConfigs.withModels(core.config.get(), true, "pro", "fast"));
        Fight fight = new Fight(new ConfiguredSwitches(core.config, null, core.selections));
        core.selections.set(fight.player(0), ModelChoice.ULTRA);
        core.selections.set(fight.player(1), ModelChoice.PRO);

        fight.run();

        assertOnlyUltra(fight, 0);
        assertOnlyLegacy(fight, 1);
        assertOnlyLegacy(fight, 2);
        assertOnlyLegacy(fight, 3);
    }

    @Test
    void onAnUltraServerProAndMiniChoicesGetOnlyLegacyWindows() {
        TestCore core = new TestCore();
        core.apply(TestConfigs.withModels(core.config.get(), true, "ultra", "pro", "fast"));
        Fight fight = new Fight(new ConfiguredSwitches(core.config, null, core.selections));
        core.selections.set(fight.player(0), ModelChoice.PRO);
        core.selections.set(fight.player(1), ModelChoice.MINI);

        fight.run();

        assertOnlyLegacy(fight, 0);
        assertOnlyLegacy(fight, 1);
        assertOnlyUltra(fight, 2);
        assertOnlyUltra(fight, 3);
    }

    @Test
    void withoutOverridesAnUltraServerSendsTheSameUltraWindowsAsBefore() {
        TestCore core = new TestCore();
        core.apply(TestConfigs.withModels(core.config.get(), true, "ultra", "pro", "fast"));
        Fight routed = new Fight(new ConfiguredSwitches(core.config, null, core.selections));
        Fight before = new Fight(fixedSwitches(true));

        routed.run();
        before.run();

        for (int index = 0; index < FIGHTERS; index++) {
            assertSameWindows(before.ultra(index), routed.ultra(index));
            assertTrue(routed.legacy(index).isEmpty());
        }
        assertTrue(routed.ultraCount() > 10, "ultra windows " + routed.ultraCount());
    }

    @Test
    void withoutOverridesALegacyServerSendsTheSameLegacyWindowsAsBefore() {
        TestCore core = new TestCore();
        core.apply(TestConfigs.withModels(core.config.get(), true, "pro", "fast"));
        Fight routed = new Fight(new ConfiguredSwitches(core.config, null, core.selections));
        Fight before = new Fight(fixedSwitches(false));

        routed.run();
        before.run();

        for (int index = 0; index < FIGHTERS; index++) {
            assertSameWindows(before.legacy(index), routed.legacy(index));
            assertTrue(routed.ultra(index).isEmpty());
        }
        assertTrue(routed.legacyCount() > 10, "legacy windows " + routed.legacyCount());
    }

    @Test
    void aFallbackSendsThePlayerBackToTheServerDefault() {
        TestCore core = new TestCore();
        core.apply(TestConfigs.withModels(core.config.get(), true, "ultra", "fast"));
        Fight fight = new Fight(new ConfiguredSwitches(core.config, null, core.selections));
        assertTrue(core.selections.set(fight.player(0), ModelChoice.PRO).fellBack());

        fight.run();

        assertOnlyUltra(fight, 0);
    }

    private static void assertOnlyUltra(Fight fight, int index) {
        assertFalse(fight.ultra(index).isEmpty(), "fighter " + index + " has no Ultra window");
        assertTrue(fight.legacy(index).isEmpty(), "fighter " + index + " has legacy windows");
    }

    private static void assertOnlyLegacy(Fight fight, int index) {
        assertFalse(fight.legacy(index).isEmpty(), "fighter " + index + " has no legacy window");
        assertTrue(fight.ultra(index).isEmpty(), "fighter " + index + " has Ultra windows");
    }

    private static void assertSameWindows(List<byte[]> expected, List<byte[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertTrue(java.util.Arrays.equals(expected.get(i), actual.get(i)), "window " + i);
        }
    }

    private static DetectionSwitches fixedSwitches(boolean ultra) {
        return new DetectionSwitches() {
            @Override
            public boolean detectionEnabled() {
                return true;
            }

            @Override
            public boolean ultraEnabled() {
                return ultra;
            }
        };
    }

    private static final class Fight {
        private final ArenaServer server = new ArenaServer();
        private final ArenaFight arena = new ArenaFight(server, FIGHTERS);
        private final Map<UUID, List<byte[]>> ultraWindows = new ConcurrentHashMap<>();
        private final Map<UUID, List<byte[]>> legacyWindows = new ConcurrentHashMap<>();
        private final PlayerOrderedInput input;

        Fight(DetectionSwitches switches) {
            PipelinePorts ports = new PipelinePorts(server, server, server, server, server, server, server, server,
                    server, switches, server, (player, window) -> of(ultraWindows, player).add(window),
                    (player, window) -> of(legacyWindows, player).add(window), server);
            this.input = new PlayerOrderedInput(new DataPipeline(ports));
        }

        UUID player(int index) {
            return arena.players().get(index);
        }

        void run() {
            input.enabled();
            for (long tick = 0; tick < TICKS; tick++) {
                arena.tickServer(input, tick);
                for (int index = 0; index < FIGHTERS; index++) {
                    arena.tickClient(input, index, tick);
                }
            }
        }

        List<byte[]> ultra(int index) {
            return of(ultraWindows, player(index));
        }

        List<byte[]> legacy(int index) {
            return of(legacyWindows, player(index));
        }

        int ultraCount() {
            return ultraWindows.values().stream().mapToInt(List::size).sum();
        }

        int legacyCount() {
            return legacyWindows.values().stream().mapToInt(List::size).sum();
        }

        private static List<byte[]> of(Map<UUID, List<byte[]>> windows, UUID player) {
            return windows.computeIfAbsent(player, ignored -> Collections.synchronizedList(new ArrayList<>()));
        }
    }
}
