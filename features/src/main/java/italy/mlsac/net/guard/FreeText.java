// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class FreeText {

    public static final int MAX_LENGTH = 100;
    private static final Pattern COLOUR_CODE = Pattern.compile("[&§](#[0-9A-Fa-f]{6}|[0-9A-Fa-fK-Ok-oRrXx])");
    private static final Pattern FORBIDDEN = Pattern.compile("[§&;|@\\\\{}]");
    private static final Pattern BLANKS = Pattern.compile("\\s+");

    private FreeText() {
    }

    public static String argumentTail(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder printable = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            printable.append(CommandShape.controlCharacter(c) ? ' ' : c);
        }
        String plain = FORBIDDEN.matcher(COLOUR_CODE.matcher(printable).replaceAll("")).replaceAll("");
        List<String> words = new ArrayList<>();
        for (String word : BLANKS.split(plain.trim())) {
            String unflagged = word.replaceFirst("^-+", "").replaceFirst("^/+", "");
            if (!unflagged.isEmpty()) {
                words.add(unflagged);
            }
        }
        String joined = String.join(" ", words);
        return joined.length() > MAX_LENGTH ? joined.substring(0, MAX_LENGTH).trim() : joined;
    }
}
