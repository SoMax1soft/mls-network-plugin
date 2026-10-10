// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import java.util.Locale;
import java.util.Set;

public record CommandHead(String word) {

    private static final Set<String> DESTRUCTIVE = Set.of(
            "kick", "ban", "tempban", "banip", "ban-ip", "ipban", "mute", "tempmute", "jail", "punish");
    private static final Set<String> BAN_FAMILY = Set.of("ban", "tempban", "banip", "ban-ip", "ipban");
    private static final Set<String> MUTE_FAMILY = Set.of("mute", "tempmute");

    public static CommandHead of(String body) {
        String text = body.trim().toLowerCase(Locale.ROOT);
        int space = text.indexOf(' ');
        String first = space >= 0 ? text.substring(0, space) : text;
        if (first.startsWith("/")) {
            first = first.substring(1);
        }
        int colon = first.indexOf(':');
        return new CommandHead(colon >= 0 ? first.substring(colon + 1) : first);
    }

    public boolean destructive() {
        return DESTRUCTIVE.contains(word);
    }

    public String reportedAction(String fallbackType) {
        if (BAN_FAMILY.contains(word)) {
            return "ban";
        }
        if (word.equals("kick")) {
            return "kick";
        }
        if (MUTE_FAMILY.contains(word)) {
            return "mute";
        }
        if (word.equals("jail")) {
            return "jail";
        }
        return fallbackType;
    }
}
