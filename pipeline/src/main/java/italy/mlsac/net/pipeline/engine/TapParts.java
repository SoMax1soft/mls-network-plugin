// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import italy.mlsac.net.pipeline.compensation.ClientSightRewind;
import italy.mlsac.net.pipeline.occlusion.OcclusionProbe;
import italy.mlsac.net.pipeline.player.ServerSamples;

public record TapParts(ServerSamples samples, ClientSightRewind lens, OcclusionProbe probe, PipelinePorts ports,
                       PlayerTurns turns) {
}
