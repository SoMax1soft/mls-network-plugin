// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.core.PresetMode;
import italy.mlsac.net.platform.PlatformProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class StartupSummary {

    record Integrations(boolean worldGuard, boolean bedrock) {

        boolean any() {
            return worldGuard || bedrock;
        }
    }

    List<String> lines(String version, long enableMillis, PlatformProfile platform, EffectiveConfig config,
                       String backend, Integrations integrations, Edition edition) {
        List<String> lines = new ArrayList<>();
        lines.add("MLSAC " + version + " enabled in " + enableMillis + " ms: " + platform.software().displayName()
                + " " + platform.serverVersion().detected() + " (" + platform.serverVersion().mode().label()
                + "), Java " + platform.javaFeatureVersion());
        lines.add("Detection: " + detection(config) + " | preset: " + preset(config) + " | backend: " + backend
                + (config.detection().enabled() && config.core().accessKey().configured() ? " (connecting...)" : ""));
        lines.add(edition.featureLine(nametags(config, platform)));
        if (integrations.any()) {
            lines.add("Integrations: WorldGuard " + onOff(integrations.worldGuard()) + ", Geyser/Floodgate "
                    + onOff(integrations.bedrock()));
        }
        return lines;
    }

    private static String detection(EffectiveConfig config) {
        if (!config.core().accessKey().configured()) {
            return "off (API key " + config.core().accessKey().describe() + ")";
        }
        if (!config.detection().enabled()) {
            return "off in the active configuration";
        }
        return "on (alert threshold " + String.format(Locale.ROOT, "%.2f", config.alerts().threshold()) + ")";
    }

    private static String preset(EffectiveConfig config) {
        if (config.core().preset().mode() == PresetMode.AUTO) {
            return "auto";
        }
        return config.core().preset().mode() == PresetMode.DISABLED ? "none" : config.core().preset().configuredName();
    }

    private static String nametags(EffectiveConfig config, PlatformProfile platform) {
        if (platform.folia()) {
            return "off on Folia";
        }
        return onOff(config.holograms().enabled());
    }

    private static String onOff(boolean value) {
        return value ? "on" : "off";
    }
}
