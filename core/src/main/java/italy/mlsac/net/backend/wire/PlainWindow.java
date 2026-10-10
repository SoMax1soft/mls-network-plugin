// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import java.util.Optional;

public final class PlainWindow {

    public static final int TICKS = UltraPayload.TICKS;
    public static final int FEATURES = UltraPayload.NARROW_FEATURES;
    public static final int FLAGS = UltraPayload.NARROW_FLAGS;
    public static final int WIDTH = UltraPayload.NARROW_WIDTH;
    public static final int WIDE_WIDTH = UltraPayload.WIDE_WIDTH;

    private final double[][] rows;

    public PlainWindow(double[][] rows) {
        if (rows.length != TICKS) {
            throw new IllegalArgumentException("a window holds " + TICKS + " rows, not " + rows.length);
        }
        int width = rows[0].length;
        if (width != WIDTH && width != WIDE_WIDTH) {
            throw new IllegalArgumentException("a plain row holds " + WIDTH + " or " + WIDE_WIDTH + " values, not "
                    + width);
        }
        double[][] copy = new double[TICKS][];
        for (int tick = 0; tick < TICKS; tick++) {
            if (rows[tick].length != width) {
                throw new IllegalArgumentException("row " + tick + " holds " + rows[tick].length + " values, not "
                        + width);
            }
            copy[tick] = rows[tick].clone();
        }
        this.rows = copy;
    }

    public static Optional<PlainWindow> fromUltraPayload(byte[] payload) {
        return fromUltraPayload(payload, WIDTH);
    }

    public static Optional<PlainWindow> fromUltraPayload(byte[] payload, int width) {
        return UltraPayload.read(payload)
                .filter(read -> width == WIDTH || read.wide())
                .map(read -> new PlainWindow(read.plainRows(width)));
    }

    public int width() {
        return rows[0].length;
    }

    public double[][] rows() {
        double[][] copy = new double[TICKS][];
        for (int tick = 0; tick < TICKS; tick++) {
            copy[tick] = rows[tick].clone();
        }
        return copy;
    }
}
