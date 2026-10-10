// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import italy.mlsac.net.logbook.Logbook;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ParticleNames {

    public static final String FALLBACK = "FLAME";

    private static final Map<String, String> OLD_SPELLING_TO_NEW = new HashMap<>();
    private static final Map<String, String> NEW_SPELLING_TO_OLD = new HashMap<>();

    static {
        pair("SPELL_WITCH", "WITCH");
        pair("EXPLOSION_HUGE", "EXPLOSION_EMITTER");
        pair("REDSTONE", "DUST");
        pair("SPELL_MOB", "ENTITY_EFFECT");
        pair("SMOKE_NORMAL", "SMOKE");
        pair("SMOKE_LARGE", "LARGE_SMOKE");
        pair("SPELL", "EFFECT");
        pair("SPELL_INSTANT", "INSTANT_EFFECT");
        pair("VILLAGER_HAPPY", "HAPPY_VILLAGER");
        pair("VILLAGER_ANGRY", "ANGRY_VILLAGER");
        pair("WATER_BUBBLE", "BUBBLE");
        pair("WATER_SPLASH", "SPLASH");
        pair("WATER_WAKE", "FISHING");
        pair("SUSPENDED_DEPTH", "UNDERWATER");
        pair("CRIT_MAGIC", "ENCHANTED_HIT");
        pair("MOB_APPEARANCE", "ELDER_GUARDIAN");
        pair("BLOCK_CRACK", "BLOCK");
        oneWay("FOOTSTEP", "BLOCK");
        oneWay("BLOCK_DUST", "BLOCK");
        pair("ITEM_CRACK", "ITEM");
        oneWay("ITEM_TAKE", "ITEM");
        pair("SNOW_SHOVEL", "ITEM_SNOWBALL");
        oneWay("SNOWBALL", "ITEM_SNOWBALL");
        pair("SLIME", "ITEM_SLIME");
        pair("DRIP_WATER", "DRIPPING_WATER");
        pair("DRIP_LAVA", "DRIPPING_LAVA");
        pair("ENCHANTMENT_TABLE", "ENCHANT");
        pair("FIREWORKS_SPARK", "FIREWORK");
        pair("TOWN_AURA", "MYCELIUM");
        pair("EXPLOSION_NORMAL", "POOF");
        pair("EXPLOSION_LARGE", "EXPLOSION");
    }

    private final EffectRegistry registry;
    private final Logbook log;

    public ParticleNames(EffectRegistry registry, Logbook log) {
        this.registry = registry;
        this.log = log;
    }

    public String resolve(String raw) {
        String name = raw == null ? FALLBACK : raw.trim().toUpperCase(Locale.ROOT);
        if (registry.hasParticle(name)) {
            return name;
        }
        Optional<String> alias = Optional.ofNullable(
                (registry.modernParticleNames() ? OLD_SPELLING_TO_NEW : NEW_SPELLING_TO_OLD).get(name));
        if (alias.isPresent() && registry.hasParticle(alias.get())) {
            return alias.get();
        }
        log.warn("[Animation] Unknown particle '" + raw + "', using " + FALLBACK);
        return FALLBACK;
    }

    private static void pair(String legacy, String modern) {
        OLD_SPELLING_TO_NEW.put(legacy, modern);
        NEW_SPELLING_TO_OLD.put(modern, legacy);
    }

    private static void oneWay(String legacy, String modern) {
        OLD_SPELLING_TO_NEW.put(legacy, modern);
    }
}
