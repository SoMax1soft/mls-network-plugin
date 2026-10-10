// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.execution;

import italy.mlsac.net.suppression.notice.TrickBroadcast;
import italy.mlsac.net.suppression.server.EffectWatcher;
import italy.mlsac.net.suppression.server.SuspectBodies;
import italy.mlsac.net.suppression.standing.SuspectStanding;
import italy.mlsac.net.suppression.suspect.SuspectRegistry;
import italy.mlsac.net.suppression.trick.TrickEffects;

public record ActionScene(SuspectRegistry registry, SuspectStanding standing, SuspectBodies bodies,
                          TrickEffects tricks, TrickBroadcast broadcast, EffectWatcher watcher) {
}
