// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.packet.HeldPacket;
import italy.mlsac.net.suppression.SuppressionKit;
import italy.mlsac.net.suppression.lag.motion.SetbackTerrain;
import italy.mlsac.net.suppression.server.EffectWatcher;
import italy.mlsac.net.suppression.server.SuspectBodies;
import italy.mlsac.net.suppression.server.SuspectStatus;

public interface SuppressionServer {

    SuspectStatus status();

    SuspectBodies bodies();

    EffectWatcher watcher();

    SetbackTerrain terrain();

    void start(SuppressionKit<HeldPacket> kit);

    void stop();
}
