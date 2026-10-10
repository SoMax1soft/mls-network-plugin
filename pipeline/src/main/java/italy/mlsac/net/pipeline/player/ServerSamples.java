// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.player;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class ServerSamples {
    private final ConcurrentHashMap<UUID, SampleTrack> tracks = new ConcurrentHashMap<>();
    private final AtomicLong revision = new AtomicLong();
    private final AtomicReference<SampleBoard> board = new AtomicReference<>(SampleBoard.EMPTY);
    private volatile Thread batchingThread;
    private boolean batchMoved;

    public PlayerSample capture(PlayerSample sample) {
        SampleTrack track = tracks.get(sample.player());
        if (track == null) {
            track = tracks.computeIfAbsent(sample.player(), ServerSamples::freshTrack);
        }
        PlayerSample previous = track.replaceSample(sample);
        if (!WorldCells.samePlace(previous, sample)) {
            boardMoved();
        }
        return previous;
    }

    public void beginCaptureBatch() {
        batchingThread = Thread.currentThread();
        batchMoved = false;
    }

    public void endCaptureBatch() {
        batchingThread = null;
        if (batchMoved) {
            batchMoved = false;
            revision.incrementAndGet();
        }
    }

    public void storePing(UUID player, int ping) {
        SampleTrack track = tracks.get(player);
        if (track != null) {
            track.storePing(ping);
        }
    }

    public int serverPing(UUID player, int fallback) {
        SampleTrack track = tracks.get(player);
        return track == null ? fallback : track.pingOr(fallback);
    }

    public PlayerSample current(UUID player) {
        SampleTrack track = tracks.get(player);
        return track == null ? null : track.sample();
    }

    public PositionHistory history(UUID player) {
        SampleTrack track = tracks.get(player);
        return track == null ? null : track.history();
    }

    public SampleBoard board() {
        SampleBoard published = board.get();
        long wanted = revision.get();
        if (published.revision() == wanted) {
            return published;
        }
        SampleBoard built = SampleBoard.of(wanted, new ArrayList<>(tracks.values()));
        SampleBoard winner = board.accumulateAndGet(built,
                (present, offered) -> offered.revision() > present.revision() ? offered : present);
        return winner.revision() >= wanted ? winner : built;
    }

    public void removeSample(UUID player) {
        SampleTrack track = tracks.get(player);
        if (track != null) {
            track.dropSample();
        }
        revision.incrementAndGet();
    }

    public void forget(UUID player) {
        tracks.remove(player);
        revision.incrementAndGet();
    }

    public void clearSamplesAndHistories() {
        for (SampleTrack track : tracks.values()) {
            track.dropSampleAndHistory();
        }
        revision.incrementAndGet();
    }

    private void boardMoved() {
        if (Thread.currentThread() == batchingThread) {
            batchMoved = true;
        } else {
            revision.incrementAndGet();
        }
    }

    private static SampleTrack freshTrack(UUID player) {
        return new SampleTrack();
    }
}
