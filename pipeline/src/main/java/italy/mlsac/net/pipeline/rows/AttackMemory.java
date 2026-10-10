// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import java.util.UUID;

final class AttackMemory {
    private static final int SATURATION = 255;
    private static final long VICTIM_RECALL_NANOS = 4_000_000_000L;

    private boolean hitRemembered;
    private long lastAttackTime;
    private UUID struckPlayer;
    private int rowsAfterHit;
    private int swingsAfterHit;
    private int missedSwingsAfterHit;
    private int slotSwapsAfterHit;
    private int rowsAfterLastSwing = SATURATION;
    private boolean swingRemembered;
    private long lastSwingTime;

    AttackHistory snapshot(long now) {
        return new AttackHistory(
                hitRemembered ? (now - lastAttackTime) / 1e6 : -1.0,
                hitRemembered ? rowsAfterHit : -1,
                swingsAfterHit,
                missedSwingsAfterHit,
                slotSwapsAfterHit,
                rowsAfterLastSwing,
                swingRemembered ? (now - lastSwingTime) / 1e6 : -1.0);
    }

    void attacked(long now, UUID target) {
        hitRemembered = true;
        lastAttackTime = now;
        rowsAfterHit = 0;
        swingsAfterHit = 0;
        missedSwingsAfterHit = 0;
        slotSwapsAfterHit = 0;
        struckPlayer = target;
    }

    void swung(long now) {
        swingsAfterHit++;
        rowsAfterLastSwing = 0;
        swingRemembered = true;
        lastSwingTime = now;
    }

    void slotChanged() {
        slotSwapsAfterHit++;
    }

    void airSwings(int count) {
        missedSwingsAfterHit += count;
    }

    void rowCompleted() {
        rowsAfterHit = Math.min(SATURATION, rowsAfterHit + 1);
        rowsAfterLastSwing = Math.min(SATURATION, rowsAfterLastSwing + 1);
    }

    boolean remembers(UUID candidate, long now) {
        return hitRemembered && now - lastAttackTime <= VICTIM_RECALL_NANOS && candidate.equals(struckPlayer);
    }

    void forgetTarget() {
        struckPlayer = null;
    }

    double millisSinceAttack(long now) {
        return hitRemembered ? (now - lastAttackTime) / 1e6 : -1.0;
    }

    int rowsAfterLastSwing() {
        return rowsAfterLastSwing;
    }
}
