// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.platform;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class RegistryNameAliases {

    private static final Map<String, String> OLD_SPELLING_TO_NEW = Map.ofEntries(
            Map.entry("SLOW", "SLOWNESS"),
            Map.entry("FAST_DIGGING", "HASTE"),
            Map.entry("SLOW_DIGGING", "MINING_FATIGUE"),
            Map.entry("INCREASE_DAMAGE", "STRENGTH"),
            Map.entry("HEAL", "INSTANT_HEALTH"),
            Map.entry("HARM", "INSTANT_DAMAGE"),
            Map.entry("JUMP", "JUMP_BOOST"),
            Map.entry("CONFUSION", "NAUSEA"),
            Map.entry("DAMAGE_RESISTANCE", "RESISTANCE"),
            Map.entry("SPELL_WITCH", "WITCH"),
            Map.entry("EXPLOSION_HUGE", "EXPLOSION_EMITTER"),
            Map.entry("EXPLOSION_LARGE", "EXPLOSION"),
            Map.entry("EXPLOSION_NORMAL", "POOF"),
            Map.entry("REDSTONE", "DUST"),
            Map.entry("SPELL_MOB", "ENTITY_EFFECT"),
            Map.entry("SPELL", "EFFECT"),
            Map.entry("SPELL_INSTANT", "INSTANT_EFFECT"),
            Map.entry("FIREWORKS_SPARK", "FIREWORK"),
            Map.entry("SMOKE_NORMAL", "SMOKE"),
            Map.entry("SMOKE_LARGE", "LARGE_SMOKE"),
            Map.entry("VILLAGER_HAPPY", "HAPPY_VILLAGER"),
            Map.entry("VILLAGER_ANGRY", "ANGRY_VILLAGER"),
            Map.entry("TOTEM", "TOTEM_OF_UNDYING"),
            Map.entry("ENCHANTMENT_TABLE", "ENCHANT"),
            Map.entry("CRIT_MAGIC", "ENCHANTED_HIT"),
            Map.entry("WATER_BUBBLE", "BUBBLE"),
            Map.entry("WATER_SPLASH", "SPLASH"),
            Map.entry("DRIP_LAVA", "DRIPPING_LAVA"),
            Map.entry("DRIP_WATER", "DRIPPING_WATER"));

    private static final Map<String, String> NEW_SPELLING_TO_OLD = invert(OLD_SPELLING_TO_NEW);

    private final boolean modernSide;

    public RegistryNameAliases(ServerVersionRules rules) {
        this.modernSide = rules.modernRegistryNames();
    }

    public List<String> candidates(String name) {
        String exact = name.trim().toUpperCase(Locale.ROOT);
        return alias(exact).map(alias -> List.of(exact, alias)).orElseGet(() -> List.of(exact));
    }

    private Optional<String> alias(String exact) {
        Map<String, String> table = modernSide ? OLD_SPELLING_TO_NEW : NEW_SPELLING_TO_OLD;
        return Optional.ofNullable(table.get(exact));
    }

    private static Map<String, String> invert(Map<String, String> source) {
        Map<String, String> inverted = new HashMap<>();
        source.forEach((legacy, modern) -> inverted.put(modern, legacy));
        return Map.copyOf(inverted);
    }
}
