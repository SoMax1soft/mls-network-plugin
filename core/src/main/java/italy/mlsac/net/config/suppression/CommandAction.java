// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

public record CommandAction(int stage, CommandPreset preset, String command, boolean animation)
        implements SuppressionAction {

    public static final String TYPE = "command";

    public String ladderText() {
        return animation ? "{ANIMATION} " + command : command;
    }

    @Override
    public String kindKey() {
        return TYPE;
    }
}
