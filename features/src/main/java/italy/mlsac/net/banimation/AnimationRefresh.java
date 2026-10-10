// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;
import java.util.function.Supplier;

final class AnimationRefresh implements AnimationVerb {

    private final AnimationLibrary library;
    private final Supplier<List<AnimationFile>> files;
    private final MessageBook messages;
    private final Logbook log;

    AnimationRefresh(AnimationLibrary library, Supplier<List<AnimationFile>> files, MessageBook messages,
                     Logbook log) {
        this.library = library;
        this.files = files;
        this.messages = messages;
        this.log = log;
    }

    @Override
    public void run(CommandActor invoker, List<String> rest) {
        String reply;
        try {
            reply = messages.prefixed("animations-reloaded", TemplateValues.of("{COUNT}", library.load(files.get())));
        } catch (RuntimeException failure) {
            log.severe("[Animation] Reloading ban animations failed", failure);
            reply = messages.prefixed("animations-reload-failed");
        }
        invoker.send(reply);
    }
}
