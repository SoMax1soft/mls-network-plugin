// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.history;

import italy.mlsac.net.commands.Completions;
import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.commands.PlayerLookup;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.commands.Target;
import italy.mlsac.net.detection.LegacyHistoryExport;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;
import java.util.Optional;

public final class FalsepositiveSubcommand implements Subcommand {

    static final String RESTORE = "restore";

    private final LegacyHistoryExport history;
    private final PlayerLookup players;
    private final MessageBook messages;

    public FalsepositiveSubcommand(LegacyHistoryExport history, PlayerLookup players, MessageBook messages) {
        this.history = history;
        this.players = players;
        this.messages = messages;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.FALSEPOSITIVE;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        if (args.size() < 2 || !args.get(0).equalsIgnoreCase(RESTORE)) {
            invoker.send(messages.prefixed("falsepositive-usage"));
            return;
        }
        Optional<Target> target = players.byNamePrefix(args.get(1));
        if (target.isEmpty()) {
            invoker.send(messages.prefixed("player-not-found", TemplateValues.of("{PLAYER}", args.get(1))));
            return;
        }
        String key = switch (history.export(target.get().id())) {
            case SAVED -> "falsepositive-success";
            case NOTHING_STORED -> "falsepositive-no-data";
            case FAILED -> "falsepositive-fail";
        };
        invoker.send(messages.prefixed(key));
    }

    @Override
    public List<String> complete(CommandActor invoker, List<String> args) {
        if (args.size() == 1) {
            return List.of(RESTORE);
        }
        return args.size() == 2 && args.get(0).equalsIgnoreCase(RESTORE)
                ? Completions.matching(players.onlineNames(), args.get(1)) : List.of();
    }
}
