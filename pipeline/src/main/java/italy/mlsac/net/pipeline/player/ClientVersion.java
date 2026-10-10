// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.player;

import java.util.Optional;

public record ClientVersion(int major, int minor, int patch) implements Comparable<ClientVersion> {
    private static final String PREFIX = "V_";
    private static final ClientVersion NEWEST = new ClientVersion(Integer.MAX_VALUE, 0, 0);
    private static final ClientVersion OLDEST = new ClientVersion(0, 0, 0);

    public static Optional<ClientVersion> parse(String name) {
        if (name == null) {
            return Optional.empty();
        }
        if (name.equals("HIGHER_THAN_SUPPORTED_VERSIONS")) {
            return Optional.of(NEWEST);
        }
        if (name.equals("LOWER_THAN_SUPPORTED_VERSIONS")) {
            return Optional.of(OLDEST);
        }
        if (!name.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String[] parts = name.substring(PREFIX.length()).split("_");
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return Optional.of(new ClientVersion(major, minor, patch));
        } catch (NumberFormatException malformed) {
            return Optional.empty();
        }
    }

    public static ClientVersion of(int major, int minor, int patch) {
        return new ClientVersion(major, minor, patch);
    }

    public boolean atLeast(int otherMajor, int otherMinor, int otherPatch) {
        return compareTo(of(otherMajor, otherMinor, otherPatch)) >= 0;
    }

    public boolean below(int otherMajor, int otherMinor, int otherPatch) {
        return compareTo(of(otherMajor, otherMinor, otherPatch)) < 0;
    }

    public boolean above(int otherMajor, int otherMinor, int otherPatch) {
        return compareTo(of(otherMajor, otherMinor, otherPatch)) > 0;
    }

    @Override
    public int compareTo(ClientVersion other) {
        if (major != other.major) {
            return Integer.compare(major, other.major);
        }
        if (minor != other.minor) {
            return Integer.compare(minor, other.minor);
        }
        return Integer.compare(patch, other.patch);
    }
}
