// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.local;

import italy.mlsac.net.config.ConfigTree;
import italy.mlsac.net.config.core.ApiKey;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.resource.ResourceSource;
import italy.mlsac.net.storage.DataFolder;
import italy.mlsac.net.yaml.YamlFormatException;
import italy.mlsac.net.yaml.YamlParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LocalConfigFile {

    public static final String FILE = "config.yml";
    private static final String SCHEMA_KEY = "_schema-version";
    private static final String KEY_LEAF = "api-key";
    private static final String API_KEY = "detection." + KEY_LEAF;
    private static final String LEGACY_API_KEY = "ai." + KEY_LEAF;
    private static final List<String> PRESERVED_ON_MIGRATION = List.of("language", "debug", API_KEY,
            "remote-config.preset", "server-identity.name", "server-identity.family",
            "server-identity.interserver.enabled", "server-identity.reporting.events-enabled",
            "server-identity.reporting.alert-threshold", "command-guard.enabled", "command-guard.mode",
            "command-guard.extra-allowed-roots", "command-guard.max-commands-per-minute",
            "command-guard.max-commands-per-player-per-minute", "command-guard.max-same-command-burst",
            "command-guard.burst-pause-minutes");
    private static final List<String> FILES_NO_LONGER_USED = List.of("modules.yml", "holograms.yml", "menu_ru.yml",
            "menu_en.yml", "menu_vi.yml", "modules/anti-esp.yml", "messages_ru.yml", "messages_en.yml",
            "messages_vi.yml");

    private final DataFolder folder;
    private final ResourceSource resources;
    private final YamlParser yaml;
    private final PluginLog log;

    public LocalConfigFile(DataFolder folder, ResourceSource resources, YamlParser yaml, PluginLog log) {
        this.folder = folder;
        this.resources = resources;
        this.yaml = yaml;
        this.log = log;
    }

    public ConfigTree prepare() throws LocalConfigException {
        retireOldFiles();
        try {
            String bundled = bundledText();
            Optional<String> existing = folder.readText(FILE);
            if (existing.isEmpty()) {
                folder.writeText(FILE, bundled);
                return parse(bundled);
            }
            String current = migrateIfNeeded(existing.get(), bundled);
            current = fillMissing(current, bundled);
            return parse(current);
        } catch (IOException failure) {
            throw new LocalConfigException("cannot read or write " + FILE + ": " + failure.getMessage(), failure);
        }
    }

    private String migrateIfNeeded(String existing, String bundled) throws IOException, LocalConfigException {
        Map<String, Object> user = parseMap(existing);
        Map<String, Object> defaults = parseMap(bundled);
        String userSchema = String.valueOf(ConfigTree.of(user).value(SCHEMA_KEY).orElse(""));
        String bundledSchema = String.valueOf(ConfigTree.of(defaults).value(SCHEMA_KEY).orElse(""));
        if (userSchema.equals(bundledSchema)) {
            return existing;
        }
        ConfigTree userTree = ConfigTree.of(user);
        Map<String, Object> kept = new LinkedHashMap<>();
        for (String path : PRESERVED_ON_MIGRATION) {
            userTree.value(path).filter(ConfigTreeScalars::isTemplateValue).ifPresent(value -> kept.put(path, value));
        }
        adoptLegacyKey(userTree, kept);
        String regenerated = new ConfigTemplate(bundled).withValues(kept);
        folder.writeText(FILE, regenerated);
        log.info(LogTopic.CONFIG, FILE + " rewritten for layout " + bundledSchema + " (was "
                + (userSchema.isEmpty() ? "unversioned" : userSchema) + "), carried over " + new ArrayList<>(kept.keySet()));
        return regenerated;
    }

    private static void adoptLegacyKey(ConfigTree userTree, Map<String, Object> kept) {
        boolean keyMissing = !kept.containsKey(API_KEY) || !new ApiKey(String.valueOf(kept.get(API_KEY))).configured();
        userTree.value(LEGACY_API_KEY).filter(ConfigTreeScalars::isScalar)
                .filter(legacy -> keyMissing && new ApiKey(String.valueOf(legacy)).configured())
                .ifPresent(legacy -> kept.put(API_KEY, String.valueOf(legacy)));
    }

    private String fillMissing(String current, String bundled) throws IOException, LocalConfigException {
        ConfigTemplate template = new ConfigTemplate(bundled);
        ConfigTree user = ConfigTree.of(parseMap(current));
        List<String> missing = template.scalarPaths().stream().filter(path -> !user.contains(path)).toList();
        if (missing.isEmpty()) {
            return current;
        }
        Map<String, Object> kept = new LinkedHashMap<>();
        for (String path : template.scalarPaths()) {
            user.value(path).filter(ConfigTreeScalars::isTemplateValue).ifPresent(value -> kept.put(path, value));
        }
        StringBuilder rebuilt = new StringBuilder(template.withValues(kept));
        Map<String, Object> extra = extraSections(user, ConfigTree.of(parseMap(bundled)));
        if (!extra.isEmpty()) {
            rebuilt.append('\n').append(yaml.dumpBlock(extra));
        }
        folder.writeText(FILE, rebuilt.toString());
        log.info(LogTopic.CONFIG, "config.yml: added missing entries " + missing);
        return rebuilt.toString();
    }

    public boolean syncMissing() throws LocalConfigException {
        try {
            Optional<String> existing = folder.readText(FILE);
            String bundled = bundledText();
            if (existing.isEmpty()) {
                folder.writeText(FILE, bundled);
                return true;
            }
            return !fillMissing(existing.get(), bundled).equals(existing.get());
        } catch (IOException failure) {
            throw new LocalConfigException("cannot sync " + FILE + ": " + failure.getMessage(), failure);
        }
    }

    private static Map<String, Object> extraSections(ConfigTree user, ConfigTree bundled) {
        Map<String, Object> extra = new LinkedHashMap<>();
        for (String key : user.keys()) {
            if (!bundled.keys().contains(key)) {
                user.value(key).ifPresent(value -> extra.put(key, ConfigTreeScalars.mutableCopy(value)));
            }
        }
        return extra;
    }

    private void retireOldFiles() {
        for (String name : FILES_NO_LONGER_USED) {
            if (!folder.isFile(name)) {
                continue;
            }
            try {
                folder.move(name, "old/" + name);
                log.info(LogTopic.CONFIG, name + " is no longer used - these settings are on the site "
                        + "(mlsac.net -> Конфиги). Moved to old/" + name);
            } catch (IOException failure) {
                log.warn(LogTopic.CONFIG, "Could not move " + name + " to old/: " + failure.getMessage());
            }
        }
        try {
            folder.deleteDirectoryIfEmpty("modules");
        } catch (IOException failure) {
            log.debug(LogTopic.CONFIG, () -> "modules/ not removed: " + failure.getMessage());
        }
    }

    private String bundledText() throws LocalConfigException {
        return resources.text(FILE).orElseThrow(() -> new LocalConfigException("bundled " + FILE + " is missing", null));
    }

    private ConfigTree parse(String text) throws LocalConfigException {
        return ConfigTree.of(parseMap(text));
    }

    private Map<String, Object> parseMap(String text) throws LocalConfigException {
        try {
            return yaml.parse(text);
        } catch (YamlFormatException invalid) {
            throw new LocalConfigException(FILE + " is not valid YAML: " + invalid.getMessage(), invalid);
        }
    }
}
