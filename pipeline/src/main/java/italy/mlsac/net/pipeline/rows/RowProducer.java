// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import italy.mlsac.net.pipeline.compensation.ClientSightRewind;
import italy.mlsac.net.pipeline.compensation.TargetMeasurement;
import italy.mlsac.net.pipeline.geometry.AimRelation;
import italy.mlsac.net.pipeline.geometry.LookPair;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.occlusion.OcclusionCell;
import italy.mlsac.net.pipeline.occlusion.WallCheckRequest;
import italy.mlsac.net.pipeline.occlusion.OcclusionKind;
import italy.mlsac.net.pipeline.occlusion.OcclusionQueue;
import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.pipeline.player.ServerSamples;
import italy.mlsac.net.pipeline.rotation.RotationChain;
import italy.mlsac.net.pipeline.rotation.RotationStep;
import java.util.UUID;

public final class RowProducer {
    private static final long SEGMENT_GAP_NANOS = 1_000_000_000L;
    private static final double JUMP_DISTANCE = 8.0;
    private static final int COMBAT_TICKS = 80;
    private static final int SATURATION = 255;

    private final UUID player;
    private final long start;
    private final RowSink sink;
    private final ServerSamples samples;
    private final ClientSightRewind lens;
    private final OcclusionQueue occlusion;
    private final ClickResolver clicks;
    private final RotationChain chain = new RotationChain();
    private final AttackMemory memory = new AttackMemory();
    private final AimedTargetSearch targetSearch = new AimedTargetSearch(memory);
    private final CrosshairHoldClock view = new CrosshairHoldClock();
    private final RowCounters counters = new RowCounters();
    private long gameTick;
    private long nextRow;
    private long segmentOrdinal;
    private long lastRowTime;
    private boolean rowWritten;
    private boolean newSegmentRequested;
    private int ticksSincePlayerAttack = SATURATION;
    private boolean lookKnown;
    private float yaw;
    private float pitch;
    private Vec3 position;
    private boolean echoMarker;
    private boolean groundedBefore;
    private boolean endMarkersSeen;
    private boolean movementSinceTickEnd = true;

    public RowProducer(UUID player, long start, RowSink sink, ServerSamples samples, ClientSightRewind lens,
            OcclusionQueue occlusion) {
        this.player = player;
        this.start = start;
        this.sink = sink;
        this.samples = samples;
        this.lens = lens;
        this.occlusion = occlusion;
        this.clicks = new ClickResolver(player, start, lens, occlusion, sink);
    }

    public void gameTick() {
        gameTick++;
        ticksSincePlayerAttack = Math.min(SATURATION, ticksSincePlayerAttack + 1);
        occlusion.runAll();
    }

    public void movement(MovementPacket packet) {
        movementSinceTickEnd = true;
        makeRow(packet, RowOrigin.MOVEMENT_PACKET);
    }

    public void noMoveTick(long nanos) {
        endMarkersSeen = true;
        if (!movementSinceTickEnd && rowWritten && lookKnown) {
            makeRow(MovementPacket.noMoveTick(player, nanos, yaw, pitch, groundedBefore), RowOrigin.TICK_END);
        }
        movementSinceTickEnd = false;
        counters.noMoveTick();
    }

    public void teleportConfirmed() {
        echoMarker = true;
    }

    public void swing(long nanos) {
        counters.swing();
        memory.swung(nanos);
    }

    public void slotChanged() {
        counters.slotChange();
        memory.slotChanged();
    }

    public void attack(AttackPacket packet) {
        long nanos = packet.nanos();
        if (nanos < start) {
            return;
        }
        PlayerSample target = samples.board().byEntity(packet.entityNumber());
        PlayerSample current = samples.current(player);
        PlayerSample own = current != null && current.freshAt(nanos) ? current : null;
        clicks.add(new PendingClick(nanos, packet.entityNumber(), packet.cancelled(), target, own, position,
                groundedBefore, memory.snapshot(nanos)));
        if (target != null && !engagedInFight()) {
            sink.fightStarted();
        }
        if (target != null) {
            ticksSincePlayerAttack = 0;
        }
        counters.attack(target != null);
        memory.attacked(nanos, target == null ? null : target.player());
    }

    public void discontinuity() {
        segmentBreak();
        lookKnown = false;
        position = null;
        memory.forgetTarget();
        ticksSincePlayerAttack = SATURATION;
    }

    public void finish() {
        clicks.resolveWithoutRow(segmentOrdinal, gameTick, currentLook(), yaw, pitch);
        occlusion.abandon();
    }

    private LookPair currentLook() {
        return lookKnown ? new LookPair(yaw, pitch, yaw, pitch) : null;
    }

    private boolean engagedInFight() {
        return ticksSincePlayerAttack <= COMBAT_TICKS;
    }

    private void segmentBreak() {
        clicks.resolveWithoutRow(segmentOrdinal, gameTick, currentLook(), yaw, pitch);
        newSegmentRequested = true;
        chain.severRotation();
        sink.segmentBroken();
        view.forget();
        echoMarker = false;
    }

