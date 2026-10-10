// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.violation;

public record ViolationSettings(double threshold, double resetValue, double multiplier, BufferDecay decay,
                                ViolationDecay vlDecay) {

    public static ViolationSettings pluginDefaults() {
        return new ViolationSettings(40.0, 20.0, 45.0, new BufferDecay(0.50, 1.5), new ViolationDecay(false, 1500, 1));
    }

    public double suppressionCapacity() {
        return Math.max(30.0, threshold);
    }
}
