// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.integrity;

import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.bus.Subscription;
import italy.mlsac.net.config.integrity.BadPacketSettings;
import italy.mlsac.net.console.DebugTally;
import italy.mlsac.net.detection.BadPacketKind;
import italy.mlsac.net.detection.BadPacketObserved;
import italy.mlsac.net.packet.ClientTickEndPacket;
import italy.mlsac.net.packet.InboundContext;
import italy.mlsac.net.packet.ListenerOrder;
import italy.mlsac.net.packet.PacketLayer;
import italy.mlsac.net.packet.PlayerPositionPacket;
import italy.mlsac.net.packet.Registration;
import italy.mlsac.net.packet.TeleportConfirmPacket;
import italy.mlsac.net.packet.ViewResetPacket;
import italy.mlsac.net.pipeline.environment.PlayerStanding;
import italy.mlsac.net.player.PlayerLeft;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class PacketIntegrityGuard {

    private final PacketLayer packets;
    private final EventBus bus;
    private final Supplier<BadPacketSettings> settings;
    private final PlayerStanding standing;
    private final ClientTickPace pace;
    private final LongSupplier clock;
    private final TeleportHistory teleports = new TeleportHistory();
    private final TickEndBudget tickEnds = new TickEndBudget();
    private final List<Registration> registrations = new ArrayList<>();
    private Subscription departure;

    public PacketIntegrityGuard(PacketLayer packets, EventBus bus, Supplier<BadPacketSettings> settings,
                                PlayerStanding standing, ClientTickPace pace, LongSupplier clock) {
        this.packets = packets;
        this.bus = bus;
        this.settings = settings;
        this.standing = standing;
        this.pace = pace;
        this.clock = clock;
    }

    public void start() {
        departure = bus.subscribe(PlayerLeft.class, left -> forget(left.player()));
        registrations.add(packets.onOutbound(PlayerPositionPacket.class, ListenerOrder.OBSERVE, context -> {
            if (!context.cancelled()) {
                teleports.issued(context.packet().viewer(), context.packet().teleportId());
            }
        }));
        registrations.add(packets.onOutbound(ViewResetPacket.class, ListenerOrder.OBSERVE, context -> {
            if (!context.cancelled()) {
                teleports.viewReset(context.packet().viewer(), clock.getAsLong());
            }
        }));
        registrations.add(packets.onInbound(TeleportConfirmPacket.class, ListenerOrder.EARLY, this::teleportConfirm));
        registrations.add(packets.onInbound(ClientTickEndPacket.class, ListenerOrder.EARLY, this::tickEnd));
    }

    public void stop() {
        registrations.forEach(Registration::unregister);
        registrations.clear();
        if (departure != null) {
            departure.cancel();
            departure = null;
        }
        teleports.clear();
        tickEnds.clear();
    }

    private void teleportConfirm(InboundContext<TeleportConfirmPacket> context) {
        TeleportConfirmPacket packet = context.packet();
        if (context.cancelled() || !watched(packet.player())) {
            return;
        }
        ConfirmJudgement judgement = teleports.judge(packet.player(), packet.warpSequence(), clock.getAsLong());
        if (judgement == ConfirmJudgement.UNVERIFIABLE) {
            DebugTally.count("in.teleport-confirm-unverifiable");
        } else if (judgement == ConfirmJudgement.FORGED) {
            context.cancel();
            report(packet.player(), BadPacketKind.FORGED_TELEPORT_CONFIRM);
        }
    }

    private void tickEnd(InboundContext<ClientTickEndPacket> context) {
        ClientTickEndPacket packet = context.packet();
        if (context.cancelled() || !watched(packet.player())) {
            return;
        }
        BadPacketSettings current = settings.get();
        TickEndSpend spend = tickEnds.spend(packet.player(), packet.receivedNanos(),
                TickEndAllowance.scaled(current.tickEndsPerSecond(), current.tickEndBurst(), pace.ticksPerSecond()));
        if (spend == TickEndSpend.WITHIN_BUDGET) {
            return;
        }
        context.cancel();
        DebugTally.count("drop.tick-end-over-budget");
        if (spend == TickEndSpend.EXCESS_REPORTED) {
            report(packet.player(), BadPacketKind.TICK_END_SPAM);
        }
    }

    private boolean watched(UUID player) {
        return settings.get().enabled() && !standing.exempt(player) && !standing.bedrock(player)
                && !standing.zoneExcluded(player);
    }

    private void report(UUID player, BadPacketKind kind) {
        DebugTally.count("badpackets." + kind.slug());
        bus.publish(new BadPacketObserved(player, kind));
    }

    private void forget(UUID player) {
        teleports.forget(player);
        tickEnds.forget(player);
    }
}
