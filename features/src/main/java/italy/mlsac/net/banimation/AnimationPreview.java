// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import italy.mlsac.net.commands.Completions;
import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.commands.PlayerLookup;
import italy.mlsac.net.commands.Target;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;
import java.util.Optional;

final class AnimationPreview implements AnimationVerb {

    private final AnimationLibrary library;
    private final AnimationDirector director;
    private final PlayerLookup players;
    private final MessageBook messages;

    AnimationPreview(AnimationLibrary library, AnimationDirector director, PlayerLookup players,
                     MessageBook messages) {
        this.library = library;
        this.director = director;
        this.players = players;
        this.messages = messages;
    }

    @Override
    public void run(CommandActor invoker, List<String> rest) {
        if (rest.size() < 2) {
            invoker.send(messages.prefixed("animation-test-usage"));
            return;
        }
        String typedPlayer = rest.get(0);
        String typedAnimation = rest.get(1);
        Optional<Target> suspect = players.byNamePrefix(typedPlayer);
        Optional<AnimationScript> script = library.find(typedAnimation);
        if (suspect.isEmpty()) {
            invoker.send(messages.prefixed("player-not-found", TemplateValues.of("{PLAYER}", typedPlayer)));
        } else if (script.isEmpty()) {
            invoker.send(messages.prefixed("animation-missing", TemplateValues.of("{ANIMATION}", typedAnimation)));
            invoker.send(messages.text("animation-missing-hint"));
        } else {
            play(invoker, suspect.get(), script.get());
        }
    }

    @Override
    public List<String> suggest(CommandActor invoker, List<String> rest) {
        return switch (rest.size()) {
            case 1 -> Completions.matching(players.onlineNames(), rest.get(0));
            case 2 -> Completions.matching(library.names(), rest.get(1));
            default -> List.of();
        };
    }

    private void play(CommandActor invoker, Target suspect, AnimationScript script) {
        String harmless = "say " + suspect.name() + " finished the " + script.name() + " animation preview";
        AnimationLaunch launch = director.start(suspect.id(), script, harmless);
        if (launch == AnimationLaunch.STARTED) {
            TemplateValues values = TemplateValues.of("{ANIMATION}", script.name()).and("{PLAYER}", suspect.name());
            invoker.send(messages.prefixed("animation-test-started", values));
            invoker.send(messages.text("animation-test-safe"));
        } else {
            invoker.send(messages.prefixed("animation-test-failed"));
        }
    }
}
