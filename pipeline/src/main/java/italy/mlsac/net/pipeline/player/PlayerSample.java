// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.player;

import italy.mlsac.net.pipeline.geometry.Vec3;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public record PlayerSample(
        UUID player,
        UUID world,
        int entityNumber,
        long capturedAt,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        double eyeLift,
        double height,
        double width,
        double fallBlocks,
        float swingCharge,
        boolean valid,
        boolean dead,
        PlayerPosture posture,
        String handMaterial,
        Float chargeTicks,
        String playModeName,
        Double interactionRange) {

    private static final long FRESH_WINDOW_NANOS = TimeUnit.MILLISECONDS.toNanos(250);
    private static final String ITEM_UNSEEN = "unknown";

    public boolean alive() {
        return valid && !dead;
    }

    public int gameModeCode() {
        return GameModeCode.of(playModeName);
    }

    public double reach() {
        if (interactionRange != null) {
            return interactionRange;
        }
        int code = gameModeCode();
        if (code == 1) {
            return 5.0;
        }
        return code >= 0 ? 3.0 : -1.0;
    }

    public double swingsPerSecond() {
        if (chargeTicks == null) {
            return -1.0;
        }
        double period = chargeTicks.floatValue();
        if (!Double.isFinite(period) || period <= 0) {
            return -1.0;
        }
        return 20.0 / period;
    }

    public boolean freshAt(long nanos) {
        long age = nanos - capturedAt;
        return alive() && age >= 0 && age <= FRESH_WINDOW_NANOS
                && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Double.isFinite(eyeLift) && Double.isFinite(height) && Double.isFinite(width)
                && Double.isFinite(fallBlocks) && Float.isFinite(swingCharge);
    }

    public PlayerSample movedTo(double newX, double newY, double newZ) {
        return new PlayerSample(player, world, entityNumber, capturedAt, newX, newY, newZ, yaw, pitch, eyeLift, height,
                width, fallBlocks, swingCharge, valid, dead, posture, handMaterial, chargeTicks, playModeName,
                interactionRange);
    }

    public PlayerSample interpolatedAt(long nanos, double newX, double newY, double newZ) {
        return new PlayerSample(player, world, entityNumber, nanos, newX, newY, newZ, yaw, pitch, eyeLift, height, width,
                fallBlocks, swingCharge, true, false, PlayerPosture.NEUTRAL, ITEM_UNSEEN, null, null, null);
    }

    public Vec3 feet() {
        return new Vec3(x, y, z);
    }

    public Vec3 eye() {
        return new Vec3(x, y + eyeLift, z);
    }
}
