// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import italy.mlsac.net.config.IssueSeverity;
import italy.mlsac.net.config.SettingsLayer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SuppressionReader {

    private static final String ROOT = "suppression";
    private static final int STEP_CAP = 20;
    private static final int RULE_CAP = 10;
    private static final int ACTION_CAP = 8;

    private final ActionParsers parsers = new ActionParsers();

    public Optional<SuppressionSettings> read(Optional<SuppressionSettings> lower, SettingsLayer layer) {
        if (!layer.has(ROOT)) {
            return lower;
        }
        Optional<SettingsLayer> section = layer.reportingAs(IssueSeverity.WARNING).section(ROOT);
        if (section.isEmpty()) {
            return lower;
        }
        SettingsLayer s = section.get();
        SuppressionSettings base = lower.orElseGet(SuppressionSettings::emptyWithDefaultRules);
        boolean repeatLast = s.bool("repeat-last").orElse(base.repeatLast());
        boolean bufferEnabled = s.bool("buffer-enabled").orElse(base.bufferEnabled());
        List<SuppressionStep> steps = readSteps(s).orElse(base.steps());
        List<BufferRule> rules = ruleList(s).orElse(base.bufferRules());
        return Optional.of(new SuppressionSettings(repeatLast, steps, bufferEnabled, rules));
    }

    private Optional<List<SuppressionStep>> readSteps(SettingsLayer section) {
        Optional<List<?>> list = section.list("steps");
        if (list.isEmpty()) {
            return Optional.empty();
        }
        List<SuppressionStep> steps = new ArrayList<>();
        Set<Integer> flags = new HashSet<>();
        for (int i = 0; i < list.get().size() && steps.size() < STEP_CAP; i++) {
            Object raw = list.get().get(i);
            String label = "steps[" + i + "]";
            if (!(raw instanceof Map<?, ?> map)) {
                section.drop(label, "not an object");
                continue;
            }
            SettingsLayer step = section.element(map, label);
            Optional<Integer> flag = step.strictInt("flag", 1, 999);
            if (flag.isEmpty() || !flags.add(flag.get())) {
                step.drop("flag", flag.isEmpty() ? "missing or outside 1..999" : "duplicate flag");
                continue;
            }
            List<SuppressionAction> actions = readActions(step, false);
            if (actions.isEmpty()) {
                step.drop("actions", "no valid action");
                flags.remove(flag.get());
                continue;
            }
            steps.add(new SuppressionStep(flag.get(), actions));
        }
        steps.sort(Comparator.comparingInt(SuppressionStep::flag));
        return Optional.of(steps);
    }

    private Optional<List<BufferRule>> ruleList(SettingsLayer section) {
        Optional<List<?>> list = section.list("buffer");
        if (list.isEmpty()) {
            return Optional.empty();
        }
        List<BufferRule> rules = new ArrayList<>();
        Set<Integer> percents = new HashSet<>();
        for (int i = 0; i < list.get().size() && rules.size() < RULE_CAP; i++) {
            Object raw = list.get().get(i);
            String label = "buffer[" + i + "]";
            if (!(raw instanceof Map<?, ?> map)) {
                section.drop(label, "not an object");
                continue;
            }
            readRule(section.element(map, label), percents).ifPresent(rules::add);
        }
        rules.sort(Comparator.comparingInt(BufferRule::percent));
        return Optional.of(rules);
    }

    private Optional<BufferRule> readRule(SettingsLayer rule, Set<Integer> percents) {
        Optional<Integer> percent = rule.strictInt("percent", 5, 100);
        if (percent.isEmpty() || percents.contains(percent.get())) {
            rule.drop("percent", percent.isEmpty() ? "missing or outside 5..100" : "duplicate percent");
            return Optional.empty();
        }
        int cooldown = 0;
        if (rule.has("cooldown-seconds")) {
            Optional<Integer> parsed = rule.strictInt("cooldown-seconds", 0, 3600);
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            cooldown = parsed.get();
        }
        List<SuppressionAction> actions = readActions(rule, true);
        if (actions.isEmpty()) {
            rule.drop("actions", "no valid action");
            return Optional.empty();
        }
        percents.add(percent.get());
        return Optional.of(new BufferRule(percent.get(), rule.bool("enabled").orElse(true), cooldown, actions));
    }

    private List<SuppressionAction> readActions(SettingsLayer owner, boolean inBufferRule) {
        Optional<List<?>> list = owner.list("actions");
        List<SuppressionAction> actions = new ArrayList<>();
        if (list.isEmpty()) {
            return actions;
        }
        for (int i = 0; i < list.get().size() && actions.size() < ACTION_CAP; i++) {
            Object raw = list.get().get(i);
            String label = "actions[" + i + "]";
            if (raw instanceof Map<?, ?> map) {
                parsers.parse(owner.element(map, label), inBufferRule).ifPresent(actions::add);
            } else {
                owner.drop(label, "not an object");
            }
        }
        return actions;
    }
}
