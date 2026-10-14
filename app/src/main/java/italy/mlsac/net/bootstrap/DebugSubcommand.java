// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.console.DebugTally;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

final class DebugSubcommand implements Subcommand {

    private static final List<String> MODES = List.of("on", "off", "auto", "status", "player");

    private final DebugReport pulse;
    private final PlayerDirectory players;
    private final MessageBook messages;

    DebugSubcommand(DebugReport pulse, PlayerDirectory players, MessageBook messages) {
        this.pulse = pulse;
        this.players = players;
        this.messages = messages;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.DEBUG;
    }

    @Override
    public void execute(CommandActor actor, List<String> args) {
        String mode = args.isEmpty() ? "status" : args.get(0).toLowerCase(Locale.ROOT);
        switch (mode) {
            case "on" -> {
                DebugTally.force(Boolean.TRUE);
                pulse.refreshNow();
                reply(actor, "debug-on", TemplateValues.none());
            }
            case "off" -> {
                DebugTally.force(Boolean.FALSE);
                DebugTally.traceTarget(null, null);
                pulse.refreshNow();
                reply(actor, "debug-off", TemplateValues.none());
            }
            case "auto" -> {
                DebugTally.force(null);
                pulse.refreshNow();
                reply(actor, "debug-auto", TemplateValues.none());
            }
            case "player" -> tracePlayer(actor, args);
            default -> pulse.summary().forEach(line -> reply(actor, "debug-status-line",
                    TemplateValues.of("{LINE}", line)));
        }
    }

    private void tracePlayer(CommandActor actor, List<String> args) {
        if (args.size() < 2 || args.get(1).equalsIgnoreCase("off")) {
            DebugTally.traceTarget(null, null);
            reply(actor, "debug-trace-off", TemplateValues.none());
            return;
        }
        Optional<PlayerView> found = players.online().stream()
                .filter(p -> p.name().equalsIgnoreCase(args.get(1))).findFirst();
        if (found.isEmpty()) {
            reply(actor, "debug-player-offline", TemplateValues.of("{PLAYER}", args.get(1)));
            return;
        }
        DebugTally.force(Boolean.TRUE);
        DebugTally.traceTarget(found.get().id(), found.get().name());
        pulse.refreshNow();
        reply(actor, "debug-trace-on", TemplateValues.of("{PLAYER}", found.get().name()));
    }

    private void reply(CommandActor actor, String key, TemplateValues values) {
        actor.send(messages.text(key, values));
    }

    @Override
    public List<String> complete(CommandActor actor, List<String> args) {
        if (args.size() <= 1) {
            String typed = args.isEmpty() ? "" : args.get(0).toLowerCase(Locale.ROOT);
            return MODES.stream().filter(m -> m.startsWith(typed)).toList();
        }
        if (args.size() == 2 && args.get(0).equalsIgnoreCase("player")) {
            String typed = args.get(1).toLowerCase(Locale.ROOT);
            return Stream.concat(Stream.of("off"), players.online().stream().map(PlayerView::name))
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
        }
        return List.of();
    }
}
