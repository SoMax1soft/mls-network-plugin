// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.region;

import java.util.Optional;

public interface RegionLookup {

    RegionLookup NONE = new RegionLookup() {
        @Override
        public boolean available() {
            return false;
        }

        @Override
        public Optional<String> topRegion(BlockPosition position) {
            return Optional.empty();
        }
    };

    boolean available();

    Optional<String> topRegion(BlockPosition position);
}
