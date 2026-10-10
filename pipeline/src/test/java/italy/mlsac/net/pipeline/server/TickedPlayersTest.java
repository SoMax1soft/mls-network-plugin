// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.bus.SynchronousEventBus;
import italy.mlsac.net.config.detection.RegionEntry;
import italy.mlsac.net.config.detection.RegionSettings;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.lifecycle.PlayerTick;
import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.player.PlayerLeft;
import italy.mlsac.net.region.RegionExemptions;
import italy.mlsac.net.region.RegionLookup;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestLogs;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class TickedPlayersTest {

    private static final UUID ARENA = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-0000001aeeae");
    private static final UUID LOBBY = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-0000001b0d9d");
    private static final UUID FIRST = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-00000004372a");
    private static final UUID SECOND = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-000000045619");

    private final ManualClock clock = new ManualClock(1_000L);
    private final PluginLog log = TestLogs.capturing(new CapturingLogSink(), clock, false);
    private final SynchronousEventBus bus = new SynchronousEventBus(log);
    private final World arena = PlayerStandIns.world(ARENA, "arena");
    private final World lobby = PlayerStandIns.world(LOBBY, "lobby");
    private final Player first = PlayerStandIns.standing(FIRST, 1, arena, 0, 64, 0).build();
    private final Player second = PlayerStandIns.standing(SECOND, 2, lobby, 0, 64, 0).build();
    private final FixedRoster roster = new FixedRoster().with(first).with(second);
    private final List<String> performed = new ArrayList<>();
    private final List<UUID> forgotten = new ArrayList<>();

    @Test
    void serverTickVisitsEveryOnlinePlayerInOrder() {
        TickedPlayers ticked = new TickedPlayers(bus, roster, List.of(recorder()));
        ticked.start();
        bus.publish(new ServerTick(5L));
        assertEquals(List.of(FIRST + "@5", SECOND + "@5"), performed);
        ticked.stop();
        bus.publish(new ServerTick(6L));
        assertEquals(2, performed.size());
    }

    @Test
    void playerTickVisitsOnlyThatPlayerAndQuitForgets() {
        TickedPlayers ticked = new TickedPlayers(bus, roster, List.of(recorder()));
        ticked.start();
        bus.publish(new PlayerTick(SECOND, 3L));
        bus.publish(new PlayerTick(UUID.randomUUID(), 3L));
        assertEquals(List.of(SECOND + "@3"), performed);
        bus.publish(new PlayerLeft(SECOND, "Second", false));
        assertEquals(List.of(SECOND), forgotten);
        ticked.stop();
    }

    @Test
    void regionPatrolEvaluatesOnFirstSightAndFeedsTheCache() {
        RegionExemptions regions = new RegionExemptions(
                () -> new RegionSettings(true, List.of(RegionEntry.parse("lobby:*"))), RegionLookup.NONE, log);
        TickedPlayers ticked = new TickedPlayers(bus, roster, List.of(new RegionPatrol(regions, clock)));
        ticked.start();
        assertFalse(regions.lastKnown(SECOND));
        bus.publish(new PlayerTick(SECOND, 1L));
        assertTrue(regions.lastKnown(SECOND));
        assertFalse(regions.lastKnown(FIRST));
        bus.publish(new ServerTick(2L));
        assertFalse(regions.lastKnown(FIRST));
        assertTrue(clock.reads(RegionPatrol.CHECK_PURPOSE).size() >= 2);
        ticked.stop();
    }

    @Test
    void regionPatrolSpacesLaterChecks() {
        RegionExemptions regions = new RegionExemptions(
                () -> new RegionSettings(true, List.of()), RegionLookup.NONE, log);
        RegionPatrol patrol = new RegionPatrol(regions, clock);
        patrol.perform(first, 0L);
        for (long tick = 1; tick <= RegionPatrol.SPACING_TICKS * 3L; tick++) {
            patrol.perform(first, tick);
        }
        assertEquals(1 + 3, clock.reads(RegionPatrol.CHECK_PURPOSE).size());
        patrol.forget(FIRST);
        patrol.perform(first, 1_000_001L);
        assertEquals(1 + 3 + 1, clock.reads(RegionPatrol.CHECK_PURPOSE).size());
    }

    private PlayerTickWork recorder() {
        return new PlayerTickWork() {
            @Override
            public void perform(Player player, long tick) {
                performed.add(player.getUniqueId() + "@" + tick);
            }

            @Override
            public void forget(UUID player) {
                forgotten.add(player);
            }
        };
    }
}
