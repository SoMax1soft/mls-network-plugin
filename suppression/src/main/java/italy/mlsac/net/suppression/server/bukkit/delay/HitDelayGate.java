// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.delay;

import italy.mlsac.net.packet.AttackPacket;
import italy.mlsac.net.packet.HeldPacket;
import italy.mlsac.net.packet.InboundContext;
import italy.mlsac.net.packet.ListenerOrder;
import italy.mlsac.net.packet.PacketLayer;
import italy.mlsac.net.packet.Registration;
import italy.mlsac.net.packet.SwingPacket;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.packet.DelayLag;
import italy.mlsac.net.suppression.lag.packet.HoldVerdict;
import italy.mlsac.net.suppression.lag.packet.PacketDisposition;
import italy.mlsac.net.suppression.lag.packet.PacketState;
import italy.mlsac.net.suppression.lag.runtime.CombatLedger;
import italy.mlsac.net.suppression.lag.runtime.LagRuntimes;
import italy.mlsac.net.suppression.standing.EffectReadout;
import italy.mlsac.net.suppression.time.MonotonicClock;
import java.util.List;
import java.util.UUID;

public final class HitDelayGate {

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final PacketLayer packets;
    private final DelayLag<HeldPacket> delay;
    private final EffectReadout readout;
    private final LagRuntimes<HeldPacket> runtimes;
    private final CombatLedger combat;
    private final MonotonicClock clock;
    private final HeldRelease release;

    public HitDelayGate(PacketLayer packets, DelayLag<HeldPacket> delay, EffectReadout readout,
                        LagRuntimes<HeldPacket> runtimes, CombatLedger combat, MonotonicClock clock,
                        HeldRelease release) {
        this.packets = packets;
        this.delay = delay;
        this.readout = readout;
        this.runtimes = runtimes;
        this.combat = combat;
        this.clock = clock;
        this.release = release;
    }

    public List<Registration> register() {
        return List.of(
                packets.onInbound(AttackPacket.class, ListenerOrder.OBSERVE, this::attackSeen),
                packets.onInbound(SwingPacket.class, ListenerOrder.OBSERVE, this::swingSeen));
    }

    private void attackSeen(InboundContext<AttackPacket> context) {
        AttackPacket attack = context.packet();
        combat.attacked(attack.player(), clock.clockMillis());
        gate(context, attack.player(), attack.receivedNanos());
    }

    private void swingSeen(InboundContext<SwingPacket> context) {
        SwingPacket swing = context.packet();
        gate(context, swing.player(), swing.receivedNanos());
    }

    private void gate(InboundContext<?> context, UUID suspectId, long receivedNanos) {
        if (context.cancelled() || !holding(suspectId)) {
            return;
        }
        HeldPacket held = context.hold();
        HoldVerdict<HeldPacket> verdict = delay.onPacket(suspectId, held, receivedNanos / NANOS_PER_MILLI,
                PacketState.LIVE);
        release.replay(verdict.releaseNow());
        if (verdict.disposition() == PacketDisposition.PASS) {
            release.replay(List.of(held));
            return;
        }
        release.arm(suspectId);
    }

    private boolean holding(UUID suspectId) {
        return readout.lagLevels(suspectId).active(LagKind.DELAY)
                || runtimes.existing(suspectId).map(runtime -> runtime.heldPackets().hasQueued()).orElse(false);
    }
}
