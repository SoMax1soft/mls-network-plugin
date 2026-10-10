// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StandingCache {

    public static final long RESAMPLE_TICKS = 100L;

    private final Map<UUID, PlayerStanding> standings = new ConcurrentHashMap<>();
    private final BedrockDetector bedrock;
    private final PermissionAuthority permissions;

    public StandingCache(BedrockDetector bedrock) {
        this(bedrock, PermissionAuthority.ALWAYS);
    }

    public StandingCache(BedrockDetector bedrock, PermissionAuthority permissions) {
        this.bedrock = bedrock;
        this.permissions = permissions;
    }

    public PlayerStanding standing(UUID player) {
        return standings.getOrDefault(player, PlayerStanding.UNKNOWN);
    }

    public void refresh(PlayerView player) {
        boolean exemptionsApply = permissions.installed();
        standings.put(player.id(), new PlayerStanding(
                exemptionsApply && player.hasPermission(GrantNode.DETECTION_EXEMPT.node()),
                exemptionsApply && player.hasPermission(GrantNode.SUPPRESSION_EXEMPT.node()),
                Audience.STAFF.test(player),
                Audience.ADMIN_OR_OP.test(player),
                bedrock.bedrockClient(player.id())));
    }

    public void forget(UUID player) {
        standings.remove(player);
    }

    public void clear() {
        standings.clear();
    }
}
