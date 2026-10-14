// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.bus.Subscription;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.pipeline.integrity.ClientTickPace;
import italy.mlsac.net.pipeline.live.ServerHooks;
import java.util.Optional;
import org.bukkit.Server;

public final class ServerTickPace implements ClientTickPace, ServerHooks {

    private static final long SAMPLE_EVERY_TICKS = 20L;

    private final EventBus bus;
    private final Optional<Server> server;
    private final PluginLog log;
    private volatile double ticksPerSecond = VANILLA_TICKS_PER_SECOND;
    private volatile boolean readable;
    private Subscription sampling;

    public ServerTickPace(EventBus bus, Optional<Server> server, PluginLog log) {
        this.bus = bus;
        this.server = server;
        this.log = log;
    }

    @Override
    public double ticksPerSecond() {
        return ticksPerSecond;
    }

    @Override
    public void start() {
        readable = server.isPresent() && MethodPresence.declares(Server.class, "getServerTickManager");
        if (readable) {
            sampling = bus.subscribe(ServerTick.class, tick -> {
                if (readable && tick.tick() % SAMPLE_EVERY_TICKS == 0) {
                    sample(server.get());
                }
            });
        }
    }

    @Override
    public void stop() {
        if (sampling != null) {
            sampling.cancel();
            sampling = null;
        }
    }

    private void sample(Server running) {
        try {
            float rate = running.getServerTickManager().getTickRate();
            if (Float.isFinite(rate) && rate > 0f) {
                ticksPerSecond = rate;
            }
        } catch (RuntimeException | LinkageError unreadable) {
            readable = false;
            ticksPerSecond = VANILLA_TICKS_PER_SECOND;
            log.debug(LogTopic.PACKETS, () -> "Server tick rate is not readable, tick-end budget stays at 20 ticks/s: "
                    + unreadable);
        }
    }
}
