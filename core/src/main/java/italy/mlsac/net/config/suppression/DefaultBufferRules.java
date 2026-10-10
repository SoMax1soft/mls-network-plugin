// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import java.util.List;

public final class DefaultBufferRules {

    private DefaultBufferRules() {
    }

    public static List<BufferRule> rules() {
        return List.of(
                rule(25, 25, 30, 10, 30),
                rule(50, 35, 35, 15, 30),
                rule(75, 65, 90, 75, 90));
    }

    private static BufferRule rule(int percent, int dealtPercent, int dealtSeconds, int takenPercent, int takenSeconds) {
        return new BufferRule(percent, true, 0, List.of(
                new DamageDealtAction(0, dealtPercent, dealtSeconds),
                new DamageTakenAction(0, takenPercent, takenSeconds)));
    }
}
