// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.response;

import italy.mlsac.net.response.BaseDamageCorrection;

public final class DamageDampening {

    static final int CORRECTION_PASSES = 2;

    private DamageDampening() {
    }

    public static void scale(DamageSheet sheet, double multiplier) {
        double target = sheet.finalDamage() * multiplier;
        for (String part : sheet.applicableParts()) {
            sheet.rewritePart(part, sheet.part(part) * multiplier);
        }
        for (int pass = 0; pass < CORRECTION_PASSES; pass++) {
            double current = sheet.finalDamage();
            if (!BaseDamageCorrection.needsCorrection(current, target)) {
                return;
            }
            sheet.rewriteBase(BaseDamageCorrection.correctedBase(sheet.baseDamage(), current, target));
        }
    }
}
