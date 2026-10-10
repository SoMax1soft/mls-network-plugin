// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.delay;

import italy.mlsac.net.packet.HeldPacket;
import italy.mlsac.net.packet.PacketSender;
import italy.mlsac.net.suppression.lag.packet.DelayLag;
import italy.mlsac.net.suppression.lag.runtime.LagRuntimes;
import italy.mlsac.net.suppression.time.MonotonicClock;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HeldRelease {

    private final DelayLag<HeldPacket> delay;
    private final LagRuntimes<HeldPacket> runtimes;
    private final PacketSender sender;
    private final MonotonicClock clock;
    private final ReleaseTimer timer;
    private final Set<UUID> armed = ConcurrentHashMap.newKeySet();

    public HeldRelease(DelayLag<HeldPacket> delay, LagRuntimes<HeldPacket> runtimes, PacketSender sender,
                       MonotonicClock clock, ReleaseTimer timer) {
        this.delay = delay;
        this.runtimes = runtimes;
        this.sender = sender;
        this.clock = clock;
        this.timer = timer;
    }

    public void replay(List<HeldPacket> packets) {
        for (HeldPacket packet : packets) {
            sender.replay(packet);
        }
    }

    public void arm(UUID suspectId) {
        if (armed.add(suspectId)) {
            planNext(suspectId);
        }
    }

    private void fire(UUID suspectId) {
        if (!sender.onNetworkThread(suspectId, () -> releaseDue(suspectId))) {
            drop(delay.releaseDue(suspectId, clock.clockMillis()));
            planNext(suspectId);
        }
    }

    private void releaseDue(UUID suspectId) {
        replay(delay.releaseDue(suspectId, clock.clockMillis()));
        planNext(suspectId);
    }

    private void drop(List<HeldPacket> packets) {
        for (HeldPacket packet : packets) {
            sender.discard(packet);
        }
    }

    private void planNext(UUID suspectId) {
        while (true) {
            OptionalLong head = earliestDue(suspectId);
            if (head.isPresent()) {
                timer.after(head.getAsLong() - clock.clockMillis(), () -> fire(suspectId));
                return;
            }
            armed.remove(suspectId);
            if (earliestDue(suspectId).isEmpty() || !armed.add(suspectId)) {
                return;
            }
        }
    }

    private OptionalLong earliestDue(UUID suspectId) {
        List<Long> dues = runtimes.existing(suspectId).map(runtime -> runtime.heldPackets().dueTimes())
                .orElse(List.of());
        return dues.isEmpty() ? OptionalLong.empty() : OptionalLong.of(dues.get(0));
    }
}
