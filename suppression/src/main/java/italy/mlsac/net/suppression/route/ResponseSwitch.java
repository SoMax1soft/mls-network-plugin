// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.route;

import italy.mlsac.net.suppression.SuppressionEngine;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.suspect.BufferReadout;
import italy.mlsac.net.suppression.trigger.BufferAnswer;
import italy.mlsac.net.suppression.trigger.FlagEvent;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class ResponseSwitch {
    private final SuppressionEngine engine;
    private final ResponseMode planned;
    private final ResponseMode inherited;
    private final AtomicReference<ResponseMode> active;

    public ResponseSwitch(SuppressionEngine engine, LegacyPenaltyLadder ladder, LegacyAlertResponses responses) {
        this.engine = engine;
        this.planned = new EngineMode(engine);
        this.inherited = new LegacyMode(ladder, responses);
        this.active = new AtomicReference<>(inherited);
    }

    public synchronized void apply(Optional<SuppressionPlan> suppression, BufferReadout buffers) {
        ResponseMode chosen = suppression.isEmpty() ? inherited : planned;
        ResponseMode leaving = active.get();
        if (leaving != chosen) {
            leaving.leave();
        }
        suppression.ifPresent(plan -> engine.applyPlan(plan, buffers));
        active.set(chosen);
    }

    public boolean engineActive() {
        return active.get() == planned;
    }

    public void onFlag(FlagEvent flag) {
        deliver(mode -> mode.onFlag(flag));
    }

    public void onAnswer(BufferAnswer answer) {
        deliver(mode -> mode.onAnswer(answer));
    }

    public void onBufferLowered(UUID suspectId, double buffer) {
        deliver(mode -> mode.onBufferLowered(suspectId, buffer));
    }

    private void deliver(Consumer<ResponseMode> event) {
        event.accept(active.get());
    }
}
