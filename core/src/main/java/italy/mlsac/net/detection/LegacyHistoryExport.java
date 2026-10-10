// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection;

import java.util.UUID;

@FunctionalInterface
public interface LegacyHistoryExport {

    LegacyHistoryExport NONE = player -> RestoreResult.NOTHING_STORED;

    RestoreResult export(UUID player);
}
