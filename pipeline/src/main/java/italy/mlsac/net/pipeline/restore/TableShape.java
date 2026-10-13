// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

import java.util.Arrays;

final class TableShape {
    final FieldKind[] kinds;
    final int[] slots;
    final int integers;
    final int numbers;
    final int singles;
    final int flags;
    final int texts;

    private TableShape(FieldKind[] kinds) {
        this.kinds = kinds;
        this.slots = new int[kinds.length];
        int[] counts = new int[FieldKind.values().length];
        for (int column = 0; column < kinds.length; column++) {
            slots[column] = counts[kinds[column].ordinal()]++;
        }
        this.integers = counts[FieldKind.INTEGER.ordinal()];
        this.numbers = counts[FieldKind.NUMBER.ordinal()];
        this.singles = counts[FieldKind.SINGLE.ordinal()];
        this.flags = counts[FieldKind.FLAG.ordinal()];
        this.texts = counts[FieldKind.TEXT.ordinal()];
    }

    static TableShape of(StagedRow row) {
        return new TableShape(Arrays.copyOf(row.kinds, row.count));
    }

    boolean fits(StagedRow row) {
        if (row.overflow || row.count != kinds.length) {
            return false;
        }
        for (int column = 0; column < kinds.length; column++) {
            if (row.kinds[column] != kinds[column]) {
                return false;
            }
        }
        return true;
    }

    int flagWords() {
        return (flags + Long.SIZE - 1) / Long.SIZE;
    }

    int columns() {
        return kinds.length;
    }
}
