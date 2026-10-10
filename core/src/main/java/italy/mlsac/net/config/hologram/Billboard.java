// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.hologram;

import java.util.Locale;
import java.util.Optional;

public enum Billboard {
    CENTER((byte) 3),
    VERTICAL((byte) 1);

    private final byte metadataValue;

    Billboard(byte metadataValue) {
        this.metadataValue = metadataValue;
    }

    public static Optional<Billboard> fromId(String raw) {
        String id = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        for (Billboard billboard : values()) {
            if (billboard.name().equals(id)) {
                return Optional.of(billboard);
            }
        }
        return Optional.empty();
    }

    public byte metadataValue() {
        return metadataValue;
    }
}
