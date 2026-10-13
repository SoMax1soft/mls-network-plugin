// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

@FunctionalInterface
interface ChunkClaim {

    boolean claim(long bytes);
}
