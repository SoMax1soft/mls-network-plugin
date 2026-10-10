// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

final class RowCounters {
    private int attacks;
    private int hitsOnPlayers;
    private int swings;
    private int slotChanges;
    private int tickEnds;

    void attack(boolean onPlayer) {
        attacks++;
        if (onPlayer) {
            hitsOnPlayers++;
        }
    }

    void swing() {
        swings++;
    }

    void slotChange() {
        slotChanges++;
    }

    void noMoveTick() {
        tickEnds++;
    }

    int airSwings() {
        return Math.max(0, swings - attacks);
    }

    int tickEnds() {
        return tickEnds;
    }

    InputCounts snapshot(int rowsAfterLastSwing) {
        return new InputCounts(attacks, hitsOnPlayers, swings, airSwings(), slotChanges, rowsAfterLastSwing);
    }

    void reset() {
        attacks = 0;
        hitsOnPlayers = 0;
        swings = 0;
        slotChanges = 0;
        tickEnds = 0;
    }
}
