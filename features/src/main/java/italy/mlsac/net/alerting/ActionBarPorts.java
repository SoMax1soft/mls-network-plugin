// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.scoring.SuspectSnapshot;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public interface ActionBarPorts {

    long CADENCE_TICKS = 10L;

    boolean online(UUID player);

    void showHotbarText(UUID viewer, String coloredText);

    Optional<SuspectSnapshot> snapshot(UUID target);

    int offenceLevel(UUID target);

    void repeatOnViewerThread(UUID viewer, long everyTicks, BooleanSupplier runWhileTrue);
}