    private void makeRow(MovementPacket packet, RowOrigin origin) {
        long now = packet.nanos();
        if (now < start) {
            return;
        }
        if (!packet.finite()) {
            discontinuity();
            return;
        }
        boolean echo = echoMarker && packet.carriesRotation() && !origin.noPacket();
        boolean jumped = position != null && packet.carriesPosition()
                && Math.abs(packet.x() - position.x()) + Math.abs(packet.y() - position.y())
                        + Math.abs(packet.z() - position.z()) > JUMP_DISTANCE;
        if (echo || jumped) {
            segmentBreak();
            position = null;
        }
        if (rowWritten && (now < lastRowTime || now - lastRowTime > SEGMENT_GAP_NANOS)) {
            segmentBreak();
        }
        boolean rowExisted = rowWritten;
        boolean newSegment = !rowWritten || newSegmentRequested;
        if (newSegmentRequested) {
            if (rowWritten) {
                segmentOrdinal++;
            }
            newSegmentRequested = false;
        }
        double dtMillis = newSegment ? 0.0 : (now - lastRowTime) / 1e6;
        PlayerSample me = samples.current(player);
        boolean selfFresh = me != null && me.freshAt(now);
        LookUpdate lookUpdate = updateLook(packet, origin, me, selfFresh);
        MoveDelta move = updatePosition(packet, newSegment);
        Vec3 previousPosition = move.valid() ? lookUpdate.previousPosition() : null;
        Vec3 aimPosition = position != null ? position : (selfFresh ? me.feet() : null);
        PlayerSample eye = selfFresh && aimPosition != null
                ? me.movedTo(aimPosition.x(), aimPosition.y(), aimPosition.z()) : null;
        PlayerSample previousEye = selfFresh && previousPosition != null
                ? me.movedTo(previousPosition.x(), previousPosition.y(), previousPosition.z()) : null;
        TargetChoice choice = lookKnown
                ? targetSearch.run(eye, yaw, pitch, samples.board(), now) : TargetChoice.NONE;
        LookPair look = lookKnown ? new LookPair(yaw, pitch, lookUpdate.yawAtLastPacket(), lookUpdate.pitchAtLastPacket()) : null;
        TargetMeasurement measurement = choice.target() == null
                ? TargetMeasurement.NONE : lens.measure(player, eye, choice.target(), now, look);
        boolean changed = view.update(choice.target() == null ? null : choice.target().player(), now,
                measurement.valid() && measurement.hit().mainEyeHit());
        AimRelation aim = measurement.valid()
                ? AimRelation.between(eye.eye(), measurement.lens().tight(), yaw, pitch) : AimRelation.NONE;
        OnTarget onTarget = view.onTarget(now);
        clicks.resolveWithRow(new RowContext(nextRow, segmentOrdinal, gameTick, now, origin.rotationSource(), eye,
                previousEye, choice.target(), measurement, aim, onTarget, lookUpdate.step(), look, yaw, pitch));
        int airSwings = counters.airSwings();
        memory.airSwings(airSwings);
        OcclusionCell cell = new OcclusionCell();
        TickRow row = new TickRow(
                new RowTiming(nextRow, segmentOrdinal, gameTick, now - start, dtMillis,
                        endMarkersSeen ? counters.tickEnds() : -1, engagedInFight(), ticksSincePlayerAttack,
                        memory.millisSinceAttack(now)),
                new PacketTraits(packet.carriesRotation(), packet.carriesPosition(), packet.grounded(), packet.cancelled(),
                        echo, origin.noPacket(), yaw, pitch),
                lookUpdate.step(), move, counters.snapshot(memory.rowsAfterLastSwing()),
                SelfState.of(selfFresh ? me : null),
                new TargetView(choice.source(), changed && rowExisted && !newSegment, view.lockedOnMillis(now),
                        choice.inRange()),
                measurement, cell, aim, onTarget);
        if (eye != null && measurement.valid()) {
            occlusion.enqueue(WallCheckRequest.of(OcclusionKind.TICK, cell, eye, previousEye,
                    measurement.lens().possible(), measurement.lens().tight(), yaw, pitch));
        }
        sink.tickRow(row);
        nextRow++;
        lastRowTime = now;
        rowWritten = true;
        counters.reset();
        memory.rowCompleted();
    }

    private LookUpdate updateLook(MovementPacket packet, RowOrigin origin, PlayerSample me, boolean selfFresh) {
        boolean lookExisted = lookKnown;
        float yawAtLastPacket = yaw;
        float pitchAtLastPacket = pitch;
        RotationStep step;
        if (packet.carriesRotation()) {
            step = chain.rotate(packet.yaw(), packet.pitch());
            yaw = packet.yaw();
            pitch = packet.pitch();
            lookKnown = true;
        } else {
            step = chain.still();
            if (!lookKnown && selfFresh && Float.isFinite(me.yaw()) && Float.isFinite(me.pitch())) {
                yaw = me.yaw();
                pitch = me.pitch();
                lookKnown = true;
            }
        }
        if (!origin.noPacket()) {
            echoMarker = false;
        }
        if (!lookExisted) {
            yawAtLastPacket = yaw;
            pitchAtLastPacket = pitch;
        }
        return new LookUpdate(step, yawAtLastPacket, pitchAtLastPacket, position);
    }

    private MoveDelta updatePosition(MovementPacket packet, boolean newSegment) {
        boolean hadPosition = position != null && !newSegment;
        MoveDelta move = new MoveDelta(hadPosition, 0.0, 0.0, 0.0);
        if (hadPosition && packet.carriesPosition()) {
            move = new MoveDelta(true, packet.x() - position.x(), packet.y() - position.y(),
                    packet.z() - position.z());
        }
        if (packet.carriesPosition()) {
            position = new Vec3(packet.x(), packet.y(), packet.z());
        }
        groundedBefore = packet.grounded();
        return move;
    }

    private record LookUpdate(RotationStep step, float yawAtLastPacket, float pitchAtLastPacket, Vec3 previousPosition) {
    }
}
