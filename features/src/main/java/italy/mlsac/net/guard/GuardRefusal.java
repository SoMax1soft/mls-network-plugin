// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.Objects;

public record GuardRefusal(GuardReason reason, String detail) {

    public GuardRefusal {
        Objects.requireNonNull(reason, "reason");
        detail = detail == null ? "" : detail;
    }

    public static GuardRefusal of(GuardReason reason) {
        return new GuardRefusal(reason, "");
    }

    public String english() {
        return reason.english().replace(GuardReason.DETAIL, DisplayText.shortened(detail));
    }
}
