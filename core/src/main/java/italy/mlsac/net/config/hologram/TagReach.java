// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.hologram;

public record TagReach(int pastChecksShown, int seenWithinBlocks) {

    static TagReach stock() {
        return new TagReach(5, 30);
    }
}
