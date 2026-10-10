// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.input;

import java.util.UUID;

public record TickEndPacket(UUID player, long nanos, String clientVersion) {
}
