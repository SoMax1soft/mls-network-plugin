// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.detection;

import java.util.Locale;
import java.util.Optional;

public sealed interface RegionEntry permits RegionEntry.WholeWorld, RegionEntry.RegionInWorld, RegionEntry.RegionAnywhere {

    boolean exempts(String world, Optional<String> topRegion);

    boolean needsRegionLookup();

    String text();

    static RegionEntry parse(String raw) {
        String entry = raw.trim().toLowerCase(Locale.ROOT);
        if (entry.endsWith(":*")) {
            return new WholeWorld(entry.substring(0, entry.length() - 2));
        }
        int colon = entry.indexOf(':');
        if (colon >= 0) {
            return new RegionInWorld(entry.substring(0, colon), entry.substring(colon + 1));
        }
        return new RegionAnywhere(entry);
    }

    record WholeWorld(String world) implements RegionEntry {

        @Override
        public boolean exempts(String currentWorld, Optional<String> topRegion) {
            return world.equalsIgnoreCase(currentWorld);
        }

        @Override
        public boolean needsRegionLookup() {
            return false;
        }

        @Override
        public String text() {
            return world + ":*";
        }
    }

    record RegionInWorld(String world, String region) implements RegionEntry {

        @Override
        public boolean exempts(String currentWorld, Optional<String> topRegion) {
            return world.equalsIgnoreCase(currentWorld) && topRegion.filter(region::equalsIgnoreCase).isPresent();
        }

        @Override
        public boolean needsRegionLookup() {
            return true;
        }

        @Override
        public String text() {
            return world + ":" + region;
        }
    }

    record RegionAnywhere(String region) implements RegionEntry {

        @Override
        public boolean exempts(String currentWorld, Optional<String> topRegion) {
            return topRegion.filter(region::equalsIgnoreCase).isPresent();
        }

        @Override
        public boolean needsRegionLookup() {
            return true;
        }

        @Override
        public String text() {
            return region;
        }
    }
}
