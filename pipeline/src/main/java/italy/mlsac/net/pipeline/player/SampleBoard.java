// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class SampleBoard {
    static final SampleBoard EMPTY = new SampleBoard(Long.MIN_VALUE, new SampleTrack[0], new long[0],
            new WorldCells[0]);

    private static final long INDEX_MASK = 0xFFFFFFFFL;

    private final long revision;
    private final SampleTrack[] members;
    private final long[] entityIndex;
    private final WorldCells[] worlds;

    private SampleBoard(long revision, SampleTrack[] members, long[] entityIndex, WorldCells[] worlds) {
        this.revision = revision;
        this.members = members;
        this.entityIndex = entityIndex;
        this.worlds = worlds;
    }

    static SampleBoard of(long revision, List<SampleTrack> tracks) {
        List<SampleTrack> present = new ArrayList<>(tracks.size());
        List<PlayerSample> placedAs = new ArrayList<>(tracks.size());
        for (SampleTrack track : tracks) {
            PlayerSample sample = track.sample();
            if (sample != null) {
                present.add(track);
                placedAs.add(sample);
            }
        }
        SampleTrack[] members = present.toArray(new SampleTrack[0]);
        long[] entityIndex = new long[members.length];
        for (int index = 0; index < members.length; index++) {
            entityIndex[index] = ((long) placedAs.get(index).entityNumber() << 32) | index;
        }
        Arrays.sort(entityIndex);
        return new SampleBoard(revision, members, entityIndex, worldsOf(present, placedAs));
    }

    long revision() {
        return revision;
    }

    public List<PlayerSample> everyone() {
        List<PlayerSample> everyone = new ArrayList<>(members.length);
        for (SampleTrack member : members) {
            PlayerSample sample = member.sample();
            if (sample != null) {
                everyone.add(sample);
            }
        }
        return everyone;
    }

    public PlayerSample byEntity(int entityNumber) {
        int low = 0;
        int high = entityIndex.length - 1;
        int found = -1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int number = (int) (entityIndex[middle] >> 32);
            if (number < entityNumber) {
                low = middle + 1;
            } else {
                if (number == entityNumber) {
                    found = middle;
                }
                high = middle - 1;
            }
        }
        if (found < 0) {
            return null;
        }
        for (int position = found; position < entityIndex.length
                && (int) (entityIndex[position] >> 32) == entityNumber; position++) {
            PlayerSample sample = members[(int) (entityIndex[position] & INDEX_MASK)].sample();
            if (sample != null && sample.entityNumber() == entityNumber) {
                return sample;
            }
        }
        return null;
    }

    public void visitNear(UUID world, double x, double z, double radius, SampleVisitor visitor) {
        for (WorldCells cells : worlds) {
            if (cells.holds(world)) {
                cells.visitNear(x, z, radius, visitor);
                return;
            }
        }
    }

    private static WorldCells[] worldsOf(List<SampleTrack> present, List<PlayerSample> placedAs) {
        List<UUID> worldIds = new ArrayList<>();
        List<List<SampleTrack>> groupedTracks = new ArrayList<>();
        List<List<PlayerSample>> groupedPlaces = new ArrayList<>();
        for (int index = 0; index < present.size(); index++) {
            PlayerSample placed = placedAs.get(index);
            if (!WorldCells.placeable(placed)) {
                continue;
            }
            int slot = worldIds.indexOf(placed.world());
            if (slot < 0) {
                worldIds.add(placed.world());
                groupedTracks.add(new ArrayList<>());
                groupedPlaces.add(new ArrayList<>());
                slot = worldIds.size() - 1;
            }
            groupedTracks.get(slot).add(present.get(index));
            groupedPlaces.get(slot).add(placed);
        }
        WorldCells[] worlds = new WorldCells[worldIds.size()];
        for (int slot = 0; slot < worlds.length; slot++) {
            worlds[slot] = WorldCells.of(worldIds.get(slot), groupedTracks.get(slot), groupedPlaces.get(slot));
        }
        return worlds;
    }
}
