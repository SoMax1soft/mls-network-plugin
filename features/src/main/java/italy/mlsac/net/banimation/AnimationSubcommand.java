// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import italy.mlsac.net.commands.Completions;
import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class AnimationSubcommand implements Subcommand {

    private static final List<String> HELP_KEYS = List.of("help-animation-list", "help-animation-reload");

    private final Map<String, AnimationVerb> verbs = new LinkedHashMap<>();
    private final MessageBook messages;

    public AnimationSubcommand(AnimationLibrary library, Supplier<List<AnimationFile>> files, MessageBook messages,
                               Logbook log) {
        this.messages = messages;
        verbs.put("list", new AnimationCatalogue(library, messages));
        verbs.put("reload", new AnimationRefresh(library, files, messages, log));
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.ANIMATION;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        if (args.isEmpty()) {
            invoker.send(messages.prefixed("animation-help-title"));
            HELP_KEYS.forEach(key -> invoker.send(messages.text(key)));
            return;
        }
        verb(args.get(0)).ifPresentOrElse(verb -> verb.run(invoker, args.subList(1, args.size())),
                () -> invoker.send(messages.prefixed("animation-unknown-verb",
                        TemplateValues.of("{ARGS}", args.get(0)))));
    }

    @Override
    public List<String> complete(CommandActor invoker, List<String> args) {
        if (args.size() <= 1) {
            return Completions.matching(List.copyOf(verbs.keySet()), Completions.last(args));
        }
        return verb(args.get(0)).map(verb -> verb.suggest(invoker, args.subList(1, args.size())))
                .orElse(List.of());
    }

    private Optional<AnimationVerb> verb(String typed) {
        return Optional.ofNullable(verbs.get(typed.toLowerCase(Locale.ROOT)));
    }
}
