// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit;

import static italy.mlsac.net.suppression.fixture.Plans.lag;
import static italy.mlsac.net.suppression.fixture.Plans.ladder;
import static italy.mlsac.net.suppression.fixture.Plans.step;

import italy.mlsac.net.packet.HeldPacket;
import italy.mlsac.net.suppression.SuppressionKit;
import italy.mlsac.net.suppression.SuppressionPorts;
import italy.mlsac.net.suppression.fixture.FakeServer;
import italy.mlsac.net.suppression.fixture.ManualClock;
import italy.mlsac.net.suppression.fixture.ScriptedRandom;
import italy.mlsac.net.suppression.fixture.Timeline;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.server.bukkit.cadence.TickRoster;
import italy.mlsac.net.suppression.trigger.FlagEvent;
import italy.mlsac.net.suppression.trigger.SuspectOrigin;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.UUID;

final class KitBench {

    static final UUID STEVE = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-000000016fb1");
    static final int LAG_SECONDS = 30;

    final ManualClock clock = new ManualClock();
    final ScriptedRandom random = new ScriptedRandom(0.5);
    final FakeServer server = new FakeServer(new Timeline(clock));
    final TickRoster roster = new TickRoster(clock);
    final SuppressionKit<HeldPacket> kit;

    KitBench(LagLevel level, LagKind... kinds) {
        SuppressionPorts ports = new SuppressionPorts(clock, clock, random, server, server, server, server, server,
                server, server, roster, server, server);
        this.kit = new SuppressionKit<>(ports, ladder(step(1, lag(level, LAG_SECONDS, 0, kinds))));
    }

    void flag() {
        kit.engine().onFlag(new FlagEvent(new TriggerContext(STEVE, "Steve", 1, 0.97, 20.0), SuspectOrigin.PLAYER));
    }
}
