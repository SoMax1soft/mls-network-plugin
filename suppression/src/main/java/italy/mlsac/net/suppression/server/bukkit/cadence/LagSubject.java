// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.cadence;

import italy.mlsac.net.suppression.lag.runtime.LagActuator;
import italy.mlsac.net.suppression.lag.runtime.TickFrame;

public interface LagSubject {

    TickFrame frame(long now, long tick);

    LagActuator actuator();
}
