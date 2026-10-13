// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.console.DebugTally;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

final class DebugSubcommand implements Subcommand {

    private static final List<String> MODES = List.of("on", "off", "auto", "status", "player");

    private final DebugPulse pulse;
    private final PlayerDirectory players;

    DebugSubcommand(DebugPulse pulse, PlayerDirectory players) {
        this.pulse = pulse;
        this.players = players;
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
                actor.send("&aMLSAC debug ON: a summary every 10 s in the console. &7/mlsac debug off to stop.");
            }
            case "off" -> {
                DebugTally.force(Boolean.FALSE);
                DebugTally.traceTarget(null, null);
                pulse.refreshNow();
                actor.send("&eMLSAC debug OFF.");
            }
            case "auto" -> {
                DebugTally.force(null);
                pulse.refreshNow();
                actor.send("&7MLSAC debug follows config.yml (debug: true/false) again.");
            }
            case "player" -> tracePlayer(actor, args);
            default -> pulse.summary().forEach(line -> actor.send("&7" + line));
        }
    }

    private void tracePlayer(CommandActor actor, List<String> args) {
        if (args.size() < 2 || args.get(1).equalsIgnoreCase("off")) {
            DebugTally.traceTarget(null, null);
            actor.send("&7Player trace off.");
            return;
        }
        Optional<PlayerView> found = players.online().stream()
                .filter(p -> p.name().equalsIgnoreCase(args.get(1))).findFirst();
        if (found.isEmpty()) {
            actor.send("&cPlayer " + args.get(1) + " is not online.");
            return;
        }
        DebugTally.force(Boolean.TRUE);
        DebugTally.traceTarget(found.get().id(), found.get().name());
        pulse.refreshNow();
        actor.send("&aTracing " + found.get().name() + ": every hit, window and answer goes to the console.");
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
