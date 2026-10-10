// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.json;

public record JsonNumber(String text) {

    public double asDouble() {
        return Double.parseDouble(text);
    }

    public float asFloat() {
        return Float.parseFloat(text);
    }

    public long asLong() {
        return Long.parseLong(text);
    }

    public int asInt() {
        return Integer.parseInt(text);
    }
}
