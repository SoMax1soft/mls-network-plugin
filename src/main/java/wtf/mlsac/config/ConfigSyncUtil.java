package wtf.mlsac.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ConfigSyncUtil {
    private ConfigSyncUtil() {
    }

    public static boolean syncPluginConfig(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try (InputStream inputStream = plugin.getResource("config.yml")) {
            if (inputStream == null) {
                return false;
            }

            YamlConfiguration current = YamlConfiguration.loadConfiguration(configFile);
            YamlConfiguration defaults = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            boolean changed = migrateLegacyDetectionSettings(current);
            changed |= copyMissing(current, defaults, "");
            if (changed) {
                current.save(configFile);
                plugin.reloadConfig();
                plugin.getLogger().info("Added or migrated missing entries in config.yml");
            }
            return changed;
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to sync config.yml: " + exception.getMessage());
            return false;
        }
    }

    public static boolean syncResourceConfig(JavaPlugin plugin, String resourceName, File configFile) {
        if (!configFile.exists()) {
            plugin.saveResource(resourceName, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        try (InputStream inputStream = plugin.getResource(resourceName)) {
            if (inputStream == null) {
                return false;
            }

            YamlConfiguration defaults = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            boolean changed = copyMissing(config, defaults, "");
            if (changed) {
                config.save(configFile);
                plugin.getLogger().info("Added missing entries to " + resourceName);
            }
            return changed;
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to sync " + resourceName + ": " + exception.getMessage());
            return false;
        }
    }

    public static boolean syncAllPluginConfigs(JavaPlugin plugin) {
        File dataFolder = plugin.getDataFolder();
        boolean changed = syncPluginConfig(plugin);
        changed |= syncResourceConfig(plugin, "messages.yml", new File(dataFolder, "messages.yml"));
        changed |= syncResourceConfig(plugin, "menu.yml", new File(dataFolder, "menu.yml"));
        changed |= syncResourceConfig(plugin, "holograms.yml", new File(dataFolder, "holograms.yml"));
        return changed;
    }

    public static FileConfiguration loadAndSync(JavaPlugin plugin, String resourceName, File configFile) {
        if (!configFile.exists()) {
            plugin.saveResource(resourceName, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        try (InputStream inputStream = plugin.getResource(resourceName)) {
            if (inputStream != null) {
                YamlConfiguration defaults = YamlConfiguration
                        .loadConfiguration(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                boolean changed = copyMissing(config, defaults, "");
                if (changed) {
                    config.save(configFile);
                    plugin.getLogger().info("Added missing entries to " + resourceName);
                }
                config.setDefaults(defaults);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to sync " + resourceName + ": " + exception.getMessage());
        }

        return config;
    }

    private static boolean migrateLegacyDetectionSettings(FileConfiguration target) {
        boolean changed = false;

        changed |= copyLegacyApiKey(target);
        changed |= copyLegacyIfMissing(target, "ai.enabled", "detection.enabled");
        changed |= copyLegacyIfMissing(target, "ai.server", "detection.endpoint");
        changed |= copyLegacyIfMissing(target, "ai.sequence", "detection.sample-size");
        changed |= copyLegacyIfMissing(target, "ai.step", "detection.sample-interval");
        changed |= copyLegacyIfMissing(target, "ai.alert.threshold", "alerts.threshold");
        changed |= copyLegacyIfMissing(target, "ai.alert.console", "alerts.console");
        changed |= copyLegacyIfMissing(target, "ai.buffer.flag", "violation.threshold");
        changed |= copyLegacyIfMissing(target, "ai.buffer.reset-on-flag", "violation.reset-value");
        changed |= copyLegacyIfMissing(target, "ai.buffer.multiplier", "violation.multiplier");
        changed |= copyLegacyIfMissing(target, "ai.buffer.decrease", "violation.decay");
        changed |= copyLegacyIfMissing(target, "ai.punishment.min-probability", "penalties.min-probability");
        changed |= copyLegacySectionIfMissing(target, "ai.models", "detection.models");
        changed |= copyLegacySectionIfMissing(target, "ai.punishment.commands", "penalties.actions");

        return changed;
    }

    private static boolean copyLegacyApiKey(FileConfiguration target) {
        if (!target.isSet("ai.api-key")) {
            return false;
        }

        String legacyApiKey = target.getString("ai.api-key", "").trim();
        if (isApiKeyPlaceholder(legacyApiKey)) {
            return false;
        }

        String currentApiKey = target.getString("detection.api-key", "");
        if (target.isSet("detection.api-key") && !isApiKeyPlaceholder(currentApiKey)) {
            return false;
        }

        target.set("detection.api-key", legacyApiKey);
        return true;
    }

    private static boolean copyLegacyIfMissing(FileConfiguration target, String legacyPath, String newPath) {
        if (target.isSet(newPath) || !target.isSet(legacyPath)) {
            return false;
        }

        target.set(newPath, target.get(legacyPath));
        return true;
    }

    private static boolean copyLegacySectionIfMissing(FileConfiguration target, String legacyPath, String newPath) {
        if (target.isSet(newPath)) {
            return false;
        }

        ConfigurationSection legacySection = target.getConfigurationSection(legacyPath);
        if (legacySection == null) {
            return false;
        }

        copySectionContents(target, legacySection, newPath);
        return true;
    }

    private static void copySectionContents(FileConfiguration target, ConfigurationSection source, String targetPath) {
        for (String key : source.getKeys(false)) {
            String childTargetPath = targetPath + "." + key;
            ConfigurationSection childSection = source.getConfigurationSection(key);
            if (childSection != null) {
                copySectionContents(target, childSection, childTargetPath);
            } else {
                target.set(childTargetPath, source.get(key));
            }
        }
    }

    private static boolean isApiKeyPlaceholder(String value) {
        if (value == null) {
            return true;
        }

        String normalized = value.trim();
        return normalized.isEmpty() || "your-api-key".equalsIgnoreCase(normalized);
    }

    private static boolean copyMissing(FileConfiguration target, FileConfiguration defaults, String path) {
        boolean changed = false;
        ConfigurationSection defaultSection = path.isEmpty()
                ? defaults
                : defaults.getConfigurationSection(path);
        if (defaultSection == null) {
            return false;
        }

        for (String key : defaultSection.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            ConfigurationSection childDefaults = defaults.getConfigurationSection(fullPath);
            if (childDefaults != null) {
                boolean createdOrRepairedSection = false;
                if (!target.isSet(fullPath)) {
                    target.createSection(fullPath);
                    changed = true;
                    createdOrRepairedSection = true;
                } else if (!target.isConfigurationSection(fullPath)) {
                    target.set(fullPath, null);
                    target.createSection(fullPath);
                    changed = true;
                    createdOrRepairedSection = true;
                }
                // Numeric-key scalar maps are user-defined thresholds, not fixed child fields.
                if (!createdOrRepairedSection && isUserKeyedScalarMap(defaults, fullPath)) {
                    continue;
                }
                changed |= copyMissing(target, defaults, fullPath);
                continue;
            }

            Object defaultValue = defaults.get(fullPath);
            if (!target.isSet(fullPath)) {
                target.set(fullPath, defaultValue);
                changed = true;
            } else if (defaultValue instanceof List && !(target.get(fullPath) instanceof List)) {
                target.set(fullPath, defaultValue);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean isUserKeyedScalarMap(FileConfiguration defaults, String path) {
        ConfigurationSection section = defaults.getConfigurationSection(path);
        if (section == null) {
            return false;
        }

        boolean hasKeys = false;
        for (String key : section.getKeys(false)) {
            hasKeys = true;
            String childPath = path + "." + key;
            if (!isIntegerKey(key) || defaults.getConfigurationSection(childPath) != null) {
                return false;
            }
        }
        return hasKeys;
    }

    private static boolean isIntegerKey(String key) {
        try {
            Integer.parseInt(key);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
