// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.response;

import italy.mlsac.net.config.SettingsLayer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

public final class AlertResponseReader {

    private static final int MAX_ENTRIES = 10;
    private static final double MAX_BUFFER = 100_000.0;
    private static final int MAX_MESSAGE = 256;

    public AlertResponseSettings read(AlertResponseSettings lower, SettingsLayer layer) {
        boolean enabled = layer.bool("alert-responses.enabled").orElse(lower.enabled());
        double step = stepPercentOf(lower.stepPercent(), layer);
        boolean reductionEnabled = layer.bool("alert-responses.damage-reduction.enabled")
                .orElse(lower.damageCutOn());
        List<DamageCutStep> stages = readStages(layer).orElse(lower.damageStages());
        boolean pranksOn = layer.bool("alert-responses.troll.enabled").orElse(lower.pranksOn());
        List<PrankRule> actions = readTroll(layer).orElse(lower.prankRules());
        return new AlertResponseSettings(enabled, step, reductionEnabled, stages, pranksOn, actions);
    }

    private static double stepPercentOf(double lower, SettingsLayer layer) {
        OptionalDouble step = layer.number("alert-responses.alerts.buffer-step-percent");
        if (step.isEmpty()) {
            return lower;
        }
        if (step.getAsDouble() > 0.0 && step.getAsDouble() <= 1.0) {
            return step.getAsDouble();
        }
        layer.drop("alert-responses.alerts.buffer-step-percent", "outside (0, 1]");
        return lower;
    }

    private Optional<List<DamageCutStep>> readStages(SettingsLayer layer) {
        String path = "alert-responses.damage-reduction.stages";
        Optional<List<?>> list = layer.list(path);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        List<DamageCutStep> stages = new ArrayList<>();
        int index = 0;
        for (Object raw : list.get()) {
            if (stages.size() >= MAX_ENTRIES) {
                break;
            }
            String label = path + "[" + index++ + "]";
            if (raw instanceof Map<?, ?> map) {
                readStage(layer.element(map, label)).ifPresent(stages::add);
            } else {
                layer.drop(label, "not an object");
            }
        }
        stages.sort(Comparator.comparingDouble(DamageCutStep::buffer));
        return Optional.of(stages);
    }

    private static Optional<DamageCutStep> readStage(SettingsLayer stage) {
        OptionalDouble buffer = stage.number("buffer");
        OptionalDouble reduction = stage.number("reduction-percent");
        OptionalDouble duration = stage.number("duration-seconds");
        if (buffer.isEmpty() || buffer.getAsDouble() <= 0 || reduction.isEmpty()
                || reduction.getAsDouble() < 0 || reduction.getAsDouble() > 100
                || duration.isEmpty() || duration.getAsDouble() < 1 || duration.getAsDouble() > 3600) {
            stage.drop("", "invalid damage-reduction stage");
            return Optional.empty();
        }
        boolean enabled = stage.bool("enabled").orElse(true);
        return Optional.of(new DamageCutStep(Math.min(MAX_BUFFER, buffer.getAsDouble()),
                reduction.getAsDouble(), (int) duration.getAsDouble(), enabled));
    }

    private Optional<List<PrankRule>> readTroll(SettingsLayer layer) {
        String path = "alert-responses.troll.actions";
        Optional<List<?>> list = layer.list(path);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        List<PrankRule> actions = new ArrayList<>();
        int index = 0;
        for (Object raw : list.get()) {
            if (actions.size() >= MAX_ENTRIES) {
                break;
            }
            String label = path + "[" + index++ + "]";
            if (raw instanceof Map<?, ?> map) {
                readAction(layer.element(map, label)).ifPresent(actions::add);
            } else {
                layer.drop(label, "not an object");
            }
        }
        actions.sort(Comparator.comparingDouble(PrankRule::buffer));
        return Optional.of(actions);
    }

    private static Optional<PrankRule> readAction(SettingsLayer action) {
        Optional<TrickKind> kind = action.text("type", 32).flatMap(TrickKind::fromId);
        OptionalDouble buffer = action.number("buffer");
        if (kind.isEmpty() || buffer.isEmpty() || buffer.getAsDouble() <= 0) {
            action.drop("", "invalid troll action");
            return Optional.empty();
        }
        TrickKind type = kind.get();
        return Optional.of(new PrankRule(type,
                Math.min(MAX_BUFFER, buffer.getAsDouble()),
                action.bool("enabled").orElse(true),
                action.intClamped("cooldown-seconds", 0, 3600).orElse(0),
                action.bool("only-sword").orElse(true),
                action.clamped("horizontal-velocity", 0.0, 10.0).orElse(1.4),
                action.clamped("vertical-velocity", 0.0, 10.0).orElse(0.45),
                action.text("message", MAX_MESSAGE).orElse("")));
    }
}
