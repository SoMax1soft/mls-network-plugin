// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import java.util.List;

public record WireVersion(int number, int featureCount, int flagCount) {

    public static final WireVersion V1 = new WireVersion(1, 48, 4);
    public static final WireVersion V2 = new WireVersion(2, 39, 7);
    public static final WireVersion V3 = new WireVersion(3, 44, 8);
    public static final WireVersion V4 = new WireVersion(4, 50, 14);
    public static final WireVersion V6 = new WireVersion(6, 54, 18);

    private static final List<WireVersion> KNOWN = List.of(V1, V2, V3, V4, V6);

    public static WireVersion of(int number) {
        for (WireVersion version : KNOWN) {
            if (version.number() == number) {
                return version;
            }
        }
        throw new IllegalArgumentException("unknown MLSU wire version " + number);
    }

    public int byteLength(int sequenceLength) {
        int floatsPerRow = featureCount + flagCount;
        return MlsuWire.PREAMBLE_SIZE + Float.BYTES * floatsPerRow * sequenceLength;
    }
}
