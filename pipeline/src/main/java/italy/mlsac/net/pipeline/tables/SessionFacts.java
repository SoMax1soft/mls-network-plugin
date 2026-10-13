// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.tables;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionFacts(String clientLabel, UUID sessionKey, UUID player, String playerNick, String label,
                           String comment, List<SessionNote> notes, Instant beganAt, Instant savedAt, int samples,
                           int attacks, int ticks, int hits) {

    public SessionFacts {
        notes = List.copyOf(notes);
    }
}
