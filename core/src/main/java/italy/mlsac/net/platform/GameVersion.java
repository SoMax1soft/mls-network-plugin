// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.platform;

import java.util.Optional;

public record GameVersion(int major, int minor, int patch) implements Comparable<GameVersion> {

    public static final GameVersion V1_16_5 = new GameVersion(1, 16, 5);

    public GameVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("negative version part");
        }
    }

    public static GameVersion of(int major, int minor, int patch) {
        return new GameVersion(major, minor, patch);
    }

    public static Optional<GameVersion> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String[] parts = text.trim().split("\\.", -1);
        if (parts.length < 2 || parts.length > 3) {
            return Optional.empty();
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = parts.length == 3 ? Integer.parseInt(parts[2]) : 0;
            if (major < 0 || minor < 0 || patch < 0) {
                return Optional.empty();
            }
            return Optional.of(new GameVersion(major, minor, patch));
        } catch (NumberFormatException notNumeric) {
            return Optional.empty();
        }
    }

    public boolean atLeast(GameVersion other) {
        return compareTo(other) >= 0;
    }

    public boolean below(GameVersion other) {
        return compareTo(other) < 0;
    }

    @Override
    public int compareTo(GameVersion other) {
        int byMajor = Integer.compare(major, other.major);
        if (byMajor != 0) {
            return byMajor;
        }
        int byMinor = Integer.compare(minor, other.minor);
        return byMinor != 0 ? byMinor : Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return patch == 0 ? major + "." + minor : major + "." + minor + "." + patch;
    }
}
