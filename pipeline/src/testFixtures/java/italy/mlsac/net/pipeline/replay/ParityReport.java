// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ParityReport {
    final String trace;
    final Map<String, KindTally> kinds = new LinkedHashMap<>();
    final List<String> files = new ArrayList<>();
    final List<String> notes = new ArrayList<>();
    final Map<String, Long> windowFeatureMismatches = new LinkedHashMap<>();
    long predictBodies;
    long predictBodiesMatched;
    long predictPayloadsMatched;
    long predictPayloadsEmitted;
    ReplayMisses misses;
    long orderedEvents;
    long orderedEventsMatching;
    long firstOrderDifference = -1;
    long cutAt = -1;

    boolean complete() {
        for (KindTally tally : kinds.values()) {
            if (tally.matched != tally.expected || tally.produced != tally.expected) {
                return false;
            }
        }
        return orderedEvents == orderedEventsMatching && (misses == null || misses.counts().isEmpty())
                && predictBodies == predictBodiesMatched && predictBodies == predictPayloadsEmitted;
    }

    ParityReport(String trace) {
        this.trace = trace;
    }

    KindTally kind(String name) {
        return kinds.computeIfAbsent(name, ignored -> new KindTally());
    }

    String render() {
        StringBuilder text = new StringBuilder();
        text.append("== ").append(trace).append('\n');
        if (cutAt >= 0) {
            text.append("replay stopped at s=").append(cutAt).append(" (a recorder command this edition cannot run)\n");
        }
        for (Map.Entry<String, KindTally> entry : kinds.entrySet()) {
            KindTally tally = entry.getValue();
            text.append(String.format("%-14s expected %6d produced %6d identical %6d", entry.getKey(),
                    tally.expected, tally.produced, tally.matched));
            if (tally.firstMismatch >= 0) {
                text.append("  first difference s=").append(tally.firstMismatch).append(' ')
                        .append(tally.firstMismatchDetail);
            }
            text.append('\n');
        }
        text.append(String.format("events with outputs %d, same output order and identities %d%s%n", orderedEvents,
                orderedEventsMatching, firstOrderDifference < 0 ? "" : " first difference s=" + firstOrderDifference));
        if (predictBodies > 0) {
            text.append(String.format("predict bodies %d, playerData = last window %d, playerData = an emitted window "
                    + "of that player %d, whole body identical %d%n", predictBodies, predictPayloadsMatched,
                    predictPayloadsEmitted, predictBodiesMatched));
        }
        if (!windowFeatureMismatches.isEmpty()) {
            text.append("window value differences by column: ").append(windowFeatureMismatches).append('\n');
        }
        for (String file : files) {
            text.append(file).append('\n');
        }
        if (misses != null) {
            for (Map.Entry<String, Integer> entry : misses.counts().entrySet()) {
                text.append("replay miss: ").append(entry.getKey()).append(" x").append(entry.getValue())
                        .append(' ').append(misses.examples().get(entry.getKey())).append('\n');
            }
        }
        for (String note : notes) {
            text.append(note).append('\n');
        }
        return text.toString();
    }
}
