// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

final class AnimationCatalogue implements AnimationVerb {

    private final AnimationLibrary library;
    private final MessageBook messages;

    AnimationCatalogue(AnimationLibrary library, MessageBook messages) {
        this.library = library;
        this.messages = messages;
    }

    @Override
    public void run(CommandActor invoker, List<String> rest) {
        String standard = AnimationLibrary.DEFAULT_ANIMATION;
        Stream.of(heading(standard), entries(standard), closing())
                .flatMap(Function.identity())
                .forEach(invoker::send);
    }

    private Stream<String> heading(String standard) {
        return Stream.of(messages.text("animation-list-title"),
                messages.text("animation-list-count", TemplateValues.of("{COUNT}", library.size())),
                messages.text("animation-list-default", TemplateValues.of("{ANIMATION}", standard)),
                messages.text("animation-list-rule"));
    }

    private Stream<String> entries(String standard) {
        return library.names().stream().map(name -> messages.text(
                name.equals(standard) ? "animation-list-entry-default" : "animation-list-entry",
                TemplateValues.of("{ANIMATION}", name)));
    }

    private Stream<String> closing() {
        return Stream.of(messages.text("animation-list-rule"), messages.text("animation-list-hint"));
    }
}
