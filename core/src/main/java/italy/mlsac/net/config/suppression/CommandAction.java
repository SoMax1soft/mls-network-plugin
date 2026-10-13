// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import java.util.Optional;

public record CommandAction(int stage, CommandPreset preset, String command, boolean animation,
                            Optional<String> animationId) implements SuppressionAction {

    public static final String TYPE = "command";

    public CommandAction(int stage, CommandPreset preset, String command, boolean animation) {
        this(stage, preset, command, animation, Optional.empty());
    }

    public String ladderText() {
        if (!animation) {
            return command;
        }
        return animationId.map(id -> "{ANIMATION:" + id + "} ").orElse("{ANIMATION} ") + command;
    }

    @Override
    public String kindKey() {
        return TYPE;
    }
}
