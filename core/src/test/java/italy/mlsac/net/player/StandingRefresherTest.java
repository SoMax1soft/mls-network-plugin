// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.testkit.FakePlayer;
import italy.mlsac.net.testkit.FakePlayers;
import italy.mlsac.net.testkit.ManualScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class StandingRefresherTest {

    @Test
    void aPermissionChangeRefreshesThatPlayerWithoutWaitingForTheResample() {
        ManualScheduler scheduler = new ManualScheduler();
        FakePlayers players = new FakePlayers();
        FakePlayer player = players.add("Steve");
        StandingCache standings = new StandingCache(id -> false);
        List<Consumer<UUID>> listeners = new ArrayList<>();
        List<String> closed = new ArrayList<>();
        PermissionChanges changes = changed -> {
            listeners.add(changed);
            return () -> closed.add("closed");
        };
        StandingRefresher refresher = new StandingRefresher(scheduler, players, standings, changes);
        refresher.start();
        scheduler.runPending();
        assertFalse(standings.standing(player.id()).bypass());
        player.grant(GrantNode.DETECTION_EXEMPT.node());
        listeners.get(0).accept(player.id());
        scheduler.runPending();
        assertTrue(standings.standing(player.id()).bypass());
        refresher.stop();
        assertTrue(closed.contains("closed"));
    }
}
