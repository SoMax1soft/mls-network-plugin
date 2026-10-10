// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import italy.mlsac.net.config.SettingsLayer;
import java.util.Optional;

final class PercentActionParser implements SuppressionVerbReader {

    @FunctionalInterface
    interface Factory {
        SuppressionAction create(int stage, int percent, int seconds);
    }

    private final String type;
    private final int maxPercent;
    private final Factory factory;

    PercentActionParser(String type, int maxPercent, Factory factory) {
        this.type = type;
        this.maxPercent = maxPercent;
        this.factory = factory;
    }

    static PercentActionParser damageDealt() {
        return new PercentActionParser(DamageDealtAction.TYPE, 100, DamageDealtAction::new);
    }

    static PercentActionParser damageTaken() {
        return new PercentActionParser(DamageTakenAction.TYPE, 300, DamageTakenAction::new);
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public boolean allowedInBufferRules() {
        return true;
    }

    @Override
    public Optional<SuppressionAction> parse(SettingsLayer action, int stage) {
        Optional<Integer> percent = action.strictInt("percent", 5, maxPercent);
        Optional<Integer> seconds = action.strictInt("seconds", 1, 3600);
        if (percent.isEmpty() || seconds.isEmpty()) {
            action.drop("", "needs percent 5.." + maxPercent + " and seconds 1..3600");
            return Optional.empty();
        }
        return Optional.of(factory.create(stage, percent.get(), seconds.get()));
    }
}
