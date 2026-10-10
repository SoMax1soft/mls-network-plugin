// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

import java.util.Objects;

public record JsonNumber(String literal) implements JsonValue {

    public JsonNumber {
        Objects.requireNonNull(literal, "literal");
        if (literal.isEmpty()) {
            throw new IllegalArgumentException("empty number literal");
        }
    }

    public static JsonNumber of(long value) {
        return new JsonNumber(Long.toString(value));
    }

    public static JsonNumber of(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("non-finite double cannot be written as JSON: " + value);
        }
        return new JsonNumber(Double.toString(value));
    }

    public double doubleValue() {
        return Double.parseDouble(literal);
    }

    public boolean isIntegerLiteral() {
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if (c == '.' || c == 'e' || c == 'E') {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object toPlain() {
        if (isIntegerLiteral()) {
            try {
                return Long.parseLong(literal);
            } catch (NumberFormatException tooLarge) {
                return doubleValue();
            }
        }
        return doubleValue();
    }
}
