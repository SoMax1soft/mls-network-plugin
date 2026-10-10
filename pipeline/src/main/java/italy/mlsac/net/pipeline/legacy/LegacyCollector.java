// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.legacy;

import italy.mlsac.net.pipeline.environment.BackendAvailability;
import italy.mlsac.net.pipeline.environment.DetectionSwitches;
import italy.mlsac.net.pipeline.environment.LegacyWindowSink;
import italy.mlsac.net.pipeline.environment.PlayerStanding;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LegacyCollector {
    private final DetectionSwitches switches;
    private final BackendAvailability backend;
    private final PlayerStanding standing;
    private final LegacyWindowSink sink;
    private final Map<UUID, LegacyCombat> states = new ConcurrentHashMap<>();

    public LegacyCollector(DetectionSwitches switches, BackendAvailability backend, PlayerStanding standing,
            LegacyWindowSink sink) {
        this.switches = switches;
        this.backend = backend;
        this.standing = standing;
        this.sink = sink;
    }

    public void gameTick(UUID player) {
        if (!switches.detectionEnabled() || !backend.usable() || !standing.valid(player)
                || standing.exempt(player)) {
            return;
        }
        LegacyCombat state = stateOf(player);
        if (state.bedrock() || switches.ultraEnabled()) {
            return;
        }
        state.gameTick();
    }

    public void attackedPlayer(UUID attacker) {
        if (switches.ultraEnabled() || !switches.detectionEnabled() || standing.exempt(attacker)
                || standing.zoneExcluded(attacker) || !standing.valid(attacker)) {
            return;
        }
        LegacyCombat state = stateOf(attacker);
        if (state.bedrock()) {
            return;
        }
        state.attackedPlayer();
    }

    public void rotation(UUID player, float yaw, float pitch) {
        if (switches.ultraEnabled() || !switches.detectionEnabled() || !backend.usable()
                || !standing.valid(player) || standing.exempt(player)) {
            return;
        }
        LegacyCombat state = states.get(player);
        if (state == null || !state.engagedInFight() || state.bedrock() || standing.zoneExcluded(player)) {
            return;
        }
        List<LegacyDelta> window = state.rotate(yaw, pitch);
        if (!window.isEmpty() && backend.clientPresent()) {
            sink.encodeAndAccept(player, () -> LegacyWindowEncoder.encode(window));
        }
    }

    public void teleported(UUID player) {
        if (standing.animating(player) || switches.ultraEnabled() || !switches.detectionEnabled()
                || standing.exempt(player) || !standing.valid(player)) {
            return;
        }
        LegacyCombat state = states.get(player);
        if (state != null) {
            state.teleported();
        }
    }

    public boolean inFight(UUID player) {
        LegacyCombat state = states.get(player);
        return state != null && state.engagedInFight();
    }

    public List<LegacyDelta> history(UUID player) {
        LegacyCombat state = states.get(player);
        return state == null ? List.of() : state.history();
    }

    public void forget(UUID player) {
        states.remove(player);
    }

    public void forgetAll() {
        states.clear();
    }

    private LegacyCombat stateOf(UUID player) {
        LegacyCombat state = states.get(player);
        if (state == null) {
            state = new LegacyCombat(standing.bedrock(player));
            states.put(player, state);
        }
        return state;
    }
}
