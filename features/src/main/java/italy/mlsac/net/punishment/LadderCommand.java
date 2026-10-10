// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import java.util.List;
import java.util.UUID;

public sealed interface LadderCommand permits LadderCommand.Animated, LadderCommand.StaffNotice, LadderCommand.Console {

    String ANIMATION_PREFIX = "{ANIMATION}";
    List<String> ANIMATION_PREFIXES = List.of(ANIMATION_PREFIX, "{BAN}", "{KICK}");
    String STAFF_NOTICE_PREFIX = "{CUSTOM_ALERT}";

    static LadderCommand parse(String configured) {
        String trimmed = configured.trim();
        for (String prefix : ANIMATION_PREFIXES) {
            if (trimmed.startsWith(prefix)) {
                return new Animated(configured, trimmed.substring(prefix.length()).trim());
            }
        }
        if (trimmed.startsWith(STAFF_NOTICE_PREFIX)) {
            return new StaffNotice(configured, trimmed.substring(STAFF_NOTICE_PREFIX.length()).trim());
        }
        return new Console(configured, trimmed);
    }

    String configured();

    String body();

    boolean consoleCommand();

    boolean punishing();

    String typeName();

    void dispatch(PenaltyDispatch dispatch, UUID player, String resolved);

    default String reportedAction() {
        return CommandHead.of(body()).reportedAction(typeName());
    }

    record Animated(String configured, String body) implements LadderCommand {

        @Override
        public boolean consoleCommand() {
            return true;
        }

        @Override
        public boolean punishing() {
            return true;
        }

        @Override
        public String typeName() {
            return "animation";
        }

        @Override
        public void dispatch(PenaltyDispatch dispatch, UUID player, String resolved) {
            dispatch.animated(player, resolved);
        }
    }

    record StaffNotice(String configured, String body) implements LadderCommand {

        @Override
        public boolean consoleCommand() {
            return false;
        }

        @Override
        public boolean punishing() {
            return false;
        }

        @Override
        public String typeName() {
            return "custom_alert";
        }

        @Override
        public void dispatch(PenaltyDispatch dispatch, UUID player, String resolved) {
            dispatch.staffNotice(resolved);
        }
    }

    record Console(String configured, String body) implements LadderCommand {

        @Override
        public boolean consoleCommand() {
            return true;
        }

        @Override
        public boolean punishing() {
            return CommandHead.of(body).destructive();
        }

        @Override
        public String typeName() {
            return "raw";
        }

        @Override
        public void dispatch(PenaltyDispatch dispatch, UUID player, String resolved) {
            dispatch.console(resolved);
        }
    }
}
