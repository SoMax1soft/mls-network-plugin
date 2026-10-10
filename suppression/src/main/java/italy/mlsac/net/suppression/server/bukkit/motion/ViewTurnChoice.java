// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.platform.GameVersion;
import java.util.Set;

public final class ViewTurnChoice {

    private final Set<GameVersion> teleportFallbackVersions;

    public ViewTurnChoice(Set<GameVersion> teleportFallbackVersions) {
        this.teleportFallbackVersions = Set.copyOf(teleportFallbackVersions);
    }

    public static ViewTurnChoice standard() {
        return new ViewTurnChoice(Set.of());
    }

    public ViewTurn choose(GameVersion server, ViewTurn positionSync, ViewTurn teleportInPlace, PluginLog log) {
        if (!teleportFallbackVersions.contains(server)) {
            return positionSync;
        }
        log.warn(LogTopic.SUPPRESSION, "rotate uses the in-place teleport fallback on server " + server);
        return teleportInPlace;
    }
}
