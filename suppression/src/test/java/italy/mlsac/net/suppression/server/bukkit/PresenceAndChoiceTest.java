// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.suppression.server.bukkit.motion.ViewTurn;
import italy.mlsac.net.suppression.server.bukkit.motion.ViewTurnChoice;
import italy.mlsac.net.suppression.server.bukkit.presence.GameModePauses;
import italy.mlsac.net.suppression.server.bukkit.presence.SuspectPresence;
import italy.mlsac.net.testkit.FakePlayer;
import italy.mlsac.net.testkit.TestCore;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PresenceAndChoiceTest {

    private final TestCore core = new TestCore();
    private final GameModePauses pauses = new GameModePauses();
    private final SuspectPresence presence = new SuspectPresence(core.players, core.regions, pauses);

    @Test
    void onlineFollowsThePlayerDirectory() {
        FakePlayer steve = core.players.add("Steve");
        assertTrue(presence.online(steve.id()));
        steve.goOffline();
        assertFalse(presence.online(steve.id()));
    }

    @Test
    void creativeOrSpectatorPausesEffects() {
        FakePlayer steve = core.players.add("Steve");
        assertFalse(presence.effectsPaused(steve.id()));
        pauses.pause(steve.id());
        assertTrue(presence.effectsPaused(steve.id()));
        pauses.resume(steve.id());
        assertFalse(presence.effectsPaused(steve.id()));
    }

    @Test
    void positionSyncIsTheDefaultRotateDelivery() {
        ViewTurn packet = (player, syncId, yaw, pitch) -> { };
        ViewTurn teleport = (player, syncId, yaw, pitch) -> { };
        assertSame(packet, ViewTurnChoice.standard().choose(GameVersion.of(1, 21, 4), packet, teleport, core.log));
        assertFalse(core.logSink.contains("fallback"));
    }

    @Test
    void listedVersionUsesTheTeleportFallbackAndSaysSoOnce() {
        ViewTurn packet = (player, syncId, yaw, pitch) -> { };
        ViewTurn teleport = (player, syncId, yaw, pitch) -> { };
        GameVersion server = GameVersion.of(1, 16, 5);
        assertSame(teleport, new ViewTurnChoice(Set.of(server)).choose(server, packet, teleport, core.log));
        assertTrue(core.logSink.contains("fallback"));
    }
}
