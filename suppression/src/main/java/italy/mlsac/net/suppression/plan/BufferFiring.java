// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

public enum BufferFiring {
    ENABLED,
    DISABLED;

    public static BufferFiring fromSwitch(Boolean bufferEnabled) {
        return Boolean.TRUE.equals(bufferEnabled) ? ENABLED : DISABLED;
    }

    public boolean fires() {
        return this == ENABLED;
    }
}
