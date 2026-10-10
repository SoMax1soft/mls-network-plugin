// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.text.MessageBook;
import java.util.List;

public final class AlertsSubcommand implements Subcommand {

    private final SubscriberSet subscribers;
    private final MessageBook messages;

    public AlertsSubcommand(SubscriberSet subscribers, MessageBook messages) {
        this.subscribers = subscribers;
        this.messages = messages;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.ALERTS;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        invoker.actorUuid().ifPresent(player -> invoker.send(messages.prefixed(
                subscribers.toggle(player) ? "alerts-enabled" : "alerts-disabled")));
    }
}
