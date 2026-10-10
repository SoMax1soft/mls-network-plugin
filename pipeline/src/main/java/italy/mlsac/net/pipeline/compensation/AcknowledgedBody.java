// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.Vec3;

public final class AcknowledgedBody {
    private static final double PROTOCOL_SCALE = 4096.0;
    private static final double MOTION_LIMIT = 8.0;
    private static final double JUMP_LIMIT = 64.0;
    private static final double DECAY = 2.0 / 3.0;

    private final ClientQuirks profile;
    private double ackX;
    private double ackY;
    private double ackZ;
    private Interpolation current;
    private Interpolation previous;
    private double motionX;
    private double motionY;
    private double motionZ;
    private Vec3 aimPointBefore;
    private Vec3 pendingJump;
    private int pingMarker = Integer.MIN_VALUE;

    public AcknowledgedBody(ClientQuirks profile, Vec3 spawn) {
        this.profile = profile;
        this.ackX = spawn.x();
        this.ackY = spawn.y();
        this.ackZ = spawn.z();
    }

    int pingMarker() {
        return pingMarker;
    }

    void pingMarker(int marker) {
        pingMarker = marker;
    }

    void acknowledge(EntityMove move) {
        Box start = current != null ? current.possible() : Box.at(acknowledged());
        if (move.carriesPosition()) {
            applyPosition(move);
        }
        previous = current;
        if (!move.carriesPosition() && profile.freezesOnRotation()) {
            current = new FrozenInterpolation(start);
        } else {
            current = new MovingInterpolation(start, acknowledged(), new Vec3(motionX, motionY, motionZ), pendingJump,
                    profile);
            pendingJump = null;
        }
    }

    private void applyPosition(EntityMove move) {
        double oldX = ackX;
        double oldY = ackY;
        double oldZ = ackZ;
        if (move.relative()) {
            ackX = shifted(ackX, move.x());
            ackY = shifted(ackY, move.y());
            ackZ = shifted(ackZ, move.z());
        } else {
            ackX = move.x();
            ackY = move.y();
            ackZ = move.z();
        }
        if (aimPointBefore != null) {
            motionX = motion(motionX, Math.abs(ackX - aimPointBefore.x()));
            motionY = motion(motionY, Math.abs(ackY - aimPointBefore.y()));
            motionZ = motion(motionZ, Math.abs(ackZ - aimPointBefore.z()));
        }
        aimPointBefore = acknowledged();
        Vec3 jump = new Vec3(Math.abs(ackX - oldX), Math.abs(ackY - oldY), Math.abs(ackZ - oldZ));
        boolean smallJump = jump.x() < JUMP_LIMIT && jump.y() < JUMP_LIMIT && jump.z() < JUMP_LIMIT;
        pendingJump = move.relative() || smallJump ? null : jump;
    }

    private double shifted(double position, double delta) {
        if (delta == 0.0) {
            return position;
        }
        if (profile.modernDeltas()) {
            return (Math.round(position * PROTOCOL_SCALE) + Math.round(delta * PROTOCOL_SCALE)) / PROTOCOL_SCALE;
        }
        return (Math.floor(position * PROTOCOL_SCALE) + Math.floor(delta * PROTOCOL_SCALE)) / PROTOCOL_SCALE;
    }

    private static double motion(double estimate, double distance) {
        double decayed = estimate * DECAY;
        return distance < MOTION_LIMIT ? Math.max(decayed, distance) : decayed;
    }

    void settle() {
        previous = null;
    }

    void clientTick() {
        if (current == null) {
            return;
        }
        if (previous == null) {
            current.advanceWithLow();
            return;
        }
        current.advance();
        previous.advanceWithLow();
        current.widenStart(previous.possible());
    }

    public Box tightBox(double width, double height) {
        return Box.player(ackX, ackY, ackZ, width, height);
    }

    public Box possibleBox(double width, double height) {
        Box union = null;
        if (current != null) {
            union = current.possible();
        }
        if (previous != null) {
            union = union == null ? previous.possible() : union.union(previous.possible());
        }
        if (union == null) {
            return tightBox(width, height);
        }
        return union.grownBySize(width, height);
    }

    private Vec3 acknowledged() {
        return new Vec3(ackX, ackY, ackZ);
    }
}
