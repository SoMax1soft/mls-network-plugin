// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.execution;

import italy.mlsac.net.suppression.plan.CommandAction;
import italy.mlsac.net.suppression.server.CommandDispatch;
import italy.mlsac.net.suppression.server.SuppressionNotices;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.List;

public final class CommandStage {

    public static final String COOLDOWN_TOKEN = "cmd skipped (cooldown)";

    private final CommandDispatch dispatch;
    private final PunishmentCooldown cooldown;
    private final SuppressionNotices notices;

    public CommandStage(CommandDispatch dispatch, PunishmentCooldown cooldown, SuppressionNotices notices) {
        this.dispatch = dispatch;
        this.cooldown = cooldown;
        this.notices = notices;
    }

    public void run(List<CommandAction> commands, TriggerContext context, ActionReport report) {
        if (commands.isEmpty()) {
            return;
        }
        boolean punishing = commands.stream().anyMatch(command -> dispatch.punishing(command.ladderText()));
        if (punishing && cooldown.blocks(context.suspectId())) {
            report.add(COOLDOWN_TOKEN);
            return;
        }
        if (punishing) {
            cooldown.record(context.suspectId());
        }
        for (CommandAction command : commands) {
            dispatchOne(command, context);
            report.add(command.token());
        }
    }

    private void dispatchOne(CommandAction command, TriggerContext context) {
        try {
            dispatch.run(command.ladderText(), context);
        } catch (RuntimeException failure) {
            notices.warn("[Suppression] Command failed for " + context.suspectName() + ": " + failure);
        }
    }
}
