// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

import italy.mlsac.net.pipeline.tables.RowFields;

final class StagedRow implements RowFields {
    static final int MAX_FIELDS = 128;

    final FieldKind[] kinds = new FieldKind[MAX_FIELDS];
    final long[] bits = new long[MAX_FIELDS];
    final String[] texts = new String[MAX_FIELDS];
    int count;
    boolean overflow;

    void reset() {
        for (int index = 0; index < count; index++) {
            texts[index] = null;
        }
        count = 0;
        overflow = false;
    }

    @Override
    public RowFields integer(long value) {
        return put(FieldKind.INTEGER, value, null);
    }

    @Override
    public RowFields number(double value) {
        return put(FieldKind.NUMBER, Double.doubleToRawLongBits(value), null);
    }

    @Override
    public RowFields single(float value) {
        return put(FieldKind.SINGLE, Float.floatToRawIntBits(value), null);
    }

    @Override
    public RowFields flag(boolean value) {
        return put(FieldKind.FLAG, value ? 1L : 0L, null);
    }

    @Override
    public RowFields text(String value) {
        return put(FieldKind.TEXT, 0L, value);
    }

    private RowFields put(FieldKind kind, long value, String text) {
        if (count == MAX_FIELDS) {
            overflow = true;
            return this;
        }
        kinds[count] = kind;
        bits[count] = value;
        texts[count] = text;
        count++;
        return this;
    }
}
