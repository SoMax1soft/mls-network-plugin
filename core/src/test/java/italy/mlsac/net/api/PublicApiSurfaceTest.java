// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.api.bukkit.ModelCheckEvent;
import italy.mlsac.net.api.bukkit.PlayerModelChangeEvent;
import italy.mlsac.net.api.bukkit.UltraFlagEvent;
import italy.mlsac.net.detection.choice.LegacyCheckObserved;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;

class PublicApiSurfaceTest {

    private static final UUID PLAYER = new UUID(7, 7);
    private static final List<Class<?>> WITHOUT_ANY_SCORE = List.of(UltraFlagEvent.class,
            PlayerModelChangeEvent.class, ModelSelection.class, MlsacModelSelection.class);

    @Test
    void noUltraOrSelectionTypeCarriesAScore() {
        for (Class<?> type : WITHOUT_ANY_SCORE) {
            for (Method method : type.getMethods()) {
                if (method.getDeclaringClass() == Object.class || method.getDeclaringClass() == Event.class) {
                    continue;
                }
                String name = method.getName().toLowerCase(Locale.ROOT);
                assertFalse(name.contains("probab") || name.contains("buffer") || name.contains("score"),
                        type.getSimpleName() + "." + method.getName());
                assertFalse(method.getReturnType() == double.class || method.getReturnType() == float.class
                        || method.getReturnType() == Double.class, type.getSimpleName() + "." + method.getName());
            }
        }
        for (RecordComponent component : ModelSelection.class.getRecordComponents()) {
            assertFalse(component.getType() == double.class, component.getName());
        }
    }

    @Test
    void ultraFlagEventsHoldOnlyThePlayer() {
        long instanceFields = java.util.Arrays.stream(UltraFlagEvent.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers())).count();
        assertEquals(2, instanceFields);
        UltraFlagEvent flag = new UltraFlagEvent(PLAYER, "Aura_00");
        assertEquals(PLAYER, flag.getPlayerId());
        assertTrue(flag.isAsynchronous());
    }

    @Test
    void checkResultsRefuseUltraAndTheServerDefault() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModelCheckEvent(PLAYER, "Aura_00", ModelChoice.ULTRA, 0.97, 40.0, true));
        assertThrows(IllegalArgumentException.class,
                () -> new ModelCheckEvent(PLAYER, "Aura_00", ModelChoice.DEFAULT, 0.97, 40.0, true));
        assertThrows(IllegalArgumentException.class,
                () -> new LegacyCheckObserved(PLAYER, "Aura_00", ModelChoice.ULTRA, 0.97, 40.0, true));
        ModelCheckEvent pro = new ModelCheckEvent(PLAYER, "Aura_00", ModelChoice.PRO, 0.91, 32.5, false);
        assertEquals(0.91, pro.getProbability());
        assertEquals(32.5, pro.getBuffer());
        assertFalse(pro.isFlagged());
        assertTrue(pro.isAsynchronous());
    }

    @Test
    void everyApiEventHasItsOwnHandlerList() {
        assertTrue(ModelCheckEvent.getHandlerList() != UltraFlagEvent.getHandlerList());
        assertTrue(PlayerModelChangeEvent.getHandlerList() != UltraFlagEvent.getHandlerList());
        PlayerModelChangeEvent change = new PlayerModelChangeEvent(ModelSelection.serverDefault(PLAYER),
                ModelSelection.granted(PLAYER, ModelChoice.ULTRA));
        assertEquals(PLAYER, change.getPlayerId());
        assertEquals(ModelChoice.ULTRA, change.getCurrent().effective());
    }
}
