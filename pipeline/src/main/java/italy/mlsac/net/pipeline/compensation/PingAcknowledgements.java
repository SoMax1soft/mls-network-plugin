// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import italy.mlsac.net.pipeline.environment.MonotonicClock;
import italy.mlsac.net.pipeline.environment.MonotonicPurpose;
import italy.mlsac.net.pipeline.environment.TransactionPings;
import java.util.UUID;

public final class PingAcknowledgements {
    private static final int BACKLOG_CAP = 40;
    private static final long NOT_MEASURED = -1L;

    private final UUID viewer;
    private final TransactionPings pings;
    private final MonotonicClock clock;
    private final ProbeIdRange ids = new ProbeIdRange();
    private final FlushedProbes flushed = new FlushedProbes();
    private final AcknowledgedWork work = new AcknowledgedWork(flushed::answeredTotal);
    private long lastRoundTripNanos = NOT_MEASURED;

    public PingAcknowledgements(UUID viewer, TransactionPings pings, MonotonicClock clock) {
        this.viewer = viewer;
        this.pings = pings;
        this.clock = clock;
    }

    public static int firstProbeId() {
        return ProbeIdRange.LOWEST;
    }

    public static boolean ownsProbe(int id) {
        return ProbeIdRange.owns(id);
    }

    public void sendProbe() {
        pings.send(viewer, ids.issue());
    }

    public void probeFlushed(int id) {
        if (ids.redeem(id)) {
            flushed.flushed(id, clock.nanos(MonotonicPurpose.TRANSACTION));
        }
    }

    public boolean answered(int id) {
        int position = flushed.positionOf(id);
        if (position < 0) {
            return false;
        }
        long now = clock.nanos(MonotonicPurpose.TRANSACTION);
        long sentAt = flushed.retireThrough(position);
        lastRoundTripNanos = Math.max(0L, now - sentAt);
        work.releaseReached();
        return true;
    }

    public void afterAcknowledged(int probes, Runnable job) {
        work.enqueue(probes, job);
    }

    public int probesFlushed() {
        return flushed.flushedTotal();
    }

    public int probesUnanswered() {
        return flushed.flushedTotal() - flushed.answeredTotal();
    }

    public boolean backlogSmall() {
        return ids.notYetFlushed() + probesUnanswered() <= BACKLOG_CAP;
    }

    public double roundTripMillis() {
        return lastRoundTripNanos == NOT_MEASURED ? -1.0 : lastRoundTripNanos / 1e6;
    }
}
