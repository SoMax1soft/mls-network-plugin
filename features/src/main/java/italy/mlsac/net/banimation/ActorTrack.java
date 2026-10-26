// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.List;
import java.util.Optional;

final class ActorTrack {

    static final double EYE_HEIGHT = 1.62;
    static final int HIDDEN_AFTER_END_TICKS = 40;

    private final ActorSpec spec;
    private Placement at;
    private Point stride = new Point(0, 0, 0);
    private int strideLeft;

    ActorTrack(ActorSpec spec) {
        this.spec = spec;
    }

    Optional<Point> position() {
        return Optional.ofNullable(at).map(Placement::point);
    }

    void walk(List<AnimationCue> cues) {
        if (at == null || strideLeft <= 0) {
            return;
        }
        strideLeft--;
        at = new Placement(at.point().plus(stride), at.yaw(), at.pitch());
        cues.add(new AnimationCue.ActorMoved(at));
    }

    void play(ActorCue cue, Placement victim, int ticksLeft, List<AnimationCue> cues) {
        switch (cue.action()) {
            case SPAWN -> spawn(cue.pose(), victim, cues);
            case SWALLOW_VICTIM -> cues.add(new AnimationCue.VictimHidden(ticksLeft + HIDDEN_AFTER_END_TICKS));
            case REMOVE -> remove(cues);
            default -> {
                if (at != null) {
                    act(cue, victim, cues);
                }
            }
        }
    }

    private void spawn(ActorPose pose, Placement victim, List<AnimationCue> cues) {
        if (at != null) {
            return;
        }
        double yaw = Math.toRadians(victim.yaw());
        Point behind = victim.point().plus(Math.sin(yaw) * spec.distanceBehind(), 0,
                -Math.cos(yaw) * spec.distanceBehind());
        at = new Placement(behind, victim.yaw(), 0f);
        cues.add(new AnimationCue.ActorSpawned(spec.kind(), at, pose));
    }

    private void remove(List<AnimationCue> cues) {
        if (at == null) {
            return;
        }
        at = null;
        strideLeft = 0;
        cues.add(new AnimationCue.ActorRemoved());
    }

    private void act(ActorCue cue, Placement victim, List<AnimationCue> cues) {
        switch (cue.action()) {
            case POSE -> cues.add(new AnimationCue.ActorPosed(cue.pose()));
            case FACE_VICTIM -> face(victim, cues);
            case STEP -> step(cue, victim, cues);
            case ATTACK -> cues.add(new AnimationCue.ActorGestured(ActorGesture.ATTACK));
            case SHAKE_TENDRILS -> cues.add(new AnimationCue.ActorGestured(ActorGesture.SHAKE_TENDRILS));
            case SONIC_BOOM -> cues.add(new AnimationCue.ActorGestured(ActorGesture.SONIC_BOOM));
            case TURN_VICTIM -> turnVictim(victim, cues);
            default -> {
            }
        }
    }

    private void face(Placement victim, List<AnimationCue> cues) {
        at = new Placement(at.point(), yawToward(at.point(), victim.point()), 0f);
        cues.add(new AnimationCue.ActorMoved(at));
    }

    private void step(ActorCue cue, Placement victim, List<AnimationCue> cues) {
        Point toward = new Point(victim.point().x() - at.point().x(), 0, victim.point().z() - at.point().z());
        double gap = toward.length();
        if (gap < 1e-6) {
            return;
        }
        int ticks = Math.max(1, cue.ticks());
        stride = toward.times(Math.min(cue.distance(), gap) / gap / ticks);
        strideLeft = ticks;
        face(victim, cues);
    }

    private void turnVictim(Placement victim, List<AnimationCue> cues) {
        Point eye = victim.point().plus(0, EYE_HEIGHT, 0);
        Point head = at.point().plus(0, spec.kind().headHeight(), 0);
        double flat = Math.hypot(head.x() - eye.x(), head.z() - eye.z());
        float pitch = (float) -Math.toDegrees(Math.atan2(head.y() - eye.y(), flat));
        cues.add(new AnimationCue.MovedTo(new Placement(victim.point(), yawToward(victim.point(), at.point()), pitch)));
    }

    static float yawToward(Point from, Point to) {
        return (float) Math.toDegrees(Math.atan2(-(to.x() - from.x()), to.z() - from.z()));
    }
}
