// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import italy.mlsac.net.logbook.Logbook;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;

public final class AnimationDirector {

    private final AnimationEffects effects;
    private final DoubleSupplier random;
    private final Logbook log;
    private final AnimationListener listener;
    private final Map<UUID, AnimationRun> runs = new ConcurrentHashMap<>();

    public AnimationDirector(AnimationEffects effects, DoubleSupplier random, Logbook log) {
        this(effects, random, log, AnimationListener.NONE);
    }

    public AnimationDirector(AnimationEffects effects, DoubleSupplier random, Logbook log, AnimationListener listener) {
        this.effects = effects;
        this.random = random;
        this.log = log;
        this.listener = listener;
    }

    public AnimationLaunch start(UUID player, AnimationScript script, String command) {
        if (!effects.online(player)) {
            effects.runCommand(command);
            return AnimationLaunch.RAN_COMMAND_NOW;
        }
        AnimationRun run = new AnimationRun(script, command, effects.location(player), random);
        if (runs.putIfAbsent(player, run) != null) {
            return AnimationLaunch.ALREADY_ANIMATING;
        }
        effects.closeInventory(player);
        effects.resyncInventory(player);
        effects.resyncInventoryNextTick(player);
        if (script.holdVictimStill()) {
            effects.freeze(player, FreezeSpec.of(script));
        }
        if (script.unequipVictim()) {
            run.itemsTaken(effects.stripInventory(player));
        }
        listener.started(player, () -> tick(player));
        return AnimationLaunch.STARTED;
    }

    public boolean tick(UUID player) {
        AnimationRun run = runs.get(player);
        if (run == null) {
            return false;
        }
        try {
            Optional<Placement> current = effects.online(player) ? effects.location(player) : Optional.empty();
            TickPlan plan = run.advance(current);
            plan.cues().forEach(cue -> cue.apply(effects, player));
            if (plan.finished()) {
                finish(player, run);
                return false;
            }
            return true;
        } catch (RuntimeException failure) {
            log.severe("[Animation] Ban animation failed for " + player + ", running its command now", failure);
            finish(player, run);
            return false;
        }
    }

    public void quit(UUID player) {
        AnimationRun run = runs.get(player);
        if (run != null) {
            finish(player, run);
        }
    }

    public boolean animating(UUID player) {
        return runs.containsKey(player);
    }

    public int abandonAll() {
        int abandoned = 0;
        for (Map.Entry<UUID, AnimationRun> entry : Map.copyOf(runs).entrySet()) {
            if (runs.remove(entry.getKey(), entry.getValue())) {
                cleanUp(entry.getKey(), entry.getValue());
                abandoned++;
            }
        }
        return abandoned;
    }

    private void finish(UUID player, AnimationRun run) {
        if (!runs.remove(player, run)) {
            return;
        }
        cleanUp(player, run);
        effects.runCommand(run.command());
    }

    private void cleanUp(UUID player, AnimationRun run) {
        listener.ended(player);
        Optional<Point> dropAt = run.start().map(Placement::point)
                .or(() -> effects.location(player).map(Placement::point));
        if (run.holdsItems()) {
            dropAt.ifPresent(point -> effects.dropRemainingItems(player, run.dropped(), point));
        }
        if (run.script().holdVictimStill() && effects.online(player)) {
            effects.unfreeze(player);
        }
    }
}
