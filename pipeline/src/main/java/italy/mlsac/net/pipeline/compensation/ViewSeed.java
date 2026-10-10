// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import italy.mlsac.net.pipeline.geometry.Vec3;
import java.util.List;
import java.util.UUID;

public record ViewSeed(String clientVersion, UUID world, Vec3 position, List<SeenPlayer> others) {

    public record SeenPlayer(UUID player, int entityNumber, UUID world, Vec3 position) {
    }
}
