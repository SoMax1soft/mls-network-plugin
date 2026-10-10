// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.standing;

import italy.mlsac.net.suppression.server.LagExemptSuspects;
import italy.mlsac.net.suppression.server.SuspectStatus;
import java.util.UUID;

public final class SuspectStanding {

    private final SuspectStatus status;
    private final CachedExemption exemption;
    private final LagExemptSuspects exemptions;

    public SuspectStanding(SuspectStatus status, CachedExemption exemption, LagExemptSuspects exemptions) {
        this.status = status;
        this.exemption = exemption;
        this.exemptions = exemptions;
    }

    public boolean online(UUID suspectId) {
        return status.online(suspectId);
    }

    public boolean exempt(UUID suspectId) {
        return exemption.exempt(suspectId);
    }

    public boolean paused(UUID suspectId) {
        return status.effectsPaused(suspectId);
    }

    public boolean shielded(UUID suspectId) {
        return exempt(suspectId) || paused(suspectId);
    }

    public boolean lagExempt(UUID suspectId) {
        return exemptions.lagExempt(suspectId);
    }

    public void forget(UUID suspectId) {
        exemption.forget(suspectId);
    }

    public void clear() {
        exemption.clear();
    }
}
