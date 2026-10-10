// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.packet;

import italy.mlsac.net.suppression.chance.EngineRandom;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.lag.LagLevelSource;
import italy.mlsac.net.suppression.lag.LagTable;
import italy.mlsac.net.suppression.lag.runtime.LagRuntime;
import italy.mlsac.net.suppression.lag.runtime.LagRuntimes;
import italy.mlsac.net.suppression.server.LagExemptSuspects;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DelayLag<P> {

    private final LagLevelSource levels;
    private final LagExemptSuspects exemptions;
    private final LagRuntimes<P> runtimes;
    private final LagTable table;
    private final EngineRandom random;

    public DelayLag(LagLevelSource levels, LagExemptSuspects exemptions, LagRuntimes<P> runtimes, LagTable table,
                    EngineRandom random) {
        this.levels = levels;
        this.exemptions = exemptions;
        this.runtimes = runtimes;
        this.table = table;
        this.random = random;
    }

    public HoldVerdict<P> onPacket(UUID suspectId, P packet, long receivedAt, PacketState state) {
        if (state == PacketState.CANCELLED_BY_OTHERS) {
            return HoldVerdict.pass();
        }
        if (exemptions.lagExempt(suspectId)) {
            return HoldVerdict.passAfter(releaseEverything(suspectId));
        }
        Optional<LagLevel> level = levels.lagLevels(suspectId).levelOf(LagKind.DELAY);
        if (level.isPresent()) {
            HeldPackets<P> held = runtimes.runtimeOf(suspectId).heldPackets();
            long hold = table.delay(level.get()).drawDelayMillis(random);
            return HoldVerdict.hold(held.holdDelayed(packet, receivedAt, hold));
        }
        Optional<LagRuntime<P>> runtime = runtimes.existing(suspectId);
        if (runtime.isPresent() && runtime.get().heldPackets().hasQueued()) {
            return HoldVerdict.hold(runtime.get().heldPackets().holdBehindQueue(packet));
        }
        return HoldVerdict.pass();
    }

    public List<P> releaseDue(UUID suspectId, long now) {
        if (exemptions.lagExempt(suspectId)) {
            return releaseEverything(suspectId);
        }
        return runtimes.existing(suspectId).map(runtime -> runtime.heldPackets().releaseDue(now)).orElse(List.of());
    }

    private List<P> releaseEverything(UUID suspectId) {
        return runtimes.existing(suspectId).map(runtime -> runtime.heldPackets().releaseAll()).orElse(List.of());
    }
}
