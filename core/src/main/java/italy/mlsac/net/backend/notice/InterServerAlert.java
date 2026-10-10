// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.notice;

public record InterServerAlert(String id, String type, String hostLabel, String playerNick, String model,
                               double probability, double buffer, int offenceLevel, String action) {
}
