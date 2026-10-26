// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;

public final class AnimationRun {

    static final double DROP_PACE = 0.8;

    private final AnimationScript script;
    private final String command;
    private final Optional<Placement> start;
    private final DoubleSupplier random;
    private final Optional<ActorTrack> actor;
    private Optional<Placement> lastSeen = Optional.empty();
    private boolean punished;
    private int itemsTaken;
    private int dropped;
    private int tick;

    public AnimationRun(AnimationScript script, String command, Optional<Placement> start, DoubleSupplier random) {
        this.script = script;
        this.command = command;
        this.start = start;
        this.random = random;
        this.actor = script.actor().map(ActorTrack::new);
    }

    public void itemsTaken(int count) {
        itemsTaken = Math.max(0, count);
    }

    public String command() {
        return command;
    }

    public AnimationScript script() {
        return script;
    }

    public int dropped() {
        return dropped;
    }

    public boolean holdsItems() {
        return itemsTaken > dropped;
    }

    public int tick() {
        return tick;
    }

    public Optional<Placement> start() {
        return start;
    }

    public boolean punished() {
        return punished;
    }

    public boolean punishesNow() {
        return !punished && script.punishAtTick() > 0 && tick >= script.punishAtTick();
    }

    public void markPunished() {
        punished = true;
        dropped = Math.max(dropped, itemsTaken);
    }

    public TickPlan advance(Optional<Placement> current) {
        tick++;
        Optional<Placement> seen = punished ? lastSeen : current;
        if (seen.isEmpty()) {
            return TickPlan.finishedNow();
        }
        lastSeen = seen;
        TickPlan plan = plan(seen.get());
        return punished ? plan.withoutVictimCues() : plan;
    }

    private TickPlan plan(Placement victim) {
        Point base = victim.point();
        List<AnimationCue> cues = new ArrayList<>();
        dropNextItem(base, cues);
        move(cues);
        actor.ifPresent(track -> track.walk(cues));
        for (ScriptStage stage : script.stages()) {
            if (stage.activeAt(tick)) {
                playStage(stage, victim, cues);
            }
        }
        if (tick >= script.lengthTicks()) {
            if (script.lightningAtEnd()) {
                cues.add(new AnimationCue.LightningAt(base));
            }
            return new TickPlan(cues, true);
        }
        return new TickPlan(cues, false);
    }

    private void dropNextItem(Point base, List<AnimationCue> cues) {
        int remainingItems = itemsTaken - dropped;
        int remainingTicks = script.lengthTicks() - tick;
        if (remainingItems <= 0 || remainingTicks <= 0) {
            return;
        }
        int paced = Math.max(1, (int) (remainingTicks * DROP_PACE / remainingItems));
        int interval = Math.max(1, Math.min(script.dropEveryTicks(), paced));
        if (tick % interval == 0) {
            cues.add(new AnimationCue.ItemDropped(dropped++, base.plus(0, 1, 0)));
        }
    }

    private void move(List<AnimationCue> cues) {
        if (script.movement().isEmpty() || start.isEmpty() || !script.movement().get().covers(tick)) {
            return;
        }
        Placement origin = start.get();
        script.movement().get().target(tick, origin.point())
                .ifPresent(point -> cues.add(new AnimationCue.MovedTo(new Placement(point, origin.yaw(), origin.pitch()))));
    }

    private void playStage(ScriptStage stage, Placement victim, List<AnimationCue> cues) {
        Point base = victim.point();
        int stageTick = tick - stage.fromTick();
        double progress = stage.progressAt(tick);
        actor.ifPresent(track -> stage.actorCues().stream().filter(cue -> cue.cueTick() == stageTick)
                .forEach(cue -> track.play(cue, victim, script.lengthTicks() - tick, cues)));
        for (SoundCue sound : stage.sounds()) {
            if (sound.playsAt(stageTick)) {
                cues.add(new AnimationCue.SoundAt(anchored(sound.anchor(), base), sound));
            }
        }
        for (TitleCue title : stage.titles()) {
            if (title.titleTick() == stageTick) {
                cues.add(new AnimationCue.TitleShown(title));
            }
        }
        if (stage.boltTicks().contains(stageTick)) {
            cues.add(new AnimationCue.LightningAt(base));
        }
        stage.trail().filter(trail -> stageTick % trail.emitEveryTicks() == 0).ifPresent(trail -> cues.add(
                new AnimationCue.ParticlesAt(trail.look(), List.of(new Emission(base, trail.count())), trail.spread(), 0)));
        for (ParticleEffect effect : stage.particles()) {
            if (stageTick % effect.emitEveryTicks() == 0) {
                Point center = anchored(effect.anchor(), base).plus(0, effect.heightAt(progress), 0);
                cues.add(new AnimationCue.ParticlesAt(effect.look(), effect.shape().emit(center,
                        effect.radiusAt(progress), effect.count(), tick, random), effect.spread(), effect.speed()));
            }
        }
    }

    private Point anchored(Anchor anchor, Point base) {
        if (anchor == Anchor.ACTOR) {
            return actor.flatMap(ActorTrack::position).orElse(base);
        }
        return base;
    }
}
