// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

import java.util.List;
import java.util.UUID;

public record SuppressionReport(UUID player, String playerNick, double probability, double buffer, Trigger trigger,
                                int level, List<String> actions) {

    public SuppressionReport {
        actions = List.copyOf(actions);
    }

    public enum Trigger {
        BUFFER("buffer"),
        STAGE("stage");

        private final String wireName;

        Trigger(String wireName) {
            this.wireName = wireName;
        }

        public String wireName() {
            return wireName;
        }
    }
}
