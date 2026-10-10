// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

import java.util.List;

public final class DefaultBufferRules {

    private DefaultBufferRules() {
    }

    public static List<BufferRulePlan> rules() {
        return List.of(
                rule(25, 25, 30, 10, 30),
                rule(50, 35, 35, 15, 30),
                rule(75, 65, 90, 75, 90));
    }

    private static BufferRulePlan rule(int percent, int dealt, int dealtSeconds, int taken, int takenSeconds) {
        return new BufferRulePlan(percent, true, 0, List.of(
                new DamageDealtAction(0, dealt, dealtSeconds),
                new DamageTakenAction(0, taken, takenSeconds)));
    }
}
