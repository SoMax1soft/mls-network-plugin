// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

public record TagLimits(int pastChecksShown, int seenWithinBlocks) {
    private static final int FEWEST_CHECKS = 1;
    private static final int MOST_CHECKS = 20;
    private static final int NEAREST_RANGE = 8;
    private static final int FARTHEST_RANGE = 128;

    public TagLimits {
        pastChecksShown = Math.max(FEWEST_CHECKS, Math.min(MOST_CHECKS, pastChecksShown));
        seenWithinBlocks = Math.max(NEAREST_RANGE, Math.min(FARTHEST_RANGE, seenWithinBlocks));
    }

    static TagLimits stock() {
        return new TagLimits(5, 30);
    }
}
