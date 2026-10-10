// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.profile;

import italy.mlsac.net.commands.Completions;
import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.commands.PlayerLookup;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.commands.Target;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;
import java.util.Optional;

public final class ProfileSubcommand implements Subcommand {

    private final PlayerLookup players;
    private final ProfileFacts facts;
    private final MessageBook messages;

    public ProfileSubcommand(PlayerLookup players, ProfileFacts facts, MessageBook messages) {
        this.players = players;
        this.facts = facts;
        this.messages = messages;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.PROFILE;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        if (args.isEmpty()) {
            invoker.send(messages.prefixed("usage-profile"));
            return;
        }
        Optional<Target> target = players.byNamePrefix(args.get(0));
        if (target.isEmpty()) {
            invoker.send(messages.prefixed("player-not-found", TemplateValues.of("{PLAYER}", args.get(0))));
            return;
        }
        String sens = facts.sensitivityPercent(target.get().id()).stream().mapToObj(Integer::toString).findFirst()
                .orElse("N/A");
        String client = facts.clientVersion(target.get().id()).orElse("Unknown");
        int detections = facts.highProbabilityCount(target.get().id());
        TemplateValues values = TemplateValues.of("{PLAYER}", target.get().name()).and("{SENS}", sens)
                .and("{CLIENT}", client).and("{DETECTIONS}", detections);
        invoker.send(messages.text("profile-header", values));
        List<String> lines = messages.lines("profile-info", values);
        if (lines.isEmpty()) {
            lines = messages.lines("profile-info-fallback", values);
        }
        lines.forEach(invoker::send);
    }

    @Override
    public List<String> complete(CommandActor invoker, List<String> args) {
        return args.size() == 1 ? Completions.matching(players.onlineNames(), args.get(0)) : List.of();
    }
}
