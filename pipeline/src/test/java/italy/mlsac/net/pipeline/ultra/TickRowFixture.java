// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import italy.mlsac.net.pipeline.compensation.LensMode;
import italy.mlsac.net.pipeline.compensation.LensReading;
import italy.mlsac.net.pipeline.compensation.TargetMeasurement;
import italy.mlsac.net.pipeline.geometry.AimRelation;
import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.HitMeasurement;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.occlusion.OcclusionCell;
import italy.mlsac.net.pipeline.rotation.RotationStep;
import italy.mlsac.net.pipeline.rows.InputCounts;
import italy.mlsac.net.pipeline.rows.MoveDelta;
import italy.mlsac.net.pipeline.rows.OnTarget;
import italy.mlsac.net.pipeline.rows.PacketTraits;
import italy.mlsac.net.pipeline.rows.RowTiming;
import italy.mlsac.net.pipeline.rows.SelfState;
import italy.mlsac.net.pipeline.rows.TargetView;
import italy.mlsac.net.pipeline.rows.TickRow;

final class TickRowFixture {
    static final Box TIGHT = Box.player(0.0, 64.0, 2.5, 0.6, 1.8);
    private static final long TICK_NANOS = 50_000_000L;

    private final long index;
    private boolean packetMissing;
    private int playerHits;
    private OcclusionCell cell = new OcclusionCell();
    private boolean fighting = true;

    private TickRowFixture(long index) {
        this.index = index;
    }

    static TickRowFixture at(long index) {
        return new TickRowFixture(index);
    }

    TickRowFixture withoutPacket() {
        packetMissing = true;
        return this;
    }

    TickRowFixture hits(int count) {
        playerHits = count;
        return this;
    }

    TickRowFixture occlusion(OcclusionCell shared) {
        cell = shared;
        return this;
    }

    TickRowFixture outsideFight() {
        fighting = false;
        return this;
    }

    TickRow build() {
        LensReading lens = new LensReading(LensMode.TRANSACTIONS, 50.0, 0, TIGHT, TIGHT);
        HitMeasurement hit = new HitMeasurement(true, true, true, 9, true, Vec3.ZERO, 0.0, 0.1, 0.1, 0.1, 0.0, 1.0);
        RowTiming timing = new RowTiming(index, 0, index, index * TICK_NANOS, 50.0, -1, fighting, 0, 0.0);
        PacketTraits traits = new PacketTraits(!packetMissing, !packetMissing, true, false, false, packetMissing, 0f,
                0f);
        InputCounts inputs = new InputCounts(playerHits, playerHits, playerHits, 0, 0, 0);
        return new TickRow(timing, traits, RotationStep.ABSENT, new MoveDelta(true, 0.1, 0.0, 0.0), inputs,
                SelfState.of(null), new TargetView(2, false, 50.0, 1), new TargetMeasurement(lens, hit), cell,
                new AimRelation(1.0, 0.5, 0.2, new Vec3(0.0, 0.0, 2.5)), new OnTarget(1, 0.0));
    }
}
