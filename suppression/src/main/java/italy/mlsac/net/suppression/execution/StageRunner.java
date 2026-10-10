// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.execution;

import italy.mlsac.net.suppression.chain.DueStage;
import italy.mlsac.net.suppression.notice.FlagStageLine;
import italy.mlsac.net.suppression.notice.StaffLines;
import italy.mlsac.net.suppression.plan.StagePlan;
import italy.mlsac.net.suppression.server.ServerThreads;
import italy.mlsac.net.suppression.server.SuppressionLedger;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.List;

public final class StageRunner {

    private final ServerThreads threads;
    private final InGameApplier applier;
    private final CommandStage commands;
    private final StaffLines lines;
    private final CurrentPlan plan;
    private final SuppressionLedger ledger;

    public StageRunner(ServerThreads threads, InGameApplier applier, CommandStage commands, StaffLines lines,
                       CurrentPlan plan) {
        this(threads, applier, commands, lines, plan, SuppressionLedger.NONE);
    }

    public StageRunner(ServerThreads threads, InGameApplier applier, CommandStage commands, StaffLines lines,
                       CurrentPlan plan, SuppressionLedger ledger) {
        this.threads = threads;
        this.applier = applier;
        this.commands = commands;
        this.lines = lines;
        this.plan = plan;
        this.ledger = ledger;
    }

    public void run(DueStage due) {
        TriggerContext context = due.context();
        StagePlan stage = due.stage().plan();
        if (stage.inGame().isEmpty()) {
            threads.onGlobalThread(() -> finish(due, new ActionReport()));
            return;
        }
        threads.onSuspectThread(context.suspectId(),
                () -> {
                    ActionReport report = applier.applyOnline(stage.inGame(), context, new EveryTrick());
                    threads.onGlobalThread(() -> finish(due, report));
                },
                () -> {
                    ActionReport report = applier.applyOffline(stage.inGame(), context);
                    threads.onGlobalThread(() -> finish(due, report));
                });
    }

    private void finish(DueStage due, ActionReport report) {
        List<String> applied = report.tokens();
        if (!applied.isEmpty()) {
            ledger.stageRan(due.context(), due.stage().position(), applied);
        }
        commands.run(due.stage().plan().commands(), due.context(), report);
        lines.send(new FlagStageLine(due.context(), due.stage()), report.tokens(), plan.get().consoleEcho());
    }
}
