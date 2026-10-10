// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonReader {
    private final String text;
    private int position;

    private JsonReader(String text) {
        this.text = text;
    }

    public static Object parse(String text) {
        JsonReader reader = new JsonReader(text);
        reader.skipSpace();
        Object value = reader.value();
        reader.skipSpace();
        if (reader.position != text.length()) {
            throw new JsonSyntaxException("trailing characters", reader.position);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object value = parse(text);
        if (!(value instanceof Map)) {
            throw new JsonSyntaxException("not an object", 0);
        }
        return (Map<String, Object>) value;
    }

    private Object value() {
        if (position >= text.length()) {
            throw new JsonSyntaxException("unexpected end", position);
        }
        char next = text.charAt(position);
        return switch (next) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> members = new LinkedHashMap<>();
        position++;
        skipSpace();
        if (peek() == '}') {
            position++;
            return members;
        }
        while (true) {
            skipSpace();
            String key = string();
            skipSpace();
            expect(':');
            skipSpace();
            members.put(key, value());
            skipSpace();
            char separator = text.charAt(position++);
            if (separator == '}') {
                return members;
            }
            if (separator != ',') {
                throw new JsonSyntaxException("expected , or }", position - 1);
            }
        }
    }

    private List<Object> array() {
        List<Object> items = new ArrayList<>();
        position++;
        skipSpace();
        if (peek() == ']') {
            position++;
            return items;
        }
        while (true) {
            skipSpace();
            items.add(value());
            skipSpace();
            char separator = text.charAt(position++);
            if (separator == ']') {
                return items;
            }
            if (separator != ',') {
                throw new JsonSyntaxException("expected , or ]", position - 1);
            }
        }
    }

    private String string() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            char character = text.charAt(position++);
            if (character == '"') {
                return out.toString();
            }
            if (character != '\\') {
                out.append(character);
                continue;
            }
            char escaped = text.charAt(position++);
            switch (escaped) {
                case '"', '\\', '/' -> out.append(escaped);
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    out.append((char) Integer.parseInt(text.substring(position, position + 4), 16));
                    position += 4;
                }
                default -> throw new JsonSyntaxException("bad escape", position - 1);
            }
        }
    }

    private JsonNumber number() {
        int begin = position;
        while (position < text.length() && "+-0123456789.eE".indexOf(text.charAt(position)) >= 0) {
            position++;
        }
        if (begin == position) {
            throw new JsonSyntaxException("unexpected character", position);
        }
        return new JsonNumber(text.substring(begin, position));
    }

    private Object literal(String word, Object value) {
        if (!text.startsWith(word, position)) {
            throw new JsonSyntaxException("expected " + word, position);
        }
        position += word.length();
        return value;
    }

    private void expect(char character) {
        if (position >= text.length() || text.charAt(position) != character) {
            throw new JsonSyntaxException("expected " + character, position);
        }
        position++;
    }

    private char peek() {
        return position < text.length() ? text.charAt(position) : '\0';
    }

    private void skipSpace() {
        while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
            position++;
        }
    }
}
