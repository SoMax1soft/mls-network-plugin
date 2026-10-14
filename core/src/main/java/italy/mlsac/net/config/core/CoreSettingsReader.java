// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.core;

import italy.mlsac.net.config.ConfigTree;
import italy.mlsac.net.config.guard.CommandGuardReader;
import italy.mlsac.net.config.integrity.BadPacketSettingsReader;
import italy.mlsac.net.text.LanguageId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CoreSettingsReader {

    private static final String DEFAULT_LANGUAGE = "ru";
    private static final double BUILT_IN_ALERT_EVENT_THRESHOLD = 0.75;

    private final CommandGuardReader guard = new CommandGuardReader();
    private final BadPacketSettingsReader badPackets = new BadPacketSettingsReader();

    public Result read(ConfigTree file) {
        List<String> warnings = new ArrayList<>();
        boolean debug = bool(file, "debug", false);
        LanguageId language = LanguageId.normalise(string(file, "language").orElse(DEFAULT_LANGUAGE));
        ApiKey accessKey = new ApiKey(file.value("detection.api-key").map(String::valueOf).orElse(ApiKey.PLACEHOLDER));
        PresetSelection.ParsedSelection preset = PresetSelection.parse(string(file, "remote-config.preset").orElse(""));
        preset.warning().ifPresent(warnings::add);
        ServerIdentity identity = new ServerIdentity(
                nonBlank(string(file, "server-identity.name")),
                nonBlank(string(file, "server-identity.family")),
                bool(file, "server-identity.interserver.enabled", false),
                bool(file, "server-identity.reporting.events-enabled", true),
                threshold(file));
        return new Result(new CoreSettings(debug, language, accessKey, preset.selection(), identity,
                guard.read(file, warnings), badPackets.read(file, warnings)), warnings);
    }

    private static double threshold(ConfigTree file) {
        Optional<Object> raw = file.value("server-identity.reporting.alert-threshold");
        double value = raw.filter(Number.class::isInstance).map(v -> ((Number) v).doubleValue())
                .orElse(BUILT_IN_ALERT_EVENT_THRESHOLD);
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : BUILT_IN_ALERT_EVENT_THRESHOLD;
    }

    private static String nonBlank(Optional<String> value) {
        String trimmed = value.map(String::trim).orElse("");
        return trimmed.isEmpty() ? ServerIdentity.DEFAULT_NAME : trimmed;
    }

    private static Optional<String> string(ConfigTree file, String path) {
        return file.value(path).filter(v -> !(v instanceof Map) && !(v instanceof List))
                .map(String::valueOf);
    }

    private static boolean bool(ConfigTree file, String path, boolean fallback) {
        return file.value(path).filter(Boolean.class::isInstance).map(Boolean.class::cast).orElse(fallback);
    }

    public record Result(CoreSettings settings, List<String> warnings) {

        public Result {
            warnings = List.copyOf(warnings);
        }
    }
}
