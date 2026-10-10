// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import italy.mlsac.net.module.CoreServices;
import italy.mlsac.net.module.FeaturePorts;
import italy.mlsac.net.module.SuppressionModule;
import italy.mlsac.net.packet.HeldPacket;
import italy.mlsac.net.player.LagExemptions;
import italy.mlsac.net.suppression.live.BackendSuppressionLedger;
import italy.mlsac.net.suppression.live.DetectionBuffers;
import italy.mlsac.net.suppression.live.EngineClocks;
import italy.mlsac.net.suppression.live.EngineEventFeed;
import italy.mlsac.net.suppression.live.FeatureCommandDispatch;
import italy.mlsac.net.suppression.live.FeatureOwnedLegacy;
import italy.mlsac.net.suppression.live.LaneThreads;
import italy.mlsac.net.suppression.live.MessageTexts;
import italy.mlsac.net.suppression.live.PlanTranslation;
import italy.mlsac.net.suppression.live.StaffNoticeRelay;
import italy.mlsac.net.suppression.live.StandingExemption;
import italy.mlsac.net.suppression.live.SuppressionMessages;
import italy.mlsac.net.suppression.live.SuppressionServer;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.route.ResponseSwitch;
import italy.mlsac.net.suppression.server.bukkit.BukkitSuppressionServer;
import italy.mlsac.net.suppression.server.bukkit.SuppressionHostParts;
import italy.mlsac.net.text.MessageDefaults;
import java.util.Objects;

public final class SuppressionAssembly implements SuppressionModule {

    private final SuppressionKit<HeldPacket> kit;
    private final SuppressionServer server;
    private final EngineEventFeed events;

    public SuppressionAssembly(CoreServices core, FeaturePorts features, LagExemptions exemptions) {
        this(core, features, exemptions, new BukkitSuppressionServer(new SuppressionHostParts(core.players(), core.regions(),
                core.scheduler(), core.packets(), new EngineClocks(core.clock(), core.random()), core.bus(),
                core.log(), core.platform(), core.host())));
    }

    public SuppressionAssembly(CoreServices core, FeaturePorts features, LagExemptions exemptions,
                               SuppressionServer server) {
        Objects.requireNonNull(core, "core");
        Objects.requireNonNull(features, "features");
        Objects.requireNonNull(exemptions, "exemptions");
        this.server = Objects.requireNonNull(server, "server");
        EngineClocks clocks = new EngineClocks(core.clock(), core.random());
        SuppressionPorts ports = new SuppressionPorts(clocks, clocks, clocks,
                new LaneThreads(core.scheduler(), core.players()), server.status(),
                new StandingExemption(core.standings()), new FeatureCommandDispatch(features.penalties()),
                new StaffNoticeRelay(features.staff(), core.notices(), core.messages(), core.log()),
                new MessageTexts(core.config(), core.messages()), server.bodies(), server.watcher(),
                server.terrain(), exemptions::exempt, new BackendSuppressionLedger(core.backend()));
        this.kit = new SuppressionKit<>(ports, SuppressionPlan.idle(), core.packets().sender()::discard);
        FeatureOwnedLegacy legacy = new FeatureOwnedLegacy();
        ResponseSwitch responses = new ResponseSwitch(kit.engine(), legacy, legacy);
        this.events = new EngineEventFeed(core.bus(), core.scheduler(), core.config(), kit.engine(), responses,
                new PlanTranslation(), new DetectionBuffers(features.detections()));
    }

    @Override
    public String name() {
        return "suppression";
    }

    @Override
    public void start() {
        events.start();
        server.start(kit);
    }

    @Override
    public void stop() {
        server.stop();
        events.stop();
    }

    @Override
    public MessageDefaults messageDefaults() {
        return SuppressionMessages.defaults();
    }
}
