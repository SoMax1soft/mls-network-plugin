// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public sealed interface LadderCommand permits LadderCommand.Animated, LadderCommand.StaffNotice, LadderCommand.Console {

    String ANIMATION_PREFIX = "{ANIMATION}";
    List<String> ANIMATION_PREFIXES = List.of(ANIMATION_PREFIX, "{BAN}", "{KICK}");
    String STAFF_NOTICE_PREFIX = "{CUSTOM_ALERT}";
    Pattern CHOSEN_ANIMATION = Pattern.compile("^\\{ANIMATION:([A-Za-z0-9_-]{1,48})}");

    static LadderCommand parse(String configured) {
        String trimmed = configured.trim();
        Matcher chosen = CHOSEN_ANIMATION.matcher(trimmed);
        if (chosen.find()) {
            return new Animated(configured, trimmed.substring(chosen.end()).trim(),
                    Optional.of(chosen.group(1).toLowerCase(Locale.ROOT)));
        }
        for (String prefix : ANIMATION_PREFIXES) {
            if (trimmed.startsWith(prefix)) {
                return new Animated(configured, trimmed.substring(prefix.length()).trim(), Optional.empty());
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

    record Animated(String configured, String body, Optional<String> animationId) implements LadderCommand {

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
            dispatch.animated(player, resolved, animationId);
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
