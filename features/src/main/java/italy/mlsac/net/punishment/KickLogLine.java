// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

public record KickLogLine(String playerNick, double probability, double buffer, int offenceLevel, String localTime,
                         String command) {
}
