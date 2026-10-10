// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import italy.mlsac.net.punishment.AnimationStage;
import java.util.UUID;

public final class LibraryStage implements AnimationStage {

    private final AnimationLibrary library;
    private final AnimationDirector director;

    public LibraryStage(AnimationLibrary library, AnimationDirector director) {
        this.library = library;
        this.director = director;
    }

    @Override
    public boolean knows(String animation) {
        return library.find(animation).isPresent();
    }

    @Override
    public void play(UUID player, String animation, String command) {
        library.find(animation).ifPresent(script -> director.start(player, script, command));
    }
}
