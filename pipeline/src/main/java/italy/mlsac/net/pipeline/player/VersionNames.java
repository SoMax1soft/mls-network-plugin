// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.player;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class VersionNames {
    private static final int REMEMBERED_NAMES = 512;

    private final ConcurrentHashMap<String, Optional<ClientVersion>> parsed = new ConcurrentHashMap<>();

    public Optional<ClientVersion> parse(String name) {
        if (name == null) {
            return Optional.empty();
        }
        Optional<ClientVersion> known = parsed.get(name);
        if (known != null) {
            return known;
        }
        Optional<ClientVersion> fresh = ClientVersion.parse(name);
        if (parsed.size() < REMEMBERED_NAMES) {
            parsed.putIfAbsent(name, fresh);
        }
        return fresh;
    }
}
