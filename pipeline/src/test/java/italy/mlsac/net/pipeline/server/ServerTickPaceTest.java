// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.pipeline.integrity.ClientTickPace;
import italy.mlsac.net.testkit.TestCore;
import java.lang.reflect.Proxy;
import java.util.Optional;
import org.bukkit.Server;
import org.bukkit.ServerTickManager;
import org.junit.jupiter.api.Test;

class ServerTickPaceTest {

    private final TestCore core = new TestCore();
    private float rate = 20f;
    private boolean broken;

    @Test
    void followsTheServerTickRateOnTheMainThreadSamples() {
        ServerTickPace pace = new ServerTickPace(core.bus, Optional.of(server()), core.log);
        pace.start();
        rate = 40f;
        core.bus.publish(new ServerTick(19));
        assertEquals(ClientTickPace.VANILLA_TICKS_PER_SECOND, pace.ticksPerSecond());
        core.bus.publish(new ServerTick(20));
        assertEquals(40.0, pace.ticksPerSecond());
        rate = Float.NaN;
        core.bus.publish(new ServerTick(40));
        assertEquals(40.0, pace.ticksPerSecond());
        pace.stop();
        rate = 60f;
        core.bus.publish(new ServerTick(60));
        assertEquals(40.0, pace.ticksPerSecond());
    }

    @Test
    void unreadableTickManagerFallsBackToVanillaForGood() {
        ServerTickPace pace = new ServerTickPace(core.bus, Optional.of(server()), core.log);
        pace.start();
        rate = 50f;
        core.bus.publish(new ServerTick(20));
        broken = true;
        core.bus.publish(new ServerTick(40));
        assertEquals(ClientTickPace.VANILLA_TICKS_PER_SECOND, pace.ticksPerSecond());
        broken = false;
        core.bus.publish(new ServerTick(60));
        assertEquals(ClientTickPace.VANILLA_TICKS_PER_SECOND, pace.ticksPerSecond());
        pace.stop();
    }

    @Test
    void withoutAServerThePaceIsVanilla() {
        ServerTickPace pace = new ServerTickPace(core.bus, Optional.empty(), core.log);
        pace.start();
        core.bus.publish(new ServerTick(20));
        assertEquals(ClientTickPace.VANILLA_TICKS_PER_SECOND, pace.ticksPerSecond());
        pace.stop();
    }

    private Server server() {
        ServerTickManager manager = (ServerTickManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {ServerTickManager.class}, (proxy, method, args) -> {
                    if (broken) {
                        throw new UnsupportedOperationException("tick manager");
                    }
                    return method.getName().equals("getTickRate") ? rate : null;
                });
        return (Server) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Server.class},
                (proxy, method, args) -> method.getName().equals("getServerTickManager") ? manager : null);
    }
}
