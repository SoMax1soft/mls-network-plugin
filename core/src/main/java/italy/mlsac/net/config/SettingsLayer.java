// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.regex.Pattern;

public final class SettingsLayer {

    private final LayerSource source;
    private final ConfigTree tree;
    private final IssueLog issues;
    private final String prefix;
    private final IssueSeverity severity;

    public SettingsLayer(LayerSource source, ConfigTree tree, IssueLog issues) {
        this(source, tree, issues, "", IssueSeverity.DEBUG);
    }

    private SettingsLayer(LayerSource source, ConfigTree tree, IssueLog issues, String prefix,
                          IssueSeverity severity) {
        this.source = source;
        this.tree = tree;
        this.issues = issues;
        this.prefix = prefix;
        this.severity = severity;
    }

    public LayerSource source() {
        return source;
    }

    public SettingsLayer reportingAs(IssueSeverity newSeverity) {
        return new SettingsLayer(source, tree, issues, prefix, newSeverity);
    }

    public boolean has(String path) {
        return tree.contains(path);
    }

    public Optional<Object> raw(String path) {
        return tree.value(path);
    }

    public Optional<SettingsLayer> section(String path) {
        Optional<ConfigTree> child = tree.section(path);
        if (child.isEmpty() && tree.contains(path)) {
            drop(path, "not a section");
        }
        return child.map(t -> new SettingsLayer(source, t, issues, qualify(path) + ".", severity));
    }

    public SettingsLayer element(Map<?, ?> map, String label) {
        return new SettingsLayer(source, ConfigTree.of(map), issues, prefix + label + ".", severity);
    }

    public Optional<Boolean> bool(String path) {
        Optional<Object> value = tree.value(path);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        if (value.get() instanceof Boolean flag) {
            issues.accepted();
            return Optional.of(flag);
        }
        drop(path, "not a boolean");
        return Optional.empty();
    }

    public OptionalDouble number(String path) {
        Optional<Object> value = tree.value(path);
        if (value.isEmpty()) {
            return OptionalDouble.empty();
        }
        if (value.get() instanceof Number number && Double.isFinite(number.doubleValue())) {
            issues.accepted();
            return OptionalDouble.of(number.doubleValue());
        }
        drop(path, "not a number");
        return OptionalDouble.empty();
    }

    public OptionalDouble clamped(String path, double min, double max) {
        OptionalDouble value = number(path);
        return value.isPresent() ? OptionalDouble.of(Math.max(min, Math.min(max, value.getAsDouble()))) : value;
    }

    public OptionalInt intClamped(String path, int min, int max) {
        OptionalDouble value = number(path);
        if (value.isEmpty()) {
            return OptionalInt.empty();
        }
        double truncated = value.getAsDouble() < 0 ? Math.ceil(value.getAsDouble()) : Math.floor(value.getAsDouble());
        return OptionalInt.of((int) Math.max(min, Math.min(max, truncated)));
    }

    public Optional<Integer> strictInt(String path, int min, int max) {
        Optional<Object> value = tree.value(path);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        if (value.get() instanceof Number number) {
            double v = number.doubleValue();
            if (v == Math.rint(v) && v >= min && v <= max) {
                issues.accepted();
                return Optional.of((int) v);
            }
            drop(path, "outside " + min + ".." + max);
            return Optional.empty();
        }
        drop(path, "not a number");
        return Optional.empty();
    }

    public Optional<String> text(String path, int maxLength) {
        Optional<Object> value = tree.value(path);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        if (value.get() instanceof String text) {
            issues.accepted();
            return Optional.of(CleanText.of(text, maxLength));
        }
        drop(path, "not a string");
        return Optional.empty();
    }

    public Optional<String> matching(String path, Pattern pattern) {
        Optional<Object> value = tree.value(path);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        if (value.get() instanceof String text && pattern.matcher(text.trim()).matches()) {
            issues.accepted();
            return Optional.of(text.trim());
        }
        drop(path, "does not match " + pattern.pattern());
        return Optional.empty();
    }

    public Optional<List<?>> list(String path) {
        Optional<Object> value = tree.value(path);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        if (value.get() instanceof List<?> list) {
            return Optional.of(list);
        }
        drop(path, "not a list");
        return Optional.empty();
    }

    public Optional<ConfigTree> tree(String path) {
        return tree.section(path);
    }

    public ConfigTree tree() {
        return tree;
    }

    public void drop(String path, String reason) {
        issues.drop(source, qualify(path), reason, severity);
    }

    public void accepted() {
        issues.accepted();
    }

    private String qualify(String path) {
        return prefix + path;
    }
}
