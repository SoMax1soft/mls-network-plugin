// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import italy.mlsac.net.suppression.chance.EngineRandom;
import italy.mlsac.net.suppression.lag.motion.SetbackTerrain;
import italy.mlsac.net.suppression.server.CommandDispatch;
import italy.mlsac.net.suppression.server.EffectWatcher;
import italy.mlsac.net.suppression.server.ExemptionProbe;
import italy.mlsac.net.suppression.server.LagExemptSuspects;
import italy.mlsac.net.suppression.server.ServerThreads;
import italy.mlsac.net.suppression.server.SuppressionLedger;
import italy.mlsac.net.suppression.server.SuppressionNotices;
import italy.mlsac.net.suppression.server.SuppressionTexts;
import italy.mlsac.net.suppression.server.SuspectBodies;
import italy.mlsac.net.suppression.server.SuspectStatus;
import italy.mlsac.net.suppression.time.MonotonicClock;
import italy.mlsac.net.suppression.time.WallClock;

public record SuppressionPorts(MonotonicClock clock, WallClock wallClock, EngineRandom random, ServerThreads threads,
                               SuspectStatus status, ExemptionProbe exemption, CommandDispatch commands,
                               SuppressionNotices notices, SuppressionTexts texts, SuspectBodies bodies,
                               EffectWatcher watcher, SetbackTerrain terrain, LagExemptSuspects lagExempt,
                               SuppressionLedger ledger) {

    public SuppressionPorts(MonotonicClock clock, WallClock wallClock, EngineRandom random, ServerThreads threads,
                            SuspectStatus status, ExemptionProbe exemption, CommandDispatch commands,
                            SuppressionNotices notices, SuppressionTexts texts, SuspectBodies bodies,
                            EffectWatcher watcher, SetbackTerrain terrain, LagExemptSuspects lagExempt) {
        this(clock, wallClock, random, threads, status, exemption, commands, notices, texts, bodies, watcher, terrain,
                lagExempt, SuppressionLedger.NONE);
    }

    public SuppressionPorts(MonotonicClock clock, WallClock wallClock, EngineRandom random, ServerThreads threads,
                            SuspectStatus status, ExemptionProbe exemption, CommandDispatch commands,
                            SuppressionNotices notices, SuppressionTexts texts, SuspectBodies bodies,
                            EffectWatcher watcher, SetbackTerrain terrain) {
        this(clock, wallClock, random, threads, status, exemption, commands, notices, texts, bodies, watcher, terrain,
                LagExemptSuspects.NONE, SuppressionLedger.NONE);
    }
}
