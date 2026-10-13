// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.dossier;

import java.util.Optional;

public record PunishmentTally(int total, int kicks, int bans, Optional<HistoryEntry> last) {
}
