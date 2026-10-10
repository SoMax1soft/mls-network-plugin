// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

public record WindowMatrix(float[][] features, float[][] flags) {

    public int length() {
        return features.length;
    }

    public WindowMatrix narrowedTo(WireVersion version) {
        float[][] narrowFeatures = new float[features.length][];
        float[][] narrowFlags = new float[flags.length][];
        for (int row = 0; row < features.length; row++) {
            narrowFeatures[row] = prefix(features[row], version.featureCount());
            narrowFlags[row] = prefix(flags[row], version.flagCount());
        }
        return new WindowMatrix(narrowFeatures, narrowFlags);
    }

    private static float[] prefix(float[] values, int count) {
        if (values.length < count) {
            throw new IllegalArgumentException("row holds " + values.length + " values, " + count + " needed");
        }
        float[] prefix = new float[count];
        System.arraycopy(values, 0, prefix, 0, count);
        return prefix;
    }
}
