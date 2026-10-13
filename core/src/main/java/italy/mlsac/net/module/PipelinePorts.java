// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.module;

import italy.mlsac.net.detection.AimProfiles;
import italy.mlsac.net.detection.CombatStatus;
import italy.mlsac.net.detection.TickRestore;
import java.util.Objects;

public record PipelinePorts(TickRestore restore, AimProfiles aim, CombatStatus combat) {

    public PipelinePorts {
        Objects.requireNonNull(restore, "restore");
        Objects.requireNonNull(aim, "aim");
        Objects.requireNonNull(combat, "combat");
    }
}
