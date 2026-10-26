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
            Optional<Placement> current = run.punished() || !effects.online(player) ? Optional.empty()
                    : effects.location(player);
            TickPlan plan = run.advance(current);
            plan.cues().forEach(cue -> cue.apply(effects, player));
            if (plan.finished()) {
                finish(player, run);
                return false;
            }
            if (run.punishesNow()) {
                return punish(player, run);
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
        if (run != null && !run.punished()) {
            finish(player, run);
        }
    }

    public boolean animating(UUID player) {
        return runs.containsKey(player);
    }

    public boolean anyAnimating() {
        return !runs.isEmpty();
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

    private boolean punish(UUID player, AnimationRun run) {
        Optional<WorldSpot> spot = effects.spot(player);
        if (spot.isEmpty()) {
            finish(player, run);
            return false;
        }
        if (run.holdsItems()) {
            effects.dropRemainingItems(player, run.dropped(), spot.get().point());
        }
        run.markPunished();
        effects.runCommand(run.command());
        listener.detached(player, spot.get(), () -> tick(player));
        return true;
    }

    private void finish(UUID player, AnimationRun run) {
        if (!runs.remove(player, run)) {
            return;
        }
        cleanUp(player, run);
        if (!run.punished()) {
            effects.runCommand(run.command());
        }
    }

    private void cleanUp(UUID player, AnimationRun run) {
        listener.ended(player);
        if (run.script().actor().isPresent() || run.punished()) {
            effects.removeActor(player);
        }
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
