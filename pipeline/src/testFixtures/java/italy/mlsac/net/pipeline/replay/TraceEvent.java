// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import java.util.ArrayList;
import java.util.List;

final class TraceEvent {
    private final TraceRecord head;
    private final List<TraceRecord> records;
    private final List<TraceRecord> ownReads = new ArrayList<>();
    private final List<TraceEvent> nested = new ArrayList<>();

    TraceEvent(TraceRecord head, List<TraceRecord> records) {
        this.head = head;
        this.records = records;
        int depth = head.depth();
        int index = 0;
        while (index < records.size()) {
            TraceRecord record = records.get(index);
            if (record.isEvent() && record.depth() == depth + 1) {
                List<TraceRecord> inner = new ArrayList<>();
                index++;
                while (index < records.size() && records.get(index).depth() > depth
                        && !(records.get(index).isEvent() && records.get(index).depth() == depth + 1)) {
                    inner.add(records.get(index));
                    index++;
                }
                nested.add(new TraceEvent(record, inner));
                continue;
            }
            if (record.isRead() && record.depth() == depth) {
                ownReads.add(record);
            }
            index++;
        }
    }

    TraceRecord head() {
        return head;
    }

    String name() {
        return head.name();
    }

    List<TraceRecord> records() {
        return records;
    }

    List<TraceRecord> ownReads() {
        return ownReads;
    }

    List<TraceEvent> nested() {
        return nested;
    }

    List<TraceRecord> outputs() {
        List<TraceRecord> outputs = new ArrayList<>();
        for (TraceRecord record : records) {
            if (record.isOutput()) {
                outputs.add(record);
            }
        }
        return outputs;
    }

    List<TraceRecord> allReads() {
        List<TraceRecord> reads = new ArrayList<>();
        for (TraceRecord record : records) {
            if (record.isRead()) {
                reads.add(record);
            }
        }
        return reads;
    }
}
