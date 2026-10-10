// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import italy.mlsac.net.pipeline.environment.BackendAvailability;
import italy.mlsac.net.pipeline.environment.WorldSightLines;
import italy.mlsac.net.pipeline.environment.DataFolder;
import italy.mlsac.net.pipeline.environment.DetectionSwitches;
import italy.mlsac.net.pipeline.environment.LegacyWindowSink;
import italy.mlsac.net.pipeline.environment.MonotonicClock;
import italy.mlsac.net.pipeline.environment.PlatformText;
import italy.mlsac.net.pipeline.environment.PlayerConnections;
import italy.mlsac.net.pipeline.environment.PlayerStanding;
import italy.mlsac.net.pipeline.environment.PlayerStates;
import italy.mlsac.net.pipeline.environment.TransactionPings;
import italy.mlsac.net.pipeline.environment.UltraWindowSink;
import italy.mlsac.net.pipeline.environment.UuidSource;
import italy.mlsac.net.pipeline.environment.WallClock;

public record PipelinePorts(
        PlayerStates states,
        PlayerStanding standing,
        PlayerConnections connections,
        WorldSightLines rays,
        MonotonicClock monotonic,
        WallClock wallClock,
        UuidSource uuids,
        DataFolder folder,
        PlatformText platform,
        DetectionSwitches switches,
        BackendAvailability backend,
        UltraWindowSink ultraWindows,
        LegacyWindowSink legacyWindows,
        TransactionPings pings) {
}
