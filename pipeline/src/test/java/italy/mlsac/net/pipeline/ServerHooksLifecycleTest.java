// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.lifecycle.PlayerTick;
import italy.mlsac.net.pipeline.compensation.ViewSeed;
import italy.mlsac.net.pipeline.environment.PlayerStates;
import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.live.ServerFacts;
import italy.mlsac.net.pipeline.live.ServerHooks;
import italy.mlsac.net.pipeline.live.ServerSide;
import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.testkit.TestCore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServerHooksLifecycleTest {

    private final List<String> calls = new ArrayList<>();

    @Test
    void assemblyStartsAndStopsTheServerHooksAndRoutesPlayerTicks() {
        TestCore core = new TestCore();
        UUID player = UUID.randomUUID();
        PipelineAssembly pipeline = new PipelineAssembly(core.services(),
                new ServerSide(states(), (world, from, to) -> RayVerdict.UNKNOWN, facts(), hooks()));
        pipeline.start();
        core.bus.publish(new PlayerTick(player, 1L));
        pipeline.stop();
        core.bus.publish(new PlayerTick(player, 2L));
        assertEquals("start", calls.get(0));
        assertEquals("stop", calls.get(calls.size() - 1));
        assertEquals(1, calls.stream().filter("online"::equals).count());
    }

    private ServerHooks hooks() {
        return new ServerHooks() {
            @Override
            public void start() {
                calls.add("start");
            }

            @Override
            public void stop() {
                calls.add("stop");
            }
        };
    }

    private PlayerStates states() {
        return new PlayerStates() {
            @Override
            public List<UUID> connectedPlayers() {
                calls.add("online");
                return List.of();
            }

            @Override
            public PlayerSample sample(UUID player) {
                calls.add("sample:" + player);
                return null;
            }

            @Override
            public int serverPing(UUID player) {
                calls.add("ping:" + player);
                return -1;
            }
        };
    }

    private ServerFacts facts() {
        return new ServerFacts() {
            @Override
            public boolean valid(UUID player) {
                return false;
            }

            @Override
            public ViewSeed viewSeed(UUID player) {
                return null;
            }

            @Override
            public UUID playerByEntity(int entityNumber) {
                return null;
            }
        };
    }
}
