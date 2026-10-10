// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import java.nio.ByteBuffer;

final class WindowComparison {
    private static final int HEADER = 20;
    private final ParityReport report;

    WindowComparison(ParityReport report) {
        this.report = report;
    }

    void compare(byte[] expected, byte[] actual) {
        if (expected.length != actual.length) {
            report.windowFeatureMismatches.merge("length", 1L, Long::sum);
            return;
        }
        ByteBuffer left = ByteBuffer.wrap(expected);
        ByteBuffer right = ByteBuffer.wrap(actual);
        int sequence = left.getInt(8);
        int features = left.getInt(12);
        int flags = left.getInt(16);
        for (int row = 0; row < sequence; row++) {
            for (int column = 0; column < features; column++) {
                int offset = HEADER + 4 * (row * features + column);
                if (left.getInt(offset) != right.getInt(offset)) {
                    report.windowFeatureMismatches.merge("f" + column, 1L, Long::sum);
                }
            }
            for (int column = 0; column < flags; column++) {
                int offset = HEADER + 4 * (sequence * features + row * flags + column);
                if (left.getInt(offset) != right.getInt(offset)) {
                    report.windowFeatureMismatches.merge("g" + column, 1L, Long::sum);
                }
            }
        }
    }
}
