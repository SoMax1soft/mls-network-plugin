// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag;

public record SnapReach(double nearestBlocks, double farthestBlocks) {

    public boolean admits(double distance) {
        return distance >= nearestBlocks && distance <= farthestBlocks;
    }
}
