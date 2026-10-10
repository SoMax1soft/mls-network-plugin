// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.response;

import java.util.Locale;

public record TrollTrick(String type, double buffer, boolean enabled, int restSeconds, boolean bladeRequired,
                         double sidewaysPush, double upwardPush, String message) {

    public TrollTrick {
        type = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        message = message == null ? "" : message;
    }

    public String latchName() {
        return "troll-" + type + "-" + buffer;
    }

    public boolean usable() {
        return enabled && !type.isEmpty() && buffer > 0;
    }
}
