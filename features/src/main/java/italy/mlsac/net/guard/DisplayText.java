// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

public final class DisplayText {

    static final int SHORT = 32;
    static final int COMMAND = 96;
    private static final String CUT = "...";

    private DisplayText() {
    }

    public static String shortened(String text) {
        return cleaned(text, SHORT);
    }

    public static String command(String text) {
        return cleaned(text, COMMAND);
    }

    private static String cleaned(String text, int limit) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' || c == '§') {
                continue;
            }
            out.append(CommandShape.controlCharacter(c) ? '?' : c);
        }
        return out.length() > limit ? out.substring(0, limit) + CUT : out.toString();
    }
}
