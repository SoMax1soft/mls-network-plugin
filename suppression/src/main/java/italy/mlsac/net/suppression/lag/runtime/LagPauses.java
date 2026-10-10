// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.runtime;

public final class LagPauses {

    public static final int AFTER_RESPAWN_TICKS = 60;
    public static final int AFTER_MOVE_TICKS = 60;
    public static final int AFTER_KNOCKBACK_TICKS = 10;
    public static final int AFTER_LAUNCH_TICKS = 30;
    public static final int AFTER_TELEPORT_TICKS = 5;
    private static final long NEVER = Long.MIN_VALUE / 2;

    private boolean awaitingRespawn;
    private long pausedUntilTick = NEVER;
    private long lastKnockbackTick = NEVER;
    private long lastLaunchTick = NEVER;
    private long lastTeleportTick = NEVER;
    private boolean launchPending;

    public synchronized void died() {
        awaitingRespawn = true;
    }

    public synchronized void respawned(long tick) {
        awaitingRespawn = false;
        pauseFor(tick, AFTER_RESPAWN_TICKS);
    }

    public synchronized void moved(long tick) {
        pauseFor(tick, AFTER_MOVE_TICKS);
    }

    public synchronized void knockedBack(long tick) {
        lastKnockbackTick = tick;
    }

    public synchronized void teleported(long tick) {
        lastTeleportTick = Math.max(lastTeleportTick, tick);
    }

    public synchronized void launched() {
        launchPending = true;
    }

    public synchronized void settle(long tick) {
        if (launchPending) {
            launchPending = false;
            lastLaunchTick = tick;
        }
    }

    public synchronized boolean dead() {
        return awaitingRespawn;
    }

    public synchronized boolean movementPaused(long tick) {
        return awaitingRespawn || tick < pausedUntilTick;
    }

    public synchronized boolean groundBlocked(long tick) {
        return tick - lastKnockbackTick < AFTER_KNOCKBACK_TICKS || tick - lastLaunchTick < AFTER_LAUNCH_TICKS
                || tick - lastTeleportTick <= AFTER_TELEPORT_TICKS || launchPending;
    }

    private void pauseFor(long tick, int ticks) {
        pausedUntilTick = Math.max(pausedUntilTick, tick + ticks);
    }
}
