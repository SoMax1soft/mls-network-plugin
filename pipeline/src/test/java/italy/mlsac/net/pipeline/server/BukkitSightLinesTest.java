// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.console.LogLevel;
import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestLogs;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class BukkitSightLinesTest {

    private static final UUID WORLD = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-00000014c511");

    private final CapturingLogSink sink = new CapturingLogSink();
    private final List<Object[]> casts = new ArrayList<>();

    @Test
    void missingWorldIsUnknown() {
        BukkitSightLines lines = new BukkitSightLines(new FixedRoster(), FixedOwnership.everything(), AirReading.UNREADABLE, log());
        assertEquals(RayVerdict.UNKNOWN, lines.trace(WORLD, new Vec3(0, 64, 0), new Vec3(2, 64, 0)));
    }

    @Test
    void unloadedEndChunkIsUnknown() {
        World world = world(Set.of(0L), arguments -> null);
        BukkitSightLines lines = sightLines(world, FixedOwnership.everything());
        assertEquals(RayVerdict.UNKNOWN, lines.trace(WORLD, new Vec3(1, 64, 1), new Vec3(17, 64, 1)));
        assertTrue(casts.isEmpty());
    }

    @Test
    void chunkOfAnotherRegionIsUnknown() {
        World world = world(Set.of(0L, 1L), arguments -> null);
        BukkitSightLines lines = sightLines(world, FixedOwnership.everything().withoutChunk(1, 0));
        assertEquals(RayVerdict.UNKNOWN, lines.trace(WORLD, new Vec3(1, 64, 1), new Vec3(17, 64, 1)));
        assertTrue(casts.isEmpty());
    }

    @Test
    void tinySegmentIsClearWithoutCasting() {
        World world = world(Set.of(0L), arguments -> null);
        BukkitSightLines lines = sightLines(world, FixedOwnership.everything());
        assertEquals(RayVerdict.CLEAR, lines.trace(WORLD, new Vec3(1, 64, 1), new Vec3(1, 64, 1.0005)));
        assertTrue(casts.isEmpty());
    }

    @Test
    void castUsesOutlinesWithoutFluidsAlongTheSegment() {
        World world = world(Set.of(0L), arguments -> null);
        BukkitSightLines lines = sightLines(world, FixedOwnership.everything());
        assertEquals(RayVerdict.CLEAR, lines.trace(WORLD, new Vec3(1, 64, 1), new Vec3(4, 68, 1)));
        assertEquals(1, casts.size());
        Object[] cast = casts.get(0);
        Location start = (Location) cast[0];
        Vector direction = (Vector) cast[1];
        assertSame(world, start.getWorld());
        assertEquals(1.0, start.getX());
        assertEquals(64.0, start.getY());
        assertEquals(0.6, direction.getX(), 1e-12);
        assertEquals(0.8, direction.getY(), 1e-12);
        assertEquals(5.0, (double) cast[2], 1e-12);
        assertEquals(FluidCollisionMode.NEVER, cast[3]);
        assertEquals(false, cast[4]);
    }

    @Test
    void hitBeforeTheEndBlocksAndHitAtTheEndDoesNot() {
        World early = world(Set.of(0L), arguments -> new RayTraceResult(new Vector(3.0, 64.0, 1.0)));
        assertEquals(RayVerdict.BLOCKED,
                sightLines(early, FixedOwnership.everything()).trace(WORLD, new Vec3(1, 64, 1), new Vec3(5, 64, 1)));
        World late = world(Set.of(0L), arguments -> new RayTraceResult(new Vector(4.9995, 64.0, 1.0)));
        assertEquals(RayVerdict.CLEAR,
                sightLines(late, FixedOwnership.everything()).trace(WORLD, new Vec3(1, 64, 1), new Vec3(5, 64, 1)));
    }

    @Test
    void failingQueryIsUnknownAndWarned() {
        World world = world(Set.of(0L), arguments -> {
            throw new IllegalStateException("chunk access off-region");
        });
        BukkitSightLines lines = sightLines(world, FixedOwnership.everything());
        assertEquals(RayVerdict.UNKNOWN, lines.trace(WORLD, new Vec3(1, 64, 1), new Vec3(5, 64, 1)));
        assertEquals(1, sink.texts(LogLevel.WARNING).size());
    }

    @Test
    void nonFiniteSegmentIsUnknown() {
        World world = world(Set.of(0L), arguments -> null);
        BukkitSightLines lines = sightLines(world, FixedOwnership.everything());
        assertEquals(RayVerdict.UNKNOWN, lines.trace(WORLD, new Vec3(Double.NaN, 64, 1), new Vec3(5, 64, 1)));
    }

    @Test
    void consecutiveCastsReuseTheirStartAndDirection() {
        World world = world(Set.of(0L), arguments -> null);
        BukkitSightLines lines = sightLines(world, FixedOwnership.everything());
        lines.trace(WORLD, new Vec3(1, 64, 1), new Vec3(4, 68, 1));
        lines.trace(WORLD, new Vec3(2, 65, 3), new Vec3(2, 65, 7));
        assertEquals(2, casts.size());
        assertSame(casts.get(0)[0], casts.get(1)[0]);
        assertSame(casts.get(0)[1], casts.get(1)[1]);
        Location start = (Location) casts.get(1)[0];
        Vector direction = (Vector) casts.get(1)[1];
        assertEquals(2.0, start.getX());
        assertEquals(65.0, start.getY());
        assertEquals(3.0, start.getZ());
        assertEquals(0.0, direction.getX());
        assertEquals(1.0, direction.getZ());
        assertEquals(4.0, (double) casts.get(1)[2]);
    }

    @Test
    void openAirBetweenEyeAndEndsIsProvenWithoutCasting() {
        List<List<Integer>> read = new ArrayList<>();
        World world = blockWorld(Set.of(0L), read, block -> false);
        BukkitSightLines lines = proofLines(world, FixedOwnership.everything());
        assertTrue(lines.provenClear(WORLD, new Vec3(1.5, 65.62, 1.5),
                new Box(3.25, 64.05, 1.25, 3.75, 65.75, 1.75)));
        assertTrue(casts.isEmpty());
        assertEquals(3 * 2 * 1, read.size());
        assertTrue(read.stream().allMatch(block -> block.get(1) >= 64 && block.get(1) <= 65));
    }

    @Test
    void anySolidBlockInTheSpanLeavesTheRaysToBeAsked() {
        World world = blockWorld(Set.of(0L), new ArrayList<>(), block -> block.equals(List.of(2, 65, 1)));
        assertFalse(proofLines(world, FixedOwnership.everything()).provenClear(WORLD, new Vec3(1.5, 65.62, 1.5),
                new Box(3.25, 64.05, 1.25, 3.75, 65.75, 1.75)));
    }

    @Test
    void spansTouchingAnUnreadableChunkAreNotScanned() {
        List<List<Integer>> read = new ArrayList<>();
        World world = blockWorld(Set.of(0L), read, block -> false);
        assertFalse(proofLines(world, FixedOwnership.everything()).provenClear(WORLD, new Vec3(14.5, 65.62, 1.5),
                new Box(16.25, 64.05, 1.25, 16.75, 65.75, 1.75)));
        World loaded = blockWorld(Set.of(0L, 1L), read, block -> false);
        assertFalse(proofLines(loaded, FixedOwnership.everything().withoutChunk(1, 0)).provenClear(WORLD,
                new Vec3(14.5, 65.62, 1.5), new Box(16.25, 64.05, 1.25, 16.75, 65.75, 1.75)));
        assertTrue(read.isEmpty());
    }

    @Test
    void wideOrBrokenSpansAndUnreadableBlocksAreNeverProven() {
        List<List<Integer>> read = new ArrayList<>();
        World world = blockWorld(Set.of(0L), read, block -> false);
        BukkitSightLines lines = proofLines(world, FixedOwnership.everything());
        assertFalse(lines.provenClear(WORLD, new Vec3(1.5, 65.62, 1.5), new Box(9.25, 64.05, 9.25, 9.75, 65.75, 9.75)));
        assertFalse(lines.provenClear(WORLD, new Vec3(Double.NaN, 65.62, 1.5),
                new Box(3.25, 64.05, 1.25, 3.75, 65.75, 1.75)));
        assertTrue(read.isEmpty());
        assertFalse(sightLines(world, FixedOwnership.everything()).provenClear(WORLD, new Vec3(1.5, 65.62, 1.5),
                new Box(3.25, 64.05, 1.25, 3.75, 65.75, 1.75)));
        assertFalse(new BukkitSightLines(new FixedRoster(), FixedOwnership.everything(), new BlockTypeAir(), log())
                .provenClear(WORLD, new Vec3(1.5, 65.62, 1.5), new Box(3.25, 64.05, 1.25, 3.75, 65.75, 1.75)));
    }

    private BukkitSightLines proofLines(World world, FixedOwnership ownership) {
        return new BukkitSightLines(new FixedRoster().with(WORLD, world), ownership, new BlockTypeAir(), log());
    }

    private World blockWorld(Set<Long> loadedChunkXs, List<List<Integer>> read, Predicate<List<Integer>> solid) {
        return StandIn.of(World.class)
                .answer("getUID", WORLD)
                .compute("isChunkLoaded", arguments -> arguments.length == 2
                        && loadedChunkXs.contains((long) (int) arguments[0]) && (int) arguments[1] == 0)
                .compute("getType", arguments -> {
                    List<Integer> block = List.of((int) arguments[0], (int) arguments[1], (int) arguments[2]);
                    read.add(block);
                    return solid.test(block) ? Material.STONE : Material.AIR;
                })
                .compute("rayTraceBlocks", arguments -> {
                    casts.add(arguments);
                    return null;
                })
                .build();
    }

    private BukkitSightLines sightLines(World world, FixedOwnership ownership) {
        return new BukkitSightLines(new FixedRoster().with(WORLD, world), ownership, AirReading.UNREADABLE, log());
    }

    private World world(Set<Long> loadedChunkXs, Function<Object[], Object> answer) {
        return StandIn.of(World.class)
                .answer("getUID", WORLD)
                .compute("isChunkLoaded", arguments -> arguments.length == 2
                        && loadedChunkXs.contains((long) (int) arguments[0]) && (int) arguments[1] == 0)
                .compute("rayTraceBlocks", arguments -> {
                    casts.add(arguments);
                    return answer.apply(arguments);
                })
                .build();
    }

    private italy.mlsac.net.console.PluginLog log() {
        return TestLogs.capturing(sink, new ManualClock(0L), false);
    }
}
