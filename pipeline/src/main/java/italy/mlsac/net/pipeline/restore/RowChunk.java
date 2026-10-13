// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

import italy.mlsac.net.pipeline.tables.RowFields;

final class RowChunk {
    private static final long ARRAY_HEADER = 16;
    private static final long OBJECT_HEADER = 16;
    private static final long REFERENCE = 8;

    private final long[] integers;
    private final double[] numbers;
    private final float[] singles;
    private final long[] flagWords;
    private final String[] texts;
    private final long[] stamps;

    RowChunk(TableShape shape, int rows) {
        this.integers = new long[rows * shape.integers];
        this.numbers = new double[rows * shape.numbers];
        this.singles = new float[rows * shape.singles];
        this.flagWords = new long[rows * shape.flagWords()];
        this.texts = new String[rows * shape.texts];
        this.stamps = new long[rows];
    }

    static long bytesFor(TableShape shape, int rows) {
        long perRow = Long.BYTES * (long) shape.integers + Double.BYTES * (long) shape.numbers
                + Float.BYTES * (long) shape.singles + Long.BYTES * (long) shape.flagWords()
                + REFERENCE * shape.texts + Long.BYTES;
        return OBJECT_HEADER + 6 * (ARRAY_HEADER + REFERENCE) + perRow * rows;
    }

    void store(int row, TableShape shape, StagedRow staged, long stamp) {
        int words = shape.flagWords();
        for (int word = 0; word < words; word++) {
            flagWords[row * words + word] = 0L;
        }
        for (int column = 0; column < shape.columns(); column++) {
            int slot = shape.slots[column];
            long bits = staged.bits[column];
            switch (shape.kinds[column]) {
                case INTEGER -> integers[row * shape.integers + slot] = bits;
                case NUMBER -> numbers[row * shape.numbers + slot] = Double.longBitsToDouble(bits);
                case SINGLE -> singles[row * shape.singles + slot] = Float.intBitsToFloat((int) bits);
                case FLAG -> {
                    if (bits != 0L) {
                        flagWords[row * words + slot / Long.SIZE] |= 1L << (slot % Long.SIZE);
                    }
                }
                case TEXT -> texts[row * shape.texts + slot] = staged.texts[column];
            }
        }
        stamps[row] = stamp;
    }

    void replay(int row, TableShape shape, RowFields out) {
        int words = shape.flagWords();
        for (int column = 0; column < shape.columns(); column++) {
            int slot = shape.slots[column];
            switch (shape.kinds[column]) {
                case INTEGER -> out.integer(integers[row * shape.integers + slot]);
                case NUMBER -> out.number(numbers[row * shape.numbers + slot]);
                case SINGLE -> out.single(singles[row * shape.singles + slot]);
                case FLAG -> out.flag((flagWords[row * words + slot / Long.SIZE] & (1L << (slot % Long.SIZE))) != 0L);
                case TEXT -> out.text(texts[row * shape.texts + slot]);
            }
        }
    }

    long stamp(int row) {
        return stamps[row];
    }
}
