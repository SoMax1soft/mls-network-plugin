// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.pipeline.player.PlayerSample;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class SampleReading {

    static final String CAPTURE_PURPOSE = "player.sample";

    private final Clock clock;
    private final PostureReading posture;
    private final HandReading hand;
    private final ChargePeriod chargePeriod;
    private final ReachReading reach;

    public SampleReading(Clock clock, PostureReading posture, HandReading hand, ChargePeriod chargePeriod,
                         ReachReading reach) {
        this.clock = clock;
        this.posture = posture;
        this.hand = hand;
        this.chargePeriod = chargePeriod;
        this.reach = reach;
    }

    public PlayerSample read(Player player) {
        long capturedAt = clock.monotonicNanos(CAPTURE_PURPOSE);
        Location at = player.getLocation();
        World world = player.getWorld();
        GameMode mode = player.getGameMode();
        return new PlayerSample(player.getUniqueId(), world.getUID(), player.getEntityId(), capturedAt,
                at.getX(), at.getY(), at.getZ(), at.getYaw(), at.getPitch(), player.getEyeHeight(),
                player.getHeight(), player.getWidth(), player.getFallDistance(), player.getAttackCooldown(),
                player.isValid(), player.isDead(), posture.read(player), hand.material(player),
                chargePeriod.ticks(player), mode == null ? null : mode.name(), reach.reach(player));
    }
}
