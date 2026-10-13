// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.commands.Completions;
import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.commands.PlayerLookup;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.commands.Target;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class MonitorSubcommand implements Subcommand {

    static final String PLAYER_MODE = "player";
    static final List<String> OLDER_CHAT_MODES = List.of("chat");
    static final List<String> OLDER_PLAYER_MODES = List.of("prob");

    private final SubscriberSet monitors;
    private final ProbTracking tracking;
    private final PlayerLookup players;
    private final MessageBook messages;

    public MonitorSubcommand(SubscriberSet monitors, ProbTracking tracking, PlayerLookup players,
                             MessageBook messages) {
        this.monitors = monitors;
        this.tracking = tracking;
        this.players = players;
        this.messages = messages;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.MONITOR;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        UUID viewer = invoker.actorUuid().orElseThrow();
        String mode = args.isEmpty() ? "" : args.get(0).toLowerCase(Locale.ROOT);
        if (mode.isEmpty() || OLDER_CHAT_MODES.contains(mode)) {
            invoker.send(messages.prefixed(monitors.toggle(viewer) ? "monitor-enabled" : "monitor-disabled"));
        } else if (mode.equals(PLAYER_MODE) || OLDER_PLAYER_MODES.contains(mode)) {
            probe(invoker, viewer, args);
        } else {
            invoker.send(messages.prefixed("monitor-usage"));
        }
    }

    private void probe(CommandActor invoker, UUID viewer, List<String> args) {
        if (tracking.stop(viewer)) {
            invoker.send(messages.prefixed("tracking-stopped"));
            return;
        }
        if (args.size() < 2) {
            invoker.send(messages.prefixed("prob-usage"));
            return;
        }
        Optional<Target> target = players.byNamePrefix(args.get(1));
        if (target.isEmpty()) {
            invoker.send(messages.prefixed("player-not-found", TemplateValues.of("{PLAYER}", args.get(1))));
            return;
        }
        invoker.send(messages.prefixed("tracking-started", TemplateValues.of("{PLAYER}", target.get().name())));
        tracking.start(viewer, target.get());
    }

    @Override
    public List<String> complete(CommandActor invoker, List<String> args) {
        if (args.size() == 1) {
            return Completions.matching(List.of(PLAYER_MODE), args.get(0));
        }
        if (args.size() == 2 && args.get(0).equalsIgnoreCase(PLAYER_MODE)) {
            return Completions.matching(players.onlineNames(), args.get(1));
        }
        return List.of();
    }
}
