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
    private int itemsTaken;
    private int dropped;
    private int tick;

    public AnimationRun(AnimationScript script, String command, Optional<Placement> start, DoubleSupplier random) {
        this.script = script;
        this.command = command;
        this.start = start;
        this.random = random;
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

    public TickPlan advance(Optional<Placement> current) {
        tick++;
        if (current.isEmpty()) {
            return TickPlan.finishedNow();
        }
        Point base = current.get().point();
        List<AnimationCue> cues = new ArrayList<>();
        dropNextItem(base, cues);
        move(cues);
        for (ScriptStage stage : script.stages()) {
            if (stage.activeAt(tick)) {
                playStage(stage, base, cues);
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

    private void playStage(ScriptStage stage, Point base, List<AnimationCue> cues) {
        int stageTick = tick - stage.fromTick();
        double progress = stage.progressAt(tick);
        for (SoundCue sound : stage.sounds()) {
            if (sound.playsAt(stageTick)) {
                cues.add(new AnimationCue.SoundAt(base, sound));
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
                Point center = base.plus(0, effect.heightAt(progress), 0);
                cues.add(new AnimationCue.ParticlesAt(effect.look(), effect.shape().emit(center,
                        effect.radiusAt(progress), effect.count(), tick, random), effect.spread(), effect.speed()));
            }
        }
    }
}
