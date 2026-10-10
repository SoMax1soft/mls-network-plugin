// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import italy.mlsac.net.config.SettingsLayer;
import java.util.Optional;

final class CommandActionParser implements SuppressionVerbReader {

    private static final int MAX_COMMAND = 256;

    @Override
    public String type() {
        return CommandAction.TYPE;
    }

    @Override
    public boolean allowedInBufferRules() {
        return false;
    }

    @Override
    public Optional<SuppressionAction> parse(SettingsLayer action, int stage) {
        Optional<String> command = action.text("command", MAX_COMMAND).filter(text -> !text.isEmpty());
        if (command.isEmpty()) {
            action.drop("command", "missing or empty command");
            return Optional.empty();
        }
        CommandPreset preset = CommandPreset.fromIdOrCustom(action.text("preset", 16).orElse(""));
        boolean animation = action.bool("animation").orElse(false);
        return Optional.of(new CommandAction(stage, preset, command.get(), animation));
    }
}
