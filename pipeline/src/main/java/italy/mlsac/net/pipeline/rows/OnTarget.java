// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

public record OnTarget(int ticks, double millis) {
    public static final OnTarget NONE = new OnTarget(0, 0.0);
}
