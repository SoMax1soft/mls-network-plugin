// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.damage;

import italy.mlsac.net.suppression.lag.hit.MeleeKind;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class DamageCauses {

    private final Set<String> takenExcluded;
    private final Set<String> dealtExcluded;
    private final Map<String, MeleeKind> melee;

    public DamageCauses(Set<String> takenExcluded, Set<String> dealtExcluded, Map<String, MeleeKind> melee) {
        this.takenExcluded = Set.copyOf(takenExcluded);
        this.dealtExcluded = Set.copyOf(dealtExcluded);
        this.melee = Map.copyOf(melee);
    }

    public static DamageCauses standard() {
        return new DamageCauses(
                Set.of("VOID", "KILL", "SUICIDE", "WORLD_BORDER", "CUSTOM", "STARVATION", "SUFFOCATION", "DROWNING",
                        "FALL", "FLY_INTO_WALL", "CRAMMING"),
                Set.of("THORNS"),
                Map.of("ENTITY_ATTACK", MeleeKind.MAIN_HIT, "ENTITY_SWEEP_ATTACK", MeleeKind.SWEEP));
    }

    public boolean takenApplies(String cause) {
        return !takenExcluded.contains(cause);
    }

    public boolean dealtApplies(String cause) {
        return !dealtExcluded.contains(cause);
    }

    public Optional<MeleeKind> meleeKind(String cause) {
        return Optional.ofNullable(melee.get(cause));
    }
}
