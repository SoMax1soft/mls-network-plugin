// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.player;

import italy.mlsac.net.pipeline.geometry.BoxSpan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class PositionHistory {
    private static final int KEPT_SAMPLES = 40;
    private static final double JUMP_LIMIT_SQUARED = 16.0;
    private static final double MIDPOINT = 0.5;

    private final PlayerSample[] ring = new PlayerSample[KEPT_SAMPLES];
    private int oldestSlot;
    private int filled;

    public synchronized void append(PlayerSample sample) {
        if (filled > 0 && sample.capturedAt() <= nth(filled - 1).capturedAt()) {
            return;
        }
        if (filled == KEPT_SAMPLES) {
            ring[oldestSlot] = sample;
            oldestSlot = (oldestSlot + 1) % KEPT_SAMPLES;
            return;
        }
        ring[(oldestSlot + filled) % KEPT_SAMPLES] = sample;
        filled++;
    }

    synchronized void clear() {
        Arrays.fill(ring, null);
        oldestSlot = 0;
        filled = 0;
    }

    public synchronized Optional<PlayerSample> at(long nanos) {
        if (filled == 0 || nanos < nth(0).capturedAt()) {
            return Optional.empty();
        }
        int floor = lastNotAfter(nanos);
        PlayerSample before = nth(floor);
        if (floor == filled - 1 || before.capturedAt() == nanos) {
            return Optional.of(before);
        }
        PlayerSample after = nth(floor + 1);
        double fraction = (double) (nanos - before.capturedAt()) / (double) (after.capturedAt() - before.capturedAt());
        PlayerSample nearer = fraction < MIDPOINT ? before : after;
        if (!bridgeable(before, after)) {
            return Optional.of(nearer);
        }
        return Optional.of(nearer.interpolatedAt(nanos,
                before.x() + (after.x() - before.x()) * fraction,
                before.y() + (after.y() - before.y()) * fraction,
                before.z() + (after.z() - before.z()) * fraction));
    }

    public synchronized List<PlayerSample> between(long lowerExclusive, long upperExclusive) {
        List<PlayerSample> inside = new ArrayList<>();
        for (int position = 0; position < filled; position++) {
            long captured = nth(position).capturedAt();
            if (captured > lowerExclusive && captured < upperExclusive) {
                inside.add(nth(position));
            }
        }
        return inside;
    }

    public synchronized void spanRun(long lowerExclusive, long upperExclusive, PlayerSample anchor, BoxSpan span) {
        for (int position = 0; position < filled; position++) {
            PlayerSample sample = nth(position);
            long captured = sample.capturedAt();
            if (captured > lowerExclusive && captured < upperExclusive && oneUnbrokenRun(sample, anchor)) {
                span.includePlayer(sample.x(), sample.y(), sample.z(), sample.width(), sample.height());
            }
        }
    }

    public static boolean oneUnbrokenRun(PlayerSample candidate, PlayerSample anchor) {
        return candidate.alive() && closeInSameWorld(candidate, anchor);
    }

    private static boolean bridgeable(PlayerSample before, PlayerSample after) {
        return before.alive() && after.alive() && closeInSameWorld(before, after);
    }

    private static boolean closeInSameWorld(PlayerSample first, PlayerSample second) {
        return first.world().equals(second.world())
                && first.feet().squaredDistanceTo(second.feet()) <= JUMP_LIMIT_SQUARED;
    }

    private int lastNotAfter(long nanos) {
        int low = 0;
        int high = filled - 1;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (nth(middle).capturedAt() <= nanos) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    private PlayerSample nth(int position) {
        return ring[(oldestSlot + position) % KEPT_SAMPLES];
    }
}
