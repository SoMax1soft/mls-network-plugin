// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import italy.mlsac.net.config.SettingsLayer;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

final class LagActionParser implements SuppressionVerbReader {

    private static final List<LagEffect> DEFAULT_EFFECTS = List.of(LagEffect.DELAY, LagEffect.MISS);

    @Override
    public String type() {
        return LagAction.TYPE;
    }

    @Override
    public boolean allowedInBufferRules() {
        return true;
    }

    @Override
    public Optional<SuppressionAction> parse(SettingsLayer action, int stage) {
        Optional<LagLevel> level = action.text("level", 16).flatMap(LagLevel::fromId);
        Optional<Integer> seconds = action.strictInt("seconds", 1, 3600);
        Optional<List<LagEffect>> effects = effectList(action);
        if (level.isEmpty() || seconds.isEmpty() || effects.isEmpty()) {
            action.drop("", "needs level light|medium|heavy, known effects and seconds 1..3600");
            return Optional.empty();
        }
        return Optional.of(new LagAction(stage, level.get(), effects.get(), seconds.get()));
    }

    private static Optional<List<LagEffect>> effectList(SettingsLayer action) {
        if (!action.has("effects")) {
            return Optional.of(DEFAULT_EFFECTS);
        }
        Optional<List<?>> list = action.list("effects");
        if (list.isEmpty() || list.get().isEmpty()) {
            return Optional.empty();
        }
        EnumSet<LagEffect> chosen = EnumSet.noneOf(LagEffect.class);
        for (Object raw : list.get()) {
            Optional<LagEffect> effect = raw instanceof String text ? LagEffect.fromId(text) : Optional.empty();
            if (effect.isEmpty()) {
                return Optional.empty();
            }
            chosen.add(effect.get());
        }
        return Optional.of(List.copyOf(chosen));
    }
}
