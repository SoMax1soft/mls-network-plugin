// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.execution;

import italy.mlsac.net.suppression.notice.BufferTriggerLine;
import italy.mlsac.net.suppression.notice.StaffLines;
import italy.mlsac.net.suppression.server.ServerThreads;
import italy.mlsac.net.suppression.server.SuppressionLedger;
import italy.mlsac.net.suppression.trigger.BufferTrigger;
import java.util.List;

public final class BufferTriggerRunner {

    private final ServerThreads threads;
    private final InGameApplier applier;
    private final StaffLines lines;
    private final CurrentPlan plan;
    private final SuppressionLedger ledger;

    public BufferTriggerRunner(ServerThreads threads, InGameApplier applier, StaffLines lines, CurrentPlan plan) {
        this(threads, applier, lines, plan, SuppressionLedger.NONE);
    }

    public BufferTriggerRunner(ServerThreads threads, InGameApplier applier, StaffLines lines, CurrentPlan plan,
                               SuppressionLedger ledger) {
        this.threads = threads;
        this.applier = applier;
        this.lines = lines;
        this.plan = plan;
        this.ledger = ledger;
    }

    public void run(BufferTrigger trigger) {
        threads.onSuspectThread(trigger.context().suspectId(),
                () -> announce(trigger, applier.applyOnline(trigger.actions(), trigger.context(), new OncePerTrigger())),
                () -> announce(trigger, applier.applyOffline(trigger.actions(), trigger.context())));
    }

    private void announce(BufferTrigger trigger, ActionReport report) {
        lines.send(new BufferTriggerLine(trigger), report.tokens(), plan.get().consoleEcho());
        List<String> applied = report.tokens();
        if (!applied.isEmpty()) {
            ledger.bufferTriggered(trigger.context(), trigger.highestPercent(), applied);
        }
    }
}
