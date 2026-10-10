// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class JsonWriterTest {

    private final JsonWriter compact = JsonWriter.compact();

    @ParameterizedTest
    @CsvSource({"0.0001, 1.0E-4", "1e7, 1.0E7", "25.0, 25.0", "45.099999999999994, 45.099999999999994",
        "0.951, 0.951", "3.2e-5, 3.2E-5"})
    void doublesUseJavaShortestForm(double value, String text) {
        assertEquals("{\"x\":" + text + "}", compact.write(JsonObject.builder().put("x", value).build()));
    }

    @ParameterizedTest
    @CsvSource({"25565, 25565", "2, 2", "-7, -7"})
    void integersArePlainDecimal(long value, String text) {
        assertEquals("{\"n\":" + text + "}", compact.write(JsonObject.builder().put("n", value).build()));
    }

    @Test
    void stringsKeepMarkupAndUnicodeRaw() {
        String command = "&cбан\t{PLAYER} <x>='y'";
        String written = compact.write(JsonObject.builder().put("command", command).build());
        assertEquals("{\"command\":\"&cбан\\t{PLAYER} <x>='y'\"}", written);
        byte[] utf8 = compact.writeUtf8(JsonObject.builder().put("c", "б").build());
        assertEquals("{\"c\":\"б\"}", new String(utf8, StandardCharsets.UTF_8));
    }

    @Test
    void controlCharactersAndSeparatorsAreEscaped() {
        String value = "q\"b\\n\n r\r f\f b\b u" + (char) 1 + " del" + (char) 0x7F + " ls" + (char) 0x2028 + " ps"
                + (char) 0x2029;
        String expected = "\"q\\\"b\\\\n\\n r\\r f\\f b\\b u\\u0001 del" + (char) 0x7F + " ls\\u2028 ps\\u2029\"";
        assertEquals("{\"v\":" + expected + "}", compact.write(JsonObject.builder().put("v", value).build()));
    }

    @Test
    void htmlSafeEscapingForSessionFiles() {
        JsonWriter pretty = new JsonWriter(JsonLayout.PRETTY, JsonEscaping.HTML_SAFE);
        JsonObject object = JsonObject.builder()
                .put("comment", "a<b>&c='d'")
                .put("list", new JsonArray(List.of(JsonNumber.of(1L), JsonNumber.of(2L))))
                .put("empty", new JsonArray(List.of()))
                .build();
        String expected = "{\n  \"comment\": \"a\\u003cb\\u003e\\u0026c\\u003d\\u0027d\\u0027\",\n  \"list\": [\n    1,\n"
                + "    2\n  ],\n  \"empty\": []\n}";
        assertEquals(expected, pretty.write(object));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteDoublesAreRejected(double value) {
        assertThrows(IllegalArgumentException.class, () -> JsonNumber.of(value));
    }

    @Test
    void parsedNumbersAreReEmittedVerbatim() throws JsonSyntaxException {
        String text = "{\"probability\":0.951,\"buffer\":45.099999999999994,\"n\":7}";
        assertEquals(text, compact.write(JsonParser.parse(text)));
    }

    @Test
    void parserRejectsTrailingContentAndBadEscapes() {
        assertThrows(JsonSyntaxException.class, () -> JsonParser.parse("{} x"));
        assertThrows(JsonSyntaxException.class, () -> JsonParser.parse("{\"a\":\"\\q\"}"));
        assertThrows(JsonSyntaxException.class, () -> JsonParser.parse("{\"a\":01}x"));
    }

    @Test
    void parserReadsUnicodeEscapes() throws JsonSyntaxException {
        JsonObject object = JsonParser.parseObject("{\"a\":\"\\u0431\\n\"}");
        assertEquals("б\n", object.string("a").orElseThrow());
    }
}
