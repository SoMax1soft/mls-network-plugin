// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

enum RowOrigin {
    MOVEMENT_PACKET(1, false),
    TICK_END(2, true);

    private final int rotationSource;
    private final boolean noPacket;

    RowOrigin(int rotationSource, boolean noPacket) {
        this.rotationSource = rotationSource;
        this.noPacket = noPacket;
    }

    int rotationSource() {
        return rotationSource;
    }

    boolean noPacket() {
        return noPacket;
    }
}
