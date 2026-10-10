// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import java.util.Map;

public enum HeldItemFamily {
    UNSEEN(0),
    BARE_HAND(1),
    BLADE(2),
    CHOPPER(3),
    CLUB(4),
    FORK(5),
    POLEARM(6),
    MINING_TOOL(7),
    DIGGING_TOOL(8),
    FARMING_TOOL(9),
    LAUNCHER(10),
    GUARD(11),
    ANYTHING_ELSE(12);

    public static final String UNSEEN_MATERIAL = "unknown";

    private static final Map<String, HeldItemFamily> WHOLE_NAMES = Map.of(
            UNSEEN_MATERIAL, UNSEEN,
            "air", BARE_HAND,
            "cave_air", BARE_HAND,
            "void_air", BARE_HAND,
            "mace", CLUB,
            "trident", FORK,
            "bow", LAUNCHER,
            "crossbow", LAUNCHER,
            "shield", GUARD);

    private static final Map<String, HeldItemFamily> LAST_WORDS = Map.of(
            "sword", BLADE,
            "axe", CHOPPER,
            "spear", POLEARM,
            "pickaxe", MINING_TOOL,
            "shovel", DIGGING_TOOL,
            "hoe", FARMING_TOOL);

    private final int code;

    HeldItemFamily(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static int familyCode(String material) {
        return familyOf(material).code;
    }

    static HeldItemFamily familyOf(String material) {
        if (material == null) {
            return UNSEEN;
        }
        HeldItemFamily whole = WHOLE_NAMES.get(material);
        if (whole != null) {
            return whole;
        }
        int underscore = material.lastIndexOf('_');
        if (underscore < 0) {
            return ANYTHING_ELSE;
        }
        return LAST_WORDS.getOrDefault(material.substring(underscore + 1), ANYTHING_ELSE);
    }
}
