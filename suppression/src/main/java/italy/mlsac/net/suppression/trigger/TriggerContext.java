// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.trigger;

import java.util.UUID;

public record TriggerContext(UUID suspectId, String suspectName, int vl, double probability, double buffer) {
}
