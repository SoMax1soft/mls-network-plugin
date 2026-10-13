// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

import java.util.Optional;

public record CommandAction(int stage, String command, boolean animation, Optional<String> animationId)
        implements PlannedAction {

    private static final String ANIMATION_PREFIX = "{ANIMATION} ";

    public CommandAction(int stage, String command, boolean animation) {
        this(stage, command, animation, Optional.empty());
    }

    public String ladderText() {
        if (!animation) {
            return command;
        }
        return animationId.map(id -> "{ANIMATION:" + id + "} ").orElse(ANIMATION_PREFIX) + command;
    }

    public String token() {
        String trimmed = command.trim();
        int space = trimmed.indexOf(' ');
        return "cmd " + (space < 0 ? trimmed : trimmed.substring(0, space));
    }

    @Override
    public void placeInto(StageBuilder builder) {
        builder.command(this);
    }
}
