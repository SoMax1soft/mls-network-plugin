// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

public record MemorySnapshot(long air200, long hits200, long air600, long hits600, double missShare,
        boolean valid) {
}
