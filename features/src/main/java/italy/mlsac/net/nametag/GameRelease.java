// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record GameRelease(int major, int minor, int patch) implements Comparable<GameRelease> {

    public static final GameRelease TEXT_DISPLAYS = new GameRelease(1, 19, 4);
    public static final GameRelease SHIFTED_DISPLAY_INDICES = new GameRelease(1, 20, 2);
    public static final GameRelease GENERIC_LIVING_SPAWN = new GameRelease(1, 19, 0);
    public static final GameRelease SINGLE_ID_DESTROY = new GameRelease(1, 17, 0);

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    public static Optional<GameRelease> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        Matcher matcher = VERSION.matcher(raw.replace('_', '.'));
        if (!matcher.find()) {
            return Optional.empty();
        }
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return Optional.of(new GameRelease(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), patch));
    }

    public boolean atLeast(GameRelease other) {
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(GameRelease other) {
        if (major != other.major) {
            return Integer.compare(major, other.major);
        }
        if (minor != other.minor) {
            return Integer.compare(minor, other.minor);
        }
        return Integer.compare(patch, other.patch);
    }
}
