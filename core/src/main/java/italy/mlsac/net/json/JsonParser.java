// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonParser {

    private static final int MAX_DEPTH = 64;
    private static final char END = (char) 0;

    private final String text;
    private int pos;

    private JsonParser(String text) {
        this.text = text;
    }

    public static JsonValue parse(String text) throws JsonSyntaxException {
        JsonParser parser = new JsonParser(text);
        parser.skipWhitespace();
        JsonValue value = parser.readValue(0);
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw new JsonSyntaxException("trailing content", parser.pos);
        }
        return value;
    }

    public static JsonObject parseObject(String text) throws JsonSyntaxException {
        JsonValue value = parse(text);
        if (value instanceof JsonObject object) {
            return object;
        }
        throw new JsonSyntaxException("expected an object", 0);
    }

    private JsonValue readValue(int depth) throws JsonSyntaxException {
        if (depth > MAX_DEPTH) {
            throw new JsonSyntaxException("nesting too deep", pos);
        }
        if (pos >= text.length()) {
            throw new JsonSyntaxException("unexpected end", pos);
        }
        char c = text.charAt(pos);
        return switch (c) {
            case '{' -> readObject(depth);
            case '[' -> readArray(depth);
            case '"' -> new JsonString(readString());
            case 't' -> literal("true", JsonBoolean.TRUE);
            case 'f' -> literal("false", JsonBoolean.FALSE);
            case 'n' -> literal("null", JsonNull.INSTANCE);
            default -> readNumber();
        };
    }

    private JsonValue literal(String word, JsonValue value) throws JsonSyntaxException {
        if (!text.startsWith(word, pos)) {
            throw new JsonSyntaxException("invalid literal", pos);
        }
        pos += word.length();
        return value;
    }

    private JsonObject readObject(int depth) throws JsonSyntaxException {
        pos++;
        Map<String, JsonValue> members = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return new JsonObject(members);
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw new JsonSyntaxException("expected member name", pos);
            }
            String name = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            members.put(name, readValue(depth + 1));
            skipWhitespace();
            char next = peek();
            pos++;
            if (next == '}') {
                return new JsonObject(members);
            }
            if (next != ',') {
                throw new JsonSyntaxException("expected ',' or '}'", pos - 1);
            }
        }
    }

    private JsonArray readArray(int depth) throws JsonSyntaxException {
        pos++;
        List<JsonValue> elements = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return new JsonArray(elements);
        }
        while (true) {
            skipWhitespace();
            elements.add(readValue(depth + 1));
            skipWhitespace();
            char next = peek();
            pos++;
            if (next == ']') {
                return new JsonArray(elements);
            }
            if (next != ',') {
                throw new JsonSyntaxException("expected ',' or ']'", pos - 1);
            }
        }
    }

    private String readString() throws JsonSyntaxException {
        pos++;
        StringBuilder out = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw new JsonSyntaxException("unterminated string", pos);
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                return out.toString();
            }
            if (c == '\\') {
                out.append(readEscape());
            } else if (c < 0x20) {
                throw new JsonSyntaxException("control character in string", pos - 1);
            } else {
                out.append(c);
            }
        }
    }

    private char readEscape() throws JsonSyntaxException {
        if (pos >= text.length()) {
            throw new JsonSyntaxException("unterminated escape", pos);
        }
        char c = text.charAt(pos++);
        return switch (c) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> readUnicodeEscape();
            default -> throw new JsonSyntaxException("invalid escape", pos - 1);
        };
    }

    private char readUnicodeEscape() throws JsonSyntaxException {
        if (pos + 4 > text.length()) {
            throw new JsonSyntaxException("short unicode escape", pos);
        }
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int digit = Character.digit(text.charAt(pos++), 16);
            if (digit < 0) {
                throw new JsonSyntaxException("invalid unicode escape", pos - 1);
            }
            value = (value << 4) | digit;
        }
        return (char) value;
    }

    private JsonNumber readNumber() throws JsonSyntaxException {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        if (peek() == '0') {
            pos++;
        } else if (isDigit(peek())) {
            skipDigits();
        } else {
            throw new JsonSyntaxException("invalid value", start);
        }
        if (peek() == '.') {
            pos++;
            requireDigit();
            skipDigits();
        }
        if (peek() == 'e' || peek() == 'E') {
            pos++;
            if (peek() == '+' || peek() == '-') {
                pos++;
            }
            requireDigit();
            skipDigits();
        }
        return new JsonNumber(text.substring(start, pos));
    }

    private void requireDigit() throws JsonSyntaxException {
        if (!isDigit(peek())) {
            throw new JsonSyntaxException("digit expected", pos);
        }
    }

    private void skipDigits() {
        while (isDigit(peek())) {
            pos++;
        }
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private char peek() {
        return pos < text.length() ? text.charAt(pos) : END;
    }

    private void expect(char c) throws JsonSyntaxException {
        if (peek() != c) {
            throw new JsonSyntaxException("expected '" + c + "'", pos);
        }
        pos++;
    }

    private void skipWhitespace() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                return;
            }
        }
    }
}
