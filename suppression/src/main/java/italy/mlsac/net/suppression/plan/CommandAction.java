// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

public record CommandAction(int stage, String command, boolean animation) implements PlannedAction {

    private static final String ANIMATION_PREFIX = "{ANIMATION} ";

    public String ladderText() {
        return animation ? ANIMATION_PREFIX + command : command;
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
