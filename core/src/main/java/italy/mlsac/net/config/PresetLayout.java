// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PresetLayout {

    private static final List<String> RELOCATED_ROOTS = List.of("general", "menus", "messages");

    public ConfigTree toEffective(Map<String, Object> presetConfig) {
        Map<String, Object> effective = new LinkedHashMap<>();
        presetConfig.forEach((key, value) -> {
            if (!RELOCATED_ROOTS.contains(key)) {
                effective.put(key, deepCopy(value));
            }
        });
        Object general = presetConfig.get("general");
        if (general instanceof Map<?, ?> generalMap) {
            relocate(generalMap, "safe-name-check", effective, "punishments", "safe-name-check");
            relocate(generalMap, "holograms", effective, null, "holograms");
            Object modules = generalMap.get("modules");
            if (modules instanceof Map<?, ?> moduleMap) {
                relocate(moduleMap, "detection", effective, "detection", "enabled");
                relocate(moduleMap, "menus", effective, "menus", "enabled");
                relocate(moduleMap, "updates", effective, "updates", "enabled");
            }
        }
        if (presetConfig.containsKey("menus")) {
            effective.put("menu-layouts", deepCopy(presetConfig.get("menus")));
        }
        if (presetConfig.containsKey("messages")) {
            effective.put("message-overrides", deepCopy(presetConfig.get("messages")));
        }
        return ConfigTree.of(effective);
    }

    private static void relocate(Map<?, ?> source, String sourceKey, Map<String, Object> target, String section,
                                 String targetKey) {
        if (!source.containsKey(sourceKey)) {
            return;
        }
        Object value = deepCopy(source.get(sourceKey));
        if (section == null) {
            target.put(targetKey, value);
            return;
        }
        Map<String, Object> sectionMap = sectionOf(target, section);
        sectionMap.put(targetKey, value);
    }

    private static Map<String, Object> sectionOf(Map<String, Object> target, String section) {
        Object existing = target.get(section);
        Map<String, Object> sectionMap = new LinkedHashMap<>();
        if (existing instanceof Map<?, ?> map) {
            map.forEach((k, v) -> sectionMap.put(String.valueOf(k), v));
        }
        target.put(section, sectionMap);
        return sectionMap;
    }

    private static Object deepCopy(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put(String.valueOf(k), deepCopy(v)));
            return copy;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(PresetLayout::deepCopy).toList();
        }
        return value;
    }
}
