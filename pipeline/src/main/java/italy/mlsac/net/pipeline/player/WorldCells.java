// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.player;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

final class WorldCells {
    private static final double CELL_BLOCKS = 8.0;
    private static final double EDGE_SLACK = 0.001;
    private static final double LOWEST_CELL = Integer.MIN_VALUE + 2.0;
    private static final double HIGHEST_CELL = Integer.MAX_VALUE - 2.0;

    private final UUID world;
    private final long[] cellKeys;
    private final int[] cellStarts;
    private final SampleTrack[] packed;

    private WorldCells(UUID world, long[] cellKeys, int[] cellStarts, SampleTrack[] packed) {
        this.world = world;
        this.cellKeys = cellKeys;
        this.cellStarts = cellStarts;
        this.packed = packed;
    }

    static WorldCells of(UUID world, List<SampleTrack> members, List<PlayerSample> placedAs) {
        int count = members.size();
        long[] keys = new long[count];
        for (int index = 0; index < count; index++) {
            PlayerSample placed = placedAs.get(index);
            keys[index] = key(cell(placed.x()), cell(placed.z()));
        }
        long[] sorted = keys.clone();
        Arrays.sort(sorted);
        int distinct = 0;
        for (int index = 0; index < count; index++) {
            if (index == 0 || sorted[index] != sorted[index - 1]) {
                sorted[distinct++] = sorted[index];
            }
        }
        long[] cellKeys = Arrays.copyOf(sorted, distinct);
        int[] starts = new int[distinct + 1];
        int[] slotOfMember = new int[count];
        for (int index = 0; index < count; index++) {
            slotOfMember[index] = Arrays.binarySearch(cellKeys, keys[index]);
            starts[slotOfMember[index] + 1]++;
        }
        for (int slot = 0; slot < distinct; slot++) {
            starts[slot + 1] += starts[slot];
        }
        int[] filled = Arrays.copyOf(starts, distinct);
        SampleTrack[] packed = new SampleTrack[count];
        for (int index = 0; index < count; index++) {
            packed[filled[slotOfMember[index]]++] = members.get(index);
        }
        return new WorldCells(world, cellKeys, starts, packed);
    }

    static boolean placeable(PlayerSample sample) {
        return sample != null && sample.world() != null && Double.isFinite(sample.x()) && Double.isFinite(sample.z());
    }

    static boolean samePlace(PlayerSample before, PlayerSample after) {
        boolean placedBefore = placeable(before);
        if (placedBefore != placeable(after)) {
            return false;
        }
        if (!placedBefore) {
            return (before == null) == (after == null)
                    && (before == null || before.entityNumber() == after.entityNumber());
        }
        return before.entityNumber() == after.entityNumber() && Objects.equals(before.world(), after.world())
                && cell(before.x()) == cell(after.x()) && cell(before.z()) == cell(after.z());
    }

    boolean holds(UUID candidate) {
        return world.equals(candidate);
    }

    void visitNear(double x, double z, double radius, SampleVisitor visitor) {
        int lowX = cell(x - radius - EDGE_SLACK);
        int highX = cell(x + radius + EDGE_SLACK);
        int lowZ = cell(z - radius - EDGE_SLACK);
        int highZ = cell(z + radius + EDGE_SLACK);
        for (int cellX = lowX; cellX <= highX; cellX++) {
            for (int cellZ = lowZ; cellZ <= highZ; cellZ++) {
                int slot = Arrays.binarySearch(cellKeys, key(cellX, cellZ));
                if (slot < 0) {
                    continue;
                }
                for (int index = cellStarts[slot]; index < cellStarts[slot + 1]; index++) {
                    PlayerSample sample = packed[index].sample();
                    if (sample != null) {
                        visitor.visit(sample);
                    }
                }
            }
        }
    }

    private static int cell(double coordinate) {
        double cell = Math.floor(coordinate / CELL_BLOCKS);
        return (int) Math.max(LOWEST_CELL, Math.min(HIGHEST_CELL, cell));
    }

    private static long key(int cellX, int cellZ) {
        return ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL);
    }
}
