// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import italy.mlsac.net.commands.Completions;
import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.commands.PlayerLookup;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.commands.Target;
import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PunishSubcommand implements Subcommand {

    static final String CONFIRM = "confirm";

    private final PlayerLookup players;
    private final PunishEvidence evidence;
    private final MaximumPunishment maximum;
    private final MessageBook messages;
    private final Logbook log;

    public PunishSubcommand(PlayerLookup players, PunishEvidence evidence, MaximumPunishment maximum,
                            MessageBook messages, Logbook log) {
        this.players = players;
        this.evidence = evidence;
        this.maximum = maximum;
        this.messages = messages;
        this.log = log;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.PUNISH;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        if (args.isEmpty()) {
            invoker.send(messages.prefixed("usage-punish"));
            return;
        }
        Optional<Target> target = players.byExactName(args.get(0));
        if (target.isEmpty()) {
            invoker.send(messages.prefixed("player-not-found", TemplateValues.of("{PLAYER}", args.get(0))));
            return;
        }
        TemplateValues name = TemplateValues.of("{PLAYER}", target.get().name());
        boolean confirmed = args.size() >= 2 && args.get(1).equalsIgnoreCase(CONFIRM);
        if (!evidence.exists(target.get().id()) && !confirmed) {
            invoker.send(messages.prefixed("punish-no-evidence", name));
            invoker.send(messages.text("punish-confirm-hint", name));
            log.info("Max punishment of " + target.get().name() + " asked by " + invoker.name()
                    + " was not run: nothing on record and no confirm given");
            return;
        }
        log.info("Max punishment of " + target.get().name() + " asked by " + invoker.name()
                + ": running the highest rung");
        Optional<Integer> level = maximum.punish(target.get().id(), target.get().name());
        invoker.send(level.isPresent()
                ? messages.prefixed("punish-success", name.and("{ACTION}", "Max VL"))
                : messages.prefixed("punish-no-action"));
    }

    @Override
    public List<String> complete(CommandActor invoker, List<String> args) {
        if (args.size() == 1) {
            return Completions.matching(players.onlineNames(), args.get(0));
        }
        return args.size() == 2 ? Completions.matching(List.of(CONFIRM), args.get(1)) : List.of();
    }

    public interface PunishEvidence {

        int offenceLevel(UUID player);

        double buffer(UUID player);

        int highProbabilityCount(UUID player);

        default boolean exists(UUID player) {
            return offenceLevel(player) > 0 || buffer(player) > 0 || highProbabilityCount(player) > 0;
        }
    }
}
