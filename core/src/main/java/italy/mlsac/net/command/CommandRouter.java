// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.command;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CommandRouter {

    private final Map<SubcommandName, Subcommand> verbs = new EnumMap<>(SubcommandName.class);
    private final MessageBook messages;
    private final CommandUsage usage;
    private final PluginLog log;

    public CommandRouter(MessageBook messages, PluginLog log) {
        this.messages = messages;
        this.log = log;
        this.usage = new CommandUsage(messages);
    }

    public synchronized void register(Subcommand subcommand) {
        verbs.put(subcommand.name(), subcommand);
    }

    public synchronized void registerAll(List<Subcommand> list) {
        list.forEach(this::register);
    }

    public void dispatch(CommandActor actor, List<String> args) {
        if (args.isEmpty()) {
            showHelp(actor);
            return;
        }
        String typed = args.get(0);
        Optional<Subcommand> verb = resolve(typed.toLowerCase(Locale.ROOT));
        verb.ifPresentOrElse(found -> admit(actor, found, rest(args)), () -> {
            actor.send(messages.prefixed("unknown-command", TemplateValues.of("{ARGS}", typed)));
            showHelp(actor);
        });
    }

    public List<String> complete(CommandActor actor, List<String> args) {
        if (args.size() > 1) {
            return resolve(args.get(0))
                    .filter(verb -> verb.name().permits(actor))
                    .map(verb -> verb.complete(actor, rest(args)))
                    .orElse(List.of());
        }
        String typed = args.isEmpty() ? "" : args.get(0).toLowerCase(Locale.ROOT);
        return registered().stream()
                .filter(name -> name.permits(actor))
                .flatMap(name -> name.typedNames().stream())
                .filter(id -> id.startsWith(typed))
                .toList();
    }

    private void admit(CommandActor actor, Subcommand verb, List<String> arguments) {
        Optional<String> refusal = VerbGate.refusal(verb.name(), actor);
        if (refusal.isPresent()) {
            actor.send(messages.prefixed(refusal.get(), TemplateValues.none()));
            return;
        }
        try {
            verb.execute(actor, arguments);
        } catch (RuntimeException failure) {
            log.error(LogTopic.COMMANDS, "Subcommand '" + verb.name().id() + "' threw " + failure, failure);
        }
    }

    private void showHelp(CommandActor actor) {
        usage.linesFor(actor, registered()).forEach(actor::send);
    }

    private static List<String> rest(List<String> args) {
        return List.copyOf(args.subList(1, args.size()));
    }

    private Optional<Subcommand> resolve(String typed) {
        return SubcommandName.parse(typed).flatMap(name -> Optional.ofNullable(lookup(name)));
    }

    private synchronized Set<SubcommandName> registered() {
        return verbs.isEmpty() ? EnumSet.noneOf(SubcommandName.class) : EnumSet.copyOf(verbs.keySet());
    }

    private synchronized Subcommand lookup(SubcommandName name) {
        return verbs.get(name);
    }
}
