// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.modelchoice;

import italy.mlsac.net.api.MlsacModelSelection;
import italy.mlsac.net.api.ModelChoice;
import italy.mlsac.net.api.ModelSelection;
import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.commands.Completions;
import italy.mlsac.net.commands.PlayerLookup;
import italy.mlsac.net.commands.Target;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ModelSubcommand implements Subcommand {

    private static final List<String> CHOICES = Arrays.stream(ModelChoice.values()).map(ModelChoice::id).toList();

    private final PlayerLookup players;
    private final MlsacModelSelection selection;
    private final MessageBook messages;

    public ModelSubcommand(PlayerLookup players, MlsacModelSelection selection, MessageBook messages) {
        this.players = players;
        this.selection = selection;
        this.messages = messages;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.MODEL;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        if (args.isEmpty()) {
            invoker.send(messages.prefixed(ModelChoiceTexts.USAGE));
            return;
        }
        Optional<Target> target = players.byExactName(args.get(0));
        if (target.isEmpty()) {
            invoker.send(messages.prefixed("player-not-found", TemplateValues.of("{PLAYER}", args.get(0))));
            return;
        }
        if (args.size() == 1) {
            invoker.send(describe(target.get(), selection.get(target.get().id()), ModelChoiceTexts.STATE));
            return;
        }
        Optional<ModelChoice> choice = ModelChoice.parse(args.get(1));
        if (choice.isEmpty()) {
            invoker.send(messages.prefixed(ModelChoiceTexts.UNKNOWN, TemplateValues.of("{MODEL}", args.get(1))));
            return;
        }
        invoker.send(describe(target.get(), selection.set(target.get().id(), choice.get()), ModelChoiceTexts.SET));
    }

    @Override
    public List<String> complete(CommandActor invoker, List<String> args) {
        if (args.size() == 1) {
            return Completions.matching(players.onlineNames(), args.get(0));
        }
        return args.size() == 2 ? Completions.matching(CHOICES, args.get(1)) : List.of();
    }

    private String describe(Target target, ModelSelection current, String grantedKey) {
        TemplateValues values = TemplateValues.of("{PLAYER}", target.name())
                .and("{MODEL}", modelName(current.effective()))
                .and("{REQUESTED}", modelName(current.requested()))
                .and("{AVAILABLE}", availableNames());
        if (current.fallback().isPresent()) {
            return messages.prefixed(ModelChoiceTexts.FALLBACK,
                    values.and("{REASON}", messages.text(ModelChoiceTexts.reasonKey(current.fallback().get()))));
        }
        return messages.prefixed(grantedKey, values);
    }

    private String availableNames() {
        String names = selection.available().stream()
                .filter(choice -> choice != ModelChoice.DEFAULT)
                .map(this::modelName)
                .collect(Collectors.joining(", "));
        return names.isEmpty() ? messages.text(ModelChoiceTexts.NOTHING_AVAILABLE) : names;
    }

    private String modelName(ModelChoice choice) {
        return messages.text(ModelChoiceTexts.nameKey(choice));
    }
}
