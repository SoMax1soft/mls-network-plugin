// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.console.LogLevel;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestLogs;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

class BukkitPlayerStatesTest {

    private static final UUID WORLD = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-00000016d2f0");
    private static final UUID FIRST = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-00000000594a");
    private static final UUID SECOND = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-000000007839");

    private final CapturingLogSink sink = new CapturingLogSink();
    private final ManualClock clock = new ManualClock(0L);
    private final PluginLog log = TestLogs.capturing(sink, clock, false);
    private final World world = PlayerStandIns.world(WORLD, "arena");

    @Test
    void connectedPlayersFollowTheServerOrder() {
        Player second = PlayerStandIns.standing(SECOND, 8, world, 0, 64, 0).build();
        Player first = PlayerStandIns.standing(FIRST, 7, world, 0, 64, 0).build();
        BukkitPlayerStates states = states(new FixedRoster().with(second).with(first), FixedOwnership.everything());
        assertEquals(List.of(SECOND, FIRST), states.connectedPlayers());
    }

    @Test
    void sampleCarriesEveryField() {
        clock.advanceMillis(5L);
        Player player = PlayerStandIns.standing(FIRST, 7, world, 4.5, -60.0, 8.25).build();
        PlayerSample sample = states(new FixedRoster().with(player), FixedOwnership.everything()).sample(FIRST);
        assertEquals(FIRST, sample.player());
        assertEquals(WORLD, sample.world());
        assertEquals(7, sample.entityNumber());
        assertEquals(5_000_000L, sample.capturedAt());
        assertEquals(4.5, sample.x());
        assertEquals(-60.0, sample.y());
        assertEquals(8.25, sample.z());
        assertEquals(12.5f, sample.yaw());
        assertEquals(-3.25f, sample.pitch());
        assertEquals(1.6200000047683716, sample.eyeLift());
        assertEquals(1.7999999523162842, sample.height());
        assertEquals(0.6000000238418579, sample.width());
        assertEquals(0.0, sample.fallBlocks());
        assertEquals(0.7f, sample.swingCharge());
        assertTrue(sample.valid());
        assertFalse(sample.dead());
        assertTrue(sample.posture().sprinting());
        assertTrue(sample.posture().submerged());
        assertFalse(sample.posture().blind());
        assertEquals("air", sample.handMaterial());
        assertEquals(5.0f, sample.chargeTicks());
        assertEquals("SURVIVAL", sample.playModeName());
        assertNull(sample.interactionRange());
        assertEquals(List.of(SampleReading.CAPTURE_PURPOSE), clock.reads());
    }

    @Test
    void heldMaterialIsLowerCase() {
        PlayerInventory inventory = StandIn.of(PlayerInventory.class)
                .answer("getItemInMainHand", new ItemStack(Material.DIAMOND_SWORD)).build();
        Player player = PlayerStandIns.standing(FIRST, 7, world, 0, 64, 0).answer("getInventory", inventory).build();
        assertEquals("diamond_sword", new HandReading(log).material(player));
    }

    @Test
    void unreadableHandIsUnknown() {
        Player player = PlayerStandIns.standing(FIRST, 7, world, 0, 64, 0)
                .compute("getInventory", arguments -> {
                    throw new IllegalStateException("inventory gone");
                }).build();
        assertEquals("unknown", new HandReading(log).material(player));
    }

    @Test
    void playerOfAnotherThreadIsNotSampled() {
        Player player = PlayerStandIns.standing(FIRST, 7, world, 0, 64, 0).build();
        BukkitPlayerStates states = states(new FixedRoster().with(player),
                FixedOwnership.everything().withoutEntity(player));
        assertNull(states.sample(FIRST));
        assertNull(states.sample(SECOND));
    }

    @Test
    void failingReadIsWarnedAndSkipped() {
        Player player = PlayerStandIns.standing(FIRST, 7, world, 0, 64, 0)
                .compute("getLocation", arguments -> {
                    throw new IllegalStateException("off thread");
                }).build();
        assertNull(states(new FixedRoster().with(player), FixedOwnership.everything()).sample(FIRST));
        assertEquals(1, sink.texts(LogLevel.WARNING).size());
    }

    @Test
    void pingIsAbsentForMissingPlayersOrFailingReads() {
        Player player = PlayerStandIns.standing(FIRST, 7, world, 0, 64, 0).build();
        Player broken = PlayerStandIns.standing(SECOND, 8, world, 0, 64, 0)
                .compute("getPing", arguments -> {
                    throw new IllegalStateException("no connection");
                }).build();
        FixedRoster roster = new FixedRoster().with(player).with(broken);
        BukkitPlayerStates states = states(roster, FixedOwnership.everything());
        assertEquals(42, states.serverPing(FIRST));
        assertEquals(PingReading.ABSENT, states.serverPing(SECOND));
        roster.remove(player);
        assertEquals(PingReading.ABSENT, states.serverPing(FIRST));
    }

    private BukkitPlayerStates states(FixedRoster roster, FixedOwnership ownership) {
        SampleReading reading = new SampleReading(clock,
                new PostureReading(new NativeClimbing(), new NativeWaterContact(), player -> false),
                new HandReading(log), player -> 5.0f, ReachReading.ABSENT);
        return new BukkitPlayerStates(roster, ownership, reading, new PlayerPing(), log);
    }
}
