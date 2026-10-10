// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.notice;

import italy.mlsac.net.suppression.plan.TrickTuning;
import italy.mlsac.net.suppression.plan.TrickType;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.HashSet;
import java.util.Set;

public final class TrickAnnouncements {

    private final TrickBroadcast broadcast;
    private final Set<String> alreadySaid = new HashSet<>();

    TrickAnnouncements(TrickBroadcast broadcast) {
        this.broadcast = broadcast;
    }

    public void announce(TrickType type, TrickTuning tuning, TriggerContext context) {
        broadcast.compose(type, tuning, context).filter(alreadySaid::add).ifPresent(broadcast::say);
    }
}
