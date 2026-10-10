// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection;

import java.util.UUID;

public record BufferChanged(UUID player, String playerNick, double buffer, BufferChangeCause cause,
                            double probability, int offenceLevel) {
}
