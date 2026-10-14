// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection;

public enum BadPacketKind {
    FORGED_TELEPORT_CONFIRM("forged-teleport-confirm"),
    TICK_END_SPAM("tick-end-spam");

    private final String slug;

    BadPacketKind(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }
}
