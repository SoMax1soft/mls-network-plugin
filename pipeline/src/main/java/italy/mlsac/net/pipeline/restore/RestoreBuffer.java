// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

import italy.mlsac.net.pipeline.compensation.ClientSightRewind;
import italy.mlsac.net.pipeline.engine.InputTap;
import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.occlusion.MarginOcclusion;
import italy.mlsac.net.pipeline.occlusion.OcclusionProbe;
import italy.mlsac.net.pipeline.occlusion.OcclusionQueue;
import italy.mlsac.net.pipeline.player.ServerSamples;
import italy.mlsac.net.pipeline.rows.NoPacketRowInput;
import italy.mlsac.net.pipeline.rows.RowProducer;
import java.time.Instant;
import java.util.UUID;

final class RestoreBuffer implements InputTap, ChunkClaim {
    static final int TICK_ROWS = 5000;
    static final int HIT_ROWS = 5000;
    static final int CHUNK_ROWS = 250;

    private final UUID player;
    private final UUID sessionKey;
    private final Instant beganAt;
    private final RowProducer producer;
    private final KeptRing kept;
    private final BufferBudget budget;
    private volatile long lastFightNanos;
    private boolean retired;
    private long claimedBytes;

    RestoreBuffer(UUID player, UUID sessionKey, Instant beganAt, long originNanos, ServerSamples samples,
            ClientSightRewind lens, OcclusionProbe probe, BufferBudget budget) {
        this.player = player;
        this.sessionKey = sessionKey;
        this.beganAt = beganAt;
        this.budget = budget;
        this.lastFightNanos = originNanos;
        this.kept = new KeptRing(new RowRing(TICK_ROWS, CHUNK_ROWS), new RowRing(HIT_ROWS, CHUNK_ROWS), this);
        this.producer = new RowProducer(player, originNanos, kept, samples, lens,
                new OcclusionQueue(new MarginOcclusion(probe)), NoPacketRowInput.CONSUMED);
    }

    UUID player() {
        return player;
    }

    long lastFightNanos() {
        return lastFightNanos;
    }

    boolean retired() {
        return retired;
    }

    void retire() {
        retired = true;
    }

    long claimedBytes() {
        return claimedBytes;
    }

    void addClaimed(long bytes) {
        claimedBytes += bytes;
    }

    long takeClaimed() {
        long bytes = claimedBytes;
        claimedBytes = 0;
        return bytes;
    }

    int keptTicks() {
        return kept.ticks().size();
    }

    int keptHits() {
        return kept.hits().size();
    }

    @Override
    public boolean claim(long bytes) {
        return budget.claim(this, bytes);
    }

    @Override
    public void movement(MovementPacket packet) {
        producer.movement(packet);
    }

    @Override
    public void noMoveTick(long nanos) {
        producer.noMoveTick(nanos);
    }

    @Override
    public void teleportConfirmed() {
        producer.teleportConfirmed();
    }

    @Override
    public void swing(long nanos) {
        producer.swing(nanos);
    }

    @Override
    public void slotChanged() {
        producer.slotChanged();
    }

    @Override
    public void attack(AttackPacket packet) {
        producer.attack(packet);
        lastFightNanos = packet.nanos();
    }

    @Override
    public void gameTick() {
        producer.gameTick();
        kept.settle();
    }

    @Override
    public void discontinuity() {
        producer.discontinuity();
    }

    @Override
    public void sampleDiscontinuity() {
    }

    RestoreContent finish() {
        producer.finish();
        kept.settle();
        return new RestoreContent(player, sessionKey, beganAt, kept.ticks(), kept.hits());
    }

    void release() {
        kept.release();
    }
}
