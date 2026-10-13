// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.guard;

import italy.mlsac.net.config.ConfigTree;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class CommandGuardReader {

    public static final String SECTION = "command-guard";
    private static final Pattern ROOT = Pattern.compile("^[a-z0-9_.\\-]{1,48}(:[a-z0-9_.\\-]{1,48})?$");
    private static final int MAX_EXTRA_ROOTS = 64;
    private static final int MAX_PER_MINUTE = 10_000;
    private static final int MAX_BURST = 1_000;
    private static final int MAX_PAUSE_MINUTES = 1_440;

    public CommandGuardSettings read(ConfigTree file, List<String> warnings) {
        CommandGuardSettings defaults = CommandGuardSettings.pluginDefaults();
        boolean enabled = file.value(SECTION + ".enabled").filter(Boolean.class::isInstance)
                .map(Boolean.class::cast).orElse(defaults.enabled());
        GuardMode mode = mode(file, warnings);
        Set<String> roots = roots(file, warnings);
        int perMinute = bounded(file, "max-commands-per-minute", defaults.maxCommandsPerMinute(), MAX_PER_MINUTE,
                warnings);
        int perPlayer = bounded(file, "max-commands-per-player-per-minute", defaults.maxCommandsPerPlayerPerMinute(),
                MAX_PER_MINUTE, warnings);
        int burst = bounded(file, "max-same-command-burst", defaults.maxSameCommandBurst(), MAX_BURST, warnings);
        int pause = bounded(file, "burst-pause-minutes", defaults.burstPauseMinutes(), MAX_PAUSE_MINUTES, warnings);
        return new CommandGuardSettings(enabled, mode, roots, perMinute, perPlayer, burst, pause);
    }

    private static GuardMode mode(ConfigTree file, List<String> warnings) {
        Optional<Object> raw = file.value(SECTION + ".mode");
        if (raw.isEmpty()) {
            return GuardMode.BLOCK;
        }
        Optional<GuardMode> parsed = GuardMode.parse(String.valueOf(raw.get()));
        if (parsed.isEmpty()) {
            warnings.add(SECTION + ".mode must be block or warn, using block");
        }
        return parsed.orElse(GuardMode.BLOCK);
    }

    private static Set<String> roots(ConfigTree file, List<String> warnings) {
        Optional<Object> raw = file.value(SECTION + ".extra-allowed-roots");
        List<Object> entries = new ArrayList<>();
        raw.ifPresent(value -> {
            if (value instanceof List<?> list) {
                entries.addAll(list);
            } else if (!(value instanceof Map<?, ?>)) {
                entries.addAll(List.of(String.valueOf(value).split(",")));
            }
        });
        Set<String> roots = new LinkedHashSet<>();
        for (Object entry : entries) {
            String root = normalised(String.valueOf(entry));
            if (root.isEmpty()) {
                continue;
            }
            if (!ROOT.matcher(root).matches() || roots.size() >= MAX_EXTRA_ROOTS) {
                warnings.add(SECTION + ".extra-allowed-roots: skipped '" + root + "'");
                continue;
            }
            roots.add(root);
        }
        return roots;
    }

    private static String normalised(String entry) {
        String root = entry.trim().toLowerCase(Locale.ROOT);
        while (root.startsWith("/")) {
            root = root.substring(1);
        }
        return root;
    }

    private static int bounded(ConfigTree file, String key, int fallback, int max, List<String> warnings) {
        Optional<Object> raw = file.value(SECTION + "." + key);
        if (raw.isEmpty()) {
            return fallback;
        }
        if (!(raw.get() instanceof Number number) || !Double.isFinite(number.doubleValue())) {
            warnings.add(SECTION + "." + key + " must be a number, using " + fallback);
            return fallback;
        }
        long value = Math.round(number.doubleValue());
        return (int) Math.max(1, Math.min(max, value));
    }
}
