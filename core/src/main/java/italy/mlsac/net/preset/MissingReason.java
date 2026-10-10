// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.preset;

import java.util.Optional;

public enum MissingReason {
    NO_PRESET("preset-missing-none"),
    MULTIPLE_PRESETS("preset-missing-multiple"),
    PRESET_NOT_FOUND("preset-missing-not-found"),
    PRESET_INVALID("preset-missing-not-found"),
    NO_OWNER("preset-missing-none"),
    NOTHING_USABLE("preset-missing-none"),
    DISABLED("preset-missing-disabled"),
    NO_ENDPOINT("");

    public static final String HEADLINE_KEY = "preset-missing";

    private final String detailKey;

    MissingReason(String detailKey) {
        this.detailKey = detailKey;
    }

    public static MissingReason fromBackend(String raw) {
        if (raw == null || raw.isEmpty()) {
            return NOTHING_USABLE;
        }
        for (MissingReason reason : values()) {
            if (reason.name().equals(raw)) {
                return reason;
            }
        }
        return NO_PRESET;
    }

    public Optional<String> detailKey() {
        return detailKey.isEmpty() ? Optional.empty() : Optional.of(detailKey);
    }

    public boolean notifies() {
        return this != NO_ENDPOINT;
    }
}
