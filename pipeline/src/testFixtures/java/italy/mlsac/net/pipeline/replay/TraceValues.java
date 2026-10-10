// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import italy.mlsac.net.pipeline.json.JsonNumber;

final class TraceValues {
    private TraceValues() {
    }

    static double real(Object value) {
        if (value instanceof JsonNumber number) {
            return number.asDouble();
        }
        return Double.parseDouble((String) value);
    }

    static float single(Object value) {
        if (value instanceof JsonNumber number) {
            return number.asFloat();
        }
        return Float.parseFloat((String) value);
    }

    static Float optionalSingle(Object value) {
        return value == null ? null : single(value);
    }

    static Double optionalReal(Object value) {
        return value == null ? null : real(value);
    }
}
