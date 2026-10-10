// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.legacy;

import java.util.List;

public final class LegacyWindowEncoder {
    private static final int TICK_FIELDS = 6;
    private static final int SEQUENCE_FIELDS = 1;
    private static final int TICK_RECORD_SIZE = 64;

    private LegacyWindowEncoder() {
    }

    public static byte[] encode(List<LegacyDelta> ticks) {
        FlatTableWriter writer = new FlatTableWriter(256 + TICK_RECORD_SIZE * ticks.size());
        int[] tables = new int[ticks.size()];
        for (int index = ticks.size() - 1; index >= 0; index--) {
            writer.startTable(TICK_FIELDS);
            float[] values = ticks.get(index).transmitted();
            for (int field = 0; field < TICK_FIELDS; field++) {
                writer.addFloat(field, values[field]);
            }
            tables[index] = writer.endTable();
        }
        int vector = writer.tableVector(tables);
        writer.startTable(SEQUENCE_FIELDS);
        writer.addOffsetField(0, vector);
        int root = writer.endTable();
        return writer.finish(root);
    }
}
