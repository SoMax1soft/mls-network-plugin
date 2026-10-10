// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

public enum JsonEscaping {

    STANDARD(false),

    HTML_SAFE(true);

    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final char LINE_SEPARATOR = (char) 0x2028;
    private static final char PARAGRAPH_SEPARATOR = (char) 0x2029;

    private final boolean escapeMarkup;

    JsonEscaping(boolean escapeMarkup) {
        this.escapeMarkup = escapeMarkup;
    }

    void appendQuoted(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            appendChar(out, value.charAt(i));
        }
        out.append('"');
    }

    private void appendChar(StringBuilder out, char c) {
        switch (c) {
            case '"' -> out.append("\\\"");
            case '\\' -> out.append("\\\\");
            case '\t' -> out.append("\\t");
            case '\b' -> out.append("\\b");
            case '\n' -> out.append("\\n");
            case '\r' -> out.append("\\r");
            case '\f' -> out.append("\\f");
            case '<', '>', '&', '=', '\'' -> appendMarkup(out, c);
            default -> appendOther(out, c);
        }
    }

    private void appendMarkup(StringBuilder out, char c) {
        if (escapeMarkup) {
            appendUnicode(out, c);
        } else {
            out.append(c);
        }
    }

    private static void appendOther(StringBuilder out, char c) {
        if (c < 0x20 || c == LINE_SEPARATOR || c == PARAGRAPH_SEPARATOR) {
            appendUnicode(out, c);
        } else {
            out.append(c);
        }
    }

    private static void appendUnicode(StringBuilder out, char c) {
        out.append('\\').append('u')
                .append(HEX[(c >> 12) & 0xF])
                .append(HEX[(c >> 8) & 0xF])
                .append(HEX[(c >> 4) & 0xF])
                .append(HEX[c & 0xF]);
    }
}
