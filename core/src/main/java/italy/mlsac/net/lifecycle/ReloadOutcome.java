// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.lifecycle;

import java.util.Optional;

public record ReloadOutcome(boolean success, Optional<String> failure) {

    public static ReloadOutcome succeeded() {
        return new ReloadOutcome(true, Optional.empty());
    }

    public static ReloadOutcome failed(String reason) {
        return new ReloadOutcome(false, Optional.of(reason));
    }
}
