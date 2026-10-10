// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

public enum Billboard {
    CENTER((byte) 3),
    VERTICAL((byte) 1);

    private final byte metadataValue;

    Billboard(byte metadataValue) {
        this.metadataValue = metadataValue;
    }

    public byte metadataValue() {
        return metadataValue;
    }
}
