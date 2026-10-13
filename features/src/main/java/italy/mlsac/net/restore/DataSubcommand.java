// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.restore;

import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.commands.CommandLanes;
import italy.mlsac.net.commands.Completions;
import italy.mlsac.net.commands.PlayerLookup;
import italy.mlsac.net.commands.Target;
import italy.mlsac.net.detection.RestoreAnswer;
import italy.mlsac.net.detection.RestoreLabel;
import italy.mlsac.net.detection.RestoreRequest;
import italy.mlsac.net.detection.TickRestore;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.BooleanSupplier;

public final class DataSubcommand implements Subcommand {

    static final String RESTORE = "restore";
    static final List<String> LABELS = List.of("cheat", "legit");

    private final TickRestore restore;
    private final PlayerLookup players;
    private final BooleanSupplier enabled;
    private final RestorePace pace;
    private final CommandLanes lanes;
    private final MessageBook messages;

    public DataSubcommand(TickRestore restore, PlayerLookup players, BooleanSupplier enabled, RestorePace pace,
                          CommandLanes lanes, MessageBook messages) {
        this.restore = restore;
        this.players = players;
        this.enabled = enabled;
        this.pace = pace;
        this.lanes = lanes;
        this.messages = messages;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.DATA;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        if (!enabled.getAsBoolean()) {
            invoker.send(messages.prefixed("data-restore-disabled"));
            return;
        }
        Optional<RestoreLabel> label = args.size() == 3 && args.get(1).equalsIgnoreCase(RESTORE)
                ? RestoreLabel.parse(args.get(0)) : Optional.empty();
        if (label.isEmpty()) {
            invoker.send(messages.prefixed("data-usage"));
            return;
        }
        Optional<Target> target = players.byNamePrefix(args.get(2));
        if (target.isEmpty()) {
            invoker.send(messages.prefixed("player-not-found", TemplateValues.of("{PLAYER}", args.get(2))));
            return;
        }
        start(invoker, label.get(), target.get());
    }

    private void start(CommandActor invoker, RestoreLabel label, Target target) {
        TemplateValues values = TemplateValues.of("{PLAYER}", target.name()).and("{LABEL}", label.spelling());
        if (!pace.begin(target.id())) {
            invoker.send(messages.prefixed("data-restore-busy", values));
            return;
        }
        OptionalLong wait = pace.waitSeconds(invoker.name(), target.id());
        if (wait.isPresent()) {
            pace.end(target.id());
            invoker.send(messages.prefixed("data-restore-cooldown", values.and("{SECONDS}", wait.getAsLong())));
            return;
        }
        invoker.send(messages.prefixed("data-restore-started", values));
        RestoreRequest request = new RestoreRequest(target.id(), label, invoker.name());
        lanes.inBackground(() -> {
            try {
                lanes.reply(invoker, List.of(answerLine(restore.restore(request), values)));
            } finally {
                pace.end(target.id());
            }
        });
    }

    private String answerLine(RestoreAnswer answer, TemplateValues values) {
        return switch (answer.outcome()) {
            case SAVED -> messages.prefixed("data-restore-saved", values.and("{TICKS}", answer.ticks())
                    .and("{HITS}", answer.hits()).and("{FOLDER}", answer.folder()));
            case NOTHING_BUFFERED -> messages.prefixed("data-restore-empty", values);
            case DISABLED -> messages.prefixed("data-restore-disabled");
            case FAILED -> messages.prefixed("data-restore-failed", values);
        };
    }

    @Override
    public List<String> complete(CommandActor invoker, List<String> args) {
        if (args.size() == 1) {
            return Completions.matching(LABELS, args.get(0));
        }
        if (args.size() == 2) {
            return Completions.matching(List.of(RESTORE), args.get(1));
        }
        if (args.size() == 3 && args.get(1).equalsIgnoreCase(RESTORE)) {
            return Completions.matching(players.onlineNames(), args.get(2));
        }
        return List.of();
    }
}
