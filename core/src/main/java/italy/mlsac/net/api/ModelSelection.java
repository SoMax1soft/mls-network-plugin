// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.api;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ModelSelection(UUID player, ModelChoice requested, ModelChoice effective,
                             Optional<FallbackReason> fallback) {

    public ModelSelection {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(effective, "effective");
        Objects.requireNonNull(fallback, "fallback");
    }

    public static ModelSelection serverDefault(UUID player) {
        return new ModelSelection(player, ModelChoice.DEFAULT, ModelChoice.DEFAULT, Optional.empty());
    }

    public static ModelSelection granted(UUID player, ModelChoice choice) {
        return new ModelSelection(player, choice, choice, Optional.empty());
    }

    public static ModelSelection fellBack(UUID player, ModelChoice requested, FallbackReason reason) {
        return new ModelSelection(player, requested, ModelChoice.DEFAULT, Optional.of(reason));
    }

    public boolean overridden() {
        return effective != ModelChoice.DEFAULT;
    }

    public boolean fellBack() {
        return fallback.isPresent();
    }
}
