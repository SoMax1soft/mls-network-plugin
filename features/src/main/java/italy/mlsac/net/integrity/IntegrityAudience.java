// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.integrity;

import italy.mlsac.net.detection.BadPacketKind;

public interface IntegrityAudience {

    void badPackets(String playerNick, BadPacketKind kind, int count, int windowSeconds);
}
