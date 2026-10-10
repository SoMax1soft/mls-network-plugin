// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.profile;

import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.text.MessageBook;
import java.util.List;

public final class ProbSubcommand implements Subcommand {

    private final MessageBook messages;

    public ProbSubcommand(MessageBook messages) {
        this.messages = messages;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.PROB;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        invoker.send(messages.prefixed("prob-deprecated"));
    }
}
