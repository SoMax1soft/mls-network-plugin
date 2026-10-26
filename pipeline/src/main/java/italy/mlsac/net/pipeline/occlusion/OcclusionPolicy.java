// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

public interface OcclusionPolicy {
    default void rowTick(long tick) {
    }

    void resolveTick(WallCheckRequest job);

    void resolveClick(WallCheckRequest job);
}
