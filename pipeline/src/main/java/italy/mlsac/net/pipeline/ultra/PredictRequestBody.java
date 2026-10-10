// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import italy.mlsac.net.pipeline.json.CompactJsonObject;
import java.util.Base64;
import java.util.UUID;

public record PredictRequestBody(
        String buildVersion,
        String hostLabel,
        String platformKind,
        String serverIp,
        int serverPort,
        boolean interServer,
        String preset,
        String sessionKey) {

    private static final int GENERATION_MARK = 2;

    public String render(byte[] playerData, UUID player, String playerNick) {
        CompactJsonObject body = new CompactJsonObject()
                .text("pluginVersion", buildVersion)
                .integer("pluginGeneration", GENERATION_MARK)
                .bool("interserverEventsSupported", true)
                .text("serverName", hostLabel)
                .text("serverFamily", platformKind)
                .text("family", platformKind)
                .text("serverIp", serverIp)
                .integer("serverPort", serverPort)
                .bool("interServer", interServer);
        if (preset != null && !preset.isEmpty()) {
            body.text("preset", preset);
        }
        if (sessionKey != null) {
            body.text("sessionId", sessionKey);
        }
        return body.text("playerData", Base64.getEncoder().encodeToString(playerData))
                .text("playerUuid", player.toString())
                .text("playerName", playerNick)
                .render();
    }
}
