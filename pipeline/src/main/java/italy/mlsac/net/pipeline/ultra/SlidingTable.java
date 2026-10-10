// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

final class SlidingTable {
    private final int capacity;
    private final PresentCells[] columns;
    private long nextRow;
    private int stored;

    SlidingTable(int capacity, int columnCount) {
        this.capacity = capacity;
        this.columns = new PresentCells[columnCount];
        for (int column = 0; column < columnCount; column++) {
            columns[column] = new PresentCells(capacity);
        }
    }

    void append(double only) {
        long row = admitRow();
        columns[0].offer(row, only);
    }

    void append(double first, double second) {
        long row = admitRow();
        columns[0].offer(row, first);
        columns[1].offer(row, second);
    }

    void append(double[] cells) {
        long row = admitRow();
        for (int column = 0; column < columns.length; column++) {
            columns[column].offer(row, cells[column]);
        }
    }

    void empty() {
        stored = 0;
        for (PresentCells column : columns) {
            column.clear();
        }
    }

    double average(int column, int quorum) {
        return columns[column].average(quorum);
    }

    private long admitRow() {
        long row = nextRow++;
        if (stored == capacity) {
            long leaving = row - capacity;
            for (PresentCells column : columns) {
                column.release(leaving);
            }
        } else {
            stored++;
        }
        return row;
    }

    private static final class PresentCells {
        private final long[] rows;
        private final double[] values;
        private int head;
        private int size;

        PresentCells(int capacity) {
            this.rows = new long[capacity];
            this.values = new double[capacity];
        }

        void offer(long row, double value) {
            if (Double.isNaN(value)) {
                return;
            }
            int slot = (head + size) % rows.length;
            rows[slot] = row;
            values[slot] = value;
            size++;
        }

        void release(long row) {
            if (size > 0 && rows[head] == row) {
                head = (head + 1) % rows.length;
                size--;
            }
        }

        void clear() {
            head = 0;
            size = 0;
        }

        double average(int quorum) {
            if (size < quorum) {
                return Double.NaN;
            }
            double total = 0.0;
            int firstEnd = Math.min(values.length, head + size);
            for (int slot = head; slot < firstEnd; slot++) {
                total += values[slot];
            }
            int wrapped = size - (firstEnd - head);
            for (int slot = 0; slot < wrapped; slot++) {
                total += values[slot];
            }
            return total / size;
        }
    }
}
