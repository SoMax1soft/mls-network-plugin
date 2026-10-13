// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandShape {

    public static final int MAX_LENGTH = 256;
    private static final Pattern SELECTOR = Pattern.compile("@[aeprsn](?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE);

    private CommandShape() {
    }

    public static Optional<GuardRefusal> refusal(String command) {
        if (command.chars().anyMatch(c -> controlCharacter((char) c))) {
            return Optional.of(GuardRefusal.of(GuardReason.CONTROL_CHARACTER));
        }
        String text = command.trim();
        if (text.length() > MAX_LENGTH) {
            return Optional.of(GuardRefusal.of(GuardReason.TOO_LONG));
        }
        if (text.indexOf(';') >= 0 || text.indexOf('|') >= 0 || text.contains("&&")) {
            return Optional.of(GuardRefusal.of(GuardReason.CHAINING));
        }
        Matcher selector = SELECTOR.matcher(text);
        if (selector.find()) {
            return Optional.of(new GuardRefusal(GuardReason.SELECTOR, selector.group()));
        }
        String withoutSlash = text.startsWith("/") ? text.substring(1) : text;
        if (withoutSlash.startsWith("/")) {
            return Optional.of(GuardRefusal.of(GuardReason.WORLDEDIT));
        }
        return Optional.empty();
    }

    static boolean controlCharacter(char c) {
        int type = Character.getType(c);
        return type == Character.CONTROL || type == Character.LINE_SEPARATOR
                || type == Character.PARAGRAPH_SEPARATOR;
    }
}
