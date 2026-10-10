// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DamageDampeningTest {

    @Test
    void everyApplicablePartIsScaled() {
        Ledger sheet = new Ledger(Map.of("BASE", 8.0, "ARMOR", -2.0, "MAGIC", -1.0), Set.of());
        DamageDampening.scale(sheet, 0.5);
        assertEquals(4.0, sheet.part("BASE"), 1e-9);
        assertEquals(-1.0, sheet.part("ARMOR"), 1e-9);
        assertEquals(2.5, sheet.finalDamage(), 1e-9);
    }

    @Test
    void theBaseIsCorrectedWhenAPartCannotBeChanged() {
        Ledger sheet = new Ledger(Map.of("BASE", 10.0, "ARMOR", -4.0), Set.of("ARMOR"));
        DamageDampening.scale(sheet, 0.5);
        assertEquals(3.0, sheet.finalDamage(), 0.01);
    }

    @Test
    void theBaseNeverGoesBelowZero() {
        Ledger sheet = new Ledger(Map.of("BASE", 1.0, "ABSORPTION", 5.0), Set.of("ABSORPTION"));
        DamageDampening.scale(sheet, 0.0);
        assertEquals(0.0, sheet.baseDamage(), 1e-9);
    }

    private static final class Ledger implements DamageSheet {

        private final Map<String, Double> parts;
        private final Set<String> locked;

        Ledger(Map<String, Double> parts, Set<String> locked) {
            this.parts = new LinkedHashMap<>(parts);
            this.locked = locked;
        }

        @Override
        public List<String> applicableParts() {
            return List.copyOf(parts.keySet());
        }

        @Override
        public double part(String name) {
            return parts.get(name);
        }

        @Override
        public void rewritePart(String name, double value) {
            if (!locked.contains(name)) {
                parts.put(name, value);
            }
        }

        @Override
        public double baseDamage() {
            return parts.get("BASE");
        }

        @Override
        public void rewriteBase(double value) {
            double old = parts.get("BASE");
            if (old != 0.0) {
                for (String name : locked) {
                    parts.put(name, parts.get(name) * value / old);
                }
            }
            parts.put("BASE", value);
        }

        @Override
        public double finalDamage() {
            return parts.values().stream().mapToDouble(Double::doubleValue).sum();
        }
    }
}
