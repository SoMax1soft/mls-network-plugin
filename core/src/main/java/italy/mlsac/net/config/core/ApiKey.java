// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.core;

import java.util.Locale;
import java.util.Objects;

public record ApiKey(String value) {

    public static final String PLACEHOLDER = "your-api-key";

    public ApiKey {
        Objects.requireNonNull(value, "value");
    }

    public boolean configured() {
        return !value.isBlank() && !PLACEHOLDER.equals(value.trim().toLowerCase(Locale.ROOT));
    }

    public String describe() {
        if (value.isBlank()) {
            return "not set";
        }
        return configured() ? "set" : "default placeholder";
    }

    @Override
    public String toString() {
        return "ApiKey[" + describe() + "]";
    }
}
