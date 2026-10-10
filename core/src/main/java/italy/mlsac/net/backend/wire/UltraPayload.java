// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import italy.mlsac.net.backend.request.WindowSubmission;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

public final class UltraPayload {

    public static final int TICKS = 60;
    public static final int NARROW_FEATURES = 50;
    public static final int NARROW_FLAGS = 14;
    public static final int WIDE_FEATURES = 54;
    public static final int WIDE_FLAGS = 18;
    public static final int NARROW_WIDTH = NARROW_FEATURES + NARROW_FLAGS;
    public static final int WIDE_WIDTH = WIDE_FEATURES + WIDE_FLAGS;
    private static final int MAGIC = 0x4D4C5355;
    private static final int NARROW_LAYOUT = 4;
    private static final int WIDE_LAYOUT = 6;
    private static final int HEADER_BYTES = 20;

    private final ByteBuffer body;
    private final int features;
    private final int flags;

    private UltraPayload(ByteBuffer body, int features, int flags) {
        this.body = body;
        this.features = features;
        this.flags = flags;
    }

    public static Optional<UltraPayload> read(byte[] payload) {
        if (payload.length < HEADER_BYTES) {
            return Optional.empty();
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);
        if (buffer.getInt() != MAGIC) {
            return Optional.empty();
        }
        int layout = buffer.getInt();
        int ticks = buffer.getInt();
        int featureCount = buffer.getInt();
        int flagCount = buffer.getInt();
        boolean narrow = layout == NARROW_LAYOUT && featureCount == NARROW_FEATURES && flagCount == NARROW_FLAGS;
        boolean wide = layout == WIDE_LAYOUT && featureCount == WIDE_FEATURES && flagCount == WIDE_FLAGS;
        if (ticks != TICKS || !(narrow || wide)
                || payload.length != HEADER_BYTES + Float.BYTES * TICKS * (featureCount + flagCount)) {
            return Optional.empty();
        }
        return Optional.of(new UltraPayload(buffer, featureCount, flagCount));
    }

    public static WindowSubmission narrowed(WindowSubmission submission) {
        Optional<UltraPayload> payload = read(submission.window()).filter(UltraPayload::wide);
        if (payload.isEmpty()) {
            return submission;
        }
        return WindowSubmission.handedOver(submission.player(), submission.playerNick(), payload.get().layoutFour());
    }

    public boolean wide() {
        return features == WIDE_FEATURES;
    }

    public byte[] layoutFour() {
        ByteBuffer out = ByteBuffer.allocate(HEADER_BYTES + Float.BYTES * TICKS * NARROW_WIDTH)
                .order(ByteOrder.BIG_ENDIAN);
        out.putInt(MAGIC).putInt(NARROW_LAYOUT).putInt(TICKS).putInt(NARROW_FEATURES).putInt(NARROW_FLAGS);
        for (int tick = 0; tick < TICKS; tick++) {
            for (int column = 0; column < NARROW_FEATURES; column++) {
                out.putInt(body.getInt(featureAt(tick, column)));
            }
        }
        for (int tick = 0; tick < TICKS; tick++) {
            for (int column = 0; column < NARROW_FLAGS; column++) {
                out.putInt(body.getInt(flagAt(tick, column)));
            }
        }
        return out.array();
    }

    public double[][] plainRows(int width) {
        if (width != NARROW_WIDTH && !(width == WIDE_WIDTH && wide())) {
            throw new IllegalArgumentException("a layout " + (wide() ? WIDE_LAYOUT : NARROW_LAYOUT)
                    + " window has no plain rows of " + width + " values");
        }
        double[][] rows = new double[TICKS][width];
        for (int tick = 0; tick < TICKS; tick++) {
            double[] row = rows[tick];
            for (int column = 0; column < NARROW_FEATURES; column++) {
                row[column] = written(featureAt(tick, column));
            }
            for (int column = 0; column < NARROW_FLAGS; column++) {
                row[NARROW_FEATURES + column] = written(flagAt(tick, column));
            }
            if (width == WIDE_WIDTH) {
                int extraFeatures = WIDE_FEATURES - NARROW_FEATURES;
                for (int column = 0; column < extraFeatures; column++) {
                    row[NARROW_WIDTH + column] = written(featureAt(tick, NARROW_FEATURES + column));
                }
                for (int column = 0; column < WIDE_FLAGS - NARROW_FLAGS; column++) {
                    row[NARROW_WIDTH + extraFeatures + column] = written(flagAt(tick, NARROW_FLAGS + column));
                }
            }
        }
        return rows;
    }

    private double written(int offset) {
        return ShortestFloat.asWritten(body.getFloat(offset));
    }

    private int featureAt(int tick, int column) {
        return HEADER_BYTES + Float.BYTES * (tick * features + column);
    }

    private int flagAt(int tick, int column) {
        return HEADER_BYTES + Float.BYTES * (TICKS * features + tick * flags + column);
    }
}
