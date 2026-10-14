// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.integrity;

public interface ClientTickPace {

    double VANILLA_TICKS_PER_SECOND = 20.0;

    ClientTickPace VANILLA = () -> VANILLA_TICKS_PER_SECOND;

    double ticksPerSecond();
}
