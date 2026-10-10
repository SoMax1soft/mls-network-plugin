// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

public enum OcclusionKind {
    TICK {
        @Override
        void resolve(OcclusionPolicy policy, WallCheckRequest job) {
            policy.resolveTick(job);
        }
    },
    CLICK {
        @Override
        void resolve(OcclusionPolicy policy, WallCheckRequest job) {
            policy.resolveClick(job);
        }
    };

    abstract void resolve(OcclusionPolicy policy, WallCheckRequest job);
}
