// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.module.CoreServices;
import italy.mlsac.net.pipeline.live.ServerHooks;
import italy.mlsac.net.pipeline.live.ServerSide;
import java.util.List;
import org.bukkit.plugin.Plugin;

public final class BukkitServerSide {

    private final ServerSide ports;

    public BukkitServerSide(CoreServices core) {
        ServerRoster roster = new BukkitRoster(core.host().plugin().map(Plugin::getServer));
        ServerTickPace pace = new ServerTickPace(core.bus(), core.host().plugin().map(Plugin::getServer), core.log());
        boolean regionThreaded = core.platform().folia();
        ThreadOwnership ownership = regionThreaded ? new RegionThreadOwnership() : new MainThreadOwnership();
        PlaceBook places = new PlaceBook(ownership);
        RegionPatrol patrol = new RegionPatrol(core.regions(), core.clock());
        List<PlayerTickWork> works = regionThreaded ? List.of(patrol, places) : List.of(patrol);
        SampleReading reading = new SampleReading(core.clock(), PostureReading.forRunningServer(),
                new HandReading(core.log()), ChargePeriod.forRunningServer(),
                new InteractionRangeAttribute(core.log()));
        this.ports = new ServerSide(
                new BukkitPlayerStates(roster, ownership, reading, PingReading.forRunningServer(), core.log()),
                new BukkitSightLines(roster, ownership, AirReading.forRunningServer(), core.log()),
                new BukkitServerFacts(roster, core.packets().clients(), places),
                ServerHooks.inOrder(new TickedPlayers(core.bus(), roster, works), pace), pace);
    }

    public ServerSide ports() {
        return ports;
    }
}
