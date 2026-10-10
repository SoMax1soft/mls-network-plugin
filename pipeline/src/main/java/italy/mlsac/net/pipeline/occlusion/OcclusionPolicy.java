// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

public interface OcclusionPolicy {
    void resolveTick(WallCheckRequest job);

    void resolveClick(WallCheckRequest job);
}
