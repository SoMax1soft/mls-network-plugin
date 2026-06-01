/*
 * This file is part of MLSAC - AI powered Anti-Cheat
 * Copyright (C) 2026 MLSAC Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package wtf.mlsac.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация анимаций из config.yml
 */
public class AnimationConfig {
    private final FileConfiguration config;
    private boolean enabled;
    private String defaultType;
    private boolean perCheckEnabled;
    private Map<String, String> checkMappings;

    public AnimationConfig(FileConfiguration config) {
        this.config = config;
        this.checkMappings = new HashMap<>();
        load();
    }

    public void load() {
        // Основные настройки
        this.enabled = config.getBoolean("penalties.animation.enabled", true);
        this.defaultType = config.getString("penalties.animation.type", "classic");

        // Настройки per-check
        this.perCheckEnabled = config.getBoolean("penalties.animation.per-check.enabled", false);

        // Загрузка маппингов
        checkMappings.clear();
        if (perCheckEnabled) {
            ConfigurationSection mappings = config.getConfigurationSection("penalties.animation.per-check.mappings");
            if (mappings != null) {
                for (String key : mappings.getKeys(false)) {
                    String animationType = mappings.getString(key);
                    if (animationType != null) {
                        checkMappings.put(key.toLowerCase(), animationType.toLowerCase());
                    }
                }
            }
        }
    }

    /**
     * Включены ли анимации
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Получить тип анимации по умолчанию
     */
    public String getDefaultType() {
        return defaultType;
    }

    /**
     * Включен ли режим per-check
     */
    public boolean isPerCheckEnabled() {
        return perCheckEnabled;
    }

    /**
     * Получить тип анимации для конкретного типа чека
     * 
     * @param checkType тип чека (killaura, reach, fly и т.д.)
     * @return тип анимации или default если не найден
     */
    public String getAnimationForCheck(String checkType) {
        if (!perCheckEnabled) {
            return defaultType;
        }

        String normalized = checkType.toLowerCase();
        return checkMappings.getOrDefault(normalized, 
               checkMappings.getOrDefault("default", defaultType));
    }

    /**
     * Получить все маппинги чеков
     */
    public Map<String, String> getCheckMappings() {
        return new HashMap<>(checkMappings);
    }

    /**
     * Установить тип анимации по умолчанию
     */
    public void setDefaultType(String type) {
        this.defaultType = type;
        config.set("penalties.animation.type", type);
    }

    /**
     * Включить/выключить анимации
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        config.set("penalties.animation.enabled", enabled);
    }

    /**
     * Включить/выключить режим per-check
     */
    public void setPerCheckEnabled(boolean enabled) {
        this.perCheckEnabled = enabled;
        config.set("penalties.animation.per-check.enabled", enabled);
    }

    /**
     * Добавить маппинг чека к анимации
     */
    public void addCheckMapping(String checkType, String animationType) {
        checkMappings.put(checkType.toLowerCase(), animationType.toLowerCase());
        config.set("penalties.animation.per-check.mappings." + checkType.toLowerCase(), animationType);
    }

    /**
     * Удалить маппинг чека
     */
    public void removeCheckMapping(String checkType) {
        checkMappings.remove(checkType.toLowerCase());
        config.set("penalties.animation.per-check.mappings." + checkType.toLowerCase(), null);
    }
}
