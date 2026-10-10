// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

public record LegacyTrickEntry(TrickType type, boolean bladeRequired, double sidewaysPush,
                               double upwardPush, String message) {
}
