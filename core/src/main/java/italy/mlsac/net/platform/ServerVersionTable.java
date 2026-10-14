// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.platform;

import java.util.List;
import java.util.Optional;

public final class ServerVersionTable {

    private static final List<GameVersion> ENTRIES = List.of(
            GameVersion.of(1, 16, 0), GameVersion.of(1, 16, 5), GameVersion.of(1, 17, 0),
            GameVersion.of(1, 18, 0), GameVersion.of(1, 19, 0), GameVersion.of(1, 20, 0),
            GameVersion.of(1, 20, 5), GameVersion.of(1, 21, 0), GameVersion.of(1, 21, 1),
            GameVersion.of(1, 21, 4), GameVersion.of(1, 21, 5), GameVersion.of(1, 21, 6),
            GameVersion.of(1, 21, 7), GameVersion.of(1, 21, 8), GameVersion.of(1, 21, 9),
            GameVersion.of(1, 21, 10), GameVersion.of(1, 21, 11));

    private final List<GameVersion> entries;

    public ServerVersionTable() {
        this(ENTRIES);
    }

    ServerVersionTable(List<GameVersion> entries) {
        this.entries = List.copyOf(entries);
    }

    public List<GameVersion> entries() {
        return entries;
    }

    public GameVersion newest() {
        return entries.get(entries.size() - 1);
    }

    public VersionResolution resolve(String bukkitVersion) {
        String raw = bukkitVersion == null ? "" : bukkitVersion;
        String numeric = VersionResolution.numericPrefix(raw).orElse(raw);
        Optional<GameVersion> parsed = GameVersion.parse(numeric);
        Optional<GameVersion> entry = parsed.flatMap(this::entryFor);
        return entry.map(version -> VersionResolution.recognised(raw, version))
                .orElseGet(() -> VersionResolution.unrecognised(raw, GameVersion.V1_16_5));
    }

    private Optional<GameVersion> entryFor(GameVersion running) {
        if (running.major() > 1) {
            return Optional.of(newest());
        }
        if (running.major() < 1) {
            return Optional.empty();
        }
        Optional<GameVersion> sameMinor = entries.stream()
                .filter(e -> e.major() == 1 && e.minor() == running.minor() && e.patch() <= running.patch())
                .max(GameVersion::compareTo);
        if (sameMinor.isPresent()) {
            return sameMinor;
        }
        boolean minorKnown = entries.stream().anyMatch(e -> e.major() == 1 && e.minor() == running.minor());
        if (minorKnown) {
            return Optional.empty();
        }
        return entries.stream()
                .filter(e -> e.major() == 1 && e.minor() < running.minor())
                .max(GameVersion::compareTo);
    }
}
