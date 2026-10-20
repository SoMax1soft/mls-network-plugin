// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import italy.mlsac.net.json.JsonArray;
import italy.mlsac.net.json.JsonNumber;
import italy.mlsac.net.json.JsonString;
import italy.mlsac.net.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

public final class WireNumbers {

    private static final JsonString MISSING = new JsonString("NaN");
    private static final double WHOLE_LIMIT = 0x1.0p53;

    private WireNumbers() {
    }

    public static JsonArray flat(double[][] rows) {
        List<JsonValue> values = new ArrayList<>(rows.length * (rows.length == 0 ? 0 : rows[0].length));
        for (double[] row : rows) {
            for (double value : row) {
                values.add(of(value));
            }
        }
        return new JsonArray(values);
    }

    public static JsonValue of(double value) {
        if (!Double.isFinite(value)) {
            return MISSING;
        }
        if (value == Math.rint(value) && Math.abs(value) < WHOLE_LIMIT) {
            return JsonNumber.of((long) value);
        }
        return JsonNumber.of(value);
    }
}
