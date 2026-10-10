// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

public record AttackHistory(
        double msSinceLastAttack,
        int rowsSinceLastAttack,
        int swingsSinceLastAttack,
        int airSwingsSinceLastAttack,
        int slotChangesSinceLastAttack,
        int rowsAfterLastSwing,
        double millisAfterSwing) {
}
