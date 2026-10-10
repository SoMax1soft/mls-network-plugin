// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import italy.mlsac.net.json.JsonArray;
import italy.mlsac.net.json.JsonNumber;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonParser;
import italy.mlsac.net.json.JsonString;
import italy.mlsac.net.json.JsonValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assumptions;

final class WireVectors {

    private WireVectors() {
    }

    static JsonObject load() throws Exception {
        String location = System.getProperty("mlsac.wire5.vectors", "");
        Assumptions.assumeTrue(!location.isEmpty() && Files.isRegularFile(Path.of(location)),
                "wire v5 vectors not present");
        return JsonParser.parseObject(Files.readString(Path.of(location), StandardCharsets.UTF_8));
    }

    static List<JsonObject> sets(JsonObject vectors) {
        return vectors.array("sets").orElseThrow().elements().stream().map(JsonObject.class::cast).toList();
    }

    static double number(JsonValue value) {
        if (value instanceof JsonNumber number) {
            return number.doubleValue();
        }
        if (value instanceof JsonString text && !"NaN".equals(text.value())) {
            throw new IllegalArgumentException("unexpected text in a number cell: " + text.value());
        }
        return Double.NaN;
    }

    static double[] row(JsonValue value) {
        List<JsonValue> cells = ((JsonArray) value).elements();
        double[] row = new double[cells.size()];
        for (int index = 0; index < row.length; index++) {
            row[index] = number(cells.get(index));
        }
        return row;
    }

    static double[][] rows(JsonValue value) {
        List<JsonValue> lines = ((JsonArray) value).elements();
        double[][] rows = new double[lines.size()][];
        for (int index = 0; index < rows.length; index++) {
            rows[index] = row(lines.get(index));
        }
        return rows;
    }

    static boolean same(double expected, double actual) {
        return Double.isNaN(expected) ? Double.isNaN(actual)
                : Double.doubleToLongBits(expected) == Double.doubleToLongBits(actual);
    }
}
