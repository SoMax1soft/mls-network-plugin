// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.json;

public enum JsonEscaping {
    COMPACT {
        @Override
        boolean escapesMarkup(char character) {
            return false;
        }
    },
    HTML_SAFE {
        @Override
        boolean escapesMarkup(char character) {
            return character == '<' || character == '>' || character == '&' || character == '=' || character == '\'';
        }
    };

    private static final char LINE_SEPARATOR = (char) 0x2028;
    private static final char PARAGRAPH_SEPARATOR = (char) 0x2029;

    abstract boolean escapesMarkup(char character);

    public String quote(String text) {
        StringBuilder out = new StringBuilder(text.length() + 2);
        out.append('"');
        for (int index = 0; index < text.length(); index++) {
            appendEscaped(out, text.charAt(index));
        }
        return out.append('"').toString();
    }

    private void appendEscaped(StringBuilder out, char character) {
        switch (character) {
            case '"' -> out.append("\\\"");
            case '\\' -> out.append("\\\\");
            case '\n' -> out.append("\\n");
            case '\t' -> out.append("\\t");
            case '\b' -> out.append("\\b");
            case '\f' -> out.append("\\f");
            case '\r' -> out.append("\\r");
            default -> appendPlain(out, character);
        }
    }

    private void appendPlain(StringBuilder out, char character) {
        if (character < 0x20 || character == LINE_SEPARATOR || character == PARAGRAPH_SEPARATOR
                || escapesMarkup(character)) {
            out.append(String.format("\\u%04x", (int) character));
        } else {
            out.append(character);
        }
    }
}
