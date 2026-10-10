// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.workload;

import java.util.concurrent.atomic.LongAdder;

public final class WorkloadCounters {

    private final LongAdder hologramNanos = new LongAdder();
    private final LongAdder hologramPackets = new LongAdder();
    private final LongAdder hologramTextBuilds = new LongAdder();
    private final LongAdder pipelineNanos = new LongAdder();
    private final LongAdder pipelineEvents = new LongAdder();
    private final LongAdder payloadNanos = new LongAdder();
    private final LongAdder payloadBuilds = new LongAdder();
    private final LongAdder payloadBytes = new LongAdder();
    private final LongAdder responseNanos = new LongAdder();
    private final LongAdder responses = new LongAdder();
    private final LongAdder virtualStepAlerts = new LongAdder();
    private final LongAdder virtualFlags = new LongAdder();
    private final LongAdder virtualSuppressedPunishments = new LongAdder();

    public void pipelineWork(long nanos, long events) {
        pipelineNanos.add(nanos);
        pipelineEvents.add(events);
    }

    public void payloadBuilt(long nanos, int bytes) {
        payloadNanos.add(nanos);
        payloadBuilds.increment();
        payloadBytes.add(bytes);
    }

    public void responseHandled(long nanos) {
        responseNanos.add(nanos);
        responses.increment();
        pipelineNanos.add(nanos);
        pipelineEvents.increment();
    }

    public void hologramWork(long nanos) {
        hologramNanos.add(nanos);
    }

    public void hologramPacketsWritten(int packets) {
        hologramPackets.add(packets);
    }

    public void hologramTextBuilt() {
        hologramTextBuilds.increment();
    }

    public void virtualStepAlert() {
        virtualStepAlerts.increment();
    }

    public void virtualFlag() {
        virtualFlags.increment();
    }

    public void virtualPunishmentsSuppressed(int commands) {
        virtualSuppressedPunishments.add(commands);
    }

    public CounterReading reading() {
        return new CounterReading(hologramNanos.sum(), hologramPackets.sum(), hologramTextBuilds.sum(),
                pipelineNanos.sum(), pipelineEvents.sum(), payloadNanos.sum(), payloadBuilds.sum(),
                payloadBytes.sum(), responseNanos.sum(), responses.sum(), virtualStepAlerts.sum(),
                virtualFlags.sum(), virtualSuppressedPunishments.sum());
    }
}
