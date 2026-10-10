// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.maintenance;

import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;

public final class ReloadSubcommand implements Subcommand {

    private final ReloadDesk desk;
    private final MessageBook messages;
    private final Logbook log;

    public ReloadSubcommand(ReloadDesk desk, MessageBook messages, Logbook log) {
        this.desk = desk;
        this.messages = messages;
        this.log = log;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.RELOAD;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        desk.scheduleFullReload();
        try {
            invoker.send(messages.prefixed("animations-reloaded",
                    TemplateValues.of("{COUNT}", desk.reloadAnimations())));
        } catch (RuntimeException failure) {
            log.severe("[Animation] Reloading ban animations failed", failure);
            invoker.send(messages.prefixed("animations-reload-failed"));
        }
        invoker.send(messages.prefixed("config-reloaded"));
    }
}
