// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTextTest {
    private static final String COMMENT = "say \"hi\" <b>=1</b> it's\nПривет";

    @Test
    void prettyDocumentEscapesHtmlSafeAndHasNoFinalNewline() {
        String text = new PrettyJsonDocument().text("comment", COMMENT).integer("count", 3)
                .textList("cols", List.of("a", "b")).textList("none", List.of()).bool("flag", false).render();
        assertEquals("{\n  \"comment\": \"say \\\"hi\\\" \\u003cb\\u003e\\u003d1\\u003c/b\\u003e it\\u0027s\\nПривет\","
                + "\n  \"count\": 3,\n  \"cols\": [\n    \"a\",\n    \"b\"\n  ],\n  \"none\": [],\n  \"flag\": false\n}",
                text);
    }

    @Test
    void compactObjectKeepsMarkupAndFieldOrder() {
        String text = new CompactJsonObject().text("a", "<=>'& ").integer("b", 2).bool("c", true).render();
        assertEquals("{\"a\":\"<=>'&\\u2028\",\"b\":2,\"c\":true}", text);
    }

    @Test
    void readerRoundTripsNumbersAsText() {
        Map<String, Object> object = JsonReader.parseObject("{\"x\":-0.4826164954927235,\"y\":[1,\"NaN\",null]}");
        assertEquals("-0.4826164954927235", ((JsonNumber) object.get("x")).text());
        assertEquals(3, ((List<?>) object.get("y")).size());
    }
}
