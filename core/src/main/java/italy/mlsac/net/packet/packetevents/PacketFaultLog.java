// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class PacketFaultLog {

    private static final long WINDOW_NANOS = 60_000_000_000L;

    private final PluginLog log;
    private final Map<Class<?>, Long> lastLogged = new ConcurrentHashMap<>();
    private final AtomicLong suppressed = new AtomicLong();

    PacketFaultLog(PluginLog log) {
        this.log = log;
    }

    void record(String packet, Throwable fault) {
        if (firstInWindow(fault)) {
            log.warn(LogTopic.PACKETS, "Packet handler failed for " + packet + ": " + fault, fault);
        }
    }

    void recordNetworkFault(String what, Throwable fault) {
        if (firstInWindow(fault)) {
            log.warn(LogTopic.PACKETS, "Network work failed during " + what + ": " + fault, fault);
        }
    }

    private boolean firstInWindow(Throwable fault) {
        long now = System.nanoTime();
        Long last = lastLogged.get(fault.getClass());
        if (last != null && now - last < WINDOW_NANOS) {
            suppressed.incrementAndGet();
            return false;
        }
        lastLogged.put(fault.getClass(), now);
        return true;
    }

    long suppressedCount() {
        return suppressed.get();
    }
}
