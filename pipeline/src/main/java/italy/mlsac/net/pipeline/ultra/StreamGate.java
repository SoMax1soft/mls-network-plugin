// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import italy.mlsac.net.console.DebugTally;
import italy.mlsac.net.pipeline.environment.DetectionSwitches;
import italy.mlsac.net.pipeline.environment.MonotonicClock;
import italy.mlsac.net.pipeline.environment.MonotonicPurpose;
import italy.mlsac.net.pipeline.environment.PlayerStanding;
import italy.mlsac.net.pipeline.turn.PlayerTurn;
import italy.mlsac.net.pipeline.turn.TurnPart;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class StreamGate {
    private static final long VERDICT_NANOS = 1_000_000_000L;

    private final DetectionSwitches switches;
    private final PlayerStanding standing;
    private final MonotonicClock clock;
    private final Map<UUID, VerdictSlot> verdicts = new ConcurrentHashMap<>();

    StreamGate(DetectionSwitches switches, PlayerStanding standing, MonotonicClock clock) {
        this.switches = switches;
        this.standing = standing;
        this.clock = clock;
    }

    boolean passes(UUID player) {
        return passes(player, false);
    }

    boolean passes(UUID player, boolean knownOnline) {
        if (!switches.ultraEnabledFor(player) || !knownOnline && !standing.valid(player)) {
            return false;
        }
        return decide(player, verdicts.get(player));
    }

    boolean passes(UUID player, PlayerTurn turn) {
        if (!switches.ultraEnabledFor(player)) {
            return false;
        }
        VerdictSlot slot = (VerdictSlot) turn.part(TurnPart.STREAM_GATE);
        if (slot == null) {
            slot = verdicts.computeIfAbsent(player, VerdictSlot::new);
            turn.keep(TurnPart.STREAM_GATE, slot);
        }
        return decide(player, slot);
    }

    private boolean decide(UUID player, VerdictSlot slot) {
        long now = clock.nanos(MonotonicPurpose.STREAM_GATE);
        Verdict cached = slot == null ? null : slot.current;
        if (cached != null && now - cached.decidedAt() < VERDICT_NANOS) {
            return cached.allowed();
        }
        String skip = standing.exempt(player) ? "bypass-permission"
                : standing.bedrock(player) ? "bedrock"
                : standing.zoneExcluded(player) ? "disabled-region" : null;
        boolean allowed = skip == null;
        if (!allowed) {
            DebugTally.count("ultra.player-skipped." + skip);
            DebugTally.trace(player, () -> "Ultra ignores the player: " + skip);
        }
        VerdictSlot target = slot != null ? slot : verdicts.computeIfAbsent(player, VerdictSlot::new);
        target.current = new Verdict(allowed, now);
        return allowed;
    }

    void forget(UUID player) {
        verdicts.remove(player);
    }

    private record Verdict(boolean allowed, long decidedAt) {
    }

    private static final class VerdictSlot {
        private volatile Verdict current;

        VerdictSlot(UUID player) {
        }
    }
}
