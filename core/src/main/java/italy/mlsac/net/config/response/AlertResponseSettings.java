// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.response;

import java.util.List;
import java.util.Optional;

public record AlertResponseSettings(boolean enabled, double stepPercent, boolean damageCutOn,
                                    List<DamageCutStep> damageStages, boolean pranksOn,
                                    List<PrankRule> prankRules) {

    public static final double DEFAULT_STEP_PERCENT = 0.33;

    public AlertResponseSettings {
        damageStages = List.copyOf(damageStages);
        prankRules = List.copyOf(prankRules);
    }

    public static AlertResponseSettings pluginDefaults() {
        return new AlertResponseSettings(true, DEFAULT_STEP_PERCENT, true,
                List.of(new DamageCutStep(10.0, 5, 8, true),
                        new DamageCutStep(20.0, 35, 12, true),
                        new DamageCutStep(30.0, 70, 16, true)),
                false,
                List.of(new PrankRule(TrickKind.SHUFFLE_INVENTORY, 20.0, true, 20, true, 1.4, 0.45,
                                DefaultTrickTexts.SHUFFLE),
                        new PrankRule(TrickKind.DROP_WEAPON, 35.0, true, 20, true, 1.9, 0.55,
                                DefaultTrickTexts.DROP),
                        new PrankRule(TrickKind.LAUNCH, 45.0, true, 15, false, 1.2, 0.6,
                                DefaultTrickTexts.LAUNCH)));
    }

    public Optional<PrankRule> trickParameters(TrickKind kind) {
        return prankRules.stream().filter(action -> action.kind() == kind).findFirst();
    }
}
