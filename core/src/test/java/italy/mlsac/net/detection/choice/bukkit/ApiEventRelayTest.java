// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.api.ModelChoice;
import italy.mlsac.net.api.ModelSelection;
import italy.mlsac.net.api.bukkit.ModelCheckEvent;
import italy.mlsac.net.api.bukkit.PlayerModelChangeEvent;
import italy.mlsac.net.api.bukkit.UltraFlagEvent;
import italy.mlsac.net.detection.choice.LegacyCheckObserved;
import italy.mlsac.net.detection.choice.UltraFlagObserved;
import italy.mlsac.net.testkit.TestCore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;

class ApiEventRelayTest {

    private static final UUID PLAYER = new UUID(9, 9);

    private final TestCore core = new TestCore();
    private final List<Event> fired = new ArrayList<>();
    private final ApiEventRelay relay = new ApiEventRelay(core.bus, fired::add);

    @Test
    void busRecordsBecomeTheMatchingApiEvents() {
        relay.start();
        core.selections.set(PLAYER, ModelChoice.PRO);
        core.bus.publish(new LegacyCheckObserved(PLAYER, "Aura_00", ModelChoice.PRO, 0.93, 41.0, true));
        core.bus.publish(new UltraFlagObserved(PLAYER, "Aura_00"));

        assertEquals(3, fired.size());
        PlayerModelChangeEvent change = assertInstanceOf(PlayerModelChangeEvent.class, fired.get(0));
        assertEquals(ModelSelection.serverDefault(PLAYER), change.getPrevious());
        assertEquals(ModelSelection.granted(PLAYER, ModelChoice.PRO), change.getCurrent());
        ModelCheckEvent check = assertInstanceOf(ModelCheckEvent.class, fired.get(1));
        assertEquals(ModelChoice.PRO, check.getModel());
        assertEquals(0.93, check.getProbability());
        assertEquals(41.0, check.getBuffer());
        assertTrue(check.isFlagged());
        UltraFlagEvent flag = assertInstanceOf(UltraFlagEvent.class, fired.get(2));
        assertEquals("Aura_00", flag.getPlayerName());
    }

    @Test
    void nothingIsFiredOnceStopped() {
        relay.start();
        relay.stop();
        core.bus.publish(new UltraFlagObserved(PLAYER, "Aura_00"));
        core.selections.set(PLAYER, ModelChoice.ULTRA);
        assertTrue(fired.isEmpty());
    }
}
