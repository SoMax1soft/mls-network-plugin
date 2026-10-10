// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class MlsuWire {
    public static final int PREAMBLE_SIZE = 20;
    public static final int MAXIMUM_BYTES = 256 * 1024;
    private static final byte[] MAGIC = "MLSU".getBytes(StandardCharsets.US_ASCII);

    private MlsuWire() {
    }

    public static byte[] encode(WireVersion version, WindowMatrix matrix) {
        int sequence = matrix.length();
        ByteBuffer buffer = ByteBuffer.allocate(version.byteLength(sequence)).order(ByteOrder.BIG_ENDIAN);
        buffer.put(MAGIC).putInt(version.number()).putInt(sequence).putInt(version.featureCount())
                .putInt(version.flagCount());
        for (float[] row : matrix.features()) {
            requireWidth(row, version.featureCount());
            for (float value : row) {
                buffer.putFloat(value);
            }
        }
        for (float[] row : matrix.flags()) {
            requireWidth(row, version.flagCount());
            for (float value : row) {
                buffer.putFloat(value);
            }
        }
        return buffer.array();
    }

    public static MlsuHeader header(byte[] payload) {
        if (payload.length < PREAMBLE_SIZE) {
            throw new IllegalArgumentException("payload shorter than the MLSU header");
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);
        byte[] magic = new byte[4];
        buffer.get(magic);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new IllegalArgumentException("payload is not MLSU");
        }
        return new MlsuHeader(buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt(), payload.length);
    }

    public static boolean recognises(byte[] payload) {
        return payload.length >= MAGIC.length && payload[0] == MAGIC[0] && payload[1] == MAGIC[1]
                && payload[2] == MAGIC[2] && payload[3] == MAGIC[3];
    }

    private static void requireWidth(float[] row, int width) {
        if (row.length != width) {
            throw new IllegalArgumentException("row width " + row.length + " does not match " + width);
        }
    }

    public record MlsuHeader(int version, int sequenceLength, int featureCount, int flagCount, int totalLength) {
    }
}
