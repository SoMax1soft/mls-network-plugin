// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit;

import static italy.mlsac.net.suppression.server.bukkit.KitBench.STEVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.lifecycle.PlayerTick;
import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.player.PlayerChangedWorld;
import italy.mlsac.net.player.PlayerDied;
import italy.mlsac.net.player.PlayerLeft;
import italy.mlsac.net.player.PlayerRespawned;
import italy.mlsac.net.player.PlayerTeleported;
import italy.mlsac.net.suppression.lag.runtime.LagPauses;
import italy.mlsac.net.suppression.lag.runtime.LagRuntime;
import italy.mlsac.net.suppression.lag.runtime.LagRuntimes;
import italy.mlsac.net.suppression.server.bukkit.cadence.LagLifeHooks;
import italy.mlsac.net.suppression.server.bukkit.cadence.ObservedTicks;
import italy.mlsac.net.suppression.server.bukkit.cadence.OwnTeleports;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LagLifeHooksTest {

    private final LagRuntimes<String> runtimes = new LagRuntimes<>();
    private final ObservedTicks ticks = new ObservedTicks();
    private final OwnTeleports ownTeleports = new OwnTeleports();
    private final LagLifeHooks hooks = new LagLifeHooks(runtimes, ticks, ownTeleports);
    private final LagRuntime<String> runtime = runtimes.runtimeOf(STEVE);

    @Test
    void ticksFollowTheServerOrThePlayersOwnCadence() {
        ticks.serverTicked(new ServerTick(10L));
        assertEquals(11L, ticks.observedTickOf(STEVE));
        ticks.playerTicked(new PlayerTick(STEVE, 4L));
        assertEquals(5L, ticks.observedTickOf(STEVE));
        assertEquals(11L, ticks.observedTickOf(UUID.randomUUID()));
        ticks.playerLeft(new PlayerLeft(STEVE, "Steve", false));
        assertEquals(11L, ticks.observedTickOf(STEVE));
    }

    @Test
    void foreignTeleportPausesMovementEffects() {
        ticks.serverTicked(new ServerTick(240L));
        hooks.teleported(new PlayerTeleported(STEVE, "COMMAND"));
        assertTrue(runtime.pauses().movementPaused(241L));
        assertTrue(runtime.pauses().movementPaused(241L + LagPauses.AFTER_MOVE_TICKS - 1));
        assertFalse(runtime.pauses().movementPaused(241L + LagPauses.AFTER_MOVE_TICKS));
    }

    @Test
    void ownTeleportDoesNotPause() {
        ticks.serverTicked(new ServerTick(240L));
        ownTeleports.begin(STEVE);
        hooks.teleported(new PlayerTeleported(STEVE, "PLUGIN"));
        ownTeleports.finish(STEVE);
        assertFalse(runtime.pauses().movementPaused(241L));
    }

    @Test
    void everyTeleportPausesGroundPulsesForFiveTicks() {
        ticks.serverTicked(new ServerTick(240L));
        ownTeleports.begin(STEVE);
        hooks.teleported(new PlayerTeleported(STEVE, "PLUGIN"));
        ownTeleports.finish(STEVE);
        assertTrue(runtime.pauses().groundBlocked(241L + LagPauses.AFTER_TELEPORT_TICKS));
        assertFalse(runtime.pauses().groundBlocked(242L + LagPauses.AFTER_TELEPORT_TICKS));
    }

    @Test
    void nestedOwnTeleportsStayMarkedUntilTheLastFinishes() {
        ownTeleports.begin(STEVE);
        ownTeleports.begin(STEVE);
        ownTeleports.finish(STEVE);
        assertTrue(ownTeleports.underway(STEVE));
        ownTeleports.finish(STEVE);
        assertFalse(ownTeleports.underway(STEVE));
    }

    @Test
    void deathRespawnAndWorldChangeDiscardHeldPackets() {
        runtime.heldPackets().holdDelayed("swing", 0L, 500L);
        hooks.died(new PlayerDied(STEVE));
        assertTrue(runtime.pauses().dead());
        assertFalse(runtime.heldPackets().hasQueued());

        runtime.heldPackets().holdDelayed("attack", 0L, 500L);
        hooks.respawned(new PlayerRespawned(STEVE));
        assertFalse(runtime.pauses().dead());
        assertFalse(runtime.heldPackets().hasQueued());

        runtime.heldPackets().holdDelayed("attack", 0L, 500L);
        hooks.worldSwitched(new PlayerChangedWorld(STEVE, UUID.randomUUID()));
        assertFalse(runtime.heldPackets().hasQueued());
    }

    @Test
    void everyDiscardHandsTheDroppedPacketsToTheDisposal() {
        List<String> freed = new ArrayList<>();
        LagRuntimes<String> disposing = new LagRuntimes<>(freed::add);
        LagLifeHooks disposingHooks = new LagLifeHooks(disposing, ticks, ownTeleports);
        disposing.runtimeOf(STEVE).heldPackets().holdDelayed("swing", 0L, 500L);
        disposing.runtimeOf(STEVE).heldPackets().holdDelayed("attack", 0L, 500L);
        disposingHooks.died(new PlayerDied(STEVE));
        disposing.runtimeOf(STEVE).heldPackets().holdDelayed("late", 0L, 500L);
        assertEquals(1, disposing.quit(STEVE));
        assertEquals(List.of("swing", "attack", "late"), freed);
    }
}
